package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.color.UIColors;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

@ModuleRegister(name = "ItemRadius", category = Category.RENDER)
public class ItemRadiusModule extends Module {
    @Getter private static final ItemRadiusModule instance = new ItemRadiusModule();

    private static final double RADIUS = 10.0;
    private static final int SEGMENTS = 250;
    private static final int LAYERS = 32;
    private static final double HEIGHT_OFFSET = 0.065;

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        boolean eye = isHolding(Items.ENDER_EYE);
        boolean sugar = isHolding(Items.SUGAR);
        if (!eye && !sugar) return;

        float tickDelta = event.partialTicks();
        Vec3d center = new Vec3d(
                mc.player.prevX + (mc.player.getX() - mc.player.prevX) * tickDelta,
                mc.player.prevY + (mc.player.getY() - mc.player.prevY) * tickDelta + HEIGHT_OFFSET,
                mc.player.prevZ + (mc.player.getZ() - mc.player.prevZ) * tickDelta
        );

        renderRadiusDisk(event.matrixStack(), center, eye, sugar);
    }

    private boolean isHolding(Item item) {
        return mc.player.getMainHandStack().isOf(item) || mc.player.getOffHandStack().isOf(item);
    }

    private void renderRadiusDisk(MatrixStack matrixStack, Vec3d center, boolean eye, boolean sugar) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        double step = Math.PI * 2.0 / SEGMENTS;

        Color firstTheme = UIColors.primary();
        Color secondTheme = UIColors.secondary();

        int boostR = eye ? 36 : (sugar ? 22 : 0);
        int boostG = eye ? 6 : (sugar ? 22 : 0);
        int boostB = eye ? 56 : (sugar ? 22 : 0);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        for (int layer = 0; layer < LAYERS; layer++) {
            double t0 = layer / (double) LAYERS;
            double t1 = (layer + 1) / (double) LAYERS;

            double r0 = RADIUS * t0;
            double r1 = RADIUS * t1;

            double arc = 1.0 - Math.abs(((t0 + t1) * 0.5) - 1.0);
            double centerFade = 1.0 - Math.abs(((t0 + t1) * 0.5) - 0.5) * 2.0;
            double alphaCurve = 1.20 + Math.max(0.0, centerFade) * 0.65 + arc * 0.2;
            int baseAlpha = eye ? 86 : 72;

            for (int i = 0; i < SEGMENTS; i++) {
                double a0 = i * step;
                double a1 = (i + 1) * step;

                double swirlA = (Math.sin((a0 * 2.0) + layer * 0.23) + 1.0) * 0.5;
                double swirlB = (Math.sin((a1 * 2.0) + (layer + 1) * 0.23) + 1.0) * 0.5;

                int c0 = themedColor(firstTheme, secondTheme, swirlA, alphaCurve * (0.2 + t0 * 0.8), baseAlpha, boostR, boostG, boostB);
                int c1 = themedColor(firstTheme, secondTheme, swirlB, alphaCurve * (0.2 + t1 * 0.8), baseAlpha, boostR, boostG, boostB);

                Vec3d p1 = new Vec3d(center.x + Math.cos(a0) * r0, center.y, center.z + Math.sin(a0) * r0).subtract(cam);
                Vec3d p2 = new Vec3d(center.x + Math.cos(a0) * r1, center.y, center.z + Math.sin(a0) * r1).subtract(cam);
                Vec3d p3 = new Vec3d(center.x + Math.cos(a1) * r1, center.y, center.z + Math.sin(a1) * r1).subtract(cam);
                Vec3d p4 = new Vec3d(center.x + Math.cos(a1) * r0, center.y, center.z + Math.sin(a1) * r0).subtract(cam);

                buffer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(c0);
                buffer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).color(c1);
                buffer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z).color(c1);
                buffer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z).color(c0);
            }
        }

        renderEdgeBand(center, cam, matrix, buffer, eye, sugar);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderEdgeBand(Vec3d center, Vec3d cam, Matrix4f matrix, BufferBuilder buffer, boolean eye, boolean sugar) {
        double step = Math.PI * 2.0 / SEGMENTS;
        double inner = RADIUS - 0.18;
        double outer = RADIUS;
        int alphaBase = eye ? 165 : 145;

        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = i * step;
            double a1 = (i + 1) * step;

            double angleMix = (Math.sin(a0 * 3.0 + System.currentTimeMillis() * 0.003) + 1.0) * 0.5;
            Color style = UIColors.gradient((int) (angleMix * 120.0));
            int rr = clamp(style.getRed() + (eye ? 30 : sugar ? 18 : 0));
            int gg = clamp(style.getGreen() + (eye ? 6 : sugar ? 18 : 0));
            int bb = clamp(style.getBlue() + (eye ? 50 : sugar ? 18 : 0));

            int outerColor = rgba(rr, gg, bb, alphaBase);
            int innerColor = rgba(rr, gg, bb, Math.max(14, alphaBase / 7));

            Vec3d p1 = new Vec3d(center.x + Math.cos(a0) * inner, center.y, center.z + Math.sin(a0) * inner).subtract(cam);
            Vec3d p2 = new Vec3d(center.x + Math.cos(a0) * outer, center.y, center.z + Math.sin(a0) * outer).subtract(cam);
            Vec3d p3 = new Vec3d(center.x + Math.cos(a1) * outer, center.y, center.z + Math.sin(a1) * outer).subtract(cam);
            Vec3d p4 = new Vec3d(center.x + Math.cos(a1) * inner, center.y, center.z + Math.sin(a1) * inner).subtract(cam);

            buffer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(innerColor);
            buffer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).color(outerColor);
            buffer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z).color(outerColor);
            buffer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z).color(innerColor);
        }
    }

    private int themedColor(Color firstTheme, Color secondTheme, double blend, double alphaScale, int baseAlpha,
                            int boostR, int boostG, int boostB) {
        int r = clamp((int) (firstTheme.getRed() + (secondTheme.getRed() - firstTheme.getRed()) * blend) + boostR);
        int g = clamp((int) (firstTheme.getGreen() + (secondTheme.getGreen() - firstTheme.getGreen()) * blend) + boostG);
        int b = clamp((int) (firstTheme.getBlue() + (secondTheme.getBlue() - firstTheme.getBlue()) * blend) + boostB);
        int a = clamp((int) (baseAlpha * alphaScale));
        return rgba(r, g, b, a);
    }

    private int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
