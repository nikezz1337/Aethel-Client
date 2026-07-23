package dev.ethereal.api.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import dev.ethereal.api.system.interfaces.QuickImports;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class MathUtil implements QuickImports {

    private final RandomUtil randomUtil = new RandomUtil();

    public double getEntityBPS(Entity entity) {
        return Math.hypot(entity.prevX - entity.getX(), entity.prevZ - entity.getZ()) * 20;
    }

    public float interpolate(double oldValue, double newValue) {
        return (float) (oldValue + (newValue - oldValue) * mc.getRenderTickCounter().getTickDelta(false));
    }

    public double interpolate(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    public float interpolate(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    public int interpolate(int oldValue, int newValue, float interpolationValue) {
        return (int) (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public double round(double value, double step) {
        double v = Math.round(value / step) * step;
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public float round(float value, float step) {
        double v = Math.round(value / step) * step;
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP).floatValue();
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public int random(int min, int max) {
        return randomUtil.random(min, max);
    }

    public float random(float min, float max) {
        return randomUtil.random(min, max);
    }

    public double random(double min, double max) {
        return randomUtil.random(min, max);
    }

    public boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    public double map(double value, double fromLow, double fromHigh, double toLow, double toHigh) {
        return toLow + (value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow);
    }

    public float easeOutQuad(float x) {
        return 1f - (1f - x) * (1f - x);
    }

    public static double randomTick(double min, double max, long delay, TimerUtil t) {
        if (t.finished(delay)) {
            t.reset();
            return MathUtil.random(min, max);
        }
        return min;
    }
}