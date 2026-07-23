package dev.ethereal.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.clickgui.module.SettingComponent;

import java.awt.*;

public class SliderComponent extends SettingComponent {
    private final SliderSetting setting;
    private boolean dragging;
    private float currentWidth;
    private float previewValue;

    private final AnimationUtil dragAnimation = new AnimationUtil();

    public SliderComponent(SliderSetting setting) {
        super(setting);
        this.setting = setting;
        this.previewValue = setting.getValue();
        updateHeight(getDefaultHeight());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(getDefaultHeight());

        dragAnimation.update();
        dragAnimation.run(dragging ? 1.0 : 0.0, 300, Easing.EXPO_OUT);

        MatrixStack matrixStack = context.getMatrices();

        float currentValue = dragging ? previewValue : setting.getValue();
        float fontSize = fontSize();
        String valueText = String.valueOf(currentValue);
        float valueWidth = Fonts.MEDIUM.getWidth(valueText, fontSize);

        int fullAlpha = (int) (getAlpha() * 255f);

        float progress = (currentValue - setting.getMin()) / (setting.getMax() - setting.getMin()) * sliderWidth();
        currentWidth = MathUtil.interpolate(currentWidth, progress, 0.2f);

        // Название слева, значение справа
        Fonts.MEDIUM.drawText(matrixStack, setting.getName(), getX(), getY(), fontSize, UIColors.textColor(fullAlpha));
        Fonts.MEDIUM.drawText(matrixStack, valueText, getX() + getWidth() - valueWidth, getY(), fontSize, UIColors.inactiveTextColor(fullAlpha));

        float sliderRound = sliderHeight() / 2f;
        float knobX = MathHelper.clamp(sliderX() + currentWidth - knobSize() / 2f, sliderX(), sliderX() + sliderWidth() - knobSize());
        float knobOffset = (knobSize() - sliderHeight()) / 2f;

        Color gradStart = UIColors.gradient(0, fullAlpha);
        Color gradEnd = UIColors.gradient(60, fullAlpha);

        // Фон слайдера - темный
        RenderUtil.RECT.draw(matrixStack, sliderX(), sliderY(), sliderWidth(), sliderHeight(), sliderRound, new Color(40, 40, 45, fullAlpha));
        
        // Градиент прогресса
        RenderUtil.GRADIENT_RECT.draw(matrixStack, sliderX(), sliderY(), currentWidth, sliderHeight(), sliderRound, gradStart, gradEnd, gradEnd, gradStart);
        
        // Белый кружок-ручка
        RenderUtil.RECT.draw(matrixStack, knobX, sliderY() - knobOffset, knobSize(), knobSize(), knobSize() / 2f, ColorUtil.setAlpha(Color.WHITE, fullAlpha));

        // Тонкая линия-разделитель снизу
        RenderUtil.RECT.draw(matrixStack, getX(), getY() + getHeight() - 0.5f, getWidth(), 0.5f, 0f, new Color(255, 255, 255, (int)(fullAlpha * 0.05f)));

        setHeight(sliderHeight() + (sliderY() - getY()) + knobSize() / 2f);

        if (dragging) {
            float newValue = (mouseX - getX()) / sliderWidth();
            newValue = setting.getMin() + newValue * (setting.getMax() - setting.getMin());
            newValue = Math.round(newValue / setting.getStep()) * setting.getStep();
            previewValue = MathUtil.round(Math.max(setting.getMin(), Math.min(setting.getMax(), newValue)), setting.getStep());
        }

        if (dragging && !MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.setValue(previewValue);
            dragging = false;
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            setting.setValue(previewValue);
        }
        dragging = false;
    }

    private float fontSize() { return scaled(5.5f); }
    private float sliderWidth() { return getWidth(); }
    private float sliderHeight() { return scaled(2.3f); }
    private float knobSize() { return scaled(6.5f); }
    private float sliderY() { return getY() + fontSize() + scaled(2.3f); }
    private float sliderX() { return getX(); }
    private float getDefaultHeight() { return fontSize() + scaled(2.3f) + knobSize(); }

    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
}