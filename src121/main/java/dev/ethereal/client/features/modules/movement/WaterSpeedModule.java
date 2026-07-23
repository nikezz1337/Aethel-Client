package dev.ethereal.client.features.modules.movement;

import com.jcraft.jorbis.Block;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.other.TextUtil;
import dev.ethereal.api.utils.player.MoveUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import lombok.Getter;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.EquipmentSlot;

@ModuleRegister(name = "WaterSpeed", category = Category.MOVEMENT)
public class WaterSpeedModule extends Module {

    @Getter
    private static final WaterSpeedModule instance = new WaterSpeedModule();

    private final TimerUtil timer = new TimerUtil();

    public WaterSpeedModule() {
        addSettings();
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
            if (!mc.player.isTouchingWater()) return;
            if (PlayerUtil.getBlock(0, 1, 0) != Blocks.ICE) return;

            boolean forward = mc.options.forwardKey.isPressed();
            boolean hasDepthStrider = false;
            ItemStack boots = mc.player.getInventory().getArmorStack(EquipmentSlot.FEET.getEntitySlotId());

            if (!boots.isEmpty()) {
                String bootsString = boots.toString().toLowerCase();
                if (bootsString.contains("depth_strider") ||
                        bootsString.contains("depth strider") ||
                        bootsString.contains("aqua_affinity") ||
                        bootsString.contains("aqua affinity")) {
                    hasDepthStrider = true;
                }

                if (!hasDepthStrider) {
                    String bootsName = boots.getName().getString().toLowerCase();
                    if (bootsName.contains("depth") || bootsName.contains("aqua") || bootsName.contains("water")) {
                        hasDepthStrider = true;
                    }
                }
            }

            ItemStack offhand = mc.player.getOffHandStack();
            boolean sfera = !offhand.isEmpty() && offhand.getItem() == Items.PLAYER_HEAD;

            float speed;

            if (hasDepthStrider) {
                if (sfera) {
                    speed = 1.054f;
                } else {
                    speed = 1.052f;
                }
            } else {
                speed = 1.054f;
            }

            if (forward) {
                mc.player.setVelocity(
                        mc.player.getVelocity().x * speed,
                        mc.player.getVelocity().y,
                        mc.player.getVelocity().z * speed
                );
            }
    }

    @Override
    public void onEnable() {
        timer.reset();
    }

    @Override
    public void onDisable() {
    }
}