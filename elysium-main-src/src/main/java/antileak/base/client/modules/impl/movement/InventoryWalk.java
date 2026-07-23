package antileak.base.client.modules.impl.movement;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventPacket;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.api.utils.player.MoveUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.ModeSetting;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class InventoryWalk extends Module {

    public static InventoryWalk INSTANCE = new InventoryWalk();

    public ModeSetting bypass = new ModeSetting("Обход", "Vanilla", "Vanilla", "Reallyworld", "Holyworld", "SpookyTime", "LonyGrief");

    private final List<ClickSlotC2SPacket> packet = new ArrayList<>();
    private int pauseTicksRemaining = 0;
    public boolean pendingInventoryClose = false;

    private int holyworldTickStop = 0;
    private boolean holyworldPendingClose = false;

    private int stopTicksOut = 0;
    private boolean stoppedStatus = false;
    private boolean previousStoppedStatus = false;
    private boolean externalMovementLock = false;
    public int ticksPostOnStop = 0;
    public boolean swapBypass = false;
    private final Queue<ClickSlotC2SPacket> windowClickPacketQueue = new LinkedList<>();

    private long waitTimerStart = 0L;

    public InventoryWalk() {
        super("GuiMove", "Позволяет перемещаться с открытым инвентарём, не прерывая процесс передвижения", ModuleCategory.MOVEMENT);
        addSettings(bypass);
    }

    private void resetWaitTimer() {
        waitTimerStart = System.currentTimeMillis();
    }

    private boolean hasTimeElapsed(long ms) {
        return System.currentTimeMillis() - waitTimerStart >= ms;
    }

    private boolean isBypassEnabled() {
        return bypass.is("Reallyworld") || bypass.is("Holyworld") || bypass.is("SpookyTime") || bypass.is("LonyGrief");
    }

    private boolean canStoppingOnWindowClick() {
        if (this.externalMovementLock) return false;
        return MoveUtils.isMoving() || this.stoppedStatus;
    }

    private int ticksWindowClickOffset(SlotActionType type) {
        return type == SlotActionType.PICKUP ? 1 : this.stopTicksOut > 1 ? 2 : 3;
    }

    private void setStop(SlotActionType type) {
        if (this.isEnable() && bypass.is("SpookyTime")) {
            this.stopTicksOut = this.ticksWindowClickOffset(type == null ? SlotActionType.PICKUP : type) + 1;
        }
    }

    private boolean getHasItemStackDragged() {
        if (!(mc.currentScreen instanceof HandledScreen)) return false;
        if (!this.canStoppingOnWindowClick()) return false;
        return mc.player.currentScreenHandler != null
                && mc.player.currentScreenHandler.getCursorStack() != null
                && !mc.player.currentScreenHandler.getCursorStack().isEmpty();
    }

    private void useAccumulatedPackets() {
        if (this.windowClickPacketQueue.isEmpty()) return;
        while (!windowClickPacketQueue.isEmpty()) {
            ClickSlotC2SPacket p = windowClickPacketQueue.poll();
            if (p != null) {
                mc.player.networkHandler.sendPacket(p);
            }
        }
        this.ticksPostOnStop = 0;
    }

    private boolean rememberClickPacket(ClickSlotC2SPacket packetIn) {
        return !this.windowClickPacketQueue.contains(packetIn) && this.windowClickPacketQueue.add(packetIn);
    }

    private void flushPendingWindowClicks() {
        for (ClickSlotC2SPacket p : packet) {
            mc.player.networkHandler.sendPacket(p);
        }
        packet.clear();
        pendingInventoryClose = false;
        holyworldPendingClose = false;
    }

    public void setExternalMovementLock(boolean lock) {
        this.externalMovementLock = lock;
        if (lock) {
            this.stopTicksOut = 0;
            this.stoppedStatus = false;
            this.previousStoppedStatus = false;
            this.ticksPostOnStop = 0;
            unpressMovementKeys();
            if (mc.player != null) {
                mc.player.setSprinting(false);
            }
        }
    }

    private void unpressMovementKeys() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    @EventLink
    public void onPacket(EventPacket event) {
        if (mc.player == null) return;
        if (event.getType() != EventPacket.Type.SEND) return;

        Object raw = event.getPacket();

        if (bypass.is("LonyGrief")) {
            if (raw instanceof ClickSlotC2SPacket click && mc.currentScreen instanceof InventoryScreen && !swapBypass) {
                packet.add(click);
                event.cancel();
            }
            return;
        }

        if (bypass.is("SpookyTime")) {
            if (raw instanceof ClickSlotC2SPacket toSend) {
                if (!swapBypass && this.canStoppingOnWindowClick() && toSend.getSlot() != -1) {
                    this.setStop(toSend.getActionType());
                    if (!this.stoppedStatus && this.rememberClickPacket(toSend)) {
                        this.ticksPostOnStop = 0;
                        event.cancel();
                    }
                }
            }
            return;
        }

        if ((bypass.is("Reallyworld") || bypass.is("Holyworld")) && mc.currentScreen instanceof InventoryScreen && MoveUtils.isMoving()) {
            if (raw instanceof ClickSlotC2SPacket clickPacket) {
                if (swapBypass) return;
                packet.add(clickPacket);
                event.cancel();
                return;
            }

            if (raw instanceof CloseHandledScreenC2SPacket) {
                if (!packet.isEmpty()) {
                    pendingInventoryClose = true;
                    if (bypass.is("Holyworld")) {
                        holyworldTickStop = 2;
                        holyworldPendingClose = true;
                    } else {
                        pauseTicksRemaining = 3;
                    }
                }
                event.cancel();
            }
        }
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (pauseTicksRemaining > 0) pauseTicksRemaining--;
        if (holyworldTickStop > 0) holyworldTickStop--;

        if (bypass.is("SpookyTime")) {
            if (this.previousStoppedStatus && this.stoppedStatus && this.stopTicksOut > 0) {
                this.useAccumulatedPackets();
            }
            this.previousStoppedStatus = this.stoppedStatus;
            if (this.getHasItemStackDragged()) {
                this.setStop(null);
            } else if (this.stopTicksOut > 0) {
                --this.stopTicksOut;
            }
            this.stoppedStatus = this.stopTicksOut > 0;
            ++this.ticksPostOnStop;
            if (this.stoppedStatus) {
                this.ticksPostOnStop = 0;
            }
        }

        KeyBinding[] moveKeys = {
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey
        };

        if (this.externalMovementLock) {
            unpressMovementKeys();
            return;
        }

        boolean suppressMovement = false;
        if (bypass.is("Reallyworld") && pauseTicksRemaining > 0) suppressMovement = true;
        if (bypass.is("Holyworld") && holyworldTickStop > 0 && !hasTimeElapsed(125)) suppressMovement = true;
        if (bypass.is("SpookyTime") && stoppedStatus) suppressMovement = true;
        if (bypass.is("LonyGrief") && pendingInventoryClose && !hasTimeElapsed(50)) suppressMovement = true;

        if (suppressMovement) return;

        if (mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof SignEditScreen) return;
        if (mc.currentScreen instanceof HandledScreen && !(mc.currentScreen instanceof InventoryScreen)) return;

        for (KeyBinding keyBinding : moveKeys) {
            boolean pressed = InputUtil.isKeyPressed(
                    mc.getWindow().getHandle(),
                    keyBinding.getDefaultKey().getCode()
            );
            keyBinding.setPressed(pressed);
        }

        if (pendingInventoryClose && bypass.is("Reallyworld")) {
            flushPendingWindowClicks();
        }
        if (holyworldPendingClose && bypass.is("Holyworld") && holyworldTickStop == 0) {
            flushPendingWindowClicks();
        }
        if (pendingInventoryClose && bypass.is("LonyGrief") && hasTimeElapsed(50)) {
            flushPendingWindowClicks();
        }
    }

    @Override
    public void onEnable() {
        this.useAccumulatedPackets();
        this.windowClickPacketQueue.clear();
        this.ticksPostOnStop = 1;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.useAccumulatedPackets();
        this.ticksPostOnStop = 1;
        packet.clear();
        pauseTicksRemaining = 0;
        holyworldTickStop = 0;
        stopTicksOut = 0;
        stoppedStatus = false;
        externalMovementLock = false;
        pendingInventoryClose = false;
        holyworldPendingClose = false;
        swapBypass = false;
        windowClickPacketQueue.clear();
        super.onDisable();
    }
}