package dev.ethereal.client.ui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ethereal.api.system.files.FileUtil;
import dev.ethereal.api.utils.color.UIColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import java.awt.Color;

public final class MenuBackgroundRenderer {
    private static final long START_TIME_MS = System.currentTimeMillis();
    private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
            FileUtil.getShader("title_panorama_background"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );

    private static GlUniform timeUniform;
    private static GlUniform resolutionUniform;
    private static GlUniform color1Uniform;
    private static ShaderProgram lastShader;

    private MenuBackgroundRenderer() {
    }

    public static void render(DrawContext context, float width, float height) {
        render(context, width, height, 1.0f);
    }

    public static void render(DrawContext context, float width, float height, float alpha) {
        if (context == null || width <= 0.0f || height <= 0.0f) return;

        float safeAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        if (safeAlpha <= 0.0f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
        if (shader != lastShader) {
            timeUniform = shader.getUniform("time");
            resolutionUniform = shader.getUniform("resolution");
            color1Uniform = shader.getUniform("color1");
            lastShader = shader;
        }

        if (timeUniform != null) {
            timeUniform.set((System.currentTimeMillis() - START_TIME_MS) * 0.001f);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null && resolutionUniform != null) {
            resolutionUniform.set(
                    Math.max(1.0f, client.getWindow().getFramebufferWidth()),
                    Math.max(1.0f, client.getWindow().getFramebufferHeight())
            );
        }

        setColor(color1Uniform, UIColors.primary());

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        int alphaByte = Math.max(0, Math.min(255, Math.round(safeAlpha * 255.0f)));

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, 0.0f, 0.0f, 0.0f).color(255, 255, 255, alphaByte);
        builder.vertex(matrix, 0.0f, height, 0.0f).color(255, 255, 255, alphaByte);
        builder.vertex(matrix, width, height, 0.0f).color(255, 255, 255, alphaByte);
        builder.vertex(matrix, width, 0.0f, 0.0f).color(255, 255, 255, alphaByte);
        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void setColor(GlUniform uniform, Color color) {
        if (uniform == null || color == null) return;

        uniform.set(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f);
    }
}