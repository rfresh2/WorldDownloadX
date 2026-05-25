package wdlx.server;

import com.google.common.base.Stopwatch;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.serialization.Lifecycle;
import lombok.SneakyThrows;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wdlx.config.Config;
import wdlx.ext.ServerLevelExt;

import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class WdlxMinecraftServer extends MinecraftServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WdlxServerSession.class);

    private static final Services NO_SERVICES = new Services(null, ServicesKeySet.EMPTY, null, null);

    @SneakyThrows
    public static WdlxMinecraftServer startServer(String name) {
        var mc = Minecraft.getInstance();
        var levelStorageAccess = mc.getLevelSource().createAccess(name);
        return MinecraftServer.spin((thread) -> {
            var s = WdlxMinecraftServer.create(
                name,
                thread,
                levelStorageAccess,
                mc.getResourcePackRepository()
            );
            LOGGER.info("World download server started");
            return s;
        });
    }

    static WdlxMinecraftServer create(
        String name,
        Thread thread,
        LevelStorageSource.LevelStorageAccess levelStorageAccess,
        PackRepository packRepository
    ) {
        var mc = Minecraft.getInstance();

        // todo: inject client registry (synced from external server)
        //  client registries will still be missing server-side only data like worldgen params
        //  but main thing we need to match is the dimension registry

        // todo: alot of this async loading is prob unnecessary, copied from GameTestServer
        var worldDataConfiguration = new WorldDataConfiguration(
            new DataPackConfig(new ArrayList(packRepository.getAvailableIds()), List.of()), mc.getConnection().enabledFeatures()
        );
        var serverGameRules = Util.make(new GameRules(mc.getConnection().enabledFeatures()), gameRules -> {
            gameRules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            gameRules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
            gameRules.getRule(GameRules.RULE_RANDOMTICKING).set(0, null);
            gameRules.getRule(GameRules.RULE_DOFIRETICK).set(false, null);
        });
        var levelSettings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, serverGameRules, worldDataConfiguration);
        var packConfig = new WorldLoader.PackConfig(packRepository, worldDataConfiguration, false, true);
        var initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.DEDICATED, 4);

        try {
            var stopwatch = Stopwatch.createStarted();
            var worldStem = Util.blockUntilDone(
                    executor -> WorldLoader.load(
                        initConfig,
                        context -> {
                            var registry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
                            var worldgen = context.datapackWorldgen();
                            var voidSettings = worldgen
                                .lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
                                .getOrThrow(FlatLevelGeneratorPresets.THE_VOID)
                                .value()
                                .settings();
                            var complete = worldgen
                                .lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT)
                                .value()
                                .createWorldDimensions()
                                .replaceOverworldGenerator(worldgen, new FlatLevelSource(voidSettings))
                                .bake(registry);
                            return new WorldLoader.DataLoadOutput<>(
                                new PrimaryLevelData(
                                    levelSettings,
                                    new WorldOptions(0L, false, false),
                                    complete.specialWorldProperty(),
                                    complete.lifecycle()), complete.dimensionsRegistryAccess()
                            );
                        },
                        WorldStem::new,
                        Util.backgroundExecutor(),
                        executor
                    )
                )
                .get();
            stopwatch.stop();
            return new WdlxMinecraftServer(
                thread,
                levelStorageAccess,
                packRepository,
                worldStem,
                i -> LoggerChunkProgressListener.createCompleted()
            );
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }
    public WdlxMinecraftServer(
        final Thread thread,
        final LevelStorageSource.LevelStorageAccess levelStorageAccess,
        final PackRepository packRepository,
        final WorldStem worldStem,
        final ChunkProgressListenerFactory chunkProgressListenerFactory
    ) {
        super(thread,
            levelStorageAccess,
            packRepository,
            worldStem,
            Proxy.NO_PROXY,
            DataFixers.getDataFixer(),
            NO_SERVICES,
            chunkProgressListenerFactory);
    }

    public void writeClientChunk(LevelChunk chunk) {
        if (Config.get().debug.logSavedChunks) {
            LOGGER.info("Saving chunk {}", chunk.getPos());
        }
        ServerLevel serverLevel = getLevel(chunk.getLevel().dimension());
        ServerLevelExt ext = (ServerLevelExt) serverLevel;
        ext.injectClientChunk(chunk);
    }

    public void writeClientEntity(Entity entity) {
        if (!Config.get().download.entities.enabled) return;
        if (Config.get().debug.logSavedEntities) {
            LOGGER.info("Saving entity: {} ({}) [{}, {}, {}]", entity.getType(), entity.getId(), entity.getX(), entity.getY(), entity.getZ());
        }
        ServerLevel serverLevel = getLevel(entity.level().dimension());
        ServerLevelExt ext = (ServerLevelExt) serverLevel;
        ext.injectClientEntity(entity);
    }

    @Override
    protected boolean initServer() throws IOException {
        this.setPlayerList(new PlayerList(this, this.registries(), this.playerDataStorage, 1) {});
        getPlayerList().setViewDistance(Minecraft.getInstance().getConnection().serverChunkRadius);
        this.loadLevel();
        var serverLevel = getLevel(Minecraft.getInstance().level.dimension());
        return true;
    }

    @Override
    public int getOperatorUserPermissionLevel() {
        return 0;
    }

    @Override
    public int getFunctionCompilationLevel() {
        return 0;
    }

    @Override
    public boolean shouldRconBroadcast() {
        return false;
    }

    @Override
    protected SampleLogger getTickTimeLogger() {
        return new LocalSampleLogger(0);
    }

    @Override
    public boolean isTickTimeLoggingEnabled() {
        return false;
    }

    @Override
    public SystemReport fillServerSystemReport(final SystemReport systemReport) {
        return systemReport;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public boolean isEpollEnabled() {
        return false;
    }

    @Override
    public boolean isCommandBlockEnabled() {
        return true;
    }

    @Override
    public boolean isPublished() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    @Override
    public boolean isSingleplayerOwner(final GameProfile gameProfile) {
        return true;
    }
}
