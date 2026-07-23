package dev.ethereal.client.features.modules.player;

import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.other.MovementInputEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.player.Cooldowns;
import dev.ethereal.api.utils.player.DirectionalInput;
import dev.ethereal.api.utils.player.InventoryFlowManager;
import dev.ethereal.api.utils.player.InventoryActionUtil;
import dev.ethereal.api.utils.player.InventoryResult;
import dev.ethereal.api.utils.player.InventoryTask;
import dev.ethereal.api.utils.player.InventoryToolkit;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Box;

@ModuleRegister(name = "Auto Totem", category = Category.COMBAT, bind = -1)
@Environment(EnvType.CLIENT)
public class AutoTotemModule extends Module {
    @Getter private static final AutoTotemModule instance = new AutoTotemModule();

    private enum Phase {
        READY,
        STOPPING,
        WAIT_STOP,
        SWAP_TOTEM,
        WAIT_SWAP,
        CLOSE_INVENTORY,
        RESTORE_SLOT,
        RESTORING,
        FINISH,
        ST_OPEN_INV,
        ST_MOVE_CURSOR,
        ST_SWAP_OFFHAND,
        ST_CLOSE_INV
    }

    private final SliderSetting healthThreshold = new SliderSetting("Health")
            .value(4.5f).range(1.0f, 20.0f).step(0.5f);
    private final SliderSetting elytraHealth = new SliderSetting("Elytra Health")
            .value(8.5f).range(1.0f, 20.0f).step(0.5f);
    private final SliderSetting crystalDistance = new SliderSetting("Crystal Distance")
            .value(4.0f).range(1.0f, 6.0f).step(1.0f);
    private final ModeSetting mode = new ModeSetting("Mode")
            .value("Default").values("Default", "Legit");
    private final SliderSetting legitDelay = new SliderSetting("Delay")
            .value(0.0f).range(0.0f, 200.0f).step(10.0f)
            .setVisible(() -> mode.is("Legit"));
    private final BooleanSetting fallCheck = new BooleanSetting("Fall").value(true);
    private final BooleanSetting saveTalismans = new BooleanSetting("Save Enchanted").value(true);
    private final BooleanSetting returnItem = new BooleanSetting("Return Item").value(true);
    private final SliderSetting eatInterruptHealth = new SliderSetting("Eat Interrupt")
            .value(2.0f).range(0.5f, 10.0f).step(0.5f);

    private Phase phase = Phase.READY;
    private int savedSlot = -1;
    private int totemSlot = -1;
    private int tickCounter = 0;
    private boolean keysOverridden = false;

    private ItemStack previousOffhandStack = ItemStack.EMPTY;
    private int previousOffhandSlot = -1;
    private boolean pendingOffhandReturn = false;

    private int legitTargetSlot = -1;
    private boolean clearReturnAfterSwap = false;
    private long lastPhaseTime = 0L;

    public AutoTotemModule() {
        addSettings(healthThreshold, elytraHealth, crystalDistance, mode, legitDelay, fallCheck,
                saveTalismans, returnItem, eatInterruptHealth);
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent event) {
        if (phase == Phase.STOPPING || phase == Phase.WAIT_STOP
                || phase == Phase.SWAP_TOTEM || phase == Phase.WAIT_SWAP) {
            event.setDirectionalInput(DirectionalInput.NONE);
            event.setJump(false);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            resetState();
            return;
        }

        if (phase != Phase.READY) {
            execute();
            return;
        }

        if (!InventoryFlowManager.script.isFinished()) return;

        boolean shouldEquip = shouldEquipTotemNow();
        if (shouldEquip) {
            tryEquipTotem();
        }

        if (phase == Phase.READY && returnItem.getValue() && pendingOffhandReturn
                && !shouldEquip && isTotemInOffhand()) {
            if (mode.is("Legit")) {
                attemptReturnPreviousItemLegit();
            } else {
                attemptReturnPreviousItem();
            }
        }
    }

    private boolean shouldEquipTotemNow() {
        if (Cooldowns.getLeftMs(Items.TOTEM_OF_UNDYING) > 0) return false;

        float health = mc.player.getHealth();
        if (mc.player.isUsingItem()) {
            return health <= eatInterruptHealth.getValue();
        }

        if (mc.player.isGliding() && health <= elytraHealth.getValue()) return true;
        if (health <= healthThreshold.getValue()) return true;
        if (fallCheck.getValue() && mc.player.fallDistance > 10) return true;
        return getClosestCrystalDistance() <= crystalDistance.getValue();
    }

    private void tryEquipTotem() {
        if (phase != Phase.READY || isTotemInOffhand() || mc.currentScreen != null) return;
        if (Cooldowns.getLeftMs(Items.TOTEM_OF_UNDYING) > 0) return;

        savedSlot = mc.player.getInventory().selectedSlot;

        InventoryResult hotbar = InventoryToolkit.findItemInHotBar(Items.TOTEM_OF_UNDYING);
        if (hotbar.found()) {
            totemSlot = hotbar.slot() + 36;
            rememberOffhandForReturn(totemSlot);
            equipByMode(totemSlot, false);
            return;
        }

        InventoryResult inv = saveTalismans.getValue()
                ? findTotemWithSaveTalismans()
                : InventoryToolkit.findItemInInventory(Items.TOTEM_OF_UNDYING);

        if (inv.found()) {
            totemSlot = inv.slot();
            if (totemSlot >= 0 && totemSlot <= 8) totemSlot += 36;
            rememberOffhandForReturn(totemSlot);
            equipByMode(totemSlot, false);
        }
    }

    private void equipByMode(int sourceSlot, boolean clearReturnOnFinish) {
        clearReturnAfterSwap = clearReturnOnFinish;
        if (mode.is("Legit")) {
            legitTargetSlot = sourceSlot;
            lastPhaseTime = 0L;
            phase = Phase.ST_OPEN_INV;
        } else {
            totemSlot = sourceSlot;
            tickCounter = 0;
            keysOverridden = false;
            phase = Phase.STOPPING;
        }
    }

    private void execute() {
        boolean isLegitPhase = phase == Phase.ST_OPEN_INV || phase == Phase.ST_MOVE_CURSOR
                || phase == Phase.ST_SWAP_OFFHAND || phase == Phase.ST_CLOSE_INV;

        if (mc.currentScreen != null && !isLegitPhase) {
            resetState();
            return;
        }

        switch (phase) {
            case STOPPING -> {
                mc.player.setSprinting(false);
                keysOverridden = true;
                tickCounter++;
                if (tickCounter >= 1) { phase = Phase.WAIT_STOP; tickCounter = 0; }
            }
            case WAIT_STOP -> {
                tickCounter++;
                double vx = Math.abs(mc.player.getVelocity().x);
                double vz = Math.abs(mc.player.getVelocity().z);
                if ((vx < 0.03 && vz < 0.03) || tickCounter >= 3) {
                    phase = Phase.SWAP_TOTEM; tickCounter = 0;
                }
            }
            case SWAP_TOTEM -> {
                if (totemSlot < 0) { resetState(); return; }
                InventoryToolkit.clickSlot(totemSlot, 40, SlotActionType.SWAP);
                phase = Phase.WAIT_SWAP; tickCounter = 0;
            }
            case WAIT_SWAP -> {
                tickCounter++;
                if (isTotemInOffhand() || tickCounter >= 2) {
                    phase = Phase.CLOSE_INVENTORY; tickCounter = 0;
                }
            }
            case CLOSE_INVENTORY -> {
                tickCounter++;
                if (tickCounter >= 1) {
                    InventoryTask.closeScreen(true);
                    phase = Phase.RESTORE_SLOT; tickCounter = 0;
                }
            }
            case RESTORE_SLOT -> {
                tickCounter++;
                if (tickCounter >= 1) {
                    InventoryToolkit.switchTo(savedSlot);
                    phase = Phase.RESTORING; tickCounter = 0;
                }
            }
            case RESTORING -> {
                tickCounter++;
                if (tickCounter >= 1) {
                    keysOverridden = false;
                    phase = Phase.FINISH;
                }
            }
            case FINISH -> finishState();

            case ST_OPEN_INV -> {
                if (!phaseReady()) return;
                mc.setScreen(new InventoryScreen(mc.player));
                phase = Phase.ST_MOVE_CURSOR;
            }
            case ST_MOVE_CURSOR -> {
                if (!phaseReady()) return;
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
                InventoryActionUtil.moveCursorToSlot(screen, legitTargetSlot);
                phase = Phase.ST_SWAP_OFFHAND;
            }
            case ST_SWAP_OFFHAND -> {
                if (!phaseReady()) return;
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
                screen.keyPressed(mc.options.swapHandsKey.getDefaultKey().getCode(), 0, 0);
                phase = Phase.ST_CLOSE_INV;
            }
            case ST_CLOSE_INV -> {
                if (!phaseReady()) return;
                InventoryActionUtil.closeCurrentScreenWithInventoryKey();
                phase = Phase.FINISH;
            }
            case READY -> {}
        }
    }

    private boolean phaseReady() {
        long now = System.currentTimeMillis();
        if (now - lastPhaseTime >= legitDelay.getValue().longValue()) {
            lastPhaseTime = now;
            return true;
        }
        return false;
    }

    private void rememberOffhandForReturn(int swapSourceSlot) {
        if (!returnItem.getValue()) { clearReturnData(); return; }
        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand == null || offhand.isEmpty()) { clearReturnData(); return; }
        previousOffhandStack = offhand.copy();
        previousOffhandSlot = swapSourceSlot;
        pendingOffhandReturn = true;
    }

    private void attemptReturnPreviousItem() {
        if (!returnItem.getValue() || !pendingOffhandReturn) return;
        if (previousOffhandStack == null || previousOffhandStack.isEmpty()) { clearReturnData(); return; }
        int slot = findPreviousOffhandSlot();
        if (slot != -1) InventoryToolkit.clickSlot(slot, 40, SlotActionType.SWAP);
        clearReturnData();
    }

    private void attemptReturnPreviousItemLegit() {
        if (!returnItem.getValue() || !pendingOffhandReturn || phase != Phase.READY) return;
        if (previousOffhandStack == null || previousOffhandStack.isEmpty()) { clearReturnData(); return; }
        int slot = findPreviousOffhandSlot();
        if (slot == -1) { clearReturnData(); return; }
        equipByMode(slot, true);
    }

    private int findPreviousOffhandSlot() {
        if (previousOffhandStack.isEmpty()) return -1;
        Item item = previousOffhandStack.getItem();
        if (isSlotContainsItem(previousOffhandSlot, item)) return previousOffhandSlot;
        InventoryResult found = InventoryToolkit.findInInventory(stack -> stack.isOf(item));
        return found.found() ? found.slot() : -1;
    }

    private boolean isSlotContainsItem(int slot, Item item) {
        if (slot < 0 || mc.player == null) return false;
        if (slot >= mc.player.currentScreenHandler.slots.size()) return false;
        ItemStack stack = mc.player.currentScreenHandler.getSlot(slot).getStack();
        return !stack.isEmpty() && stack.isOf(item);
    }

    private InventoryResult findTotemWithSaveTalismans() {
        InventoryResult nonEnchanted = InventoryToolkit.findInInventory(
                stack -> stack.isOf(Items.TOTEM_OF_UNDYING) && !stack.hasEnchantments());
        if (nonEnchanted.found()) return nonEnchanted;
        return InventoryToolkit.findItemInInventory(Items.TOTEM_OF_UNDYING);
    }

    private double getClosestCrystalDistance() {
        double minDist = Double.MAX_VALUE;
        Box box = mc.player.getBoundingBox().expand(crystalDistance.getValue());
        for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, box, e -> true)) {
            minDist = Math.min(minDist, mc.player.getPos().distanceTo(crystal.getPos()));
        }
        return minDist;
    }

    private boolean isTotemInOffhand() {
        return mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    private void clearReturnData() {
        previousOffhandStack = ItemStack.EMPTY;
        previousOffhandSlot = -1;
        pendingOffhandReturn = false;
    }

    private void finishState() {
        if (clearReturnAfterSwap) clearReturnData();
        resetState();
    }

    private void resetState() {
        keysOverridden = false;
        phase = Phase.READY;
        savedSlot = -1;
        totemSlot = -1;
        tickCounter = 0;
        legitTargetSlot = -1;
        clearReturnAfterSwap = false;
        lastPhaseTime = 0L;
    }

    public boolean isSwapping() {
        return phase != Phase.READY;
    }

    @Override
    public void onDisable() {
        resetState();
        clearReturnData();
        super.onDisable();
    }
}