package wdlx.world;

import net.fabricmc.loader.api.FabricLoader;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wdlx.WorldDownloadX;
import wdlx.events.ChunkUnloadEvent;
import wdlx.events.EntityUnloadEvent;
import wdlx.events.LevelChangeEvent;
import wdlx.ext.PlayerDataStorageExt;
import wdlx.util.Notifications;

import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class WdlSession implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(WdlSession.class);

    final String name;
    final LevelStorageSource levelStorageSource;
    final LevelStorageSource.LevelStorageAccess levelSourceAccess;
    final PlayerDataStorage playerDataStorage;
    WdlLevelManager wdlLevel;

    public WdlSession(
        String name
    ) {
        this.name = name;
        var mc = Minecraft.getInstance();
        if (!mc.getLevelSource().isNewLevelIdAcceptable(name)) {
            throw new RuntimeException("Invalid level name");
        }
        try {
            this.levelStorageSource = mc.getLevelSource();
            this.levelSourceAccess = mc.getLevelSource().validateAndCreateAccess(name);
            this.playerDataStorage = levelSourceAccess.createPlayerStorage();
            start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create level", e);
        }
    }

    synchronized void start() {
        WorldDownloadX.EVENT_BUS.register(this);
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) {
            throw new RuntimeException("No level loaded");
        }
        this.wdlLevel = new WdlLevelManager(this, level);
        LOGGER.info("Started WdlSession: {}", name);
        Notifications.chat("WDL Started");
    }

    void flushLoadedChunks() {
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
                    wdlLevel.writeChunk(chunk);
                    count++;
                }
            }
        }
        LOGGER.info("Flushed {} chunks", count);
    }

    void flushLoadedEntities() {
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
                var aabb = new AABB(
                    x << 4, level.getMinBuildHeight(), z << 4,
                    (x + 1) << 4, level.getMaxBuildHeight(), (z + 1) << 4
                );
                var entities = level.getEntities(player, aabb);
                entities.removeIf(e -> e == player);
                if (!entities.isEmpty()) {
                    var chunkEntities = new ChunkEntities<>(new ChunkPos(x, z), entities);
                    wdlLevel.writeEntities(chunkEntities);
                    count += entities.size();
                }
            }
        }
        LOGGER.info("Flushed {} entities", count);
    }

    @Override
    public synchronized void close() throws Exception {
        WorldDownloadX.EVENT_BUS.unregister(this);
        try {
            flushLoadedChunks();
            flushLoadedEntities();
            writePlayerData();
            writeLevelDat();
            writeIcon();
            writeWdlxMetadata();
        } catch (Exception e) {
            LOGGER.error("Failed to close WdlSession", e);
            Notifications.chatError("Error while saving world: " + e.getMessage());
        } finally {
            if (this.wdlLevel != null) {
                this.wdlLevel.close();
                this.wdlLevel = null;
            }

            levelSourceAccess.close();
        }

        LOGGER.info("Closed WdlSession: {}", name);
        Notifications.chat("WDL Saved!");
    }

    void writeLevelDat() {
        CompoundTag parentTag = new CompoundTag();
        CompoundTag nbt = new CompoundTag();
        WdlLevelDatSerializer.writeLevelData(this, nbt);
        parentTag.put("Data", nbt);
        levelSourceAccess.saveLevelData(parentTag);
    }

    void writePlayerData() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            ((PlayerDataStorageExt) playerDataStorage).setCensor(true);
            playerDataStorage.save(player);
        }
    }

    void writeWdlxMetadata() {
        try {
            var path = levelSourceAccess.getLevelDirectory().path().resolve("wdlx.properties");
            var props = new Properties();
            props.setProperty("version", FabricLoader.getInstance().getModContainer(WorldDownloadX.MOD_ID).get().getMetadata().getVersion().getFriendlyString());
            props.setProperty("date", DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()));
            props.setProperty("mc", SharedConstants.VERSION_STRING);
            var serverData = Minecraft.getInstance().getConnection().getServerData();
            if (serverData != null) {
                props.setProperty("server-ip", serverData.ip);
                props.setProperty("server-name", serverData.name);
            }
            try (var writer = Files.newBufferedWriter(path)) {
                props.store(writer, null);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write wdlx.properties", e);
        }
    }

    void writeIcon() {
        try {
            var serverData = Minecraft.getInstance().getConnection().getServerData();
            if (serverData == null) return;
            var iconBytes = serverData.getIconBytes();
            if (iconBytes == null) return;
            var path = levelSourceAccess.getLevelDirectory().path().resolve("icon.png");
            Files.write(path, iconBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        wdlLevel.writeChunk(event.chunk());
    }

    @EventHandler
    public void onLevelChange(LevelChangeEvent event) {
        try {
            if (event.newLevel() != null) {
                wdlLevel.close();
                wdlLevel = new WdlLevelManager(this, event.newLevel());
            } else {
                close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onEntityUnload(EntityUnloadEvent event) {

    }
}
