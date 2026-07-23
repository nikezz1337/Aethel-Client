package dev.ethereal.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.system.files.FileUtil;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.client.features.modules.render.targetesp.TargetEspMode;
import dev.ethereal.client.features.modules.render.targetesp.TargetEspModule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TargetEspCrystals extends TargetEspMode {

    private final List<CrystalData> crystals = new ArrayList<>();
    private final Random random = new Random();
    private Object lastTarget = null;

    // тиковые значения для интерполяции
    private float animationTick     = 0f;
    private float prevAnimationTick = 0f;

    @Override
    public void onUpdate() {
        updateTarget();

        prevAnimationTick = animationTick;
        animationTick += TargetEspModule.getInstance().getCrystalsSpeed();

        if (currentTarget != lastTarget) {
            crystals.clear();
            if (currentTarget != null) generateCrystals();
            lastTarget = currentTarget;
        }
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw() || crystals.isEmpty()) return;

        // интерполируем animationTick между тиками — вот откуда плавность
        float tickDelta   = mc.getRenderTickCounter().getTickDelta(true);
        float smoothTick  = MathUtil.interpolate(prevAnimationTick, animationTick, tickDelta);

        float alphaAnim = (float) MathUtil.interpolate(prevShowAnimation, showAnimation.getValue());

        MatrixStack ms = event.matrixStack();
        RenderUtil.WORLD.startRender(ms);

        double tx = getTargetX() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double ty = getTargetY() - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double tz = getTargetZ() - mc.getEntityRenderDispatcher().camera.getPos().getZ();

        float orbitSpeed = 0.02f * TargetEspModule.getInstance().getCrystalsSpeed();

        for (CrystalData crystal : crystals) {
            float floatAnim  = (float) Math.sin(Math.toRadians(smoothTick + crystal.index * 35)) * 0.06f;
            float orbitAngle = crystal.baseAngle + smoothTick * orbitSpeed;

            double rx = tx + (Math.cos(orbitAngle) * crystal.baseRadius);
            double ry = ty + crystal.pos.y() + floatAnim;
            double rz = tz + (Math.sin(orbitAngle) * crystal.baseRadius);

            Color finalColor = UIColors.gradient(crystal.index * 30);

            renderCrystal(ms, rx, ry, rz, crystal, finalColor, alphaAnim, smoothTick);
            renderGlow(ms, rx, ry, rz, finalColor, alphaAnim * 0.45f, crystal.scale);
        }

        RenderUtil.WORLD.endRender(ms);
    }

    private void renderCrystal(MatrixStack ms, double x, double y, double z,
                               CrystalData data, Color color, float alpha, float smoothTick) {
        ms.push();
        ms.translate(x, y, z);
        float s = data.scale * 0.12f;
        ms.scale(s, s, s);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(data.rot.x()));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(data.rot.y() + smoothTick * 1.5f));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(data.rot.z() + smoothTick * 0.7f));

        float[] c = ColorUtil.normalize(ColorUtil.setAlpha(color, (int) (220 * alpha)));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = ms.peek().getPositionMatrix();
        float w = 1.0f, h = 2.0f;

        drawFace(buffer, matrix, 0, h, 0, -w, 0, w,  w, 0,  w,  c);
        drawFace(buffer, matrix, 0, h, 0,  w, 0, w,  w, 0, -w,  c);
        drawFace(buffer, matrix, 0, h, 0,  w, 0, -w, -w, 0, -w, c);
        drawFace(buffer, matrix, 0, h, 0, -w, 0, -w, -w, 0,  w, c);
        drawFace(buffer, matrix, 0, -h, 0,  w, 0,  w, -w, 0,  w, c);
        drawFace(buffer, matrix, 0, -h, 0,  w, 0, -w,  w, 0,  w, c);
        drawFace(buffer, matrix, 0, -h, 0, -w, 0, -w,  w, 0, -w, c);
        drawFace(buffer, matrix, 0, -h, 0, -w, 0,  w, -w, 0, -w, c);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        ms.pop();
    }

    private void drawFace(BufferBuilder b, Matrix4f m,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float x3, float y3, float z3, float[] c) {
        b.vertex(m, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        b.vertex(m, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
        b.vertex(m, x3, y3, z3).color(c[0], c[1], c[2], c[3]);
    }

    private void renderGlow(MatrixStack ms, double x, double y, double z,
                            Color color, float alpha, float scale) {
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -mc.gameRenderer.getCamera().getYaw() + 180f));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                -mc.gameRenderer.getCamera().getPitch() + 180f));

        float size = 0.5f * scale;
        float[] c = ColorUtil.normalize(ColorUtil.setAlpha(color, (int) (255 * alpha)));

        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = ms.peek().getPositionMatrix();
        buffer.vertex(matrix, -size,  size, 0).texture(0, 1).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix,  size,  size, 0).texture(1, 1).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix,  size, -size, 0).texture(1, 0).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix, -size, -size, 0).texture(0, 0).color(c[0], c[1], c[2], c[3]);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        ms.pop();
    }

    private void generateCrystals() {
        crystals.clear();
        int   count      = TargetEspModule.getInstance().getCrystalsCount();
        float height     = currentTarget.getHeight();
        float width      = currentTarget.getWidth();
        float baseRadius = Math.max(0.75f, width * 0.9f + 0.35f);
        float minY       = 0.15f;
        float maxY       = Math.max(minY + 0.2f, height - 0.15f);

        for (int i = 0; i < count; i++) {
            float angle = (float) (((float) i / count) * Math.PI * 2.0);
            float y     = minY + (maxY - minY) * (float) (i % 3) / 2f;
            crystals.add(new CrystalData(
                    new Vector3f(0, y, 0),
                    new Vector3f(20f + random.nextFloat() * 40f,
                            random.nextFloat() * 360f,
                            random.nextFloat() * 360f),
                    0.75f + random.nextFloat() * 0.35f,
                    i, angle,
                    baseRadius + (random.nextFloat() - 0.5f) * 0.18f
            ));
        }
    }

    private static class CrystalData {
        final Vector3f pos, rot;
        final float scale, baseAngle, baseRadius;
        final int index;
        CrystalData(Vector3f pos, Vector3f rot, float scale,
                    int index, float baseAngle, float baseRadius) {
            this.pos = pos; this.rot = rot; this.scale = scale;
            this.index = index; this.baseAngle = baseAngle; this.baseRadius = baseRadius;
        }
    }
}
