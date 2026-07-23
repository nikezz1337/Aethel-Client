package dev.ethereal.client.features.modules.combat.aura;

public class StopWatch {

    private long startTime;

    public StopWatch() {
        reset();
    }

    public long getStartTime() { return startTime; }

    public boolean finished(final double delay) {
        return System.currentTimeMillis() - delay >= startTime;
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
    }

    public void reset(long delay) {
        this.startTime = System.currentTimeMillis() + delay;
    }

    public long elapsedTime() {
        return System.currentTimeMillis() - this.startTime;
    }

    public boolean passed(long time) {
        return System.currentTimeMillis() - startTime > time;
    }

    public String elapsedTimeFormatted() {
        long elapsed = elapsedTime();
        long hours = elapsed / 3600000;
        long minutes = (elapsed % 3600000) / 60000;
        long seconds = (elapsed % 60000) / 1000;

        StringBuilder formatted = new StringBuilder();
        if (hours > 0) {
            formatted.append(hours).append("ч, ");
        }
        if (minutes > 0 || hours > 0) {
            formatted.append(minutes).append("м, ");
        }
        formatted.append(seconds).append("с");

        return formatted.toString();
    }
}
