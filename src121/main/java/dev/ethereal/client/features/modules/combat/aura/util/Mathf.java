package dev.ethereal.client.features.modules.combat.aura.util;

import dev.ethereal.client.features.modules.combat.aura.util.time.TimerUtil;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public class Mathf {
    private static final SecureRandom secureRandom = new SecureRandom();

    public static float randomValue(float min, float max) {
        return ThreadLocalRandom.current().nextFloat(min, max);
    }

    public static float random(float min, float max) {
        return ThreadLocalRandom.current().nextFloat(min, max);
    }

    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static float randomNew(float min, float max) {
        return ThreadLocalRandom.current().nextFloat(min, max);
    }

    public static float randomWithUpdate(float min, float max, float interval, TimerUtil timer) {
        if (timer.hasReached(interval)) {
            timer.reset();
            return randomValue(min, max);
        }
        return min;
    }

    public static double interpolate(Vec3d current, Vec3d previous, float partialTicks) {
        return previous.x + (current.x - previous.x) * partialTicks;
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(value, min));
    }

    public static int randomInt(int bound) {
        return secureRandom.nextInt(bound);
    }
}
