package wdlx.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.entity.ChunkEntities;

import java.util.concurrent.CompletableFuture;

public class WdlLevelManager implements AutoCloseable {
    final WdlSession wdlSession;
    final ClientLevel level;
    final WdlRegionStorage chunkStorage;
    final WdlRegionStorage entityStorage;
    final WdlRegionStorage poiStorage;

    public WdlLevelManager(
        WdlSession wdlSession,
        ClientLevel level
    ) {
        this.wdlSession = wdlSession;
        this.level = level;
        var levelSourceAccess = wdlSession.levelSourceAccess;
        this.chunkStorage = WdlRegionStorage.createChunkStorage(level, levelSourceAccess);
        this.entityStorage = WdlRegionStorage.createEntityStorage(level, levelSourceAccess);
        this.poiStorage = WdlRegionStorage.createPoiStorage(level, levelSourceAccess);
    }

    @Override
    public void close() throws Exception {
        chunkStorage.synchronize(true).join();
        chunkStorage.close();
        entityStorage.synchronize(true).join();
        entityStorage.close();
        poiStorage.synchronize(true).join();
        poiStorage.close();
    }

    public CompletableFuture<Void> writeChunk(ChunkAccess chunk) {
        var nbt = WdlChunkSerializer.write(level, chunk);
        return chunkStorage.write(chunk.getPos(), nbt);
    }

    public CompletableFuture<Void> writeEntities(ChunkEntities<Entity> entities) {
        if (entities.isEmpty()) {
            return entityStorage.write(entities.getPos(), null);
        }
        ListTag listTag = new ListTag();
        entities.getEntities().forEach(entity -> {
            CompoundTag nbt = new CompoundTag();
            if (entity.save(nbt)) {
                nbt.putByte("NoAI", (byte) 1);
                nbt.putByte("NoGravity", (byte) 1);
                nbt.putByte("Invulnerable", (byte) 1);
                nbt.putByte("Silent", (byte) 1);
                listTag.add(nbt);
            }
        });
        CompoundTag compoundTag = NbtUtils.addCurrentDataVersion(new CompoundTag());
        compoundTag.put("Entities", listTag);
        compoundTag.put("Position", new IntArrayTag(new int[]{entities.getPos().x, entities.getPos().z}));
        return entityStorage.write(entities.getPos(), compoundTag);
    }
}
