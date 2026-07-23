package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Hand;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.player.InventoryUtil;

@ModuleRegister(name = "SpookyJoiner", category = Category.OTHER)
public class JoinerModule extends Module {
    @Getter private static final JoinerModule instance = new JoinerModule();

    private boolean compassClick = false;
    private boolean restart;

    private double lastY;

    @Override
    public void toggle() {
        super.toggle();
        compassClick = false;
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            lastY = mc.player.getY();
        }
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {

        double dy = mc.player.getY() - lastY;
        if (dy == 5) {
            setEnabled(false);
            return;
        }
        lastY = mc.player.getY();

        if ((!compassClick || restart) && mc.currentScreen == null) {
            int compassSlot = InventoryUtil.findItem(Items.COMPASS, true);
            if (compassSlot == -1) return;

            InventoryUtil.swapToSlot(compassSlot);
            InventoryUtil.useItem(Hand.MAIN_HAND);
            compassClick = true;
        }

        if (compassClick && mc.currentScreen instanceof GenericContainerScreen screen) {
            for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
                ItemStack stack = screen.getScreenHandler().getSlot(i).getStack();
                if (stack.getName().getString().contains("Дуэли")) {
                    InventoryUtil.swapSlots(i, 4);
                    compassClick = false;
                    restart = false;
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!event.isReceive()) return;

        if (event.packet() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString();
            if (message.contains("Вы уже подключены на этот сервер")
                    || message.contains("Сервер заполнен")
                    || message.contains("Вы были кикнуты с сервера 1duels")) {
                compassClick = false;
                restart = true;
                event.setCancel(true);
            }
        }
    }
}
