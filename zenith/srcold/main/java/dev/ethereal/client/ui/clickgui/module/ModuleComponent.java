package dev.ethereal.client.ui.clickgui.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.setting.*;
import dev.ethereal.api.system.backend.KeyStorage;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.clickgui.module.settings.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleComponent extends ExpandableComponent {
    private final List<SettingComponent> settings = new ArrayList<>();
    private final Module module;
    @Setter private float round;
    @Setter private boolean last;
    @Setter private int index;

    private boolean bind;

    private final TimerUtil shakeTimer = new TimerUtil();

    private final AnimationUtil enableAnimation = new AnimationUtil();
    private final AnimationUtil bindAnimation = new AnimationUtil();
    private final AnimationUtil hoverAnimation = new AnimationUtil();

    public ModuleComponent(Module module) {
        this.module = module;

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bool) {
                settings.add(new BooleanComponent(bool));
            }

            if (setting instanceof MultiBooleanSetting multi) {
                settings.add(new MultiBooleanComponent(multi));
            }

            if (setting instanceof ModeSetting mode) {
                settings.add(new ModeComponent(mode));
            }

            if (setting instanceof SliderSetting slider) {
                settings.add(new SliderComponent(slider));
            }

            if (setting instanceof ColorSetting color) {
                settings.add(new ColorComponent(color));
            }

            if (setting instanceof RunSetting DoniKuni) {
                settings.add(new ButtonComponent(DoniKuni));
            }

            if (setting instanceof BindSetting sex) {
                settings.add(new BindComponent(sex));
            }

            if (setting instanceof ThemeSetting theme) {
                settings.add(new ThemeComponent(theme));
            }
        }

        enableAnimation.setValue(module.isEnabled() ? 1.0 : 0.0);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        updateOpen();
        hoverAnimation.update();
        enableAnimation.update();
        bindAnimation.update();
        bindAnimation.run(bind ? 1.0 : 0.0, 400, Easing.EXPO_OUT);
        hoverAnimation.run(MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getDefaultHeight()) ? 1.0 : 0.0, 300, Easing.QUINT_OUT);
        enableAnimation.run(module.isEnabled() ? 1.0 : 0.0, 200, Easing.EXPO_OUT);

        int fullAlpha = (int) (getAlpha() * 255f);

        String bindText = (bind ? "Binding: " : "Bind: ") + KeyStorage.getBind(module.getBind());
        String defaultText = module.getName();

        float fontSize = getDefaultHeight() * 0.375f;

        float openAnim = getAnim();
        float nameAnim = 1f - (float) bindAnimation.getValue();
        float bindAnim = (float) bindAnimation.getValue();

        float enabledAnim = (float) enableAnimation.getValue();

        // Текст: белый для активных, серый для неактивных
        Color textColor1 = new Color(255, 255, 255, (int)(fullAlpha * MathHelper.lerp(enabledAnim, 0.45f, 1f) * nameAnim));
        Color textColor2 = new Color(255, 255, 255, (int)(fullAlpha * 0.8f * bindAnim));

        float moduleRound = scaled(2.5f);

        // Считаем реальную высоту настроек
        float settingsH = getHeight() - getDefaultHeight();
        float totalH = getDefaultHeight() + settingsH;

        int bgAlpha = (int)(235 * getAlpha());
        Color darkBg = new Color(8, 8, 10, bgAlpha);
        Color activeBg = new Color(
            Math.min(235, UIColors.primary().getRed() / 3),
            Math.min(235, UIColors.primary().getGreen() / 3),
            Math.min(235, UIColors.primary().getBlue() / 3),
            bgAlpha
        );
        Color moduleBg = ColorUtil.interpolate(activeBg, darkBg, enabledAnim);

        // Единый фон на весь блок (модуль + настройки)
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), totalH, moduleRound, moduleBg);


        float textPadding = scaled(4f);
        float textY = getY() + getDefaultHeight() / 2f - fontSize / 2f -0.75f;

        // Если биндим - показываем "Binding..." по центру
        if (bind) {
            String bindingText = "Binding...";
            Fonts.MEDIUM.drawCenteredText(matrixStack, bindingText, getX() + getWidth() / 2f, textY, fontSize, textColor2);
        } else {
            // Название модуля слева
            if (nameAnim > 0) {
                Fonts.MEDIUM.drawText(matrixStack, defaultText, getX() + textPadding, textY, fontSize, textColor1);
            }

            // Справа: иконка настроек
            float rightX = getX() + getWidth() - textPadding;

            // Иконка настроек если есть настройки
            if (!settings.isEmpty()) {
                float iconSize = fontSize * 1.1f;
                float iconX = rightX - iconSize;
                float iconY = getY() + getDefaultHeight() / 2f - iconSize / 2f;
                Color iconColor = enabledAnim > 0f
                    ? ColorUtil.interpolate(ColorUtil.setAlpha(UIColors.primary(), fullAlpha), ColorUtil.setAlpha(UIColors.inactiveTextColor(), (int)(fullAlpha * 0.4f)), enabledAnim)
                    : ColorUtil.setAlpha(UIColors.inactiveTextColor(), (int)(fullAlpha * 0.4f));
                Fonts.ICONS.drawText(matrixStack, "S", iconX, iconY, iconSize, iconColor);
            }
        }

        // Рисуем настройки ПОСЛЕ модуля, чтобы они были внутри scissor панели
        if (openAnim > 0.0) renderSettings(context, mouseX, mouseY, delta, openAnim);
    }

    private void renderSettings(DrawContext context, int mouseX, int mouseY, float delta, float openAnim) {
        MatrixStack matrixStack = context.getMatrices();

        float govnarik = offset() * (1f + 0.7f * (1f - openAnim));
        float componentY = getY() + getDefaultHeight();

        List<SettingComponent> visibleSettings = settings.stream()
            .filter(s -> s.getVisibleAnimation().getValue() > 0.0)
            .toList();

        for (int i = 0; i < settings.size(); i++) {
            SettingComponent setting = settings.get(i);
            setting.getVisibleAnimation().update();
            setting.getVisibleAnimation().run(setting.getSetting().isVisible() ? 1.0 : 0.0, 120, Easing.SINE_OUT);
            float visibleAnim = (float) setting.getVisibleAnimation().getValue();
            if (visibleAnim > 0.0) {
                setting.setX(getX() + govnarik);
                setting.setY(componentY);
                setting.setWidth(getWidth() - govnarik * 2f);
                setting.setAlpha(visibleAnim * openAnim * getAlpha());

                setting.render(context, mouseX, mouseY, delta);

                float stepH = (setting.getHeight() + gap()) * visibleAnim * openAnim;

                // Разделитель по центру gap между настройками (не после последней)
                boolean isLast = visibleSettings.isEmpty() || setting == visibleSettings.getLast();
                if (!isLast && gap() > 0) {
                    float sepY = componentY + setting.getHeight() * visibleAnim + gap() * visibleAnim * openAnim / 2f;
                    int sepAlpha = (int)(255 * visibleAnim * openAnim * getAlpha() * 0.12f);
                    RenderUtil.RECT.draw(matrixStack, getX() + govnarik, sepY, getWidth() - govnarik * 2f, scaled(0.5f), 0f,
                        new Color(255, 255, 255, sepAlpha));
                }

                componentY += stepH;
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bind) {
            boolean deleteButton = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE;
            module.setBind(deleteButton ? -999 : keyCode);
            bind = false;
        }

        if (isNotOver()) return;

        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.9) continue;
            setting.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean hoveredToDefault = MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getDefaultHeight());

        if (bind && button != 1 && button != 2 && button != 0) {
            module.setBind(-100 + button);
            bind = false;
            return;
        }

        if (hoveredToDefault) {
            switch (button) {
                case 0 -> module.toggle();
                case 1 -> {
                    if (!settings.isEmpty()) {
                        toggleOpen();
                    }

                    if (!isOpen()) {
                        for (SettingComponent setting : settings) {
                            if (setting instanceof ExpandableSettingComponent e) {
                                e.setOpen(false);
                            }
                        }
                    }
                }

                case 2 -> bind = !bind;
            }

            return;
        }

        if (isNotOver()) return;

        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.9) continue;
            setting.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (isNotOver()) return;

        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.9) continue;
            setting.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    }

    public float getDefaultHeight() {
        return scaled(16f);
    }
}
