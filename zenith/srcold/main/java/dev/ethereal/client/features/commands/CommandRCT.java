package dev.ethereal.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ethereal.api.accessor.IPlayerListHud;
import dev.ethereal.api.command.Command;
import dev.ethereal.api.command.CommandRegister;
import dev.ethereal.api.utils.player.PlayerUtil;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.screen.slot.SlotActionType;

@CommandRegister(name = "rct")
public class CommandRCT extends Command {

    private static final int slotLite = 12;
    private boolean processing = false;

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (processing) {
                return SINGLE_SUCCESS;
            }

            if (mc.player == null) return SINGLE_SUCCESS;

            if (!PlayerUtil.isHW()) {
                print("Команда работает только на HolyWorld");
                return SINGLE_SUCCESS;
            }

            int target = parseNumFromTab();
            if (target == -1) {
                print("Не удалось определить номер сервера");
                return SINGLE_SUCCESS;
            }

            int[] slots = calcSlots(target);
            if (slots == null) {
                return SINGLE_SUCCESS;
            }

            int catSlot = slots[0];
            int numSlot = slots[1];

            mc.player.networkHandler.sendChatMessage("/hub");
            processing = true;

            new Thread(() -> reconnect(catSlot, numSlot), "RCT").start();

            return SINGLE_SUCCESS;
        });
    }

    private int parseNumFromTab() {
        try {
            if (mc.inGameHud == null || mc.inGameHud.getPlayerListHud() == null) return -1;

            var playerListHud = mc.inGameHud.getPlayerListHud();
            var header = ((IPlayerListHud) playerListHud).getHeaderText();
            
            if (header == null) return -1;

            String tab = header.getString();
            if (!tab.contains("Лайт")) return -1;

            String[] parts = tab.split("#");
            if (parts.length < 2) return -1;

            String num = parts[1].trim().split("[^0-9]")[0];
            return Integer.parseInt(num);
        } catch (Exception e) {
            return -1;
        }
    }

    private int[] calcSlots(int num) {
        int cat, slot;

        if (num >= 1 && num <= 16) {
            cat = 0;
            slot = 17 + num;
        }
        else if (num >= 17 && num <= 37) {
            cat = 1;
            slot = 18 + (num - 17);
        }
        else if (num >= 38 && num <= 53) {
            cat = 2;
            slot = 18 + (num - 38);
        }
        else if (num >= 54 && num <= 69) {
            cat = 3;
            slot = 18 + (num - 54);
        } else {
            return null;
        }

        return new int[]{cat, slot};
    }

    private void reconnect(int catSlot, int numSlot) {
        try {
            int ping = getPing();
            int base = Math.max(50, ping);

            Thread.sleep(300 + base);

            if (!validPlayer()) {
                processing = false;
                return;
            }

            if (!openMenu(base)) {
                processing = false;
                return;
            }

            int[] slots = {slotLite, catSlot, numSlot};

            for (int i = 0; i < slots.length; i++) {
                if (!chestOpen()) {
                    processing = false;
                    return;
                }

                int clicks = (i == slots.length - 1) ? 15 : 10;

                if (!click(slots[i], clicks, base)) {
                    processing = false;
                    return;
                }

                Thread.sleep(40 + base / 2);
            }

            click(numSlot, 10, base);
            processing = false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            processing = false;
        } catch (Exception e) {
            processing = false;
        }
    }

    private boolean openMenu(int base) throws InterruptedException {
        for (int i = 0; i < 15; i++) {
            if (!validPlayer()) return false;

            mc.player.networkHandler.sendChatMessage("/lite");

            long start = System.currentTimeMillis();
            long timeout = 200L + base * 2L;

            while (System.currentTimeMillis() - start < timeout) {
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    Thread.sleep(80 + base / 2);
                    return true;
                }
                Thread.sleep(10);
            }
        }
        return false;
    }

    private boolean click(int slot, int count, int base) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            if (!chestOpen() || !validPlayer()) return false;

            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot,
                0,
                SlotActionType.PICKUP,
                mc.player
            );
            
            Thread.sleep(3 + base / 25);
        }
        return true;
    }

    private int getPing() {
        try {
            if (mc.getNetworkHandler() != null && mc.player != null) {
                var info = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                return info != null ? info.getLatency() : 50;
            }
        } catch (Exception ignored) {
        }
        return 50;
    }

    private boolean chestOpen() {
        return mc.currentScreen instanceof GenericContainerScreen && validPlayer();
    }

    private boolean validPlayer() {
        return mc.player != null && mc.player.currentScreenHandler != null;
    }
}
