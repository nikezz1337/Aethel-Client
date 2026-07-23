package dev.ethereal.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.ScissorUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.clickgui.module.ExpandableComponent;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModeComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final ModeSetting setting;

    private final List<Bound> bounds = new ArrayList<>();
    private final Map<String, AnimationUtil> modeAnimations = new HashMap<>();

    public ModeComponent(ModeSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(13f);

        for (String mode : setting.getModes()) {
            modeAnimations.put(mode, new AnimationUtil());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        updateOpen();

        float fontSize = scaled(5.5f);
        float baseHeight = scaled(12f);
        float padding = scaled(0f);
        float anim = getValue();

        String valueText = setting.getValue();
        String name = setting.getName();
        float valueWidth = Fonts.MEDIUM.getWidth(valueText, fontSize);

        int fullAlpha = (int) (getAlpha() * 255f);

        // БЕЗ фона - чистый стиль
        
        Fonts.MEDIUM.drawText(matrixStack, name, getX(), getY() + baseHeight / 2f - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));
        Fonts.MEDIUM.drawText(matrixStack, valueText, getX() + getWidth() - valueWidth, getY() + baseHeight / 2f - fontSize / 2f, fontSize, UIColors.inactiveTextColor((int) ((1f - anim * 0.3f) * fullAlpha)));

        // Тонкая линия-разделитель снизу
        RenderUtil.RECT.draw(matrixStack, getX(), getY() + baseHeight - 0.5f, getWidth(), 0.5f, 0f, new Color(255, 255, 255, (int)(fullAlpha * 0.05f)));

        if (anim > 0.0) {
            bounds.clear();
            float currentX = getX() + padding;
            float currentY = getY() + baseHeight + scaled(2f);
            float chipHeight = scaled(9.5f);
            float chipPadding = scaled(2f);

            fullAlpha = (int) (getAlpha() * anim * 255f);

            for (String mode : setting.getModes()) {
                AnimationUtil modeAnim = modeAnimations.get(mode);
                modeAnim.update();
                modeAnim.run(setting.is(mode) ? 1.0 : 0.0, 200, Easing.EXPO_OUT);

                float textWidth = Fonts.MEDIUM.getWidth(mode, fontSize);
                float chipWidth = textWidth + scaled(4.5f);

                if (currentX + chipWidth > getX() + getWidth()) {
                    currentX = getX() + padding;
                    currentY += chipHeight + chipPadding;
                }

                bounds.add(new Bound(currentX, currentY, chipWidth, chipHeight, mode));

                float modeAnimValue = (float) modeAnim.getValue();
                // Синий если выбран, темный если нет
                Color chipBg = ColorUtil.interpolate(UIColors.gradient(0, fullAlpha), new Color(40, 40, 45, fullAlpha), modeAnimValue);

                RenderUtil.RECT.draw(matrixStack, currentX, currentY, chipWidth, chipHeight, scaled(2f), chipBg);
                Fonts.MEDIUM.drawCenteredText(matrixStack, mode, currentX + chipWidth / 2f, currentY + chipHeight / 2f - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));

                currentX += chipWidth + chipPadding;
            }

            float totalHeight = (currentY - getY() + chipHeight) * anim;
            setHeight(Math.max(totalHeight, baseHeight) + scaled(2f));
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
        if (button != 0) return; // Только ЛКМ для выбора
        
        for (Bound bound : bounds) {
            if (MouseUtil.isHovered(mouseX, mouseY, bound.x, bound.y, bound.width, bound.height)) {
                setting.setValue(bound.value);
            }
        }
    }

    private record Bound(float x, float y, float width, float height, String value) {}

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
