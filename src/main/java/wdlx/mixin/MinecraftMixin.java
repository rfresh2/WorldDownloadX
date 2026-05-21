package wdlx.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wdlx.WorldDownloadX;
import wdlx.events.ClientTickEvent;
import wdlx.events.DisconnectEvent;
import wdlx.events.LevelChangeEvent;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "runTick", at = @At("HEAD"))
    public void renderTickHead(CallbackInfo ci) {
        WorldDownloadX.EVENT_BUS.call(ClientTickEvent.RenderPre.INSTANCE);
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    public void renderTickPost(CallbackInfo ci) {
        WorldDownloadX.EVENT_BUS.call(ClientTickEvent.RenderPost.INSTANCE);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHead(CallbackInfo ci) {
        WorldDownloadX.EVENT_BUS.call(ClientTickEvent.Pre.INSTANCE);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void tickPost(CallbackInfo ci) {
        WorldDownloadX.EVENT_BUS.call(ClientTickEvent.Post.INSTANCE);
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    public void disconnectPre(CallbackInfo ci) {
        WorldDownloadX.EVENT_BUS.call(DisconnectEvent.INSTANCE);
    }

    @Shadow
    public ClientLevel level;

    @Inject(method = "setLevel", at = @At("HEAD"))
    public void setLevelPre(
        ClientLevel clientLevel,
        ReceivingLevelScreen.Reason reason,
        CallbackInfo ci
    ) {
        WorldDownloadX.EVENT_BUS.call(new LevelChangeEvent(this.level, clientLevel));
    }
}
