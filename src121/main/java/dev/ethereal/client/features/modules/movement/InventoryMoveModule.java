package dev.ethereal.client.features.modules.movement;

import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.player.other.CloseScreenEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.Setting;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.other.SlownessManager;
import dev.ethereal.api.utils.player.MoveUtil;
import java.util.LinkedList;
import lombok.Generated;
import lombok.Getter;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;

@ModuleRegister(
    name = "Inventory Move",
    category = Category.MOVEMENT
)
public class InventoryMoveModule extends Module {
    @Getter
    private static final InventoryMoveModule instance = new InventoryMoveModule();
    public final ModeSetting swapMode = (new ModeSetting("Swap mode")).value("Vanilla").values(new String[]{"Vanilla", "Grim"});
    private final BooleanSetting onlyInventory = new BooleanSetting("Только инвентарь").value(false);
    private final LinkedList<Packet<?>> packet = new LinkedList<>();
    private final TimerUtil timerUtil = new TimerUtil();
    private boolean slowed = false;
    private int queuedFlushTicks = -1;

    public InventoryMoveModule() {
        this.addSettings(new Setting[]{this.swapMode, this.onlyInventory});
    }

    public boolean isLegit() {
        return isEnabled() && this.swapMode.is("Grim");
    }

    public boolean isBasic() {
        return !isEnabled() || this.swapMode.is("Vanilla");
    }

    @EventHandler
    public void onCloseScreen(CloseScreenEvent event) {
        if (mc.currentScreen instanceof InventoryScreen && !this.packet.isEmpty() && this.isLegit()) {
            this.queuedFlushTicks = 1;
            this.slowed = true;
            this.timerUtil.reset();
            event.setCancel(true);
        }
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (this.isLegit() && this.queuedFlushTicks >= 0) {
            for (KeyBinding keyBinding : MoveUtil.getMovementKeys()) {
                keyBinding.setPressed(false);
            }

            if (this.queuedFlushTicks-- <= 0) {
                while(!this.packet.isEmpty()) {
                    this.sendPacket(this.packet.removeFirst());
                }

                this.queuedFlushTicks = -1;
                this.slowed = false;
                this.timerUtil.reset();
            }
            return;
        }

        if (!this.timerUtil.finished(50L) && this.isLegit()) {
            this.slowed = true;

            for (KeyBinding keyBinding : MoveUtil.getMovementKeys()) {
                keyBinding.setPressed(false);
            }

            this.slowed = false;
        } else if (this.canMoveInCurrentScreen() && !this.slowed && !SlownessManager.slowed) {
            MoveUtil.updateMovementKeys();
        }
    }

    private boolean canMoveInCurrentScreen() {
        if (mc.currentScreen == null) return true;

        if (this.onlyInventory.getValue()) {
            return mc.currentScreen instanceof InventoryScreen;
        }

        return !(mc.currentScreen instanceof ChatScreen)
                && !(mc.currentScreen instanceof SignEditScreen)
                && !(mc.currentScreen instanceof AnvilScreen)
                && !(mc.currentScreen instanceof AbstractCommandBlockScreen)
                && !(mc.currentScreen instanceof StructureBlockScreen)
                && !(mc.currentScreen instanceof GenericContainerScreen);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.isSend() && this.isLegit() && (MoveUtil.isMoving() || mc.options.jumpKey.isPressed())) {
            Packet<?> pacl = event.packet();
            if (pacl instanceof ClickSlotC2SPacket) {
                if (mc.currentScreen instanceof InventoryScreen) {
                    this.packet.add(pacl);
                    event.setCancel(true);
                }
            } else if ((pacl instanceof ButtonClickC2SPacket || pacl instanceof CreativeInventoryActionC2SPacket || pacl instanceof SlotChangedStateC2SPacket) && mc.currentScreen instanceof InventoryScreen) {
                this.packet.add(pacl);
                event.setCancel(true);
            }

            if (pacl instanceof CloseHandledScreenC2SPacket && mc.currentScreen instanceof InventoryScreen) {
                this.packet.add(pacl);
                event.setCancel(true);
            }
        }
    }
}
