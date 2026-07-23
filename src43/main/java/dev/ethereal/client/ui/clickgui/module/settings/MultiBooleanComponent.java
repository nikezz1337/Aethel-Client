package dev.ethereal.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.clickgui.module.ExpandableComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MultiBooleanComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final MultiBooleanSetting setting;

    private final AnimationUtil settingsAnimation = new AnimationUtil();

    private final List<BooleanComponent> booleans = new ArrayList<>();

    public MultiBooleanComponent(MultiBooleanSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(12f);

        for (BooleanSetting value : setting.getValue()) {
            booleans.add(new BooleanComponent(value, true));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        updateOpen();
        settingsAnimation.update();

        if (isOpen()) {
            getAnim().run(1.0, 200, Easing.EXPO_OUT);
            settingsAnimation.run(getValue() >= 0.9 ? 1.0 : 0.0, 200, Easing.EXPO_OUT);
        } else {
            settingsAnimation.run(0.0, 150, Easing.EXPO_OUT);
            if (settingsAnimation.getValue() <= 0.1) {
                getAnim().run(0.0, 200, Easing.EXPO_OUT);
            }
        }

        float openAnim = getValue();
        float settingsAnim = (float) settingsAnimation.getValue();

        float fontSize = scaled(5.5f);
        float baseHeight = scaled(12f);
        int fullAlpha = (int) (getAlpha() * 255f);

        String arrow = ">";
        float arrowWidth = Fonts.MEDIUM.getWidth(arrow, fontSize);

        // БЕЗ фона - чистый стиль
        
        Fonts.MEDIUM.drawText(matrixStack, setting.getName(), getX(), getY() + baseHeight / 2f - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));
        
        // Стрелка справа
        matrixStack.push();
        matrixStack.translate(getX() + getWidth() - arrowWidth / 2f, getY() + baseHeight / 2f, 0);
        matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(90 * openAnim));
        Fonts.MEDIUM.drawCenteredText(matrixStack, arrow, 0, -fontSize / 2f, fontSize, UIColors.inactiveTextColor(fullAlpha));
        matrixStack.pop();
        
        // Тонкая линия-разделитель снизу
        RenderUtil.RECT.draw(matrixStack, getX(), getY() + baseHeight - 0.5f, getWidth(), 0.5f, 0f, new Color(255, 255, 255, (int)(fullAlpha * 0.05f)));

        if (openAnim > 0.0) {
            float currentY = scaled(1f);

            for (BooleanComponent component : booleans) {
                AnimationUtil anim = component.getVisibleAnimation();
                anim.update();
                anim.run(component.getSetting().isVisible() ? 1.0 : 0.0, 120, Easing.SINE_OUT);
                component.setX(getX());
                component.setY(getY() + baseHeight + currentY);
                component.setWidth(getWidth());
                currentY += (float) (component.getHeight() * anim.getValue());
            }

            setHeight(baseHeight + (currentY + scaled(0.5f)) * openAnim);

            if (settingsAnim > 0.0) {
                for (BooleanComponent component : booleans) {
                    component.setAlpha((float) (component.getVisibleAnimation().getValue() * getAlpha() * settingsAnim));
                    component.render(context, mouseX, mouseY, delta);
                }
            }
        } else {
            updateHeight(12f);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(12f))) {
            if (button == 1) toggleOpen(); // ПКМ для раскрытия
            return;
        }

        if (isNotOver()) return;

        for (BooleanComponent aBoolean : booleans) {
            if (aBoolean.getVisibleAnimation().getValue() < 0.8) continue;
            aBoolean.mouseClicked(mouseX, mouseY, button); // ЛКМ для переключения внутри
        }
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
