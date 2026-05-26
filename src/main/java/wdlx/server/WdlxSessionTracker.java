package wdlx.server;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class WdlxSessionTracker {
    private final LongSet savedChunks = new LongOpenHashSet();
    private final LongSet newlySavedChunks = new LongOpenHashSet();
    private final Set<UUID> savedEntities = new HashSet<>();

    public synchronized void addNewlySavedChunk(long chunk) {
        savedChunks.add(chunk);
        newlySavedChunks.add(chunk);
    }

    public synchronized void addNewlySavedChunk(int x, int z) {
        addNewlySavedChunk(ChunkPos.asLong(x, z));
    }

    public synchronized void addSavedChunk(long chunk) {
        savedChunks.add(chunk);
    }

    public synchronized void addSavedChunk(int x, int z) {
        addSavedChunk(ChunkPos.asLong(x, z));
    }

    public synchronized void addSavedEntity(UUID uuid) {
        savedEntities.add(uuid);
    }
}
