package wdlx.events;

import net.minecraft.world.level.chunk.LevelChunk;

public record ChunkLoadEvent(LevelChunk chunk, boolean alreadyLoaded) { }
