package wdlx.server;

import io.netty.channel.embedded.EmbeddedChannel;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wdlx.WorldDownloadX;
import wdlx.api.Session;
import wdlx.config.Config;
import wdlx.events.ChunkLoadEvent;
import wdlx.events.ChunkUnloadEvent;
import wdlx.events.EntityUnloadEvent;
import wdlx.events.LevelChangeEvent;
import wdlx.util.Notifications;
import wdlx.util.Wait;
import xaeroplus.module.impl.TickTaskExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class WdlxServerSession implements AutoCloseable, Session {
    private static final Logger LOGGER = LoggerFactory.getLogger(WdlxServerSession.class);
    final String name;
    final WdlxMinecraftServer server;
    final Minecraft mc = Minecraft.getInstance();
    final WdlxSessionTracker tracker = new WdlxSessionTracker();

    public WdlxServerSession(String name) {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            throw new IllegalStateException("Can't start WDLX server if client not connected");
        }
        this.name = name;
        // todo: async start
        //  but need to be careful about control state management
        //  i.e. only one wdl server at a time
        this.server = WdlxMinecraftServer.startServer(name, tracker);
        CompletableFuture.runAsync(() -> {
            Wait.waitUntil(server::isReady, 5);
            WorldDownloadX.EVENT_BUS.register(this);
            Notifications.chat("WDL Started");
        }).thenAcceptAsync(v -> {
            flushLoadedChunks();
        }, TickTaskExecutor.INSTANCE);
    }

    @Override
    public void close() {
        LOGGER.info("Stopping WdlServerSession");
        WorldDownloadX.EVENT_BUS.unregister(this);
        try {
            flushMaps();
            flushLoadedEntities();
            flushPlayer();
            flushLoadedChunks();
        } catch (Exception e) {
            LOGGER.error("Failed to close WdlServerSession", e);
            Notifications.chatError("Error while saving world: " + e.getMessage());
        }
        // todo: return the completablefuture
        server.submit(() -> {
            server.saveEverything(false, true, true);
        }).thenAcceptAsync(s -> {
            var worldPath = server.getWorldPath(LevelResource.ROOT);
            server.halt(true);
            cleanupEmptyRegionFiles(worldPath);
            LOGGER.info("Stopped WdlServerSession");
            Notifications.chat("WDL Stopped");
        }, TickTaskExecutor.INSTANCE);
    }

    void cleanupEmptyRegionFiles(Path worldPath) {
        var count = new AtomicInteger();
        try (var paths = Files.walk(worldPath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".mca"))
                .filter(this::isEmptyFile)
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        count.incrementAndGet();
                    } catch (IOException e) {
                        LOGGER.warn("Failed to delete empty region file {}", path, e);
                    }
                });
        } catch (IOException e) {
            LOGGER.warn("Failed to scan for empty region files in {}", worldPath, e);
        }
        if (count.get() > 0) {
            LOGGER.info("Deleted {} empty region files", count.get());
        }
    }

    boolean isEmptyFile(Path path) {
        try {
            return Files.size(path) == 0;
        } catch (IOException e) {
            LOGGER.warn("Failed to inspect region file {}", path, e);
            return false;
        }
    }

    // todo: should we create the player at the start of wdl? or at the closing?
    //  if we do it at the start we could possibly update their state throughout the wdl
    //  which may be necessary to preserve transient state like echest contents
    ServerPlayer createPlayerDupe() {
        var level = server.getLevel(mc.level.dimension());
        CommonListenerCookie commonListenerCookie = CommonListenerCookie.createInitial(mc.getGameProfile(), false);
        ServerPlayer serverPlayer = new ServerPlayer(
            server, level, commonListenerCookie.gameProfile(), commonListenerCookie.clientInformation()
        );
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, serverPlayer, commonListenerCookie);
        serverPlayer.setGameMode(mc.gameMode.getPlayerMode());
        serverPlayer.getInventory().items.clear();
        for (var i = 0; i < mc.player.getInventory().items.size(); i++) {
            serverPlayer.getInventory().items.set(i, mc.player.getInventory().items.get(i).copy());
        }
        for (var i = 0; i < mc.player.getEnderChestInventory().items.size(); i++) {
            serverPlayer.getEnderChestInventory().items.set(i, mc.player.getEnderChestInventory().items.get(i).copy());
        }
        serverPlayer.setPos(mc.player.position());
        serverPlayer.setXRot(mc.player.getXRot());
        serverPlayer.setYRot(mc.player.getYRot());
        return serverPlayer;
    }

    void flushPlayer() {
        if (!Config.get().download.player.enabled) return;
        if (Config.get().debug.logSavedPlayers) {
            LOGGER.info("Saving player: {} ({}) [{}, {}, {}]", mc.player.getGameProfile(), mc.player.getId(), mc.player.getX(), mc.player.getY(), mc.player.getZ());
        }
        server.execute(this::createPlayerDupe);
    }

    // todo: track which maps we encounter during the wdl and only flush those
    void flushMaps() {
        if (!Config.get().download.maps.enabled) return;
        var clientMapData = mc.level.mapData;
        clientMapData.forEach((mapId, mapData) -> {
            if (Config.get().debug.logSavedMaps) {
                LOGGER.info("Saving map: {}", mapId.key());
            }
            server.overworld().getDataStorage().set(mapId.key(), mapData);
        });
    }

    void flushLoadedChunks() {
        var level = mc.level;
        var player = mc.player;
        var serverChunkRadius = mc.getConnection().serverChunkRadius;
        var centerX = player.chunkPosition().x;
        var centerZ = player.chunkPosition().z;
        int count = 0;
        for (int x = centerX - serverChunkRadius; x <= centerX + serverChunkRadius; x++) {
            for (int z = centerZ - serverChunkRadius; z <= centerZ + serverChunkRadius; z++) {
                var chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk != null) {
                    server.execute(() -> server.writeClientChunk(chunk));
                    count++;
                }
            }
        }
        LOGGER.info("Flushed {} chunks", count);
    }

    void flushLoadedEntities() {
        AtomicInteger count = new AtomicInteger();
        mc.level.entitiesForRendering().forEach(entity -> {
            if (entity instanceof Player) return;
            server.execute(() -> server.writeClientEntity(entity));
            count.incrementAndGet();
        });
        LOGGER.info("Flushed {} entities", count.get());
    }

    @EventHandler
    public void handleLevelChange(LevelChangeEvent event) {
        // todo: continue wdl through dimension switches
        LOGGER.info("Closing session due to level change");
        close();
    }

    @EventHandler
    public void handleChunkLoad(ChunkLoadEvent event) {
        server.execute(() -> server.writeClientChunk(event.chunk()));
    }

    @EventHandler
    public void handleChunkUnload(ChunkUnloadEvent event) {
        server.execute(() -> server.writeClientChunk(event.chunk()));
    }

    @EventHandler
    public void handleEntityUnload(EntityUnloadEvent event) {
        if (event.reason().shouldSave()) {
            server.execute(() -> server.writeClientEntity(event.entity()));
        }
    }

    @Override
    public boolean active() {
        return server.isRunning();
    }

    @Override
    public CompletableFuture<LongSet> savedChunks() {
        return CompletableFuture.supplyAsync(tracker::getSavedChunks, server);
    }

    @Override
    public CompletableFuture<LongSet> newlySavedChunks() {
        return CompletableFuture.supplyAsync(tracker::getNewlySavedChunks, server);
    }
}
