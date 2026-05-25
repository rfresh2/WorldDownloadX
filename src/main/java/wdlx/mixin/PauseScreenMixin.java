package wdlx.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wdlx.WorldDownloadX;

@Mixin(PauseScreen.class)
public class PauseScreenMixin {

    @Inject(method = "createPauseMenu", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"
    ))
    public void injectButtonsIntoPauseMenu(
        CallbackInfo ci,
        @Local GridLayout.RowHelper rowHelper
    ) {
        Component buttonText;
        if (WorldDownloadX.API.isDownloading()) {
            buttonText = Component.literal("[WDLX] ").append(Component.translatable("wdlx.gui.pause.toggle.stop"));
        } else {
            buttonText = Component.literal("[WDLX] ").append(Component.translatable("wdlx.gui.pause.toggle.start"));
        }

        var toggleWdlxButton = Button.builder(
            buttonText, b -> {
                if (WorldDownloadX.API.isDownloading()) {
                    WorldDownloadX.WDL_MANAGER.stop();
                } else {
                    WorldDownloadX.WDL_MANAGER.start();
                }
                Minecraft.getInstance().setScreen(null);
            }
        ).width(204).build();
        rowHelper.addChild(toggleWdlxButton, 2);
    }
}
