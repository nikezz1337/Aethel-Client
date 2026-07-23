package com.paymod;

import com.paymod.config.ModConfig;
import com.paymod.gui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PayModClient implements ClientModInitializer {
    private static final KeyBinding.Category PAYMOD_CATEGORY = KeyBinding.Category.create(
        Identifier.of("paymod", "main")
    );
    private static KeyBinding toggleKey;
    private static KeyBinding configKey;
    private static boolean enabled = false;
    private static long lastCheckTime = 0;
    private static boolean waitingForMoney = false;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.paymod.toggle", InputUtil.Type.KEYSYM, 80, PAYMOD_CATEGORY)
        );
        configKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.paymod.config", InputUtil.Type.KEYSYM, 79, PAYMOD_CATEGORY)
        );

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null) return;

        if (toggleKey.wasPressed()) {
            enabled = !enabled;
            client.player.sendMessage(
                Text.literal(enabled ? "\u00a7a\u0412\u043a\u043b\u044e\u0447\u0435\u043d" : "\u00a7c\u0412\u044b\u043a\u043b\u044e\u0447\u0435\u043d"), false
            );
        }

        if (configKey.wasPressed()) {
            client.setScreen(new ConfigScreen(null));
        }

        if (enabled && !waitingForMoney) {
            long now = System.currentTimeMillis();
            if (now - lastCheckTime >= 60000) {
                lastCheckTime = now;
                if (client.player.networkHandler != null) {
                    client.player.networkHandler.sendChatCommand("money");
                    waitingForMoney = true;
                }
            }
        }
    }

    private void onGameMessage(Text message, boolean overlay) {
        if (!waitingForMoney) return;
        processMoneyResponse(message);
    }

    private void processMoneyResponse(Text message) {
        waitingForMoney = false;
        String text = message.getString();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        double balance = extractBalance(text);
        if (balance <= 0) return;

        double keepAmount = ModConfig.getKeepAmount();

        if (balance > keepAmount) {
            double excess = balance - keepAmount;
            String target = ModConfig.getTargetPlayer();
            String amount = formatAmount(excess);

            mc.player.networkHandler.sendChatCommand("pay " + target + " " + amount);
            mc.player.sendMessage(
                Text.literal("\u00a7a\u041f\u0435\u0440\u0435\u0432\u0435\u0434\u0435\u043d\u043e \u00a7f" + amount + " \u00a7a\u0438\u0433\u0440\u043e\u043a\u0443 \u00a7f" + target + " \u00a7a(\u0411\u0430\u043b\u0430\u043d\u0441: \u00a7f" + formatAmount(balance) + "\u00a7a)"), false
            );
        } else {
            mc.player.sendMessage(
                Text.literal("\u00a7e\u0411\u0430\u043b\u0430\u043d\u0441: \u00a7f" + formatAmount(balance) + " \u00a7e(\u041d\u0443\u0436\u043d\u043e: \u00a7f" + formatAmount(keepAmount) + "\u00a7e)"), false
            );
        }
    }

    private double extractBalance(String message) {
        String cleaned = message.replaceAll("[,\\s]", "");
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+\\.?\\d*");
        java.util.regex.Matcher matcher = pattern.matcher(cleaned);
        double max = 0;
        while (matcher.find()) {
            try {
                double val = Double.parseDouble(matcher.group());
                if (val > max) max = val;
            } catch (NumberFormatException ignored) {}
        }
        return max;
    }

    private String formatAmount(double amount) {
        if (amount == (long) amount) {
            return String.format("%.0f", amount);
        }
        return String.format("%.2f", amount);
    }
}
