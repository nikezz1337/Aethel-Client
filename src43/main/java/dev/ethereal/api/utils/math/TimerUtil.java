package dev.ethereal.api.utils.math;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimerUtil {
    private long millis;

    public TimerUtil() {
        reset();
    }

    public boolean finished(float delay) {
        return System.currentTimeMillis() - millis >= (long) delay;
    }

    public boolean finished(long delay) {
        return System.currentTimeMillis() - millis >= delay;
    }

    public boolean finished(final double delay) {
        return System.currentTimeMillis() - delay >= millis;
    }

    public void reset() {
        this.millis = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.millis;
    }
}
