package dev.aethel.module.list.combat.aura.util.time;

public class TimerUtil {
    private long lastMs = System.currentTimeMillis();

    public boolean hasReached(long delay) {
        return System.currentTimeMillis() - lastMs >= delay;
    }

    public boolean hasReached(float delay) {
        return System.currentTimeMillis() - lastMs >= (long) delay;
    }

    public void reset() {
        lastMs = System.currentTimeMillis();
    }

    public long getElapsed() {
        return System.currentTimeMillis() - lastMs;
    }

    public boolean sleep(long delay) {
        if (hasReached(delay)) {
            reset();
            return true;
        }
        return false;
    }
}
