package antileak.base.client.modules.impl.combat;

import com.adl.nativeprotect.Native;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.impl.movement.InventoryWalk;
import antileak.base.client.modules.settings.implement.BooleanSetting;
import antileak.base.client.modules.settings.implement.FloatSetting;

@Native
public class AutoTotem extends Module {

    public static AutoTotem INSTANCE = new AutoTotem();

    private final FloatSetting health = new FloatSetting("Здоровье", 4, 1, 20, 0.1f);
    private final FloatSetting healthOnElytra = new FloatSetting("Здоровье на элитре", 11, 1, 20, 0.1f);

    private final BooleanSetting crystalsCheck = new BooleanSetting("Работать на кристалы", false);
    private final FloatSetting crystalDistance = new FloatSetting("Дистанция до кристала", 8, 1, 20, 1)
            .visible(crystalsCheck::isState);

    private final BooleanSetting reactCrystalHand = new BooleanSetting("Кристал в руке", false);

    private final BooleanSetting reactFall = new BooleanSetting("Падение", false);
    private final FloatSetting fallDistance = new FloatSetting("Блоков падения", 3, 1, 20, 1)
            .visible(reactFall::isState);
    private final BooleanSetting bypassRW = new BooleanSetting("Обход RW", true);

    private final BooleanSetting reactNaked = new BooleanSetting("Без брони", false);
    private final FloatSetting nakedHealth = new FloatSetting("HP без части брони", 10, 1, 20, 1)
            .visible(reactNaked::isState);

    private int swapBackSlot = -1;
    private float cooldownTicks;

    public AutoTotem() {
        super("AutoTotem", "Автоматически берёт тотем в опасности", ModuleCategory.COMBAT);
        addSettings(
                health,
                healthOnElytra,
                crystalsCheck,
                crystalDistance,
                reactCrystalHand,
                reactFall,
                fallDistance,
                bypassRW,
                reactNaked,
                nakedHealth
        );
    }

    @EventLink
    public void onUpdate(final EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        updateSwap();
    }

    private boolean condition() {
        if (mc.player.isCreative() || mc.player.isSpectator()) return false;

        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        if (hp <= health.getValue().floatValue()) return true;

        if (hp <= healthOnElytra.getValue().floatValue()
                && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA)
            return true;

        if (crystalsCheck.isState()) {
            for (var entity : mc.world.getEntities()) {
                if (!(entity instanceof EndCrystalEntity)) continue;
                double dist = mc.player.getEyePos().distanceTo(entity.getBoundingBox().getCenter());
                if (dist <= crystalDistance.getValue().floatValue()) return true;
            }
        }

        if (reactCrystalHand.isState()) {
            for (var entity : mc.world.getEntities()) {
                if (!(entity instanceof PlayerEntity player)) continue;
                if (player == mc.player) continue;
                if (mc.player.distanceTo(player) > 10) continue;
                if (player.getMainHandStack().getItem() == Items.END_CRYSTAL
                        || player.getOffHandStack().getItem() == Items.END_CRYSTAL)
                    return true;
            }
        }

        if (reactFall.isState()) {
            if (mc.player.fallDistance >= fallDistance.getValue().floatValue()) return true;
        }

        if (reactNaked.isState()) {
            boolean missingArmor = mc.player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                    || mc.player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                    || mc.player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                    || mc.player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
            if (missingArmor && hp <= nakedHealth.getValue().floatValue()) return true;
        }

        return false;
    }

    private boolean hasTotemInOffhand() {
        return mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    private int findTotemSlot() {
        for (int i = 9; i <= 44; i++) {
            var stack = mc.player.playerScreenHandler.getSlot(i).getStack();
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }

    private void swapToOffhand(int screenSlot) {
        if (screenSlot >= 36 && screenSlot <= 44) {
            int hotbarIndex = screenSlot - 36;
            doSwap(() -> mc.interactionManager.clickSlot(0, 45, hotbarIndex, SlotActionType.SWAP, mc.player));
        } else {
            doSwap(() -> mc.interactionManager.clickSlot(0, screenSlot, 40, SlotActionType.SWAP, mc.player));
        }
    }

    private void updateSwap() {
        boolean cond = condition();

        if (cond && !hasTotemInOffhand()) {
            int totemSlot = findTotemSlot();
            if (totemSlot == -1) return;

            if (swapBackSlot == -1) {
                var offhand = mc.player.getOffHandStack();
                swapBackSlot = offhand.isEmpty() ? -2 : totemSlot;
            }

            swapToOffhand(totemSlot);
        }

        if (!cond && swapBackSlot != -1) {
            if (swapBackSlot >= 9) {
                int ret = swapBackSlot;
                swapBackSlot = -1;
                swapToOffhand(ret);
            } else {
                int totemSlot = findTotemSlot();
                swapBackSlot = -1;
                if (totemSlot != -1 && hasTotemInOffhand()) {
                    swapToOffhand(totemSlot);
                }
            }
            cooldownTicks = 3;
        }
    }

    private void doSwap(Runnable action) {
        InventoryWalk iw = InventoryWalk.INSTANCE;
        boolean needsBypass = iw != null && iw.isEnable() && mc.currentScreen instanceof InventoryScreen;

        if (needsBypass && bypassRW.isState()) {
            iw.swapBypass = true;
        }

        action.run();

        if (needsBypass && bypassRW.isState()) {
            iw.swapBypass = false;
        }
    }

    @Override
    public void onDisable() {
        cooldownTicks = 0;
        swapBackSlot = -1;
        super.onDisable();
    }
}
