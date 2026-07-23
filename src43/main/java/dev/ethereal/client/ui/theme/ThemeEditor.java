package dev.ethereal.client.ui.theme;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.other.WindowResizeEvent;
import dev.ethereal.api.system.configs.ThemeManager;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.UIComponent;
import dev.ethereal.client.ui.clickgui.module.settings.ColorComponent;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ThemeEditor extends UIComponent {
    @Getter private static final ThemeEditor instance = new ThemeEditor();

    private final List<ThemeSelectable> themeSelectables = new ArrayList<>();

    private final Theme defaultTheme = new Theme("Quality");
    private Theme currentTheme = defaultTheme;

    private final ColorComponent primaryColorPicker;
    private final ColorComponent secondaryColorPicker;

    private final AnimationUtil openAnimation = new AnimationUtil();
    @Setter private boolean open;
    @Setter private float anim;

    public ThemeEditor() {
        setWidth(scaled(95f));
        setHeight(scaled(150f));
        
        // Создаем 2 колорпикера для primary и secondary
        primaryColorPicker = new ColorComponent(currentTheme.getElementColors().get(0));
        secondaryColorPicker = new ColorComponent(currentTheme.getElementColors().get(1));

        WindowResizeEvent.getInstance().subscribe(new Listener<>(-1, event -> {
            setWidth(scaled(95f));
        }));
    }

    private int alphaAnim() {
        return (int) (openAnimation.getValue() * anim * 255);
    }

    public void init() {
        refresh();
    }

    private void refresh() {
        ThemeManager.getInstance().refresh();
    }

    public void save(boolean last) {
        if (!last) {
            ThemeManager.getInstance().saveAll();
        } else if (currentTheme != null) {
            ThemeManager.getInstance().saveLastSelected(currentTheme);
        }
    }

    public void load() {
        ThemeManager.getInstance().refresh();
        Theme last = ThemeManager.getInstance().loadLastSelected();
        if (last != null) {
            themeSelectables.add(new ThemeSelectable(last));
            currentTheme = last;
        } else {
            themeSelectables.add(new ThemeSelectable(defaultTheme));
            currentTheme = defaultTheme;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnimation.update();
        openAnimation.run(open ? 1.0 : 0.0, 100, Easing.SINE_OUT);
        if (openAnimation.getValue() <= 0.1) return;

        float round = scaled(3f);
        float headerHeight = scaled(16f);
        float headerFontSize = scaled(6.5f);
        String text = "Theme Editor";
        float textWidth = Fonts.MEDIUM.getWidth(text, headerFontSize);
        setWidth(Math.max(textWidth * 1.8f, scaled(95f)));

        MatrixStack matrixStack = context.getMatrices();

        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), round, UIColors.backgroundBlur(alphaAnim()));
        Fonts.MEDIUM.drawText(matrixStack, text, getX() + getWidth() / 2f - textWidth / 2f, getY() + headerHeight / 2f - headerFontSize / 2f, headerFontSize, UIColors.textColor(alphaAnim()));

        float xOffset = getX() + offset();
        float widthOffset = getWidth() - offset() * 2f;
        float currentY = getY() + headerHeight + gap();

        // Рисуем список тем
        for (ThemeSelectable theme : themeSelectables) {
            theme.setAlpha((float) (openAnimation.getValue() * anim));
            theme.setX(xOffset);
            theme.setY(currentY);
            theme.setWidth(widthOffset);
            theme.setHeight(scaled(14f));

            theme.render(context, mouseX, mouseY, delta);
            currentY += theme.getHeight() + gap();
        }

        // Рисуем 2 колорпикера
        float colorHeight = scaled(12f);
        float colorFontSize = scaled(5.5f);
        float colorSize = scaled(9f);
        float colorRound = scaled(2f);
        
        // Primary Color
        String primaryText = "Primary";
        Fonts.MEDIUM.drawText(matrixStack, primaryText, xOffset, currentY + colorHeight / 2f - colorFontSize / 2f, colorFontSize, UIColors.textColor(alphaAnim()));
        float primaryColorX = xOffset + widthOffset - colorSize;
        RenderUtil.RECT.draw(matrixStack, primaryColorX, currentY + colorHeight / 2f - colorSize / 2f, colorSize, colorSize, colorRound, 
            ColorUtil.setAlpha(currentTheme.getPrimaryColor(), alphaAnim()));
        
        primaryColorPicker.setX(xOffset);
        primaryColorPicker.setY(currentY + colorHeight);
        primaryColorPicker.setWidth(widthOffset);
        primaryColorPicker.setAlpha(alphaAnim() / 255f);
        primaryColorPicker.render(context, mouseX, mouseY, delta);
        
        currentY += colorHeight + primaryColorPicker.getHeight() + gap();
        
        // Secondary Color
        String secondaryText = "Secondary";
        Fonts.MEDIUM.drawText(matrixStack, secondaryText, xOffset, currentY + colorHeight / 2f - colorFontSize / 2f, colorFontSize, UIColors.textColor(alphaAnim()));
        float secondaryColorX = xOffset + widthOffset - colorSize;
        RenderUtil.RECT.draw(matrixStack, secondaryColorX, currentY + colorHeight / 2f - colorSize / 2f, colorSize, colorSize, colorRound, 
            ColorUtil.setAlpha(currentTheme.getSecondaryColor(), alphaAnim()));
        
        secondaryColorPicker.setX(xOffset);
        secondaryColorPicker.setY(currentY + colorHeight);
        secondaryColorPicker.setWidth(widthOffset);
        secondaryColorPicker.setAlpha(alphaAnim() / 255f);
        secondaryColorPicker.render(context, mouseX, mouseY, delta);
        
        currentY += colorHeight + secondaryColorPicker.getHeight() + gap();

        setHeight(currentY - getY());
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open) return;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) return;

        // Клик по темам
        for (ThemeSelectable theme : themeSelectables) {
            if (MouseUtil.isHovered(mouseX, mouseY, theme.getX(), theme.getY(), theme.getWidth(), theme.getHeight())) {
                if (button == 0) {
                    currentTheme = theme.getTheme();
                    ThemeManager.getInstance().saveLastSelected(currentTheme);
                    
                    // Обновляем колорпикеры для новой темы
                    primaryColorPicker.mouseReleased(0, 0, 0); // Закрываем если открыт
                    secondaryColorPicker.mouseReleased(0, 0, 0);
                }
                return;
            }
        }
        
        // Клик по колорпикерам
        primaryColorPicker.mouseClicked(mouseX, mouseY, button);
        secondaryColorPicker.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (!open) return;
        
        primaryColorPicker.mouseReleased(mouseX, mouseY, button);
        secondaryColorPicker.mouseReleased(mouseX, mouseY, button);
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {

    }
}
