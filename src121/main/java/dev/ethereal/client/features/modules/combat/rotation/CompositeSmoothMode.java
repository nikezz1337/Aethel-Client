package dev.ethereal.client.features.modules.combat.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class CompositeSmoothMode extends AngleSmoothMode {
    private final AngleSmoothMode[] modes;

    public CompositeSmoothMode(String name, AngleSmoothMode... modes) {
        super(name);
        this.modes = modes;
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        Angle nextAngle = currentAngle;
        Angle nextTarget = targetAngle;
        for (AngleSmoothMode mode : modes) {
            nextAngle = mode.limitAngleChange(nextAngle, nextTarget, vec3d, entity);
            nextTarget = nextAngle;
        }
        return nextAngle;
    }

    @Override
    public Vec3d randomValue() {
        Vec3d random = Vec3d.ZERO;
        for (AngleSmoothMode mode : modes) {
            random = random.add(mode.randomValue());
        }
        return random;
    }

    @Override
    public void onAttack() {
        for (AngleSmoothMode mode : modes) {
            mode.onAttack();
        }
    }

    @Override
    public void onMiss() {
        for (AngleSmoothMode mode : modes) {
            mode.onMiss();
        }
    }
}
