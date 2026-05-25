package wdlx.server;

import com.google.common.base.Stopwatch;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.serialization.Lifecycle;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
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
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import wdlx.config.Config;
import wdlx.ext.ServerLevelExt;

import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class WdlxMinecraftServer extends MinecraftServer {
    private static final Services NO_SERVICES = new Services(null, ServicesKeySet.EMPTY, null, null);
    private static final WorldOptions WORLD_OPTIONS = new WorldOptions(0L, false, false);
    private static final GameRules TEST_GAME_RULES = Util.make(new GameRules(FeatureFlags.VANILLA_SET), gameRules -> {
        gameRules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        gameRules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        gameRules.getRule(GameRules.RULE_RANDOMTICKING).set(0, null);
        gameRules.getRule(GameRules.RULE_DOFIRETICK).set(false, null);
    });

    public static WdlxMinecraftServer create(
        final Thread thread,
        final LevelStorageSource.LevelStorageAccess levelStorageAccess,
        final PackRepository packRepository
    ) {
        // todo: inject client registry (synced from external server)
        //  client registries will still be missing server-side only data like worldgen params
        //  but main thing we need to match is the dimension registry

        // todo: alot of this async loading is prob unnecessary, copied from GameTestServer
        packRepository.reload();
        WorldDataConfiguration worldDataConfiguration = new WorldDataConfiguration(
            new DataPackConfig(new ArrayList(packRepository.getAvailableIds()), List.of()), FeatureFlags.REGISTRY.allFlags()
        );
        LevelSettings levelSettings = new LevelSettings("World Download Level", GameType.CREATIVE, false, Difficulty.NORMAL, true, TEST_GAME_RULES, worldDataConfiguration);
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, worldDataConfiguration, false, true);
        WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.DEDICATED, 4);

        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            WorldStem worldStem = Util.blockUntilDone(
                    executor -> WorldLoader.load(
                        initConfig,
                        context -> {
                            Registry<LevelStem> registry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
                            WorldDimensions.Complete complete = context.datapackWorldgen()
                                .lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT) // todo: what we really need is the flat world void preset
                                .value()
                                .createWorldDimensions()
                                .bake(registry);
                            return new WorldLoader.DataLoadOutput<>(
                                new PrimaryLevelData(levelSettings, WORLD_OPTIONS, complete.specialWorldProperty(), complete.lifecycle()), complete.dimensionsRegistryAccess()
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
        ServerLevel serverLevel = getLevel(chunk.getLevel().dimension());
        ServerLevelExt ext = (ServerLevelExt) serverLevel;
        ext.injectClientChunk(chunk);
    }

    public void writeClientEntity(Entity entity) {
        if (!Config.get().download.entities.enabled) return;
        ServerLevel serverLevel = getLevel(entity.level().dimension());
        ServerLevelExt ext = (ServerLevelExt) serverLevel;
        ext.injectClientEntity(entity);
    }

    @Override
    protected boolean initServer() throws IOException {
        this.setPlayerList(new PlayerList(this, this.registries(), this.playerDataStorage, 1) {});
        getPlayerList().setViewDistance(Minecraft.getInstance().getConnection().serverChunkRadius);
        this.loadLevel();
        ServerLevel serverLevel = getLevel(Minecraft.getInstance().level.dimension());
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
