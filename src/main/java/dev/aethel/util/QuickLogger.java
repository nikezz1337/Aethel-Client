package dev.aethel.util;

public interface QuickLogger {
    default void log(String message) {
        System.out.println("[Aethel] " + message);
    }

    default void logChat(String message) {
        if (IMinecraft.mc.player != null) {
            IMinecraft.mc.player.sendMessage(net.minecraft.text.Text.literal("§7[§bAethel§7] §f" + message), false);
        }
    }
}
