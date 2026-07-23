package dev.aethel.module.list.combat.aura.util;

import dev.aethel.util.IMinecraft;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GCDUtil implements IMinecraft {
    public float getFixRotate(float rot) {
        return getDeltaMouse(rot) * getGCDValue();
    }

    public float getGCDValue() {
        return getGCD();
    }

    public float getGCD() {
        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue() * 0.6f + 0.2f;
        float pow = sensitivity * sensitivity * sensitivity * 8.0f;
        return pow * 0.15f;
    }

    public float getDeltaMouse(float delta) {
        return Math.round(delta / getGCDValue());
    }
}