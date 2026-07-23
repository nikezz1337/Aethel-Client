package dev.aethel.util.math;

import com.google.common.eventbus.Subscribe;
import dev.aethel.Aethel;
import dev.aethel.event.list.EventPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;

import java.util.LinkedList;
import java.util.Queue;

public class TPSGetter {
    private final Queue<Float> tpsHistory = new LinkedList<>();
    private float TPS = 20;
    private float averageTPS = 20;
    private float adjustTicks = 0;
    private long timestamp;

    public TPSGetter() {
        Aethel.getInstance().getEventBus().register(this);
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() != null && e.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            updateTPS();
        }
    }

    private void updateTPS() {
        long delay = System.nanoTime() - timestamp;

        float maxTPS = 20;
        float rawTPS = maxTPS * (1e9f / delay);
        float boundedTPS = MathHelper.clamp(rawTPS, 0, maxTPS);

        TPS = (float) round(boundedTPS);
        adjustTicks = boundedTPS - maxTPS;
        timestamp = System.nanoTime();

        updateAverageTPS();
    }

    private void updateAverageTPS() {
        if (tpsHistory.size() >= 10) {
            tpsHistory.poll();
        }
        tpsHistory.add(TPS);

        float sum = 0;
        for (float tps : tpsHistory) {
            sum += tps;
        }
        averageTPS = (float) round(sum / tpsHistory.size());
    }

    public double round(final double input) {
        return Math.round(input * 100.0) / 100.0;
    }

    public float getTPS() {
        return TPS;
    }

    public float getAverageTPS() {
        return averageTPS;
    }
}
