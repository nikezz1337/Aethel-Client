package dev.ethereal.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import dev.ethereal.api.module.setting.RunSetting;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.clickgui.module.SettingComponent;

import java.awt.*;

public class ButtonComponent extends SettingComponent {
    private final RunSetting setting;

    private final AnimationUtil hoverAnimation = new AnimationUtil();

    public ButtonComponent(RunSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(12f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(12f);

        MatrixStack matrixStack = context.getMatrices();
        int fullAlpha = (int) (getAlpha() * 255f);

        hoverAnimation.update();
        hoverAnimation.run(hovered(mouseX, mouseY) ? 1.0 : 0.0, 300, Easing.EXPO_OUT);

        float hoverAnim = (float) hoverAnimation.getValue();
        // Синий при наведении, темный без наведения
        Color btnBg = ColorUtil.interpolate(UIColors.gradient(0, fullAlpha), new Color(40, 40, 45, fullAlpha), hoverAnim);

        float fontSize = scaled(5.5f);
        float round = scaled(2f);
        
        // Кнопка по центру
        float btnWidth = getWidth() * 0.9f;
        float btnX = getX() + (getWidth() - btnWidth) / 2f;
        
        RenderUtil.RECT.draw(matrixStack, btnX, getY(), btnWidth, getHeight(), round, btnBg);
        Fonts.MEDIUM.drawCenteredText(matrixStack, setting.getName(), getX() + getWidth() / 2f, getY() + getHeight() / 2f - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));
        
        // Тонкая линия-разделитель снизу
        RenderUtil.RECT.draw(matrixStack, getX(), getY() + getHeight() - 0.5f, getWidth(), 0.5f, 0f, new Color(255, 255, 255, (int)(fullAlpha * 0.05f)));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered(mouseX, mouseY)) {
            if (setting.getValue() != null) {
                setting.getValue().run();
            }
        }
    }

    private boolean hovered(double mouseX, double mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(12f));
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
