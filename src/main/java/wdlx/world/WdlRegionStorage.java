package wdlx.world;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class WdlRegionStorage extends SimpleRegionStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(WdlRegionStorage.class);
    private LevelStorageSource.LevelStorageAccess levelStorageAccess;

    public static WdlRegionStorage createChunkStorage(
        Level level,
        LevelStorageSource.LevelStorageAccess levelStorageAccess
    ) {
        return new WdlRegionStorage(
            new RegionStorageInfo(levelStorageAccess.getLevelId(), level.dimension(), "chunk"),
            levelStorageAccess.getDimensionPath(level.dimension()).resolve("region"),
            Minecraft.getInstance().getFixerUpper(),
            true,
            DataFixTypes.CHUNK,
            levelStorageAccess
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
            DataFixTypes.ENTITY_CHUNK,
            levelStorageAccess
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
            DataFixTypes.POI_CHUNK,
            levelStorageAccess
        );
    }

    public WdlRegionStorage(RegionStorageInfo info, Path folder, DataFixer fixerUpper, boolean sync, DataFixTypes dataFixType, LevelStorageSource.LevelStorageAccess levelStorageAccess) {
        super(
            info,
            folder,
            fixerUpper,
            sync,
            dataFixType
        );
        this.levelStorageAccess = levelStorageAccess;
    }
    static final Pattern regionFileRegex = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");

    public CompletableFuture<LongSet> allSavedChunkPositions() {
        return worker.submitTask(() -> Either.left(allSavedChunks0()));
    }

    private LongSet allSavedChunks0() {
        LongSet chunks = new LongOpenHashSet();
        var regionFilesPath = worker.storage.folder;
        try {
            Files.list(regionFilesPath)
                .filter(p -> p.endsWith(".mca"))
                .forEach(p -> {
                    var file = p.toFile();
                    var matcher = regionFileRegex.matcher(file.getName());
                    if (matcher.matches()) {
                        int regionX = Integer.parseInt(matcher.group(1));
                        int regionZ = Integer.parseInt(matcher.group(2));
                        int minChunkX = regionX << 5;
                        int minChunkZ = regionZ << 5;
                        // todo: i think this can be optimized further if we create our own reader
                        //       but its nice not having to own that logic, so leaving it for now
                        try (var regionFile = new RegionFile(worker.storageInfo(), p, regionFilesPath, true)) {
                            for (int cx = 0; cx < 32; cx++) {
                                for (int cz = 0; cz < 32; cz++) {
                                    var chunkPos = new ChunkPos(cx + minChunkX, cz + minChunkZ);
                                    if (regionFile.doesChunkExist(chunkPos)) {
                                        chunks.add(chunkPos.toLong());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to read region file {}", p, e);
                        }
                    }
                });
        } catch (Exception e) {
            LOGGER.error("Failed to list region files", e);
        }
        return chunks;
    }
}
