package wdlx.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Config {
    public static final ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
        .id(ResourceLocation.fromNamespaceAndPath("wdlx", "config"))
        .serializer(config -> GsonConfigSerializerBuilder.create(config)
            .setPath(FabricLoader.getInstance().getConfigDir().resolve("wdlx.json"))
            .appendGsonBuilder(b -> b
                .setPrettyPrinting()
                .disableHtmlEscaping())
            .build())
        .build();

    static {
        HANDLER.load();
    }

    public static Config get() {
        return HANDLER.instance();
    }

    @SerialEntry
    public Download download = new Download();

    @SerialEntry
    public Debug debug = new Debug();

    public static class Download {
        public Entities entities = new Entities();
        public Player player = new Player();
        public Statistics statistics = new Statistics();
        public Maps maps = new Maps();
        public Backup backup = new Backup();

        public static class Entities {
            public boolean enabled = true;
            public boolean freeze = false;
        }

        public static class Player {
            public boolean enabled = true;
        }

        public static class Statistics {
            // todo: implement statistics
            public boolean enabled = true;
        }

        public static class Maps {
            public boolean enabled = true;
        }

        public static class Backup {
            // todo: implement zip backup
            public boolean enabled = true;
        }
    }

    public static class Debug {
        public boolean logSettings = false;
        public boolean logSavedChunks = false;
        public boolean logSavedEntities = false;
        public boolean logSavedPlayers = false;
        public boolean logSavedStatistics = false;
        public boolean logSavedMaps = false;
    }

    public static Screen generateScreen(Screen parent) {
        var defaultConfig = new Config();
        // todo: setting descriptions where useful
        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("WorldDownloadX"))
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("wdlx.config.download.category"))
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("wdlx.config.download.entities.group"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("wdlx.config.download.entities.enabled"))
                        .binding(defaultConfig.download.entities.enabled, () -> Config.get().download.entities.enabled, (value) -> Config.get().download.entities.enabled = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("wdlx.config.download.entities.freeze"))
                        .binding(defaultConfig.download.entities.freeze, () -> Config.get().download.entities.freeze, (value) -> Config.get().download.entities.freeze = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .build())
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("wdlx.config.download.player.group"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("wdlx.config.download.player.enabled"))
                        .binding(defaultConfig.download.player.enabled, () -> Config.get().download.player.enabled, (value) -> Config.get().download.player.enabled = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .build())
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("wdlx.config.download.statistics.group"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("wdlx.config.download.statistics.enabled"))
                        .binding(defaultConfig.download.statistics.enabled, () -> Config.get().download.statistics.enabled, (value) -> Config.get().download.statistics.enabled = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .build())
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("wdlx.config.download.maps.group"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("wdlx.config.download.maps.enabled"))
                        .binding(defaultConfig.download.maps.enabled, () -> Config.get().download.maps.enabled, (value) -> Config.get().download.maps.enabled = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .build())
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("wdlx.config.download.backup.group"))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("wdlx.config.download.backup.enabled"))
                        .binding(defaultConfig.download.backup.enabled, () -> Config.get().download.backup.enabled, (value) -> Config.get().download.backup.enabled = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                    .build())
                .build())
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("wdlx.config.debug.category"))
                .option(Option.<Boolean>createBuilder()
                    .name(Component.translatable("wdlx.config.debug.logSettings"))
                    .binding(defaultConfig.debug.logSettings, () -> Config.get().debug.logSettings, (value) -> Config.get().debug.logSettings = value)
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .option(Option.<Boolean>createBuilder()
                    .name(Component.translatable("wdlx.config.debug.logSavedChunks"))
                    .binding(defaultConfig.debug.logSavedChunks, () -> Config.get().debug.logSavedChunks, (value) -> Config.get().debug.logSavedChunks = value)
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .option(Option.<Boolean>createBuilder()
                    .name(Component.translatable("wdlx.config.debug.logSavedEntities"))
                    .binding(defaultConfig.debug.logSavedEntities, () -> Config.get().debug.logSavedEntities, (value) -> Config.get().debug.logSavedEntities = value)
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .option(Option.<Boolean>createBuilder()
                    .name(Component.translatable("wdlx.config.debug.logSavedPlayers"))
                    .binding(defaultConfig.debug.logSavedPlayers, () -> Config.get().debug.logSavedPlayers, (value) -> Config.get().debug.logSavedPlayers = value)
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .option(Option.<Boolean>createBuilder()
                    .name(Component.translatable("wdlx.config.debug.logSavedStatistics"))
                    .binding(defaultConfig.debug.logSavedStatistics, () -> Config.get().debug.logSavedStatistics, (value) -> Config.get().debug.logSavedStatistics = value)
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .option(Option.<Boolean>createBuilder()
                    .name(Component.translatable("wdlx.config.debug.logSavedMaps"))
                    .binding(defaultConfig.debug.logSavedMaps, () -> Config.get().debug.logSavedMaps, (value) -> Config.get().debug.logSavedMaps = value)
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .build())
            .build()
            .generateScreen(parent);
    }
}
