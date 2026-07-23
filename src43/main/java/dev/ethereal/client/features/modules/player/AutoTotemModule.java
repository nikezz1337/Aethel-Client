package dev.ethereal.client.features.modules.player;

import java.util.stream.IntStream;

import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.other.SlownessManager;
import dev.ethereal.api.utils.other.StopWatch;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.player.MoveUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.client.features.modules.movement.InventoryMoveModule;
import dev.ethereal.client.features.modules.movement.noslow.NoSlowModule;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.MaceItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

@ModuleRegister(
        name = "Auto Totem",
        category = Category.COMBAT,
        bind = -1
)
@Environment(EnvType.CLIENT)

public class AutoTotemModule extends Module {
    private final MultiBooleanSetting options = new MultiBooleanSetting("Настройки").value(
            new BooleanSetting("Здоровье с элитрами").value(false),
            new BooleanSetting("Динамит").value(false),
            new BooleanSetting("Падение").value(false),
            new BooleanSetting("Булава").value(false),
            new BooleanSetting("Эндер-кристалл").value(false)
    );
    private final SliderSetting health = new SliderSetting("Здоровье").value(4.0F).range(1.0F, 20.0F).step(0.5F);
    private final SliderSetting elytraHealth = new SliderSetting("Здоровье на элитре").value(9.0F).range(0.0F, 20.0F).step(0.5F)
            .setVisible(() -> this.options.isEnabled("Здоровье с элитрами"));
    private final SliderSetting crystalDistance = new SliderSetting("Дистанция до кристалла").value(4.0F).range(1.0F, 10.0F).step(1.0F)
            .setVisible(() -> this.options.isEnabled("Эндер-кристалл"));
    private final SliderSetting tntDistance = new SliderSetting("Дистанция до динамита").value(30.0F).range(3.0F, 50.0F).step(1.0F)
            .setVisible(() -> this.options.isEnabled("Динамит"));
    private final BooleanSetting noBall = new BooleanSetting("Не свапать если шар").value(false);
    private int oldSlot = -1;
    private ItemStack oldOffhandItem = ItemStack.EMPTY;
    int nonEnchantedTotems;

    public AutoTotemModule() {
        addSettings(options, health, elytraHealth, crystalDistance, tntDistance, noBall);
    }

    @Getter
    private static final AutoTotemModule instance = new AutoTotemModule();

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            update();
        }));

        addEvents(tickEvent);
    }

    private void update() {
        if (mc.player != null && mc.player.isAlive() && mc.world != null) {
            this.nonEnchantedTotems = (int)IntStream.range(0, 36)
                    .mapToObj(i -> mc.player.getInventory().getStack(i))
                    .filter(s -> s.getItem() == Items.TOTEM_OF_UNDYING && !s.hasEnchantments())
                    .count();
            this.bypass();
        } else {
            this.resetSwapBack();
        }
    }

    private void bypass() {
        int slot = this.findNonEnchantedTotemSlot();
        boolean totemInHand = this.isTotemInHands();
        if (this.canSwap()) {
            if (slot >= 0 && !totemInHand) {
                if (mc.currentScreen == null) {
                    if (this.oldOffhandItem.isEmpty() && !mc.player.getOffHandStack().isEmpty()) {
                        this.oldOffhandItem = mc.player.getOffHandStack().copy();
                        this.oldSlot = slot;
                    }

                    long delay = PlayerUtil.isST() ? 150L : 1;
                    if (SlownessManager.isEnabled()) {
                        SlownessManager.applySlowness(delay, () -> InventoryUtil.swapToOffhand(slot));
                    } else {
                        InventoryUtil.swapToOffhand(slot);
                    }
                }
            }
        } else if (this.oldSlot != -1 && !this.oldOffhandItem.isEmpty()) {
            if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                if (mc.currentScreen == null) {
                    long delay = PlayerUtil.isST() ? 150L : 1L;
                    if (SlownessManager.isEnabled()) {
                        SlownessManager.applySlowness(delay, () -> {
                            InventoryUtil.swapToOffhand(oldSlot);
                            this.resetSwapBack();
                        });
                    } else {
                        InventoryUtil.swapToOffhand(oldSlot);
                        this.resetSwapBack();
                    }
                }
            } else {
                this.resetSwapBack();
            }
        }
    }

    private void resetSwapBack() {
        this.oldOffhandItem = ItemStack.EMPTY;
        this.oldSlot = -1;
    }

    private int findNonEnchantedTotemSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING && !stack.hasEnchantments()) {
                return i < 9 ? i + 36 : i;
            }
        }

        for (int ix = 0; ix < 36; ix++) {
            ItemStack stack = mc.player.getInventory().getStack(ix);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return ix < 9 ? ix + 36 : ix;
            }
        }

        return -1;
    }

    public boolean isTotemInHands() {
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();
        if (mainHand.getItem() == Items.TOTEM_OF_UNDYING) {
            return !mainHand.hasEnchantments() || this.nonEnchantedTotems <= 0;
        } else {
            return offHand.getItem() != Items.TOTEM_OF_UNDYING ? false : !offHand.hasEnchantments() || this.nonEnchantedTotems <= 0;
        }
    }

    private boolean canSwap() {
        boolean flag1 = this.elytraCheck();
        boolean flag2 = this.checkCrystal();
        boolean flag3 = this.checkTnt();
        boolean flag4 = this.checkFall();
        boolean flag6 = mc.player.getHealth() + this.getAbsorption() <= this.health.getValue();
        boolean flag7 = checkMace();
        return flag1 || flag2 || flag3 || flag4 || flag6 || flag7;
    }

    private boolean elytraCheck() {
        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        boolean elytra = chestStack.getItem() == Items.ELYTRA && this.options.isEnabled("Здоровье с элитрами");
        return elytra && this.checkHealth();
    }

    private boolean checkFall() {
        return this.options.isEnabled("Падение") && mc.player.fallDistance > 2.0;
    }

    private boolean checkHealth() {
        return mc.player.getHealth() + this.getAbsorption() <= this.elytraHealth.getValue();
    }

    private boolean checkCrystal() {
        if (!this.options.isEnabled("Эндер-кристалл")) {
            return false;
        } else {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) <= this.crystalDistance.getValue()) {
                    return !(mc.player.getOffHandStack().getItem() instanceof PlayerHeadItem) || !this.noBall.getValue();
                }
            }

            return false;
        }
    }

    private boolean checkMace() {
        if (!this.options.isEnabled("Булава")) {
            return false;
        }
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) {
                double yDiff = Math.abs(mc.player.getY() - entity.getY());
                if (yDiff > 25 && mc.player.distanceTo(entity) <= 10) {
                    return ((PlayerEntity) entity).getMainHandStack().getItem() instanceof MaceItem;
                }
            }
        }
        return false;
    }

    private boolean checkTnt() {
        if (!this.options.isEnabled("Динамит")) {
            return false;
        } else {
            for (Entity entity : mc.world.getEntities()) {
                float distance = mc.player.distanceTo(entity);
                if ((entity instanceof TntEntity || entity instanceof TntMinecartEntity) && distance <= this.tntDistance.getValue()) {
                    return true;
                }
            }

            return false;
        }
    }

    private float getAbsorption() {
        return mc.player.getAbsorptionAmount();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.resetSwapBack();
    }
}
