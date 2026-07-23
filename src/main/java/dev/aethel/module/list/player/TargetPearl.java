package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ModuleInformation(
        moduleName = "TargetPearl",
        moduleCategory = ModuleCategory.PLAYER,
        moduleDesc = "Авто-бросок жемчуга в ответ на жемчуг врага"
)
public class TargetPearl extends Module {

    private final Set<UUID> seenPearls = new HashSet<>();

    public final SliderSetting radius =
            new SliderSetting("Радиус", 30.0, 5.0, 100.0, 1.0);
    public final SliderSetting delay =
            new SliderSetting("Тики", 10.0, 1.0, 40.0, 1.0);

    private int lastThrowTick = 0;

    public TargetPearl() {
    }

    @Subscribe
    public void onUpdate(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        lastThrowTick++;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EnderPearlEntity pearl) {
                UUID pearlId = pearl.getUuid();
                if (seenPearls.contains(pearlId)) continue;
                if (pearl.age > 2) continue;

                Entity owner = pearl.getOwner();
                if (owner instanceof PlayerEntity player && player != mc.player) {
                    if (mc.player.distanceTo(player) > radius.getFloatValue()) continue;
                    if (lastThrowTick < delay.getIntValue()) continue;

                    int pearlSlot = findPearlSlot();
                    if (pearlSlot == -1) continue;

                    float[] angles = getAnglesFromVelocity(
                            pearl.getVelocity().x,
                            pearl.getVelocity().y,
                            pearl.getVelocity().z);
                    smoothLookAt(angles[0], angles[1]);
                    throwPearl(pearlSlot);

                    lastThrowTick = 0;
                    seenPearls.add(pearlId);
                }
            }
        }
    }

    private void throwPearl(int pearlSlot) {
        int oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = pearlSlot;
        usePearl();
        mc.player.getInventory().selectedSlot = oldSlot;
    }

    private void usePearl() {
        ItemStack stack = mc.player.getMainHandStack();
        if (stack.getItem() instanceof EnderPearlItem) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
    }

    private int findPearlSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof EnderPearlItem) {
                return i;
            }
        }
        return -1;
    }

    private float[] getAnglesFromVelocity(double vx, double vy, double vz) {
        float yaw = (float) (Math.toDegrees(Math.atan2(vz, vx)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(vy, Math.sqrt(vx * vx + vz * vz)));
        return new float[]{yaw, pitch};
    }

    private void smoothLookAt(float targetYaw, float targetPitch) {
        mc.player.setYaw(targetYaw);
        mc.player.setPitch(targetPitch);
    }

    @Override
    public void onDisable() {
        seenPearls.clear();
        lastThrowTick = 0;
        super.onDisable();
    }
}
