package dev.ethereal.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.system.backend.KeyStorage;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Font;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.clickgui.module.SettingComponent;

import java.awt.*;

public class BindComponent extends SettingComponent {
    private final BindSetting setting;

    private final AnimationUtil animation = new AnimationUtil();

    private boolean bind;

    public BindComponent(BindSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(12f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(12f);

        MatrixStack matrixStack = context.getMatrices();

        animation.update();
        animation.run(bind ? 1.0 : 0.0, 300, Easing.EXPO_OUT);

        float fontSize = scaled(5.5f);
        float halfY = getY() + getHeight() / 2f;
        int fullAlpha = (int) (getAlpha() * 255f);

        // Название слева
        Fonts.MEDIUM.drawText(matrixStack, setting.getName(), getX(), halfY - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));

        float anim = (float) animation.getValue();
        float reverseAnim = 1f - anim;

        String noneText = "None";
        String valueText = setting.getValue() == -999 ? noneText : KeyStorage.getBind(setting.getValue());
        String bindingText = "...";
        float valueWidth = Fonts.MEDIUM.getWidth(valueText, fontSize);
        float bindingWidth = Fonts.MEDIUM.getWidth(bindingText, fontSize);

        float totalWidth = (valueWidth * reverseAnim) + (bindingWidth * anim);

        float bindX = getX() + getWidth() - totalWidth - scaled(3.5f);
        float bindY = halfY - fontSize / 2f - scaled(1.5f);
        float bindWidth = totalWidth + scaled(7f);
        float bindHeight = fontSize + scaled(3f);
        float bindRound = scaled(2f);
        
        // Фон для бинда - темный
        RenderUtil.RECT.draw(matrixStack, bindX, bindY, bindWidth, bindHeight, bindRound, new Color(40, 40, 45, fullAlpha));

        if (reverseAnim > 0)
            Fonts.MEDIUM.drawText(matrixStack, valueText, bindX + scaled(3.5f), halfY - fontSize / 2f, fontSize, UIColors.textColor((int) (reverseAnim * fullAlpha)));

        if (anim > 0)
            Fonts.MEDIUM.drawText(matrixStack, bindingText, bindX + scaled(3.5f), halfY - fontSize / 2f, fontSize, UIColors.inactiveTextColor((int) (anim * fullAlpha)));
        
        // Тонкая линия-разделитель снизу
        RenderUtil.RECT.draw(matrixStack, getX(), getY() + getHeight() - 0.5f, getWidth(), 0.5f, 0f, new Color(255, 255, 255, (int)(fullAlpha * 0.05f)));
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bind) {
            setting.setValue(keyCode == GLFW.GLFW_KEY_DELETE ? -999 : keyCode);
            bind = false;
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (bind && button != 1 && button != 0) {
            setting.setValue(-100 + button);
            bind = false;
            return;
        }

        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            bind = !bind;
        }
    }

    private int getAlphaFrom(float anim) {
        return (int) (anim * getAlpha() * 255f);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {

    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {

    }
}
