package dev.ethereal.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.clickgui.module.SettingComponent;

import java.awt.*;
import java.time.Duration;

public class BooleanComponent extends SettingComponent {
    private final BooleanSetting setting;

    private final AnimationUtil toggleAnimation = new AnimationUtil();
    private final boolean inMenu;
    private Color color;

    public BooleanComponent(BooleanSetting setting) {
        this(setting, false);
    }

    public BooleanComponent(BooleanSetting setting, boolean inMenu) {
        super(setting);
        this.setting = setting;
        updateHeight(inMenu ? 11f : 12f);
        toggleAnimation.setValue(setting.getValue() ? 1.0 : 0.0);
        this.inMenu = inMenu;
        this.color = inMenu ? UIColors.widgetBlur() : UIColors.backgroundBlur();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float height = inMenu ? 11f : 12f;
        updateHeight(height);

        this.color = inMenu ? UIColors.widgetBlur() : UIColors.backgroundBlur();

        toggleAnimation.update();
        toggleAnimation.run(setting.getValue() ? 1.0 : 0.0, 150, Easing.EXPO_OUT);

        MatrixStack matrixStack = context.getMatrices();
        float fontSize = scaled(inMenu ? 5.3f : 5.5f);
        int fullAlpha = (int) (getAlpha() * 255f);

        float anim = (float) toggleAnimation.getValue();
        
        // БЕЗ фона - чистый стиль
        
        // Название слева
        Fonts.MEDIUM.drawText(matrixStack, setting.getName(), getX(), getY() + getHeight() / 2f - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));

        // Toggle справа
        float toggleHeight = scaled(inMenu ? 7f : 7.5f);
        float toggleWidth = toggleHeight * 1.85f;
        float toggleX = getX() + getWidth() - toggleWidth;
        float toggleY = getY() + getHeight() / 2f - toggleHeight / 2f;
        float toggleRound = toggleHeight / 2f;

        // Кружок внутри toggle
        float circleSize = toggleHeight * 0.68f;
        float circleGap = (toggleHeight - circleSize) / 2f;
        float circleY = toggleY + circleGap;
        float circleX = toggleX + circleGap + ((toggleWidth - circleSize - circleGap * 2f) * anim);

        // Фон toggle - темный или синий
        Color toggleBg = ColorUtil.interpolate(UIColors.gradient(0, fullAlpha), new Color(40, 40, 45, fullAlpha), anim);
        RenderUtil.RECT.draw(matrixStack, toggleX, toggleY, toggleWidth, toggleHeight, toggleRound, toggleBg);
        
        // Белый кружок
        RenderUtil.RECT.draw(matrixStack, circleX, circleY, circleSize, circleSize, circleSize / 2f, ColorUtil.setAlpha(Color.WHITE, fullAlpha));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.toggle();
        }
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
