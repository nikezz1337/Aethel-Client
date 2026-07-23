package dev.ethereal.client.features.modules.combat;

import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.other.MovementInputEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.player.DirectionalInput;
import dev.ethereal.api.utils.player.InventoryActionUtil;
import dev.ethereal.api.utils.player.InventoryTask;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@ModuleRegister(name = "AutoSwap", category = Category.COMBAT, bind = -1)
@Environment(EnvType.CLIENT)
public class ItemSwapModule extends Module {
    @Getter private static final ItemSwapModule instance = new ItemSwapModule();

    private enum SwapPhase {
        READY,
        INV_STOP,
        INV_SWAP,
        INV_RESTORE,
        FINISH,
        ST_OPEN_INV,
        ST_MOVE_CURSOR,
        ST_SWAP_OFFHAND,
        ST_CLOSE_INV
    }

    final BindSetting bind = new BindSetting("Bind");
    private final ModeSetting swapMode = new ModeSetting("Swap Mode").value("Single").values("Single", "Selector");
    private final ModeSetting mode = new ModeSetting("Mode").value("Default").values("Default", "Legit");
    final SliderSetting selectorLimit = new SliderSetting("Selector Limit")
            .value(3.0f).range(1.0f, 10.0f).step(1.0f)
            .setVisible(() -> swapMode.is("Selector"));
    private final SliderSetting legitDelay = new SliderSetting("Delay")
            .value(0.0f).range(0.0f, 200.0f).step(10.0f)
            .setVisible(() -> mode.is("Legit"));
    final ModeSetting firstItem = new ModeSetting("First Item")
            .value("Totem").values("Totem", "Head", "Gapple", "Shield");
    final ModeSetting secondItem = new ModeSetting("Second Item")
            .value("Totem").values("Totem", "Head", "Gapple", "Shield");

    SwapPhase swapPhase = SwapPhase.READY;
    private Slot targetSlot = null;
    int targetSlotId = -1;
    private long lastPhaseTime = 0L;
    private boolean keysOverridden = false;

    public ItemSwapModule() {
        addSettings(bind, swapMode, mode, selectorLimit, legitDelay, firstItem, secondItem);
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent event) {
        if (swapPhase == SwapPhase.INV_STOP || swapPhase == SwapPhase.INV_SWAP || swapPhase == SwapPhase.INV_RESTORE) {
            event.setDirectionalInput(DirectionalInput.NONE);
            event.setJump(false);
        }
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (mc.player == null || mc.currentScreen != null) return;
        if (event.action() != 1 || swapPhase != SwapPhase.READY) return;
        if (InventoryActionUtil.matchesBind(event, bind.getValue())) {
            handleBindPress();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.interactionManager == null) {
            resetState();
            return;
        }
        if (swapPhase == SwapPhase.READY) return;
        if (isLegitPhase(swapPhase)) {
            processLegitSwap();
        } else {
            processDefaultSwap();
        }
    }

    private void handleBindPress() {
        if (swapMode.is("Selector")) {
            List<SwapCandidate> candidates = collectSelectorCandidates();
            if (!candidates.isEmpty()) {
                mc.setScreen(new AutoSwapSelectorScreen(this, candidates));
            }
            return;
        }
        Slot hotbarSlot = findValidSlot(s -> s.id >= 36 && s.id <= 44);
        Slot slot = hotbarSlot != null ? hotbarSlot : findValidSlot(s -> s.id >= 0 && s.id <= 35);
        startSwap(slot);
    }

    public List<SwapCandidate> collectSelectorCandidates() {
        return collectSelectorCandidates(selectorLimit.getValue().intValue());
    }

    public List<SwapCandidate> collectSelectorCandidates(int limit) {
        if (mc.player == null || mc.player.currentScreenHandler == null) return List.of();
        return InventoryTask.slots()
                .filter(this::isSelectorSlotCandidate)
                .sorted(selectorComparator())
                .limit(Math.max(1, Math.min(10, limit)))
                .map(SwapCandidate::new)
                .toList();
    }

    public void startSwapBySlotId(int slotId) {
        Slot slot = InventoryTask.getSlot(s -> s.id == slotId);
        startSwap(slot);
    }

    void startSwap(Slot slot) {
        if (slot == null) return;
        if (mode.is("Legit")) {
            targetSlotId = slot.id;
            lastPhaseTime = 0L;
            swapPhase = SwapPhase.ST_OPEN_INV;
        } else {
            targetSlot = slot;
            keysOverridden = true;
            swapPhase = SwapPhase.INV_STOP;
        }
    }

    private void processDefaultSwap() {
        if (mc.currentScreen != null) {
            resetState();
            return;
        }
        switch (swapPhase) {
            case INV_STOP -> {
                mc.player.setSprinting(false);
                keysOverridden = true;
                swapPhase = SwapPhase.INV_SWAP;
            }
            case INV_SWAP -> {
                if (targetSlot != null) {
                    InventoryTask.moveItem(targetSlot, 45, false, false);
                }
                swapPhase = SwapPhase.INV_RESTORE;
            }
            case INV_RESTORE -> {
                swapPhase = SwapPhase.FINISH;
            }
            case FINISH -> resetState();
            default -> {}
        }
    }

    private void processLegitSwap() {
        if (mc.currentScreen != null
                && swapPhase != SwapPhase.ST_MOVE_CURSOR
                && swapPhase != SwapPhase.ST_SWAP_OFFHAND
                && swapPhase != SwapPhase.ST_CLOSE_INV) {
            resetState();
            return;
        }
        switch (swapPhase) {
            case ST_OPEN_INV -> {
                if (!phaseReady()) return;
                mc.setScreen(new InventoryScreen(mc.player));
                swapPhase = SwapPhase.ST_MOVE_CURSOR;
            }
            case ST_MOVE_CURSOR -> {
                if (!phaseReady()) return;
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
                InventoryActionUtil.moveCursorToSlot(screen, targetSlotId);
                swapPhase = SwapPhase.ST_SWAP_OFFHAND;
            }
            case ST_SWAP_OFFHAND -> {
                if (!phaseReady()) return;
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
                screen.keyPressed(mc.options.swapHandsKey.getDefaultKey().getCode(), 0, 0);
                swapPhase = SwapPhase.ST_CLOSE_INV;
            }
            case ST_CLOSE_INV -> {
                if (!phaseReady()) return;
                InventoryActionUtil.closeCurrentScreenWithInventoryKey();
                swapPhase = SwapPhase.FINISH;
            }
            case FINISH -> {
                if (!phaseReady()) return;
                resetState();
            }
            default -> {}
        }
    }

    private Slot findValidSlot(Predicate<Slot> slotPredicate) {
        if (mc.player == null) return null;
        Predicate<Slot> combined = s -> s.id != 45 && s.id >= 0 && s.id <= 44 && slotPredicate.test(s);

        Item firstType  = itemFor(firstItem.getValue());
        Item secondType = itemFor(secondItem.getValue());
        ItemStack offhand = mc.player.getOffHandStack();
        Item offHandItem = offhand.getItem();
        String offHandName = offhand.getName().getString();

        if (offHandItem == firstType) {
            Slot s = findSlotOfType(secondType, combined, offHandName);
            if (s != null) return s;
        }

        if (offHandItem == secondType) {
            Slot s = findSlotOfType(firstType, combined, offHandName);
            if (s != null) return s;
        }

        if (offHandItem != firstType && offHandItem != secondType) {
            Slot s = findSlotOfType(firstType, combined, offHandName);
            if (s != null) return s;
            s = findSlotOfType(secondType, combined, offHandName);
            if (s != null) return s;
        }

        return null;
    }

    private Slot findSlotOfType(Item item, Predicate<Slot> combined, String offHandName) {
        return mc.player.currentScreenHandler.slots.stream()
                .filter(combined)
                .filter(s -> s.getStack().getItem() == item
                        && !s.getStack().getName().getString().equals(offHandName))
                .max(Comparator.comparing(s -> s.getStack().hasEnchantments()))
                .orElse(null);
    }

    private boolean isSelectorSlotCandidate(Slot slot) {
        if (slot == null || slot.getStack().isEmpty()) return false;
        if (slot.id < 0 || slot.id > 44 || slot.id == 45) return false;
        Item item = slot.getStack().getItem();
        if (item != itemFor(firstItem.getValue()) && item != itemFor(secondItem.getValue())) return false;
        ItemStack offhand = mc.player.getOffHandStack();
        return !Objects.equals(slot.getStack().getName().getString(), offhand.getName().getString())
                || slot.getStack().getItem() != offhand.getItem();
    }

    private Comparator<Slot> selectorComparator() {
        return Comparator
                .comparingInt((Slot slot) -> (slot.id >= 36 && slot.id <= 44) ? 0 : 1)
                .thenComparingInt(slot -> slot.id);
    }

    private Item itemFor(String value) {
        return switch (value) {
            case "Head"   -> Items.PLAYER_HEAD;
            case "Gapple" -> Items.GOLDEN_APPLE;
            case "Shield" -> Items.SHIELD;
            default       -> Items.TOTEM_OF_UNDYING;
        };
    }

    private boolean phaseReady() {
        long now = System.currentTimeMillis();
        if (now - lastPhaseTime >= legitDelay.getValue().longValue()) {
            lastPhaseTime = now;
            return true;
        }
        return false;
    }

    private boolean isLegitPhase(SwapPhase phase) {
        return switch (phase) {
            case ST_OPEN_INV, ST_MOVE_CURSOR, ST_SWAP_OFFHAND, ST_CLOSE_INV -> true;
            default -> false;
        };
    }

    private void resetState() {
        swapPhase    = SwapPhase.READY;
        targetSlot   = null;
        targetSlotId = -1;
        lastPhaseTime = 0L;
        keysOverridden = false;
    }

    public boolean isSwapping() {
        return swapPhase != SwapPhase.READY;
    }

    @Override
    public void onDisable() {
        resetState();
        if (mc.currentScreen instanceof AutoSwapSelectorScreen) {
            mc.setScreen(null);
        }
        super.onDisable();
    }

    public static final class SwapCandidate {
        private final int slotId;
        private final ItemStack stack;
        private final String displayName;

        public SwapCandidate(Slot slot) {
            this.slotId = slot.id;
            this.stack = slot.getStack().copy();
            this.displayName = stack.getName().getString();
        }

        public int getSlotId()       { return slotId; }
        public ItemStack getStack()  { return stack; }
        public String getDisplayName() { return displayName; }
    }
}
