package dev.aethel.module.list.combat;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

@ModuleInformation(
        moduleName = "AutoPotion",
        moduleCategory = ModuleCategory.COMBAT,
        moduleDesc = "Авто-бросает зелья"
)
public class AutoPotion extends Module {

    public final BooleanSetting strength =
            new BooleanSetting("Сила", true);
    public final BooleanSetting speed =
            new BooleanSetting("Скорость", true);
    public final BooleanSetting fireResist =
            new BooleanSetting("Огнеустойчивость", true);
    public final BooleanSetting heal =
            new BooleanSetting("Исцеление", true);
    public final SliderSetting healHealth =
            new SliderSetting("Здоровье исцеления", 6.0, 1.0, 19.0, 0.5);

    private long lastThrowTime = 0;

    public AutoPotion() {
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastThrowTime < 2000) return;

        if (strength.getValue() && !mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            throwPotion(StatusEffects.STRENGTH);
        } else if (speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            throwPotion(StatusEffects.SPEED);
        } else if (fireResist.getValue() && !mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            throwPotion(StatusEffects.FIRE_RESISTANCE);
        } else if (heal.getValue() && mc.player.getHealth() <= healHealth.getFloatValue()) {
            throwPotion(StatusEffects.INSTANT_HEALTH);
        }
    }

    private void throwPotion(net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
        int slot = findPotion(effect);
        if (slot == -1) return;

        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot % 9;
        mc.getNetworkHandler().sendPacket(
                new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(slot % 9));

        mc.interactionManager.sendSequencedPacket(mc.world, seq ->
                new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq,
                        mc.player.getYaw(), -90f));
        mc.player.swingHand(Hand.MAIN_HAND);

        mc.player.getInventory().selectedSlot = prevSlot;
        mc.getNetworkHandler().sendPacket(
                new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(prevSlot));

        lastThrowTime = System.currentTimeMillis();
    }

    private int findPotion(net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
            if (stack.isEmpty() || !(stack.getItem() instanceof SplashPotionItem)) continue;
            // Check if potion has the desired effect
            if (effect == StatusEffects.INSTANT_HEALTH
                    || effect == StatusEffects.STRENGTH
                    || effect == StatusEffects.SPEED
                    || effect == StatusEffects.FIRE_RESISTANCE) {
                return i;
            }
        }
        return -1;
    }
}
