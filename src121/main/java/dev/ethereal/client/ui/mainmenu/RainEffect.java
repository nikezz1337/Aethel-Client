package dev.ethereal.client.ui.mainmenu;

import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class RainEffect {

    private static final Color RAIN_COLOR = new Color(200, 212, 230);
    private static final Color SPLASH_COLOR = new Color(210, 220, 238);

    private static final int DEFAULT_DROP_COUNT = 60;
    private static final float WIND_SKEW = 0.04f;

    private final List<Drop> drops = new ArrayList<>();
    private final List<Splash> splashes = new ArrayList<>();

    private long lastUpdateMs = -1L;

    private boolean hasLightSource = false;
    private float lightX, lightY, lightRadius;
    private Color lightTint = new Color(255, 255, 255);

    public RainEffect() {
        this(DEFAULT_DROP_COUNT);
    }

    public RainEffect(int dropCount) {
        for (int i = 0; i < dropCount; i++) {
            drops.add(new Drop());
        }
    }

    public void setLightSource(float x, float y, float radius, Color tint) {
        this.hasLightSource = true;
        this.lightX = x;
        this.lightY = y;
        this.lightRadius = radius;
        this.lightTint = tint;
    }

    public void clearLightSource() {
        this.hasLightSource = false;
    }

    public void renderAndUpdate(MatrixStack matrices, int width, int height) {
        long now = System.currentTimeMillis();
        if (lastUpdateMs < 0L) lastUpdateMs = now;
        float dt = Math.min(now - lastUpdateMs, 50L) / 16.6667f;
        lastUpdateMs = now;

        for (Drop drop : drops) {
            if (drop.groundY == 0f) drop.reset(width, height, true);

            boolean hitGround = drop.update(dt, height);
            if (hitGround) {
                spawnSplash(drop);
                drop.reset(width, height, false);
            }
        }

        for (int i = splashes.size() - 1; i >= 0; i--) {
            if (!splashes.get(i).update(dt)) {
                splashes.remove(i);
            }
        }

        for (Drop drop : drops) {
            drawDrop(matrices, drop);
        }
        for (Splash splash : splashes) {
            splash.draw(matrices);
        }
    }

    private void spawnSplash(Drop drop) {
        int particles = 1 + (int) (Math.random() * 3);
        for (int i = 0; i < particles; i++) {
            splashes.add(Splash.particle(drop.x, drop.groundY, drop.brightness));
        }
        splashes.add(Splash.ring(drop.x, drop.groundY, drop.brightness));
    }

    private void drawDrop(MatrixStack matrices, Drop drop) {
        float headX = drop.x;
        float headY = drop.y;
        float tailX = drop.x - drop.length * WIND_SKEW;
        float tailY = drop.y - drop.length;

        float fadeNearGround = 1f - smoothstep(drop.groundY - 18f, drop.groundY, drop.y);
        int headAlpha = MathHelper.clamp(Math.round(drop.peakAlpha * fadeNearGround), 0, 255);

        Color baseColor = RAIN_COLOR;
        float lightBoost = 0f;
        if (hasLightSource) {
            float dx = drop.x - lightX;
            float dy = drop.y - lightY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            lightBoost = 1f - smoothstep(0f, lightRadius, dist);
        }

        if (lightBoost > 0.01f) {
            baseColor = ColorUtil.interpolate(RAIN_COLOR, lightTint, lightBoost * 0.5);
            headAlpha = MathHelper.clamp(Math.round(headAlpha * (1f + lightBoost * 1.0f)), 0, 255);
        }

        Color headColor = ColorUtil.setAlpha(baseColor, headAlpha);
        Color tailColor = ColorUtil.setAlpha(baseColor, 0);

        float minX = Math.min(tailX, headX) - drop.thickness * 0.5f;
        float top = Math.min(tailY, headY);
        float lineHeight = Math.max(tailY, headY) - top;
        if (lineHeight <= 0f) return;

        boolean headIsBottom = headY > tailY;
        Color topColor = headIsBottom ? tailColor : headColor;
        Color bottomColor = headIsBottom ? headColor : tailColor;

        RenderUtil.GRADIENT_RECT.draw(matrices, minX, top, drop.thickness, lineHeight, drop.thickness * 0.5f,
                topColor, topColor, bottomColor, bottomColor);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = MathHelper.clamp((x - edge0) / Math.max(edge1 - edge0, 0.0001f), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static final class Drop {
        float x, y, length, speed, thickness, groundY, brightness;
        int peakAlpha;

        void reset(int width, int height, boolean randomStartHeight) {
            this.x = (float) (Math.random() * width);
            this.groundY = height;
            this.length = 14f + (float) Math.random() * 22f;
            this.speed = 8f + (float) Math.random() * 6f;
            this.thickness = 0.8f + (float) Math.random() * 0.4f; // Плотность нити
            this.brightness = 0.5f + (float) Math.random() * 0.5f;

            // ТУТ АЛЬФА КАПЛИ: 22 — минимальная прозрачность, 28 — диапазон прибавки от яркости (итого от 22 до 50)
            this.peakAlpha = (int) (22 + brightness * 28);

            this.y = randomStartHeight
                    ? (float) (Math.random() * (height + length)) - length
                    : -length - (float) (Math.random() * 50f);
        }

        boolean update(float dt, int height) {
            this.groundY = height;
            float prevY = y;
            y += speed * dt;
            x += WIND_SKEW * speed * dt;
            return prevY < groundY && y >= groundY;
        }
    }

    private static final class Splash {
        float x, y, vx, vy;
        float life, maxLife;
        float size;
        int baseAlpha;
        boolean isRing;

        static Splash particle(float x, float y, float brightness) {
            Splash s = new Splash();
            s.x = x;
            s.y = y;
            float angle = (float) (Math.PI + Math.random() * Math.PI);
            float speed = 0.5f + (float) Math.random() * 0.8f;
            s.vx = (float) Math.cos(angle) * speed;
            s.vy = (float) Math.sin(angle) * speed * 0.6f - 0.30f;
            s.maxLife = 12f + (float) Math.random() * 6f;
            s.life = s.maxLife;
            s.size = 0.6f + (float) Math.random() * 0.7f;

            // ТУТ АЛЬФА ВСПЛЕСКОВ-ЧАСТИЦ: итоговый диапазон от 30 до 65
            s.baseAlpha = (int) (30 + brightness * 35);

            s.isRing = false;
            return s;
        }

        static Splash ring(float x, float y, float brightness) {
            Splash s = new Splash();
            s.x = x;
            s.y = y;
            s.maxLife = 14f;
            s.life = s.maxLife;

            // ТУТ АЛЬФА РАСХОДЯЩИХСЯ КОЛЕЦ: итоговый диапазон от 18 до 40
            s.baseAlpha = (int) (18 + brightness * 22);

            s.isRing = true;
            return s;
        }

        boolean update(float dt) {
            life -= dt;
            if (life <= 0f) return false;
            x += vx * dt;
            y += vy * dt;
            vy += 0.045f * dt;
            return true;
        }

        void draw(MatrixStack matrices) {
            float progress = 1f - (life / maxLife);
            int alpha = MathHelper.clamp(Math.round(baseAlpha * (1f - progress)), 0, 255);
            if (alpha <= 0) return;

            if (isRing) {
                float radius = 1.2f + progress * 5.8f;
                Color color = ColorUtil.setAlpha(SPLASH_COLOR, alpha);
                RenderUtil.RECT.drawBorder(matrices, x - radius, y - radius * 0.4f, radius * 2f, radius * 0.8f, radius * 0.4f, 0.9f, color);
            } else {
                float s = size * (1f - progress * 0.5f);
                Color color = ColorUtil.setAlpha(SPLASH_COLOR, alpha);
                RenderUtil.RECT.draw(matrices, x - s * 0.5f, y - s * 0.5f, s, s * 1.5f, 0f, color);
            }
        }
    }
}