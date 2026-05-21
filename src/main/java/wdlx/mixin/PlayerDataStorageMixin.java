package wdlx.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wdlx.ext.PlayerDataStorageExt;

@Mixin(PlayerDataStorage.class)
public class PlayerDataStorageMixin implements PlayerDataStorageExt {

    @Unique private boolean censor = false;

    @Inject(method = "save", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/nio/file/Path;)V"
    ))
    public void save(
        CallbackInfo ci,
        @Local CompoundTag tag
    ) {
        if (censor) {
            tag.remove("LastDeathLocation");
        }
    }

    @Override
    public void setCensor(final boolean censor) {
        this.censor = censor;
    }
}
