package dev.ethereal.api.utils.combat;


import lombok.Getter;

public class TpsCalculator {
    @Getter
    private static final TpsCalculator instance = new TpsCalculator();

    private long lastPacketTime = -1;
    private float currentTps = 20f;

    public void onTimeUpdate() {
        long now = System.currentTimeMillis();
        if (lastPacketTime != -1) {
            long elapsed = now - lastPacketTime;
            float msPerTick = elapsed / 20f;
            float tps = Math.min(20f, 1000f / msPerTick);
            currentTps = currentTps * 0.8f + tps * 0.2f;
        }
        lastPacketTime = now;
    }

    public float getAdjustTicks() {
        if (currentTps >= 19.5f) return 0.0f;
        return (20f - currentTps) / 20f * 0.5f;
    }

    public float getTps() {
        return currentTps;
    }

    public void reset() {
        lastPacketTime = -1;
        currentTps = 20f;
    }
}
