package wdlx.server;

import io.netty.channel.embedded.EmbeddedChannel;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wdlx.WorldDownloadX;
import wdlx.api.Session;
import wdlx.config.Config;
import wdlx.events.ChunkUnloadEvent;
import wdlx.events.EntityUnloadEvent;
import wdlx.events.LevelChangeEvent;
import wdlx.util.Notifications;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class WdlxServerSession implements AutoCloseable, Session {
    private static final Logger LOGGER = LoggerFactory.getLogger(WdlxServerSession.class);
    final String name;
    final WdlxMinecraftServer server;
    final Minecraft mc = Minecraft.getInstance();

    public WdlxServerSession(String name) {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            throw new IllegalStateException("Can't start WDLX server if client not connected");
        }
        this.name = name;
        this.server = WdlxMinecraftServer.startServer(name);
        WorldDownloadX.EVENT_BUS.register(this);
        Notifications.chat("WDL Started");
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
        server.executeBlocking(() -> {
            server.saveEverything(false, true, true);
        });
        var worldPath = server.getWorldPath(LevelResource.ROOT);
        server.halt(true);
        cleanupEmptyRegionFiles(worldPath);
        LOGGER.info("Stopped WdlServerSession");
        Notifications.chat("WDL Stopped");
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
        createPlayerDupe();
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
                    server.writeClientChunk(chunk);
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
            server.writeClientEntity(entity);
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
    public void handleChunkUnload(ChunkUnloadEvent event) {
        server.writeClientChunk(event.chunk());
    }

    @EventHandler
    public void handleEntityUnload(EntityUnloadEvent event) {
        if (event.reason().shouldSave()) {
            server.writeClientEntity(event.entity());
        }
    }

    @Override
    public boolean active() {
        return server.isRunning();
    }

    @Override
    public CompletableFuture<LongSet> savedChunks() {
        // todo: highly cacheable if we track which chunks we save afterwards and increment
        return CompletableFuture.supplyAsync(() -> {
            var regionFileRegex = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
            var set = new LongOpenHashSet();
            server.getAllLevels().forEach(level -> {
                try {
                    var regionFilesPath = level.getChunkSource().chunkMap.worker.storage.folder;
                    Files.list(regionFilesPath)
                        .filter(p -> p.endsWith(".mca"))
                        .forEach(p -> {
                            var file = p.toFile();
                            var matcher = regionFileRegex.matcher(file.getName());
                            if (matcher.matches()) {
                                var futures = new ArrayList<CompletableFuture<Void>>();
                                int regionX = Integer.parseInt(matcher.group(1));
                                int regionZ = Integer.parseInt(matcher.group(2));
                                int minChunkX = regionX << 5;
                                int minChunkZ = regionZ << 5;
                                // todo: this can probably be optimized
                                for (int x = minChunkX; x < minChunkX + 32; x++) {
                                    for (int z = minChunkZ; z < minChunkZ + 32; z++) {
                                        var future = level.getChunkSource().getChunkFuture(x, z, ChunkStatus.FULL, false)
                                            .thenAccept(chunkResult -> {
                                                chunkResult.ifSuccess(chunk -> {
                                                    synchronized (set) {
                                                        set.add(ChunkPos.asLong(chunk.getPos().x, chunk.getPos().z));
                                                    }
                                                });
                                            });
                                        futures.add(future);
                                    }
                                }
                                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                            }
                        });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return set;
        }, server);
    }
}
