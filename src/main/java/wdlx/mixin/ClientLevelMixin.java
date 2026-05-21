package wdlx.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wdlx.WorldDownloadX;
import wdlx.events.EntityLoadEvent;
import wdlx.events.EntityUnloadEvent;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "addEntity", at = @At("RETURN"))
    public void addEntityPost(Entity entity, CallbackInfo ci) {
        WorldDownloadX.EVENT_BUS.call(new EntityLoadEvent(entity));
    }

    @Inject(method = "removeEntity", at = @At("RETURN"))
    public void removeEntityPost(
        CallbackInfo ci,
        @Local Entity entity
    ) {
        if (entity == null) return;
        WorldDownloadX.EVENT_BUS.call(new EntityUnloadEvent(entity, entity.getRemovalReason()));
    }
}
