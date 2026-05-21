package wdlx.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class Notifications {
    public static void chat(String message) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.getChat().addMessage(Component.literal("[WDLX] ").append(Component.literal(message)));
        }
    }

    public static void chatError(String message) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.getChat().addMessage(Component
                .literal("[WDLX] ")
                .withStyle(ChatFormatting.RED)
                .append(Component
                    .literal(message)
                    .withStyle(ChatFormatting.WHITE)));
        }
    }

    public static void toast(String message) {
        var mc = Minecraft.getInstance();
        mc.execute(() -> mc.getToasts().addToast(
            SystemToast.multiline(
                mc,
                SystemToast.SystemToastId.WORLD_BACKUP,
                Component.literal("[WDLX]").withStyle(ChatFormatting.WHITE),
                Component.literal(message)
            )
        ));
    }
}
