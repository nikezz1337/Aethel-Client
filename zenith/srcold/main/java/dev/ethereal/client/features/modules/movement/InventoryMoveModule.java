package dev.ethereal.client.features.modules.movement;

import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.player.other.CloseScreenEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.Setting;
import dev.ethereal.api.system.client.ThreadManager;
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
    public final ModeSetting swapMode = (new ModeSetting("Swap mode")).value("Legit").values(new String[]{"Vanilla", "Grim"});
    private final LinkedList<Packet<?>> packet = new LinkedList();
    private final TimerUtil timerUtil = new TimerUtil();
    private boolean slowed = false;

    public InventoryMoveModule() {
        this.addSettings(new Setting[]{this.swapMode});
    }

    public boolean isLegit() {
        return this.swapMode.is("Grim");
    }

    public boolean isBasic() {
        return this.swapMode.is("Vanilla");
    }

    public void onEvent() {
        EventListener closeScreenEvent = CloseScreenEvent.getInstance().subscribe(new Listener((event) -> this.closeScreenEvent()));
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener((event) -> this.updateEvent()));
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener((event) -> this.packetEvent((PacketEvent.PacketEventData) event)));
        this.addEvents(new EventListener[]{closeScreenEvent, updateEvent, packetEvent});
    }

    private void closeScreenEvent() {
        if (mc.currentScreen instanceof InventoryScreen && !this.packet.isEmpty() && this.isLegit()) {
            ThreadManager.run(() -> {
                this.slowed = true;
                this.timerUtil.reset();

                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }

                while(!this.packet.isEmpty()) {
                    this.sendPacket(this.packet.removeFirst());
                }

                this.slowed = false;
            });
            CloseScreenEvent.getInstance().setCancel(true);
        }
    }

    private void updateEvent() {
        if (!this.timerUtil.finished(100L) && this.isLegit()) {
            this.slowed = true;

            for (KeyBinding keyBinding : MoveUtil.getMovementKeys()) {
                keyBinding.setPressed(false);
            }

            this.slowed = false;
        } else if (!(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof SignEditScreen) && !(mc.currentScreen instanceof AnvilScreen) && !(mc.currentScreen instanceof AbstractCommandBlockScreen) && !(mc.currentScreen instanceof StructureBlockScreen) && !this.slowed && !SlownessManager.slowed) {
            MoveUtil.updateMovementKeys();
        }
    }

    private void packetEvent(PacketEvent.PacketEventData event) {
        if (event.isSend() && this.isLegit() && (MoveUtil.isMoving() || mc.options.jumpKey.isPressed())) {
            Packet<?> pacl = event.packet();
            if (pacl instanceof ClickSlotC2SPacket) {
                ClickSlotC2SPacket clickPacket = (ClickSlotC2SPacket)pacl;
                if (mc.currentScreen instanceof InventoryScreen) {
                    this.packet.add(pacl);
                    PacketEvent.getInstance().setCancel(true);
                }
            } else if ((pacl instanceof ButtonClickC2SPacket || pacl instanceof CreativeInventoryActionC2SPacket || pacl instanceof SlotChangedStateC2SPacket) && mc.currentScreen instanceof InventoryScreen) {
                this.packet.add(pacl);
                PacketEvent.getInstance().setCancel(true);
            }

            if (pacl instanceof CloseHandledScreenC2SPacket) {
                this.packet.add(pacl);
                PacketEvent.getInstance().setCancel(true);
            }
        }
    }
}