package dev.aethel.ui.component.impl;

import net.minecraft.client.util.math.MatrixStack;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.ui.component.Component;
import dev.aethel.util.cursor.CursorManager;
import dev.aethel.util.render.helper.HoverUtil;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;

public class BooleanComponent extends Component {
    private final BooleanSetting setting;
    private final Animation toggleAnim = new Animation(Easing.EXPO_OUT, 150);

    public BooleanComponent(BooleanSetting setting) {
        this.setting = setting;
        toggleAnim.setValue(setting.getValue() ? 1f : 0f);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        float alpha = Math.max(Math.min(getAlphaAnimSetting().getValue(), 1), 0);
        if (alpha < 0.02f) return;
        int alphaInt = (int) (255 * alpha);

        toggleAnim.run(setting.getValue());
        float anim = (float) toggleAnim.getValue();

        // Name слева
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), setting.getName(),
                x + 4.5f, y + 5f,
                ColorProvider.rgba(255, 255, 255, alphaInt), 6.5f, 0.4f, 1f, width);

        // Тоггл справа — как в src43
        float toggleHeight = 9f;
        float toggleWidth = toggleHeight * 1.85f;
        float toggleX = x + width - toggleWidth - 4f;
        float toggleY = y + (13f - toggleHeight) / 2f;
        float toggleRound = toggleHeight / 2f;

        // Кружок внутри
        float circleSize = toggleHeight * 0.68f;
        float circleGap = (toggleHeight - circleSize) / 2f;
        float circleY = toggleY + circleGap;
        float circleX = toggleX + circleGap + ((toggleWidth - circleSize - circleGap * 2f) * anim);

        // Фон тоггла
        int toggleBg = ColorProvider.interpolateColor(
                ColorProvider.rgba(40, 40, 45, alphaInt),
                ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt),
                anim
        );
        DrawUtil.drawRound(toggleX, toggleY, toggleWidth, toggleHeight, toggleRound, toggleBg);

        // Белый кружок
        DrawUtil.drawRound(circleX, circleY, circleSize, circleSize, circleSize / 2f,
                ColorProvider.rgba(255, 255, 255, alphaInt));

        if (HoverUtil.isHovered(mouseX, mouseY, x + 4, y, width - 8, 13f)) {
            CursorManager.requestHand();
        }

        setHeight(13);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && HoverUtil.isHovered(mouseX, mouseY, x + 4, y, width - 8, 13f)) {
            setting.toggle();
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {}

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {}

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
