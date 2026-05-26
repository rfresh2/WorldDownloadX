package wdlx.util;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import wdlx.api.WdlXApi;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawFeatureFactory;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

public class XaeroPlusIntegration {

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("xaeroplus")) return;

        Globals.drawManager.registry().register(
            DrawFeatureFactory.asyncChunkHighlights(
                "WorldDownloadX",
                XaeroPlusIntegration::chunkHighlightSupplier,
                () -> ColorHelper.getColor(0, 255, 0, 100)
            )
        );
    }

    private static Long2LongMap chunkHighlightSupplier(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        if (ChunkUtils.getActualDimension() != dimension) return Long2LongMaps.EMPTY_MAP;
        if (!WdlXApi.getInstance().isDownloading()) {
            return Long2LongMaps.EMPTY_MAP;
        }
        var chunkSet = WdlXApi.getInstance().activeSession().savedChunks().join();
        var map = new Long2LongOpenHashMap(chunkSet.size());
        for (var chunk : chunkSet) {
            map.put(chunk, 0);
        }
        return map;
    }
}
