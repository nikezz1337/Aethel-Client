package dev.ethereal.client.features.modules.movement.spider;

import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.client.features.modules.combat.AuraModule;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

@ModuleRegister(name = "Spider", category = Category.MOVEMENT)
public class SpiderModule extends Module {
    @Getter
    private static final SpiderModule instance = new SpiderModule();

    float ticks;

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;
            if (!mc.player.horizontalCollision) return;

            boolean bucketPulse = ticks % 5 == 0;
            boolean boostPulse = ticks % 4 != 3;

            int bucketSlot = findWaterBucketSlot();
            if (bucketSlot == -1) return;


            if (mc.player.getInventory().selectedSlot != bucketSlot) {
                mc.player.getInventory().selectedSlot = bucketSlot;
            }

            double y = boostPulse ? 0.18 : 0.03;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.setVelocity(
                    mc.player.getVelocity().x,
                    y,
                    mc.player.getVelocity().z
            );
            ticks++;
        }));

        addEvents(updateEvent);
    }

    private int findWaterBucketSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                return i;
            }
        }
        return -1;
    }
}
