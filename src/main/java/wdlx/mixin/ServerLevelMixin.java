package wdlx.mixin;

import net.lenni0451.reflect.Objects;
import net.lenni0451.reflect.stream.RStream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
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

    @Shadow
    public abstract ServerLevel getLevel();

    @Shadow
    public abstract void addDuringTeleport(final Entity entity);

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
                RStream.of(ChunkAccess.class, serverChunk)
                    .fields()
                    .filterStatic(false)
                    .forEach(field -> {
                        field.set(serverChunk, field.get(chunk));
                    });
                serverChunk.markUnsaved();
            });
    }

    // todo: need special handling for players i think
    @Override
    public void injectClientEntity(Entity entity) {
        var entityCopy = Objects.allocate(entity.getClass());
        RStream.of(entity)
            .withSuper()
            .fields()
            .filterStatic(false)
            .forEach(field -> {
                field.copy(entityCopy);
            });
        RStream.of(entityCopy)
            .withSuper()
            .fields()
            .filterStatic(false)
            .filter(fieldWrapper -> fieldWrapper.type() == Level.class)
            .forEach(fieldWrapper -> fieldWrapper.set(entityCopy, getLevel()));
        addDuringTeleport(entityCopy);
    }
}
