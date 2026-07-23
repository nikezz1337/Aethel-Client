package dev.aethel.util.chat;

import dev.aethel.config.ChatUtils;
import dev.aethel.util.IMinecraft;

public class ChatUtil implements IMinecraft {
    public static void send(Object message) {
        ChatUtils.send(message.toString());
    }

    public static void send(Object... messages) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(messages[i].toString());
        }
        ChatUtils.send(sb.toString());
    }
}
