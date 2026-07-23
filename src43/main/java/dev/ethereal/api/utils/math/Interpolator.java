package dev.ethereal.api.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.util.math.MathHelper;

import java.security.SecureRandom;

@UtilityClass
public class Interpolator {

    public double lerp(double input, double target, double step) {
        return input + step * (target - input);
    }

    public float lerp(float input, float target, double step) {
        return (float) (input + step * (target - input));
    }

    public int lerp(int input, int target, double step) {
        return (int) (input + step * (target - input));
    }

    public float randomLerp(float min, float max) {
        return Interpolator.lerp(max, min, new SecureRandom().nextFloat());
    }

    public float randomLera(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }
}
