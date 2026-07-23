package dev.ethereal.client.features.modules.combat.aura.util;

import dev.ethereal.api.system.interfaces.QuickImports;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GCDUtil implements QuickImports {
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