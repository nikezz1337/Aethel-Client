package dev.aethel.util.math;

import dev.aethel.util.IMinecraft;
import net.minecraft.util.math.MathHelper;

public class Counter implements IMinecraft {

    private static int currentFPS;

    public static int getCurrentFPS() {
        return currentFPS;
    }

    public static void updateFPS() {
        int prevFPS = mc.getCurrentFps();
        currentFPS = MathHelper.lerp(0.5f, prevFPS, currentFPS);
    }
}
