package wdlx.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wdlx.ext.ServerLevelExt;
import wdlx.server.WdlxMinecraftServer;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements ServerLevelExt {

    @Shadow
    @NotNull
    public abstract MinecraftServer getServer();

    @Shadow
    public abstract ServerChunkCache getChunkSource();

    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void wdlLevelTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (getServer() instanceof WdlxMinecraftServer) {
            ci.cancel();
            getChunkSource().tick(hasTimeLeft, true);
            entityManager.tick();
        }
    }

    @Override
    public void injectClientChunk(LevelChunk chunk) {
        getChunkSource().getChunkFuture(chunk.getPos().x, chunk.getPos().z, ChunkStatus.FULL, true)
            .thenAccept(serverChunkResult -> {
                var serverChunk = serverChunkResult.orElseThrow(() -> new RuntimeException("Failed to get server chunk"));
                var serverSections = serverChunk.sections;
                for (var i = 0; i < chunk.sections.length; i++) {
                    var clientSection = chunk.sections[i];
                    serverSections[i] = clientSection;
                }
                serverChunk.markUnsaved();
            });
        // todo: reconsider approach
        //  also need to handle entities & block entities & maybe biomes
    }
}
