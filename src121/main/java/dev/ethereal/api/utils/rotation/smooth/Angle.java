package dev.ethereal.api.utils.rotation.smooth;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.system.interfaces.QuickImports;

@Getter
@Setter
@ToString
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Angle implements QuickImports {
    public static Angle DEFAULT = new Angle(0, 0);
    float yaw, pitch;

    public Angle adjustSensitivity() {
        float sensitivity = (float) (net.minecraft.client.MinecraftClient.getInstance().options.getMouseSensitivity().getValue() * 0.6 + 0.2);
        float gcd = sensitivity * sensitivity * sensitivity * 8.0F * 0.15F;

        float previousYaw = mc.player.prevYaw;
        float previousPitch = mc.player.prevPitch;

        float adjustedYaw = adjustAxis(yaw, previousYaw, gcd);
        float adjustedPitch = adjustAxis(pitch, previousPitch, gcd);

        return new Angle(adjustedYaw, MathHelper.clamp(adjustedPitch, -90.0F, 90.0F));
    }

    public Angle random(float amount) {
        return new Angle(
                yaw + (float) (Math.random() * amount * 2.0F - amount),
                pitch + (float) (Math.random() * amount * 2.0F - amount)
        );
    }

    private float adjustAxis(float axisValue, float previousValue, float gcd) {
        float delta = axisValue - previousValue;
        return previousValue + Math.round(delta / gcd) * gcd;
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

    public final Vec3d toVector() {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
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
