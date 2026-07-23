package dev.ethereal.api.utils.rotation.manager;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.rotation.RotationUtil;

@Getter
@Setter
public class Rotation {
    private float yaw;
    private float pitch;

    public static final Rotation DEFAULT = new Rotation(0f, 0f);

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Rotation clamp() {
        this.pitch = MathHelper.clamp(this.pitch, -90f, 90f);
        return this;
    }

    /** Angular distance to target (hypot of yaw+pitch deltas). */
    public float getDelta(Rotation target) {
        float yawDelta   = MathHelper.wrapDegrees(target.getYaw()   - yaw);
        float pitchDelta = target.getPitch() - pitch;
        return (float) Math.hypot(yawDelta, pitchDelta);
    }

    /** Smooth interpolation between this and target. */
    public Rotation interpolate(Rotation target, float factor) {
        float iYaw   = MathHelper.wrapDegrees(yaw + MathHelper.wrapDegrees(target.getYaw() - yaw) * factor);
        float iPitch = MathHelper.clamp(pitch + (target.getPitch() - pitch) * factor, -90f, 90f);
        return new Rotation(iYaw, iPitch);
    }

    /** GCD sensitivity adjustment — keeps rotations legit. */
    public Rotation adjustSensitivity() {
        double gcd = MouseUtil.getGCD();
        Rotation prev = RotationManager.getInstance().getServerRotation();
        float adjYaw   = adjustAxis(yaw,   prev.yaw,   gcd);
        float adjPitch = adjustAxis(pitch, prev.pitch, gcd);
        return new Rotation(adjYaw, MathHelper.clamp(adjPitch, -90f, 90f));
    }

    private float adjustAxis(float value, float previous, double gcd) {
        float delta = value - previous;
        return previous + Math.round(delta / gcd) * (float) gcd;
    }

    public Vec3d getVector() {
        float f = pitch * 0.017453292f;
        float g = -yaw  * 0.017453292f;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public Rotation rotationDeltaTo(Rotation target) {
        return RotationUtil.calculateDelta(this, target);
    }

    @Override
    public String toString() {
        return "Rotation(yaw=" + yaw + ", pitch=" + pitch + ")";
    }

    public record VecRotation(Rotation rotation, Vec3d vec) {
        @Override
        public String toString() {
            return "VecRotation(rotation=" + rotation + ", vec=" + vec + ")";
        }
    }
}
