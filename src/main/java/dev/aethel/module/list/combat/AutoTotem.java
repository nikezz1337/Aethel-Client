package dev.aethel.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import dev.aethel.event.list.EventPlayerUpdate;
import dev.aethel.event.list.EventPopTotem;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.module.list.combat.aura.StopWatch;
import dev.aethel.util.player.other.InventoryUtil;
import dev.aethel.util.text.ValueUnit;

@ModuleInformation(moduleName = "Auto Totem", moduleCategory = ModuleCategory.COMBAT, moduleDesc = "Авто-подставляет тотем бессмертия")
public class AutoTotem extends Module {
    private final SliderSetting health = new SliderSetting("Здоровье", new ValueUnit("ХП", 1.0), 4, 1, 20, 0.1f);
    private final SliderSetting healthOnElytra = new SliderSetting("Здоровье на элитре", new ValueUnit("ХП", 1.0), 11, 1, 20, 0.1f);
    private final BooleanSetting crystalsCheck = new BooleanSetting("Работать на кристалы", false);

    private int oldSlot = -1;
    private ItemStack oldOffhandItem = ItemStack.EMPTY;

    private float cooldownTicks;
    private final StopWatch totemStopWatch = new StopWatch();

    @Subscribe
    private void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null) return;
        if (cooldownTicks > 0) cooldownTicks--;
        update();
    }

    @Subscribe
    private void onPopTotem(EventPopTotem e) {
        if (mc.player == null || e.getPlayer() != mc.player) return;
        cooldownTicks = 5;
    }

    private boolean condition() {
        if (mc.player.isCreative() || mc.player.isSpectator()) return false;

        var crystalsNearby = false;
        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;
            if (mc.player.getY() >= entity.getY() && mc.player.getEyePos().distanceTo(entity.getBoundingBox().getCenter()) <= 8) {
                crystalsNearby = true;
                break;
            }
        }

        return (mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= health.getValue()
                || ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= healthOnElytra.getValue()
                    && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA)
                || (this.crystalsCheck.getValue() && crystalsNearby);
    }

    private void update() {
        if (mc.player == null || !mc.player.isAlive() || mc.world == null) {
            resetSwapBack();
            return;
        }

        int totemSlot = findTotemSlot();
        boolean totemInOffhand = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;

        // Свапаем тотем в оффхенд если нужно
        if (condition() && totemStopWatch.passed(300) && totemSlot != -1 && !totemInOffhand) {
            if (mc.currentScreen == null) {
                if (oldOffhandItem.isEmpty() && !mc.player.getOffHandStack().isEmpty()) {
                    oldOffhandItem = mc.player.getOffHandStack().copy();
                    oldSlot = totemSlot;
                }
                swapTotem(totemSlot);
                totemStopWatch.reset();
            }
        }

        // Возвращаем оригинальный предмет обратно в оффхенд когда не нужен тотем
        if (!condition() && oldSlot != -1 && !oldOffhandItem.isEmpty()) {
            if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                if (mc.currentScreen == null) {
                    swapTotem(oldSlot);
                    resetSwapBack();
                }
            } else {
                resetSwapBack();
            }
        }
    }

    private int findTotemSlot() {
        // Сначала ищем без зачарований
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING && !stack.hasEnchantments()) {
                return InventoryUtil.toScreenSlot(i);
            }
        }
        // Потом любой тотем
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return InventoryUtil.toScreenSlot(i);
            }
        }
        return -1;
    }

    private void swapTotem(int screenSlot) {
        InventoryUtil.swapBypass(() -> InventoryUtil.swapToOffhand(screenSlot), 100);
    }

    private void resetSwapBack() {
        oldOffhandItem = ItemStack.EMPTY;
        oldSlot = -1;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetSwapBack();
    }
}
