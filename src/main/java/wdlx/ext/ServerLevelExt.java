package wdlx.ext;

import net.minecraft.world.level.chunk.LevelChunk;

public interface ServerLevelExt {
    void injectClientChunk(LevelChunk chunk);
}
