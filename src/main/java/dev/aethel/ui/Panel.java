package dev.aethel.ui;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import dev.aethel.Aethel;
import dev.aethel.module.ModuleCategory;
import dev.aethel.ui.component.Component;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.helper.HoverUtil;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.math.Scissor;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Panel implements IMinecraft {
    public float x, y, width, height;
    public final ModuleCategory category;
    public List<ModuleComponent> moduleComponents = new ArrayList<>();
    private Animation animation = new Animation(Easing.QUINTIC_OUT, 350);
    private Animation animationAlpha = new Animation(Easing.BOUNCE_OUT, 350);
    private final Animation scrollbarAnim = new Animation(Easing.CUBIC_IN_OUT, 220);
    float scroll;
    float maxScroll;

    private final ClickGuiFrame parent;

    public Panel(ModuleCategory category, ClickGuiFrame parent) {
        this.category = category;
        this.parent = parent;
        Aethel.getInstance().getModuleStorage().getModules().stream()
                .filter(m -> m.getCategory() == this.category)
                .sorted(Comparator.comparing(m -> m.getName().toLowerCase()))
                .forEach(m -> moduleComponents.add(new ModuleComponent(m, this)));
    }

    public void clampScroll() {
        if (maxScroll > 0) {
            scroll = MathHelper.clamp(scroll, -maxScroll, 0);
        } else {
            scroll = 0;
        }
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        animationAlpha.setValue(1f);
        float alpha = 255f;
        float cornerRadius = 7f; // Минимальное закругление
        float headerHeight = 19f;
        float innerInset = 6f; // Чуть больше отступ — внутренняя панель уже

        // Тень под внешней панелью
        Matrix4f shadowMat = matrixStack.peek().getPositionMatrix();
        DrawUtil.drawShadow(shadowMat, x, y, width, height, cornerRadius, 12f, ColorProvider.rgba(0, 0, 0, 100));

        // Внешняя панель — серая #161616
        int panelColor = ColorProvider.rgba(23, 23, 24, 255);
        DrawUtil.drawRound(x, y, width, height, cornerRadius, panelColor);

        String title = category.name();
        String capitalizedTitle = title.toUpperCase();
        float titleSize = 9.5f;
        float titleWidth = Fonts.SFBOLD.get().getWidth(capitalizedTitle, titleSize);
        float titleX = x + width / 2f - titleWidth / 2f;

        // Заголовок — жирный белый по центру
        DrawUtil.drawText(Fonts.SFBOLD.get(), capitalizedTitle, titleX, y + 6.5f, ColorProvider.rgba(255, 255, 255, 255), titleSize);

        // Тонкий разделитель под заголовком
        DrawUtil.drawRound(x + 6f, y + headerHeight, width - 12f, 0.5f, 0.25f, ColorProvider.rgba(255, 255, 255, 18));

        // Внутренняя (вторая) панель — полностью чёрная, уже и с минимальным закруглением
        float innerX = x + innerInset;
        float innerY = y + headerHeight + 2f;
        float innerWidth = width - innerInset * 2f;
        float innerHeight = height - headerHeight - 2f - innerInset;
        DrawUtil.drawShadow(shadowMat, innerX, innerY, innerWidth, innerHeight, 5.5f, 6f, ColorProvider.rgba(0, 0, 0, 40));
        DrawUtil.drawRound(innerX, innerY, innerWidth, innerHeight, 5.5f, ColorProvider.rgba(17, 17, 17, 255));

        float offset = 0;
        clampScroll();
        animation.run(scroll);

        Scissor.push();
        Scissor.setFromComponentCoordinates(innerX, innerY, innerWidth, innerHeight);

        float rowSpacing = 3f; // уменьшенный отступ между модулями
        float rowPadding = 4f;

        for (ModuleComponent component : moduleComponents) {
            if (parent.searchCheck(component.getModule().getName())) {
                continue;
            }

            component.setX(innerX + rowPadding);
            component.setY((float) (innerY + rowPadding + offset + animation.getValue()));
            component.setWidth(innerWidth - rowPadding * 2f);

            float baseHeight = 17; // уменьшенная высота строки модуля
            float extraHeight = 0;
            if (component.getAnimation().getValue() > 0.01f) {
                for (Component comp : component.getComponents()) {
                    float visibleProgress = MathHelper.clamp(comp.getAlphaAnimSetting().getValue(), 0f, 1f);
                    if (comp.isVisible() || visibleProgress > 0f) {
                        extraHeight += comp.getHeight() * visibleProgress;
                    }
                }
            }
            component.setHeight(baseHeight + (extraHeight * (float) component.getAnimation().getValue()));

            Scissor.setFromComponentCoordinates(innerX, innerY, innerWidth, innerHeight);
            component.render(matrixStack, mouseX, mouseY, partialTicks);
            Scissor.setFromComponentCoordinates(innerX, innerY, innerWidth, innerHeight);

            offset += component.getHeight() + rowSpacing;
        }
        maxScroll = Math.max(0, offset - (innerHeight - rowPadding * 2f));

        Scissor.unset();
        Scissor.pop();
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (HoverUtil.isHovered(mouseX, mouseY, x, y + 20, width, height - 20)) {
            for (ModuleComponent moduleComponent : moduleComponents) {
                if (!parent.searchCheck(moduleComponent.getModule().getName())) {
                    moduleComponent.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (ModuleComponent moduleComponent : moduleComponents) {
            if (!parent.searchCheck(moduleComponent.getModule().getName())) {
                moduleComponent.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, height)) {
            scroll += (float) (verticalAmount * 30f);
            clampScroll();
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleComponent moduleComponent : moduleComponents) {
            moduleComponent.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void charTyped(char chr, int modifiers) {
        for (ModuleComponent moduleComponent : moduleComponents) {
            moduleComponent.charTyped(chr, modifiers);
        }
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public List<ModuleComponent> getModuleComponents() {
        return moduleComponents;
    }

    public void setModuleComponents(List<ModuleComponent> moduleComponents) {
        this.moduleComponents = moduleComponents;
    }

    public Animation getAnimation() {
        return animation;
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
    }

    public Animation getAnimationAlpha() {
        return animationAlpha;
    }

    public void setAnimationAlpha(Animation animationAlpha) {
        this.animationAlpha = animationAlpha;
    }

    public Animation getScrollbarAnim() {
        return scrollbarAnim;
    }

    public float getScroll() {
        return scroll;
    }

    public void setScroll(float scroll) {
        this.scroll = scroll;
    }

    public float getMaxScroll() {
        return maxScroll;
    }

    public void setMaxScroll(float maxScroll) {
        this.maxScroll = maxScroll;
    }

    public ClickGuiFrame getParent() {
        return parent;
    }
}