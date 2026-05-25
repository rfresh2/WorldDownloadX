package wdlx.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wdlx.config.Config;
import wdlx.server.WdlxMinecraftServer;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract Level level();

    @Shadow
    public abstract EntityType<?> getType();

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    public void savePost(CompoundTag nbt, final CallbackInfoReturnable<CompoundTag> cir) {
        if (level().getServer() instanceof WdlxMinecraftServer && getType() != EntityType.PLAYER) {
            if (Config.get().download.entities.freeze) {
                nbt.putByte("NoAI", (byte) 1);
                nbt.putByte("NoGravity", (byte) 1);
                nbt.putByte("Invulnerable", (byte) 1);
                nbt.putByte("Silent", (byte) 1);
            }
        }
    }
}
