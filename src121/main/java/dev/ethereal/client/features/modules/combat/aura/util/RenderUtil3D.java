package dev.ethereal.client.features.modules.combat.aura.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class RenderUtil3D {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static ShaderProgram posColorProgram() {
        return mc.getShaderLoader().getOrCreateProgram(ShaderProgramKeys.POSITION_COLOR);
    }

    public static void renderDiamond(MatrixStack matrix, float size, Color color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(posColorProgram());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        float h = size / 2;

        buffer.vertex(matrix.peek(), 0, h, 0).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -h, 0, -h).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -h, 0, h).color(r, g, b, a);
        buffer.vertex(matrix.peek(), h, 0, h).color(r, g, b, a);
        buffer.vertex(matrix.peek(), h, 0, -h).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -h, 0, -h).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix.peek(), 0, -h, 0).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -h, 0, -h).color(r, g, b, a);
        buffer.vertex(matrix.peek(), h, 0, -h).color(r, g, b, a);
        buffer.vertex(matrix.peek(), h, 0, h).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -h, 0, h).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -h, 0, -h).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderRing(MatrixStack matrix, float radius, float heightOffset, float entityHeight, Color color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(posColorProgram());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        int segments = 36;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(i * (360.0 / segments));
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            buffer.vertex(matrix.peek(), (float) x, heightOffset, (float) z).color(r, g, b, a / 2);
            buffer.vertex(matrix.peek(), (float) x, heightOffset + entityHeight * 0.1f, (float) z).color(r, g, b, 0);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderBox(MatrixStack matrix, Color color) {
        RenderSystem.setShader(posColorProgram());
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        float hs = 0.5f;
        buffer.vertex(matrix.peek(), -hs, -hs, -hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -hs, -hs, hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), hs, -hs, hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), hs, -hs, -hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -hs, -hs, -hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -hs, hs, -hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), hs, hs, -hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), hs, -hs, -hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), hs, -hs, hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), hs, hs, hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -hs, hs, hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -hs, -hs, hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -hs, hs, hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), -hs, hs, -hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), hs, hs, -hs).color(r, g, b, a);
        buffer.vertex(matrix.peek(), hs, hs, hs).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
