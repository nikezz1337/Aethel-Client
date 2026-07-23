package dev.ethereal.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import dev.ethereal.api.utils.render.display.*;
import org.joml.Matrix4f;

import java.awt.*;

@UtilityClass
public class RenderUtil {
    public RectRender RECT = new RectRender();
    public BlurRectRender BLUR_RECT = new BlurRectRender();
    public GradientRectRender GRADIENT_RECT = new GradientRectRender();
    public TextureRectRender TEXTURE_RECT = new TextureRectRender();

    public OtherRender OTHER = new OtherRender();
    public WorldRender WORLD = new WorldRender();
    public BoxRender BOX = new BoxRender();
    
    public void drawRect(MatrixStack matrices, float x, float y, float width, float height, Color color) {
        RECT.draw(matrices, x, y, width, height, 0f, color);
    }
    
    public void drawGradientBorder(MatrixStack matrices, float x, float y, float width, float height, float borderWidth, Color color1, Color color2) {
        // Верхняя граница
        GRADIENT_RECT.draw(matrices, x, y, width, borderWidth, 0f, color1, color2, color2, color1);
        // Нижняя граница
        GRADIENT_RECT.draw(matrices, x, y + height - borderWidth, width, borderWidth, 0f, color1, color2, color2, color1);
        // Левая граница
        GRADIENT_RECT.draw(matrices, x, y, borderWidth, height, 0f, color1, color1, color2, color2);
        // Правая граница
        GRADIENT_RECT.draw(matrices, x + width - borderWidth, y, borderWidth, height, 0f, color1, color1, color2, color2);
    }


    public static void drawFilledTriangle(DrawContext context, float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, x1, y1, 0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrix, x2, y2, 0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrix, x3, y3, 0f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

//        // Правильный способ установки шейдера в 1.21+
//        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // RenderSystem.disableCull(); // можно добавить, если мешает

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
    }
    
    public void drawShadow(MatrixStack matrices, float x, float y, float width, float height, float blur, float round, Color color) {
        // Простая реализация тени через градиент с прозрачностью
        float shadowOffset = blur / 2f;
        Color transparent = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
        
        // Рисуем тень как градиент от центра к краям
        GRADIENT_RECT.draw(matrices, x - shadowOffset, y - shadowOffset, 
            width + shadowOffset * 2, height + shadowOffset * 2, 
            round, transparent, transparent, color, color);
    }
}
