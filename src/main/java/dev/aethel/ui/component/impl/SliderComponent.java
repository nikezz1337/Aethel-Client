package dev.aethel.ui.component.impl;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.ui.component.Component;
import dev.aethel.util.cursor.CursorManager;
import dev.aethel.util.render.helper.HoverUtil;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderComponent extends Component {
    private final SliderSetting setting;
    private boolean drag;
    private final Animation sliderAnimation = new Animation(Easing.QUINTIC_OUT, 300);

    public SliderComponent(SliderSetting setting) {
        this.setting = setting;
    }

    private double round(double num, double increment) {
        var v = (double) Math.round(num / increment) * increment;
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        float alpha = Math.min(getAlphaAnimSetting().getValue(), 1);
        int alphaInt = (int) (255 * alpha);

        String numberText = formatNumber(setting.getValue());
        float trackWidth = width - 9f;

        sliderAnimation.run((float) (trackWidth * (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin())));

        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), setting.getName(), x + 4.5f, y + 3f, ColorProvider.rgba(255, 255, 255, alphaInt), 6.5f, 0.6f, 1.0f, trackWidth);

        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), numberText, x + width - 4.5f - Fonts.SFSEMIBOLD.get().getWidth(numberText, 6.5f), y + 1f, ColorProvider.rgba(200, 200, 200, alphaInt), 6.5f);

        float trackY = y + 14f;
        DrawUtil.drawRound(x + 3f, trackY - 3.5f, trackWidth + 1, 3, 0.5f, ColorProvider.rgba(60, 60, 65, (int)(100 * alpha)));
        DrawUtil.drawRound(x + 3.5f, trackY - 3, trackWidth, 2, 0.5f, ColorProvider.rgba(20, 20, 25, alphaInt));

        float fillWidth = MathHelper.clamp(sliderAnimation.getValue(), 0, trackWidth);
        DrawUtil.drawRound(x + 3.5f, trackY - 3.5f, fillWidth, 3, 0.5f,
                ColorProvider.setAlpha(ColorProvider.getThemeColorTwo(), alphaInt),
                ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt));

        float circleX = x + 3.5f + fillWidth;
        DrawUtil.drawRound(circleX - 2.5f, trackY - 4f, 5, 4, 0.75f, ColorProvider.rgba(255, 255, 255, alphaInt));

        if (drag) {
            CursorManager.requestIBeam();
            double val = (mouseX - (x + 3.5f)) / trackWidth * (setting.getMax() - setting.getMin()) + setting.getMin();
            setting.setValue((float) MathHelper.clamp(round(val, setting.getStep()), setting.getMin(), setting.getMax()));
        }

        setHeight(15);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (HoverUtil.isHovered(mouseX, mouseY, x + 3f, y + 8f, width - 6f, 8f) && button == 0) {
            drag = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        drag = false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) drag = false;
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
