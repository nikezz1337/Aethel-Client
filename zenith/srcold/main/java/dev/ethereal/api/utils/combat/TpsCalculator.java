package dev.ethereal.api.utils.combat;

import lombok.Getter;

/**
 * Tracks server TPS by measuring time between WorldTimeUpdateS2CPacket arrivals.
 * The packet is sent every 20 server ticks, so elapsed time / 20 = ms per tick.
 */
public class TpsCalculator {
    @Getter private static final TpsCalculator instance = new TpsCalculator();

    private long lastPacketTime = -1;
    private float currentTps = 20f;

    /** Called from mixin on every WorldTimeUpdateS2CPacket */
    public void onTimeUpdate() {
        long now = System.currentTimeMillis();
        if (lastPacketTime != -1) {
            long elapsed = now - lastPacketTime;
            // packet arrives every 20 server ticks
            float msPerTick = elapsed / 20f;
            float tps = Math.min(20f, 1000f / msPerTick);
            // smooth with simple EMA
            currentTps = currentTps * 0.8f + tps * 0.2f;
        }
        lastPacketTime = now;
    }

    /**
     * Returns the cooldown progress offset adjusted for server TPS.
     * At 20 TPS returns 0.0f (no adjustment needed).
     * At lower TPS returns a positive offset so the threshold is met earlier.
     */
    public float getAdjustTicks() {
        if (currentTps >= 19.5f) return 0.0f;
        // ratio of real tick speed vs expected
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
