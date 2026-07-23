package dev.ethereal.api.utils.combat;

import dev.ethereal.api.system.interfaces.QuickImports;

public class ClickScheduler implements QuickImports {
    private long lastAttackTime = System.currentTimeMillis();

    public boolean isCooldownComplete() {
        return mc.player != null && mc.player.getAttackCooldownProgress(0.0f) >= 0.985f;
    }

    public boolean isOneTickBeforeAttack() {
        if (mc.player == null) return false;
        float cd = mc.player.getAttackCooldownProgress(0.0f);
        return cd >= 0.82f && cd < 0.99f;
    }

    public boolean isTwoTicksBeforeAttack() {
        if (mc.player == null) return false;
        float cd = mc.player.getAttackCooldownProgress(0.0f);
        return cd >= 0.65f && cd < 0.99f;
    }

    public void recalculateAfterAttack() {
        lastAttackTime = System.currentTimeMillis();
    }
}