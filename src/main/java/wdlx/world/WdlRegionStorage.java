package wdlx.world;

import com.mojang.datafixers.DataFixer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Path;

public class WdlRegionStorage extends SimpleRegionStorage {

    public static WdlRegionStorage createChunkStorage(
        Level level,
        LevelStorageSource.LevelStorageAccess levelStorageAccess
    ) {
        return new WdlRegionStorage(
            new RegionStorageInfo(levelStorageAccess.getLevelId(), level.dimension(), "chunk"),
            levelStorageAccess.getDimensionPath(level.dimension()).resolve("region"),
            Minecraft.getInstance().getFixerUpper(),
            true,
            DataFixTypes.CHUNK
        );
    }

    public static WdlRegionStorage createEntityStorage(
        Level level,
        LevelStorageSource.LevelStorageAccess levelStorageAccess
    ) {
        return new WdlRegionStorage(
            new RegionStorageInfo(levelStorageAccess.getLevelId(), level.dimension(), "entities"),
            levelStorageAccess.getDimensionPath(level.dimension()).resolve("entities"),
            Minecraft.getInstance().getFixerUpper(),
            true,
            DataFixTypes.ENTITY_CHUNK
        );
    }

    public static WdlRegionStorage createPoiStorage(
        Level level,
        LevelStorageSource.LevelStorageAccess levelStorageAccess
    ) {
        return new WdlRegionStorage(
            new RegionStorageInfo(levelStorageAccess.getLevelId(), level.dimension(), "poi"),
            levelStorageAccess.getDimensionPath(level.dimension()).resolve("poi"),
            Minecraft.getInstance().getFixerUpper(),
            true,
            DataFixTypes.POI_CHUNK
        );
    }

    public WdlRegionStorage(RegionStorageInfo info, Path folder, DataFixer fixerUpper, boolean sync, DataFixTypes dataFixType) {
        super(
            info,
            folder,
            fixerUpper,
            sync,
            dataFixType
        );
    }
}
