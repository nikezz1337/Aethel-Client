package dev.ethereal.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import dev.ethereal.api.module.setting.ThemeSetting;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.clickgui.module.ExpandableComponent;
import dev.ethereal.client.ui.theme.ThemeEditor;

import java.awt.*;

public class ThemeComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final ThemeSetting setting;
    private final ColorComponent primaryColorPicker;
    private final ColorComponent secondaryColorPicker;

    public ThemeComponent(ThemeSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(12f);
        
        // Создаем колорпикеры для кастомной темы
        primaryColorPicker = new ColorComponent(ThemeEditor.getInstance().getCurrentTheme().getElementColors().get(0));
        secondaryColorPicker = new ColorComponent(ThemeEditor.getInstance().getCurrentTheme().getElementColors().get(1));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();
        updateOpen();

        float fontSize = scaled(5.3f);
        float baseHeight = scaled(12f);
        float padding = scaled(2f);
        float anim = getValue();

        int fullAlpha = (int) (getAlpha() * 255f);

        // Фон компонента
        RenderUtil.RECT.draw(matrixStack, getX() - 2.5f, getY(), getWidth() + 5f, getHeight(), scaled(2f), UIColors.settingsBackground(fullAlpha));
        
        // Название
        Fonts.MEDIUM.drawText(matrixStack, setting.getName(), getX() + padding + 1, getY() + baseHeight / 2f - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));
        
        // Текущая тема справа
        String currentTheme = setting.getSelectedPreset().getName();
        float textWidth = Fonts.MEDIUM.getWidth(currentTheme, fontSize);
        Fonts.MEDIUM.drawText(matrixStack, currentTheme, getX() + getWidth() - padding - textWidth - 2, getY() + baseHeight / 2f - fontSize / 2f, fontSize, UIColors.inactiveTextColor((int) ((1f - anim * 0.5f) * fullAlpha)));

        if (anim > 0.0) {
            float currentX = getX() + padding;
            float currentY = getY() + baseHeight + scaled(1.5f);
            float boxSize = scaled(18f);
            float boxGap = scaled(2f);
            float boxRound = scaled(3f);

            fullAlpha = (int) (getAlpha() * anim * 255f);

            // Рисуем квадратики с градиентами
            for (ThemeSetting.ThemePreset preset : setting.getPresets()) {
                if (currentX + boxSize > getX() + getWidth()) {
                    currentX = getX() + padding;
                    currentY += boxSize + boxGap;
                }

                boolean isSelected = setting.getSelectedPreset() == preset;
                boolean isCustom = preset.getName().equals("Custom");
                
                // Градиент для квадратика
                Color color1 = ColorUtil.setAlpha(preset.getPrimary(), fullAlpha);
                Color color2 = ColorUtil.setAlpha(preset.getSecondary(), fullAlpha);
                
                // Рамка если выбрано
                if (isSelected) {
                    RenderUtil.RECT.draw(matrixStack, currentX - 1, currentY - 1, boxSize + 2, boxSize + 2, boxRound, UIColors.textColor(fullAlpha));
                }
                
                // Градиентный квадратик
                RenderUtil.GRADIENT_RECT.draw(matrixStack, currentX, currentY, boxSize, boxSize, boxRound, color1, color2, color2, color1);
                
                // Название темы под квадратиком
                float nameSize = scaled(4.5f);
                float nameWidth = Fonts.MEDIUM.getWidth(preset.getName(), nameSize);
                Fonts.MEDIUM.drawText(matrixStack, preset.getName(), currentX + boxSize / 2f - nameWidth / 2f, currentY + boxSize + scaled(1f), nameSize, UIColors.textColor(fullAlpha));

                currentX += boxSize + boxGap;
            }

            float totalHeight = currentY - getY() + boxSize + scaled(6f);
            
            // Если выбран Custom, показываем колорпикеры
            if (setting.getSelectedPreset().getName().equals("Custom")) {
                currentY += boxSize + scaled(6f);
                
                // Primary color picker
                primaryColorPicker.setX(getX() + padding);
                primaryColorPicker.setY(currentY);
                primaryColorPicker.setWidth(getWidth() - padding * 2);
                primaryColorPicker.setAlpha(anim);
                primaryColorPicker.render(context, mouseX, mouseY, delta);
                
                currentY += primaryColorPicker.getHeight() + boxGap;
                
                // Secondary color picker
                secondaryColorPicker.setX(getX() + padding);
                secondaryColorPicker.setY(currentY);
                secondaryColorPicker.setWidth(getWidth() - padding * 2);
                secondaryColorPicker.setAlpha(anim);
                secondaryColorPicker.render(context, mouseX, mouseY, delta);
                
                totalHeight = currentY - getY() + secondaryColorPicker.getHeight();
            }
            
            setHeight(Math.max(totalHeight, baseHeight) * anim + baseHeight);
        } else {
            updateHeight(12f);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(12f))) {
            toggleOpen();
            return;
        }

        if (isNotOver()) return;
        
        float padding = scaled(2f);
        float currentX = getX() + padding;
        float currentY = getY() + scaled(12f) + scaled(1.5f);
        float boxSize = scaled(18f);
        float boxGap = scaled(2f);

        // Клик по квадратикам
        for (ThemeSetting.ThemePreset preset : setting.getPresets()) {
            if (currentX + boxSize > getX() + getWidth()) {
                currentX = getX() + padding;
                currentY += boxSize + boxGap;
            }

            if (MouseUtil.isHovered(mouseX, mouseY, currentX, currentY, boxSize, boxSize)) {
                setting.setSelectedPreset(preset);
                setting.applyToTheme(ThemeEditor.getInstance().getCurrentTheme());
                return;
            }

            currentX += boxSize + boxGap;
        }
        
        // Клик по колорпикерам если Custom выбран
        if (setting.getSelectedPreset().getName().equals("Custom")) {
            primaryColorPicker.mouseClicked(mouseX, mouseY, button);
            secondaryColorPicker.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (setting.getSelectedPreset().getName().equals("Custom")) {
            primaryColorPicker.mouseReleased(mouseX, mouseY, button);
            secondaryColorPicker.mouseReleased(mouseX, mouseY, button);
            
            // Обновляем кастомные цвета в настройке
            setting.setCustomPrimary(ThemeEditor.getInstance().getCurrentTheme().getPrimaryColor());
            setting.setCustomSecondary(ThemeEditor.getInstance().getCurrentTheme().getSecondaryColor());
        }
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
