package dev.ethereal.api.utils.rotation.smooth;

public class StopWatch {

    private long startTime;

    public StopWatch() {
        reset();
    }

    public boolean finished(final double delay) {
        return System.currentTimeMillis() - delay >= startTime;
    }

    public boolean every(final double delay) {
        boolean finished = this.finished(delay);
        if (finished) reset();
        return finished;
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
    }

    public int elapsedTime() {
        long elapsed = System.currentTimeMillis() - this.startTime;
        if (elapsed > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (elapsed < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) elapsed;
    }

    public StopWatch setMs(long ms) {
        this.startTime = System.currentTimeMillis() - ms;
        return this;
    }
}
