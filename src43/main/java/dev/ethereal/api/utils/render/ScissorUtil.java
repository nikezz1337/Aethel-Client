package dev.ethereal.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import dev.ethereal.api.system.interfaces.QuickImports;

import java.util.Stack;

@UtilityClass
public class ScissorUtil implements QuickImports {
    
    private final Stack<Scissor> scissorStack = new Stack<>();
    
    // Старый метод для обратной совместимости
    public void start(MatrixStack matrixStack, float x, float y, float width, float height) {
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        push(matrix, x, y, width, height);
    }

    public void stop(MatrixStack matrixStack) {
        pop();
    }
    
    // Новый метод как в Essence - работает с Matrix4f
    public void push(Matrix4f matrix4f, float x, float y, float width, float height) {
        Scissor newScissor = new Scissor();
        
        // Трансформируем позицию через матрицу
        Vector3f pos = matrix4f.transformPosition(x, y, 0, new Vector3f());
        // Получаем масштаб из матрицы
        Vector3f size = matrix4f.getScale(new Vector3f()).mul(width, height, 0);
        
        newScissor.set(pos.x, pos.y, size.x, size.y);

        // Если уже есть scissor в стеке - пересекаем области
        if (!scissorStack.isEmpty()) {
            Scissor currentScissor = scissorStack.peek().copy();

            int newX = Math.max(currentScissor.x, newScissor.x);
            int newY = Math.max(currentScissor.y, newScissor.y);
            int newWidth = Math.min(currentScissor.x + currentScissor.width, newScissor.x + newScissor.width) - newX;
            int newHeight = Math.min(currentScissor.y + currentScissor.height, newScissor.y + newScissor.height) - newY;

            if (newWidth <= 0 || newHeight <= 0) {
                newScissor.set(0, 0, 0, 0);
            } else {
                newScissor.set(newX, newY, newWidth, newHeight);
            }
        }

        scissorStack.push(newScissor);
        setScissor(newScissor);
    }
    
    public void pop() {
        if (!scissorStack.isEmpty()) {
            scissorStack.pop();
            if (scissorStack.isEmpty()) {
                RenderSystem.disableScissor();
            } else {
                setScissor(scissorStack.peek());
            }
        }
    }
    
    private void setScissor(Scissor scissor) {
        int scaleFactor = (int) mc.getWindow().getScaleFactor();
        int x = scissor.x * scaleFactor;
        int y = mc.getWindow().getHeight() - (scissor.y * scaleFactor + scissor.height * scaleFactor);
        int width = scissor.width * scaleFactor;
        int height = scissor.height * scaleFactor;

        if (width > 0 && height > 0) {
            RenderSystem.enableScissor(x, y, width, height);
        } else {
            RenderSystem.disableScissor();
        }
    }
    
    private static class Scissor {
        public int x, y;
        public int width, height;

        public void set(double x, double y, double width, double height) {
            this.x = Math.max(0, (int) Math.round(x));
            this.y = Math.max(0, (int) Math.round(y));
            this.width = Math.max(0, (int) Math.round(width));
            this.height = Math.max(0, (int) Math.round(height));
        }

        Scissor copy() {
            Scissor newScissor = new Scissor();
            newScissor.set(this.x, this.y, this.width, this.height);
            return newScissor;
        }
    }
}
