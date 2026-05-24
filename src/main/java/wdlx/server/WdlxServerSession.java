package wdlx.server;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wdlx.WorldDownloadX;
import wdlx.api.Session;
import wdlx.util.Notifications;
import wdlx.util.Wait;

import java.util.concurrent.CompletableFuture;

public class WdlxServerSession implements AutoCloseable, Session {
    private static final Logger LOGGER = LoggerFactory.getLogger(WdlxServerSession.class);
    final String name;
    final WdlxMinecraftServer server;

    public WdlxServerSession(String name) {
        this.name = name;
        try {
            var levelStorageAccess = Minecraft.getInstance().getLevelSource().createAccess(name);
            this.server = MinecraftServer.spin((thread) -> {
                var s = WdlxMinecraftServer.create(
                    thread,
                    levelStorageAccess,
                    Minecraft.getInstance().getResourcePackRepository()
                );
                LOGGER.info("World download server started");
                return s;
            });
            Wait.waitUntil(server::isReady, 5);
            Notifications.chat("WDL Started");
        } catch (Exception e) {
            throw new RuntimeException("Failed starting WdlxServerSession", e);
        }
        WorldDownloadX.EVENT_BUS.register(this);
    }

    @Override
    public void close() throws Exception {
        LOGGER.info("Stopping WdlServerSession");
        WorldDownloadX.EVENT_BUS.unregister(this);
        try {
            flushLoadedChunks();
        } catch (Exception e) {
            LOGGER.error("Failed to close WdlServerSession", e);
            Notifications.chatError("Error while saving world: " + e.getMessage());
        }
        server.executeBlocking(() -> {
            server.saveEverything(false, true, true);
        });
        server.halt(true);
        LOGGER.info("Stopped WdlServerSession");
        Notifications.chat("WDL Stopped");
    }

    private void flushLoadedChunks() {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) {
            throw new RuntimeException("No level loaded");
        }
        var player = mc.player;
        if (player == null) {
            throw new RuntimeException("No player loaded");
        }
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

    @Override
    public boolean active() {
        return server.isRunning();
    }

    @Override
    public CompletableFuture<LongSet> savedChunks() {
        return null;
    }
}
