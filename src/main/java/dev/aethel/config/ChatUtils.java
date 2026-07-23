package dev.aethel.config;

import dev.aethel.module.settings.impl.Theme;
import dev.aethel.module.settings.impl.ThemeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class ChatUtils {

    private static final String PREFIX = "Aethel";

    public static void send(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            System.out.println("[Aethel] " + message);
            return;
        }

        MutableText text = buildPrefix();
        text.append(Text.literal(" \u00bb ")
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(0xA0A0A0))
                ));
        text.append(parseColoredText(message));
        mc.player.sendMessage(text, false);
    }

    private static MutableText buildPrefix() {
        Theme theme = ThemeManager.getInstance().getCurrentTheme();
        int c1 = theme.getColorFirst();
        int c2 = theme.getColorSecond();

        MutableText text = Text.literal("");
        for (int i = 0; i < PREFIX.length(); i++) {
            float ratio = (float) i / PREFIX.length();
            int color = Theme.interpolateColorClean(c1, c2, ratio);
            text.append(Text.literal(String.valueOf(PREFIX.charAt(i)))
                    .setStyle(Style.EMPTY
                            .withBold(true)
                            .withColor(TextColor.fromRgb(color))
                    ));
        }
        return text;
    }

    private static MutableText parseColoredText(String message) {
        MutableText result = Text.literal("");
        Theme theme = ThemeManager.getInstance().getCurrentTheme();
        int defaultColor = theme.colorText & 0xFFFFFF;

        int i = 0;
        while (i < message.length()) {
            if (message.charAt(i) == '\u00a7' && i + 1 < message.length()) {
                char code = Character.toLowerCase(message.charAt(i + 1));
                Integer color = getSectionColor(code);
                i += 2;
                int end = message.indexOf('\u00a7', i);
                if (end == -1) end = message.length();
                String part = message.substring(i, end);
                if (color != null) {
                    result.append(Text.literal(part)
                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
                } else {
                    result.append(Text.literal(part));
                }
                i = end;
            } else {
                int end = message.indexOf('\u00a7', i);
                if (end == -1) end = message.length();
                String part = message.substring(i, end);
                result.append(Text.literal(part)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(defaultColor))));
                i = end;
            }
        }
        return result;
    }

    private static Integer getSectionColor(char code) {
        return switch (code) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> null;
        };
    }
}
