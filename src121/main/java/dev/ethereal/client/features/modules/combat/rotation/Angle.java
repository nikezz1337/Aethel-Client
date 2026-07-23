package dev.ethereal.client.features.modules.combat.rotation;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.system.interfaces.QuickImports;

import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

@Getter
@Setter
@ToString
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Angle implements QuickImports {
    public static Angle DEFAULT = new Angle(0, 0);
    float yaw, pitch;

    public static Angle cameraAngle() {
        return new Angle(mc.player.getYaw(), mc.player.getPitch());
    }

    public Angle adjustSensitivity() {
        double d4 = mc.options.getMouseSensitivity().getValue() * 0.8D;
        double gcd = d4 * d4 * d4 * 8.0D * 0.15;

        Angle previousAngle = new Angle(mc.player.prevYaw, mc.player.prevPitch);

        float adjustedYaw = adjustAxis(yaw, previousAngle.yaw, gcd);
        float adjustedPitch = adjustAxis(pitch, previousAngle.pitch, gcd);

        return new Angle(adjustedYaw, MathHelper.clamp(adjustedPitch, -90f, 90f));
    }

    public Angle random(float f) {
        return new Angle(
                yaw + (float) (ThreadLocalRandom.current().nextFloat(-f, f)),
                pitch + (float) (ThreadLocalRandom.current().nextFloat(-f, f))
        );
    }

    private float adjustAxis(float axisValue, float previousValue, double gcd) {
        float delta = axisValue - previousValue;
        return previousValue + Math.round(delta / gcd) * (float) gcd;
    }

    public final Vec3d toVector() {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public Angle addYaw(float yaw) {
        return new Angle(this.yaw + yaw, this.pitch);
    }

    public Angle addPitch(float pitch) {
        this.pitch = MathHelper.clamp(this.pitch + pitch, -90, 90);
        return this;
    }

    public Angle of(Angle angle) {
        return new Angle(angle.getYaw(), angle.getPitch());
    }

    @ToString
    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class VecRotation {
        final Angle angle;
        final Vec3d vec;
    }
}
