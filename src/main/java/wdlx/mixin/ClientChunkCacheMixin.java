package wdlx.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wdlx.WorldDownloadX;
import wdlx.events.ChunkUnloadEvent;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {
    @Inject(method = "drop", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientChunkCache$Storage;drop(ILnet/minecraft/world/level/chunk/LevelChunk;)V"
    ))
    public void drop(
        CallbackInfo ci,
        @Local LevelChunk chunk
    ) {
        WorldDownloadX.EVENT_BUS.call(new ChunkUnloadEvent(chunk));
    }

    @WrapOperation(method = "updateViewRadius", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientChunkCache$Storage;inRange(II)Z"
    ))
    public boolean updateViewRadius(
        final ClientChunkCache.Storage instance,
        final int x,
        final int z,
        final Operation<Boolean> original,
        @Local LevelChunk chunk
    ) {
        var result = original.call(instance, x, z);
        if (!result) {
            WorldDownloadX.EVENT_BUS.call(new ChunkUnloadEvent(chunk));
        }
        return result;
    }
}
