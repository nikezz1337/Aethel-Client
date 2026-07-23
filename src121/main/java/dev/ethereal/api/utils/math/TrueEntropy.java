package dev.ethereal.api.utils.math;

import java.lang.management.ManagementFactory;

public class TrueEntropy {
    private double x1, x2;
    private double r1, r2;
    private double drift1, drift2;
    private int counter = 0;

    public TrueEntropy() {
        long seed = System.nanoTime() ^ ManagementFactory.getRuntimeMXBean().getStartTime();
        x1 = (double)(seed & 0xFFFF) / 65536.0;
        x2 = (double)((seed >>> 16) & 0xFFFF) / 65536.0;
        r1 = 3.57 + (double)((seed >>> 32) & 0xFFF) / 4096.0 * 0.43;
        r2 = 3.57 + (double)((seed >>> 44) & 0xFFF) / 4096.0 * 0.43;
        drift1 = 0.001 + (x1 * 0.01);
        drift2 = 0.001 + (x2 * 0.01);
    }

    public double uniform() {
        x1 = r1 * x1 * (1.0 - x1);
        x2 = r2 * x2 * (1.0 - x2);

        r1 += drift1 * (x2 - 0.5);
        r2 += drift2 * (x1 - 0.5);
        if (r1 < 3.57) r1 = 3.57 + (r1 - Math.floor(r1)) * 0.43;
        if (r1 > 4.0) r1 = 4.0 - (r1 - Math.floor(r1)) * 0.43;
        if (r2 < 3.57) r2 = 3.57 + (r2 - Math.floor(r2)) * 0.43;
        if (r2 > 4.0) r2 = 4.0 - (r2 - Math.floor(r2)) * 0.43;

        long nano = System.nanoTime();
        long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
        int hardwareNoise = (int)(nano ^ (nano >>> 17) ^ jvmUptime) & 0x7FFFFFFF;

        double mixed = (x1 + x2 + (hardwareNoise * 5.960464477539063e-8)) * 0.3333333333333;
        mixed = (mixed * 1.6180339887498948482) % 1.0;

        counter++;
        if (counter % (100 + (int)(x2 * 500)) == 0) {
            x1 = (hardwareNoise * 1.1102230246251565e-16) % 1.0;
        }
        return mixed;
    }

    public double gaussian() {
        double sum = 0.0;
        for (int i = 0; i < 4; i++) {
            sum += uniform() - 0.5;
        }
        return sum * 1.7320508075688772;
    }

    public int nextInt(int bound) {
        return (int)(uniform() * bound);
    }
}