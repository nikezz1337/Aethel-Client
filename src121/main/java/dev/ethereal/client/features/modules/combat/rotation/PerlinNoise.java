package dev.ethereal.client.features.modules.combat.rotation;

import java.util.Random;

public class PerlinNoise {
    private final int[] permutation;
    private final Random random;

    public PerlinNoise(long seed) {
        this.random = new Random(seed);
        this.permutation = new int[512];

        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = p[i];
            p[i] = p[j];
            p[j] = temp;
        }

        for (int i = 0; i < 512; i++) {
            permutation[i] = p[i & 255];
        }
    }

    public double noise(double x) {
        int X = (int) Math.floor(x) & 255;
        x -= Math.floor(x);

        double u = fade(x);

        int a = permutation[X];
        int b = permutation[X + 1];

        return lerp(u, grad(a, x), grad(b, x - 1));
    }

    public double noise(double x, double y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);

        double u = fade(x);
        double v = fade(y);

        int a = permutation[X] + Y;
        int aa = permutation[a];
        int ab = permutation[a + 1];
        int b = permutation[X + 1] + Y;
        int ba = permutation[b];
        int bb = permutation[b + 1];

        return lerp(v,
                lerp(u, grad(aa, x, y), grad(ba, x - 1, y)),
                lerp(u, grad(ab, x, y - 1), grad(bb, x - 1, y - 1))
        );
    }

    public double octaveNoise(double x, double y, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int hash, double x) {
        return (hash & 1) == 0 ? x : -x;
    }

    private double grad(int hash, double x, double y) {
        int h = hash & 3;
        double u = h < 2 ? x : y;
        double v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
