package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.api.module.Module;
import dev.ethereal.api.utils.animation.Easing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;

public class RotationStrategy {
    public static final RotationStrategy TARGET = new RotationStrategy(RotationMode.SMOOTH, true, false);

    public RotationStrategy(RotationMode mode, boolean moveCorrection, boolean freeMoveCorrection) {}
    public RotationStrategy(RotationMode mode, boolean moveCorrection) {}
    public RotationStrategy clientLook(boolean value) { return this; }
    public RotationStrategy ticksUntilReset(int ticks) { return this; }
    public RotationStrategy easing(Easing typeInterp) { return this; }
    public RotationStrategy easingStrength(float strength) { return this; }
    
    public RotationPlan createRotationPlan(Rotation rotation, Module provider) {
        return new RotationPlan();
    }
    
    public RotationPlan createRotationPlan(Rotation rotation, Vec3d vec, Entity entity, Module provider) {
        return new RotationPlan();
    }
}
