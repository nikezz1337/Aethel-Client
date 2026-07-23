package ru.zenith.implement.features.modules.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.zenith.implement.features.modules.combat.killaura.rotation.Angle;

public class InstantSmoothMode extends AngleSmoothMode {
    public InstantSmoothMode() {
        super("Instant");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {

        return targetAngle;
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}
