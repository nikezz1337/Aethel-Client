package dev.aethel.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import dev.aethel.event.list.EventPacket;
import dev.aethel.event.list.EventPlayerUpdate;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.util.packet.NetworkUtils;
import dev.aethel.util.player.other.SlownessManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@ModuleInformation(
        moduleName = "Gui Move",
        moduleCategory = ModuleCategory.MOVEMENT,
        moduleDesc = "Движение в инвентаре с обходом"
)
public class GuiMove extends Module {

    private final BooleanSetting bypass = new BooleanSetting("Обход", false);
    private final BooleanSetting onlyInventory = new BooleanSetting("Только инвентарь", false);

    private final LinkedList<Packet<?>> queuedPackets = new LinkedList<>();
    private boolean wasInInventory;

    public GuiMove() {}

    @Subscribe
    public void onUpdate(EventPlayerUpdate event) {
        if (mc.player == null || !isEnabled()) return;

        boolean inInventory = mc.currentScreen instanceof InventoryScreen;

        // Обход: отправляем квейкнутые пакеты при закрытии инвентаря
        if (bypass.getValue() && wasInInventory && !inInventory && !queuedPackets.isEmpty()) {
            List<Packet<?>> toSend = new ArrayList<>(queuedPackets);
            queuedPackets.clear();
            SlownessManager.addTask(new SlownessManager.SlowTask(60, 0, () -> {
                for (Packet<?> p : toSend) {
                    NetworkUtils.sendSilentPacket(p);
                }
            }));
            wasInInventory = inInventory;
            return;
        }
        wasInInventory = inInventory;

        if (mc.currentScreen == null) return;
        if (mc.currentScreen instanceof ChatScreen) return;
        if (!canMoveInCurrentScreen()) return;
        if (inInventory && isCursorNotEmpty()) return;

        // Обновляем клавиши движения
        long handle = mc.getWindow().getHandle();
        for (KeyBinding k : getMovementKeys()) {
            k.setPressed(InputUtil.isKeyPressed(handle, k.getDefaultKey().getCode()));
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (mc.player == null || !isEnabled()) return;

        // Обход: квейкаем пакеты вместо отправки
        if (bypass.getValue() && mc.currentScreen instanceof InventoryScreen) {
            Packet<?> packet = e.getPacket();

            if (packet instanceof ClickSlotC2SPacket) {
                queuedPackets.add(packet);
                e.cancelEvent();
                return;
            }

            if (packet instanceof CloseHandledScreenC2SPacket) {
                queuedPackets.add(packet);
                e.cancelEvent();
            }
        }
    }

    private boolean canMoveInCurrentScreen() {
        if (mc.currentScreen == null) return true;

        if (onlyInventory.getValue()) {
            return mc.currentScreen instanceof InventoryScreen;
        }

        return !(mc.currentScreen instanceof SignEditScreen)
                && !(mc.currentScreen instanceof AnvilScreen)
                && !(mc.currentScreen instanceof GenericContainerScreen);
    }

    private boolean isCursorNotEmpty() {
        return mc.player != null && !mc.player.currentScreenHandler.getCursorStack().isEmpty();
    }

    private static KeyBinding[] getMovementKeys() {
        return new KeyBinding[]{
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey,
                mc.options.sprintKey
        };
    }

    @Override
    public void onDisable() {
        queuedPackets.clear();
        wasInInventory = false;

        for (KeyBinding keyBinding : getMovementKeys()) {
            keyBinding.setPressed(false);
        }
    }
}
