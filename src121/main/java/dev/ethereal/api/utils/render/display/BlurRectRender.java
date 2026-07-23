package dev.ethereal.api.utils.render.display;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.system.files.FileUtil;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.render.KawaseBlurProgram;
import dev.ethereal.client.features.modules.render.InterfaceModule;

import java.awt.*;

public class BlurRectRender implements QuickImports {
    private static final ShaderProgramKey shaderKey = new ShaderProgramKey(FileUtil.getShader("rect/blurred_rect"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, float radius, Color color, float mix) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color, color, color, color, mix);
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, float radius, Color color) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color, color, color, color, InterfaceModule.getGlassy());
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color) {
        draw(matrixStack, x, y, width, height, radius, color, color, color, color, InterfaceModule.getGlassy());
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight) {
        draw(matrixStack, x, y, width, height, radius, topLeft, topRight, bottomLeft, bottomRight, InterfaceModule.getGlassy());
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight, float mix) {
        float z = 0f;
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        Framebuffer fbo = KawaseBlurProgram.fbos.getFirst();

        float smoothness = 0.8f;
        float hPad = -smoothness / 2.0f + smoothness * 2.0f;
        float vPad = smoothness / 2.0f + smoothness;
        float ax = x - hPad / 2.0f;
        float ay = y - vPad / 2.0f;
        float aw = width + hPad;
        float ah = height + vPad;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        // --- Слой 1: блюр текстура с закруглением ---
        if (mix < 1f) {
            int texW = fbo.textureWidth;
            int texH = fbo.textureHeight;
            double scale = (double) texW / mc.getWindow().getScaledWidth();
            float fx = (float) (x * scale);
            float fy = (float) (y * scale);
            float fw = (float) (width * scale);
            float fh = (float) (height * scale);
            float uLeft   = fx / texW;
            float uRight  = (fx + fw) / texW;
            float vTop    = 1f - (fy / texH);
            float vBottom = 1f - ((fy + fh) / texH);

            // Полностью непрозрачный белый — шейдер сам смешает блюр
            Color blurColor = new Color(255, 255, 255, 255);
            float[] bc = ColorUtil.normalize(blurColor);

            RenderSystem.setShaderTexture(0, fbo.getColorAttachment());
            ShaderProgram shader = RenderSystem.setShader(shaderKey);
            shader.getUniform("uSize").set(width, height);
            shader.getUniform("uRadius").set(radius.x, radius.z, radius.w, radius.y);
            shader.getUniform("uMix").set(0f); // чистый блюр без цветного смешения
            shader.getUniform("uSmoothness").set(smoothness);
            shader.getUniform("uAlpha").set((1f - mix) * (topLeft.getAlpha() / 255f)); // сила блюра от ползунка * альфа цвета
            shader.getUniform("uTopLeftColor").set(bc[0], bc[1], bc[2], bc[3]);
            shader.getUniform("uBottomLeftColor").set(bc[0], bc[1], bc[2], bc[3]);
            shader.getUniform("uBottomRightColor").set(bc[0], bc[1], bc[2], bc[3]);
            shader.getUniform("uTopRightColor").set(bc[0], bc[1], bc[2], bc[3]);

            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            builder.vertex(matrix4f, ax,      ay,      z).texture(uLeft,  vTop).color(blurColor.getRGB());
            builder.vertex(matrix4f, ax,      ay + ah, z).texture(uLeft,  vBottom).color(blurColor.getRGB());
            builder.vertex(matrix4f, ax + aw, ay + ah, z).texture(uRight, vBottom).color(blurColor.getRGB());
            builder.vertex(matrix4f, ax + aw, ay,      z).texture(uRight, vTop).color(blurColor.getRGB());
            BufferRenderer.drawWithGlobalProgram(builder.end());
        }

        // --- Слой 2: цветной оверлей поверх блюра ---
        float[] c1 = ColorUtil.normalize(topLeft);
        float[] c2 = ColorUtil.normalize(bottomLeft);
        float[] c3 = ColorUtil.normalize(bottomRight);
        float[] c4 = ColorUtil.normalize(topRight);

        RenderSystem.setShaderTexture(0, fbo.getColorAttachment());
        ShaderProgram shader2 = RenderSystem.setShader(shaderKey);
        shader2.getUniform("uSize").set(width, height);
        shader2.getUniform("uRadius").set(radius.x, radius.z, radius.w, radius.y);
        shader2.getUniform("uMix").set(1f); // только цвет, без блюр текстуры
        shader2.getUniform("uSmoothness").set(smoothness);
        shader2.getUniform("uAlpha").set((c1[3] + c2[3] + c3[3] + c4[3]) / 4f);
        shader2.getUniform("uTopLeftColor").set(c1[0], c1[1], c1[2], c1[3]);
        shader2.getUniform("uBottomLeftColor").set(c2[0], c2[1], c2[2], c2[3]);
        shader2.getUniform("uBottomRightColor").set(c3[0], c3[1], c3[2], c3[3]);
        shader2.getUniform("uTopRightColor").set(c4[0], c4[1], c4[2], c4[3]);

        BufferBuilder builder2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder2.vertex(matrix4f, ax,      ay,      z).texture(0, 0).color(topLeft.getRGB());
        builder2.vertex(matrix4f, ax,      ay + ah, z).texture(0, 0).color(bottomLeft.getRGB());
        builder2.vertex(matrix4f, ax + aw, ay + ah, z).texture(0, 0).color(bottomRight.getRGB());
        builder2.vertex(matrix4f, ax + aw, ay,      z).texture(0, 0).color(topRight.getRGB());
        BufferRenderer.drawWithGlobalProgram(builder2.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
