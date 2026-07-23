package dev.aethel.module.list.movement;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.event.list.WorldLoadEvent;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.util.base.Instance;
import net.minecraft.entity.effect.StatusEffects;

@ModuleInformation(
    moduleName = "Sprint",
    moduleCategory = ModuleCategory.MOVEMENT,
    moduleDesc = "Автоматически включает спринт"
)
public class Sprint extends Module {


    public int tickStop = 0;


    @Subscribe
    public void onWorldLoad(WorldLoadEvent e) {
        tickStop = 3;
    }

    private boolean canStartSprinting() {
        if (mc.player == null) return false;
        boolean hasBlindness = mc.player.hasStatusEffect(StatusEffects.BLINDNESS);
        return !mc.player.isSprinting() && mc.player.input.hasForwardMovement() && !hasBlindness && !mc.player.isGliding();
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        boolean horizontal = mc.player.horizontalCollision && !mc.player.collidedSoftly;
        boolean sneaking = mc.player.isSneaking() && !mc.player.isSwimming();

        if (tickStop > 0 || sneaking) {
            mc.player.setSprinting(false);
        } else if (canStartSprinting() && !horizontal && !mc.options.sprintKey.isPressed()) {
            mc.player.setSprinting(true);
        }
        tickStop--;
    }
}
