package wdlx.events;

import net.minecraft.world.level.chunk.LevelChunk;

public record ChunkUnloadEvent(LevelChunk chunk) { }
