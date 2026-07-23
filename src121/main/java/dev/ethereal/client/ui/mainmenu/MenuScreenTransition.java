package dev.ethereal.client.ui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.awt.Color;

public final class MenuScreenTransition {
    private static final long DURATION_MS = 360L;
    private static final float OLD_END_SCALE = 1.12f;
    private static final float NEW_START_SCALE = 0.965f;

    private static SimpleFramebuffer snapshot;
    private static long startTimeMs;
    private static boolean active;
    private static int scaledWidth;
    private static int scaledHeight;

    private MenuScreenTransition() {
    }

    public static void start(Screen from, Screen to) {
        if (from == null || to == null || from == to) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null || client.getFramebuffer() == null || client.world != null) {
            active = false;
            return;
        }

        Framebuffer mainFramebuffer = client.getFramebuffer();
        int textureWidth = Math.max(1, mainFramebuffer.textureWidth);
        int textureHeight = Math.max(1, mainFramebuffer.textureHeight);

        if (snapshot == null) {
            snapshot = new SimpleFramebuffer(textureWidth, textureHeight, false);
        } else if (snapshot.textureWidth != textureWidth || snapshot.textureHeight != textureHeight) {
            snapshot.resize(textureWidth, textureHeight);
        }

        snapshot.beginWrite(false);
        mainFramebuffer.draw(snapshot.textureWidth, snapshot.textureHeight);
        mainFramebuffer.beginWrite(false);

        scaledWidth = Math.max(1, client.getWindow().getScaledWidth());
        scaledHeight = Math.max(1, client.getWindow().getScaledHeight());
        startTimeMs = Util.getMeasuringTimeMs();
        active = true;
    }

    public static void pushNewScreenTransform(DrawContext context) {
        if (!active || context == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null || client.world != null) {
            active = false;
            return;
        }

        float progress = progress();
        float eased = easeOutCubic(progress);
        float scale = MathHelper.lerp(eased, NEW_START_SCALE, 1.0f);
        float width = Math.max(1.0f, client.getWindow().getScaledWidth());
        float height = Math.max(1.0f, client.getWindow().getScaledHeight());

        renderSnapshot(context, 0.0f, 0.0f, width, height, 1.0f, -10.0f);
        context.getMatrices().push();
        context.getMatrices().translate(width * 0.5f, height * 0.5f, 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-width * 0.5f, -height * 0.5f, 0.0f);
    }

    public static void popNewScreenTransform(DrawContext context) {
        if (!active || context == null) {
            return;
        }

        context.getMatrices().pop();
    }

    public static void render(DrawContext context) {
        if (!active || snapshot == null || context == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            active = false;
            return;
        }

        float progress = progress();
        if (progress >= 1.0f) {
            active = false;
            return;
        }

        float width = client != null && client.getWindow() != null ? client.getWindow().getScaledWidth() : scaledWidth;
        float height = client != null && client.getWindow() != null ? client.getWindow().getScaledHeight() : scaledHeight;
        if (width <= 0.0f || height <= 0.0f) {
            width = scaledWidth;
            height = scaledHeight;
        }

        float eased = easeOutCubic(progress);
        float alpha = 1.0f - eased;
        float scale = MathHelper.lerp(eased, 1.0f, OLD_END_SCALE);

        float drawWidth = width * scale;
        float drawHeight = height * scale;
        float x = (width - drawWidth) * 0.5f;
        float y = (height - drawHeight) * 0.5f;
        renderSnapshot(context, x, y, drawWidth, drawHeight, alpha, 250.0f);
    }

    private static float progress() {
        return MathHelper.clamp((Util.getMeasuringTimeMs() - startTimeMs) / (float) DURATION_MS, 0.0f, 1.0f);
    }

    private static void renderSnapshot(DrawContext context, float x, float y, float width, float height, float alpha, float z) {
        if (snapshot == null || alpha <= 0.01f) {
            return;
        }

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        int color = withAlpha(Color.WHITE, alpha).getRGB();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, snapshot.getColorAttachment());

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix, x, y, z).texture(0.0f, 1.0f).color(color);
        builder.vertex(matrix, x, y + height, z).texture(0.0f, 0.0f).color(color);
        builder.vertex(matrix, x + width, y + height, z).texture(1.0f, 0.0f).color(color);
        builder.vertex(matrix, x + width, y, z).texture(1.0f, 1.0f).color(color);
        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static float easeOutCubic(float value) {
        float inv = 1.0f - value;
        return 1.0f - inv * inv * inv;
    }

    private static Color withAlpha(Color color, float alpha) {
        int alphaInt = MathHelper.clamp(Math.round(alpha * 255.0f), 0, 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaInt);
    }
}
