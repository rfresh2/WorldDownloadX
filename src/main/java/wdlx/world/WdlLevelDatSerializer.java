package wdlx.world;

import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.WorldData;

import java.util.Set;

public class WdlLevelDatSerializer {
    public static void writeLevelData(WdlSession session, CompoundTag nbt) {
        var mc = Minecraft.getInstance();
        var serverBrand = mc.getConnection().serverBrand();
        if (serverBrand != null) {
            nbt.put("ServerBrands", stringCollectionToTag(Set.of(serverBrand)));
        }

        nbt.putBoolean("WasModded", true);

//        if (!this.removedFeatureFlags.isEmpty()) {
//            nbt.put("removed_features", stringCollectionToTag(this.removedFeatureFlags));
//        }

        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("Name", SharedConstants.getCurrentVersion().getName());
        compoundTag.putInt("Id", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        compoundTag.putBoolean("Snapshot", !SharedConstants.getCurrentVersion().isStable());
        compoundTag.putString("Series", SharedConstants.getCurrentVersion().getDataVersion().getSeries());
        nbt.put("Version", compoundTag);
        NbtUtils.addCurrentDataVersion(nbt);
        nbt.put("WorldGenSettings", worldGenSettings());
        nbt.putInt("GameType", GameType.SPECTATOR.getId());
        nbt.putInt("SpawnX", mc.level.getLevelData().getSpawnPos().getX());
        nbt.putInt("SpawnY", mc.level.getLevelData().getSpawnPos().getY());
        nbt.putInt("SpawnZ", mc.level.getLevelData().getSpawnPos().getZ());
        nbt.putFloat("SpawnAngle", mc.level.getLevelData().getSpawnAngle());
        nbt.putLong("Time", mc.level.getGameTime());
        nbt.putLong("DayTime", mc.level.getDayTime());
        nbt.putLong("LastPlayed", Util.getEpochMillis());
        nbt.putString("LevelName", session.name);
        nbt.putInt("version", WorldData.ANVIL_VERSION_ID);
        nbt.putInt("clearWeatherTime", 0);
        nbt.putInt("rainTime", 0);
        nbt.putBoolean("raining", false);
        nbt.putInt("thunderTime", 0);
        nbt.putBoolean("thundering", false);
        nbt.putBoolean("hardcore", false);
        nbt.putBoolean("allowCommands", true);
        nbt.putBoolean("initialized", true);
        mc.level.getWorldBorder().createSettings().write(nbt);
        nbt.putByte("Difficulty", (byte) mc.level.getLevelData().getDifficulty().getId());
        nbt.putBoolean("DifficultyLocked", false);
        nbt.put("GameRules", gameRules(mc.level.getGameRules()));
        nbt.put("DragonFight", new CompoundTag());
        var playerNbt = new CompoundTag();
        mc.player.save(playerNbt);
        playerNbt.remove("LastDeathLocation");
        playerNbt.putString("Dimension", mc.level.dimension().location().toString());
        nbt.put("Player", playerNbt);
        nbt.put("CustomBossEvents", new CompoundTag());
        nbt.put("ScheduledEvents", new ListTag());
        nbt.putInt("WanderingTraderSpawnDelay", 0);
        nbt.putInt("WanderingTraderSpawnChance", 0);
    }

    private static ListTag stringCollectionToTag(Set<String> stringCollection) {
        ListTag listTag = new ListTag();
        stringCollection.stream().map(StringTag::valueOf).forEach(listTag::add);
        return listTag;
    }

    private static CompoundTag gameRules(GameRules clientRules) {
        var nbt = clientRules.createTag();
        nbt.putString(GameRules.RULE_DO_WARDEN_SPAWNING.getId(), "false");
        nbt.putString(GameRules.RULE_DOFIRETICK.getId(), "false");
        nbt.putString(GameRules.RULE_DO_VINES_SPREAD.getId(), "false");
        nbt.putString(GameRules.RULE_DOMOBSPAWNING.getId(), "false");
        nbt.putString(GameRules.RULE_DAYLIGHT.getId(), "false");
        nbt.putString(GameRules.RULE_KEEPINVENTORY.getId(), "true");
        nbt.putString(GameRules.RULE_MOBGRIEFING.getId(), "false");
        nbt.putString(GameRules.RULE_DO_TRADER_SPAWNING.getId(), "false");
        nbt.putString(GameRules.RULE_DO_PATROL_SPAWNING.getId(), "false");
        nbt.putString(GameRules.RULE_WEATHER_CYCLE.getId(), "false");
        return nbt;
    }

    private static CompoundTag worldGenSettings() {
        var nbt = new CompoundTag();
        nbt.putByte("bonus_chest", (byte) 0);
        nbt.putLong("seed", 0L);
        nbt.putByte("generate_features", (byte) 0);
        var dimensions = new CompoundTag();
        nbt.put("dimensions", dimensions);
        for (var dim : Minecraft.getInstance().getConnection().levels()) {
            var dimNbt = new CompoundTag();
            dimensions.put(dim.location().toString(), dimNbt);
            dimNbt.put("generator", voidGeneratorTag());
            dimNbt.putString("type", dim.location().toString()); // todo: check with non-vanilla?
        }
        return nbt;
    }

    private static CompoundTag voidGeneratorTag() {
        var nbt = new CompoundTag();
        var settings = new CompoundTag();
        nbt.put("settings", settings);
        settings.putByte("features", (byte) 1);
        settings.putString("biome", "minecraft:the_void");
        var layers = new ListTag();
        settings.put("layers", layers);
        var layer = new CompoundTag();
        layers.add(layer);
        layer.putString("block", "minecraft:air");
        layer.putInt("height", 1);
        settings.put("structure_overrides", new ListTag());
        settings.putByte("lakes", (byte) 0);
        nbt.putString("type", "minecraft:flat");
        return nbt;
    }
}
