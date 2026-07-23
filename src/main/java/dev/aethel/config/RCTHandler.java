package dev.aethel.config;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventPacket;
import dev.aethel.event.list.EventTick;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.other.StopWatch;
import dev.aethel.util.world.ServerUtil;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;

public class RCTHandler implements IMinecraft {

    private static final RCTHandler INSTANCE = new RCTHandler();
    private final StopWatch stopWatch = new StopWatch();
    private int targetAnarchy;
    private boolean waitingHub;
    private boolean waitingMenu;
    private boolean waitingTab;
    private boolean sentLite;

    private RCTHandler() {}

    public static RCTHandler getInstance() {
        return INSTANCE;
    }

    public void reconnect(int anarchy) {
        if (anarchy > 0 && anarchy < 64) {
            targetAnarchy = anarchy;
            waitingHub = true;
            waitingMenu = false;
            waitingTab = false;
            sentLite = false;
            stopWatch.reset();
        } else {
            ChatUtils.send("[RCT] Не верный " + Formatting.RED + "лайт");
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (targetAnarchy == 0 || !(e.getPacket() instanceof GameMessageS2CPacket message)) return;
        String text = message.content().getString().toLowerCase();
        if (!text.contains("хаб") && text.contains("не удалось")) {
            ChatUtils.send("[RCT] На данную анархию " + Formatting.RED + "нельзя" + Formatting.RESET + " зайти");
            reset();
        }
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (targetAnarchy == 0 || mc.player == null || mc.world == null) return;

        if (!ServerUtil.isHolyWorld()) {
            reset();
            return;
        }

        if (ServerUtil.isPvp()) {
            reset();
            return;
        }

        int currentAnarchy = ServerUtil.getAnarchy();

        if (!waitingHub && !waitingMenu && !waitingTab && currentAnarchy == targetAnarchy) {
            ChatUtils.send("[RCT] Успешно телепортирован на анархию " + targetAnarchy);
            reset();
            return;
        }

        if (waitingHub) {
            if (currentAnarchy == -1) {
                waitingHub = false;
                waitingMenu = true;
                sentLite = false;
                stopWatch.reset();
            } else {
                if (stopWatch.every(500)) {
                    mc.player.networkHandler.sendChatCommand("hub");
                }
            }
            return;
        }

        if (mc.currentScreen instanceof GenericContainerScreen screen
                && screen.getTitle().getString().equals("Выбор Лайт анархии:")) {
            waitingTab = false;
            boolean tabsScreen = ((net.minecraft.screen.GenericContainerScreenHandler) screen.getScreenHandler()).getInventory().size() < 10;

            int tabSlot;
            int itemOffset;
            if (targetAnarchy < 16) {
                tabSlot = tabsScreen ? 0 : 0;
                itemOffset = 0;
            } else if (targetAnarchy < 32) {
                tabSlot = tabsScreen ? 1 : 1;
                itemOffset = 16;
            } else if (targetAnarchy < 48) {
                tabSlot = tabsScreen ? 2 : 2;
                itemOffset = 32;
            } else {
                tabSlot = tabsScreen ? 3 : 3;
                itemOffset = 48;
            }

            if (tabsScreen) {
                if (waitingMenu) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, tabSlot, SlotActionType.QUICK_MOVE, mc.player);
                    waitingMenu = false;
                    waitingTab = true;
                    stopWatch.reset();
                }
            } else if (!waitingTab) {
                int itemSlot = 18 + targetAnarchy - itemOffset;
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
                waitingMenu = false;
                waitingTab = false;
                stopWatch.reset();
            }
            return;
        }

        if (waitingMenu && !sentLite) {
            mc.player.networkHandler.sendChatCommand("lite");
            sentLite = true;
            stopWatch.reset();
            return;
        }

        if (stopWatch.finished(3000) && !waitingMenu && !waitingTab) {
            reset();
        }
    }

    private void reset() {
        targetAnarchy = 0;
        waitingHub = false;
        waitingMenu = false;
        waitingTab = false;
        sentLite = false;
    }
}
