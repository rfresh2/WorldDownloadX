package wdlx.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wdlx.server.WdlxMinecraftServer;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @WrapOperation(method = "applyStep", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/chunk/status/ChunkStep;apply(Lnet/minecraft/world/level/chunk/status/WorldGenContext;Lnet/minecraft/util/StaticCache2D;Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;"
    ))
    public CompletableFuture<ChunkAccess> disableChunkGeneration(ChunkStep instance, WorldGenContext worldGenContext, StaticCache2D<GenerationChunkHolder> cache, ChunkAccess chunk, Operation<CompletableFuture<ChunkAccess>> original) {
        if (worldGenContext.level().getServer() instanceof WdlxMinecraftServer) {
            return CompletableFuture.completedFuture(
                new EmptyLevelChunk(
                    worldGenContext.level(),
                    chunk.getPos(),
                    worldGenContext.level().registryAccess().registryOrThrow(Registries.BIOME).getHolder(0).get()
                )
            );
        }
        return original.call(instance, worldGenContext, cache, chunk);
    }

    // todo: look into what changes may be needed to ignore writing region files for our transient empty chunks
//    @Inject(method = "save", at = @At(
//        value = ""
//    ))
}
