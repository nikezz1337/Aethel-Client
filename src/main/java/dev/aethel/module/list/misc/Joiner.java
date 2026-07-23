package dev.aethel.module.list.misc;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventPacket;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.ModeSetting;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@ModuleInformation(
    moduleName = "Joiner",
    moduleCategory = ModuleCategory.MISC,
    moduleDesc = "Авто-вход на сервер"
)
public class Joiner extends Module {

    private final ModeSetting server = new ModeSetting("Сервер", "SpookyTime", "SpookyTime");

    private boolean compassClicked;
    private boolean restart;
    private boolean waiting;
    private long lastClickTime;

    @Override
    public void toggle() {
        super.toggle();
        compassClicked = false;
        restart = false;
        waiting = false;
    }

    @Subscribe
    public void onEvent(EventTick event) {
        if (mc.player == null) return;

        if (mc.currentScreen instanceof GenericContainerScreen screen && (compassClicked || restart)) {
            for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
                if (screen.getScreenHandler().getSlot(i).getStack().getName().getString().contains("Дуэли")) {
                    mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, i, 0, SlotActionType.PICKUP, mc.player);
                    compassClicked = false;
                    restart = false;
                    waiting = true;
                    lastClickTime = System.currentTimeMillis();
                    break;
                }
            }
        } else if ((!compassClicked || restart) && !waiting && mc.currentScreen == null) {
            int slot = findCompass();
            if (slot == -1) return;
            mc.player.getInventory().selectedSlot = slot;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            compassClicked = true;
        }

        if (waiting && System.currentTimeMillis() - lastClickTime > 3000) {
            waiting = false;
            toggle();
        }
    }

    @Subscribe
    public void onEvent(EventPacket event) {
        if (event.getType() != EventPacket.Type.RECEIVE) return;

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String msg = packet.content().getString();
            if (msg.contains("Сервер заполнен") || msg.contains("Вы были кикнуты с сервера 1duels")) {
                compassClicked = false;
                restart = true;
                waiting = false;
            }
        }
    }

    private int findCompass() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.COMPASS) return i;
        }
        return -1;
    }
}
