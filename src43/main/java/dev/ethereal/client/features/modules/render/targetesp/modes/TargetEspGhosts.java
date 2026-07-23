package dev.ethereal.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.system.files.FileUtil;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.client.features.modules.render.targetesp.TargetEspMode;

import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;

public class TargetEspGhosts extends TargetEspMode {

    // время жизни одной точки — достаточно долгое чтобы перекрывать расстояние между тиками
    private static final int   MAX_TIME_MS  = 500;
    // за один тик спавним несколько промежуточных точек чтобы не было разрывов
    private static final int   SPAWN_PER_TICK = 4;
    private static final int   CORNER_COUNT = 3;
    private static final int   DELAY_XZ_MS  = 2000;
    private static final int   DELAY_Y_MS   = 1200;
    private static final float BASE_SCALE   = 0.22f;

    private final CopyOnWriteArrayList<GlowPoint> points = new CopyOnWriteArrayList<>();

    @Override
    public void onUpdate() {
        updateTarget();

        long now = System.currentTimeMillis();
        points.removeIf(p -> now - p.spawnTime >= MAX_TIME_MS);

        if (!canDraw() || currentTarget == null) {
            points.clear();
            return;
        }

        // спавним SPAWN_PER_TICK промежуточных кадров за один тик
        // это убирает разрывы при низком FPS и на высоких скоростях орбиты
        for (int sub = 0; sub < SPAWN_PER_TICK; sub++) {
            long subTime = now - (long)((SPAWN_PER_TICK - 1 - sub) * (50.0 / SPAWN_PER_TICK));
            spawnPoints(subTime);
        }
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (!canDraw() || points.isEmpty()) return;

        float alpha = (float) MathUtil.interpolate(prevSizeAnimation, sizeAnimation.getValue());

        setupRenderState();
        MatrixStack stack = event.matrixStack();
        stack.push();
        drawPoints(stack, alpha);
        stack.pop();
        resetRenderState();
    }

    private void spawnPoints(long time) {
        if (currentTarget == null) return;

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        float ex = (float) MathHelper.lerp(tickDelta, currentTarget.prevX, currentTarget.getX());
        float ey = (float) MathHelper.lerp(tickDelta, currentTarget.prevY, currentTarget.getY());
        float ez = (float) MathHelper.lerp(tickDelta, currentTarget.prevZ, currentTarget.getZ());

        float xzRange = currentTarget.getWidth()  + 0.05f;
        float yRange  = currentTarget.getHeight() + 0.05f;

        for (int corner = 0; corner < CORNER_COUNT; corner++) {
            float cornersPC = corner / (float) CORNER_COUNT;

            float xzRotate = ((time + (int)(DELAY_XZ_MS * cornersPC)) % DELAY_XZ_MS)
                    / (float) DELAY_XZ_MS * 360f;
            float yLerpPC  = ((time + (int)(DELAY_Y_MS * cornersPC)) % DELAY_Y_MS)
                    / (float) DELAY_Y_MS;

            yLerpPC = (yLerpPC > 0.5f ? 1f - yLerpPC : yLerpPC) * 2f;
            yLerpPC = 0.35f * (1f - (float) Math.cos(Math.PI * yLerpPC));

            double yawRad = Math.toRadians(cornersPC * 360f + xzRotate);

            float px = ex - (float) Math.sin(yawRad) * xzRange;
            float py = ey + yRange * yLerpPC + 0.32f;
            float pz = ez + (float) Math.cos(yawRad) * xzRange;

            points.add(new GlowPoint(px, py, pz, time));
        }
    }

    private void drawPoints(MatrixStack stack, float alpha) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d  camPos = camera.getPos();
        long   now    = System.currentTimeMillis();

        float hurtPC = currentTarget != null
                ? Math.max(0f, (float) Math.sin(currentTarget.hurtTime * (18f * Math.PI / 180f)))
                : 0f;

        for (GlowPoint p : points) {
            float t = Math.min((now - p.spawnTime) / (float) MAX_TIME_MS, 1f);

            // fade: быстро нарастает, медленно угасает
            float fadeAlpha = (t < 0.2f ? t / 0.2f : 1f - (t - 0.2f) / 0.8f) * alpha;
            if (fadeAlpha <= 0.01f) continue;

            float s = BASE_SCALE * (1f - t * 0.3f);

            Color color = blendHurtColor(UIColors.gradient((int)(t * 360)), hurtPC, fadeAlpha);
            float[] c = ColorUtil.normalize(color);

            stack.push();
            stack.translate(p.x - camPos.x, p.y - camPos.y, p.z - camPos.z);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            // три слоя для объёмного свечения
            renderQuad(stack, s * 1.8f, c, c[3] * 0.2f);
            renderQuad(stack, s,        c, c[3] * 0.6f);
            renderQuad(stack, s * 0.5f, c, c[3]);
            stack.pop();
        }
    }

    private void renderQuad(MatrixStack stack, float halfSize, float[] c, float alpha) {
        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        float a = MathUtil.clamp(alpha, 0f, 1f);
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(matrix, -halfSize,  halfSize, 0f).texture(0f, 1f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix,  halfSize,  halfSize, 0f).texture(1f, 1f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix,  halfSize, -halfSize, 0f).texture(1f, 0f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix, -halfSize, -halfSize, 0f).texture(0f, 0f).color(c[0], c[1], c[2], a);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private Color blendHurtColor(Color base, float hurtPC, float alpha) {
        hurtPC = MathUtil.clamp(hurtPC, 0f, 1f);
        int r = MathUtil.clamp((int)(base.getRed()   + (200 - base.getRed())  * hurtPC), 0, 255);
        int g = MathUtil.clamp((int)(base.getGreen() -  base.getGreen()       * hurtPC), 0, 255);
        int b = MathUtil.clamp((int)(base.getBlue()  -  base.getBlue()        * hurtPC), 0, 255);
        int a = MathUtil.clamp((int)(alpha * 255), 0, 255);
        return ColorUtil.setAlpha(new Color(r, g, b), a);
    }

    private void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();   // не рендерим сквозь игрока
        RenderSystem.depthMask(false);    // но не пишем в depth buffer (аддитивный блендинг)
        RenderSystem.disableCull();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ONE,       GlStateManager.DstFactor.ZERO);
    }

    private void resetRenderState() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static class GlowPoint {
        final float x, y, z;
        final long  spawnTime;
        GlowPoint(float x, float y, float z, long spawnTime) {
            this.x = x; this.y = y; this.z = z; this.spawnTime = spawnTime;
        }
    }
}