package dev.ethereal.client.ui.clickgui;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.client.services.RenderService;
import dev.ethereal.client.ui.theme.ThemeEditor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ScreenClickGUI extends Screen implements QuickImports {
    @Getter private static final ScreenClickGUI instance = new ScreenClickGUI();

    private boolean open;
    private final AnimationUtil openAnimation = new AnimationUtil();

    private final List<Panel> panels = new ArrayList<>();
    private final ThemeEditor themeEditor = ThemeEditor.getInstance();
    private final SearchComponent searchComponent = new SearchComponent();

    public ScreenClickGUI() {
        super(Text.of(""));

        for (int i = 0; i < Category.values().length; i++) {
            Category category = Category.values()[i];
            Panel panel = new Panel(category);
            panel.setCategoryIndex(i * 45);
            panels.add(panel);
        }

        searchComponent.setOnTextChanged(this::updateSearch);
    }

    private void updateSearch() {
        String searchText = searchComponent.getText().toLowerCase();
        for (Panel panel : panels) {
            panel.setSearchText(searchText);
        }
    }

    @Override
    public void close() {
        ThemeEditor.getInstance().save(false);
        open = false;
        super.close();
    }

    @Override
    protected void init() {
        ThemeEditor.getInstance().init();
        // Сбрасываем поиск при открытии GUI
        searchComponent.reset();
        open = true;
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        openAnimation.update();
        openAnimation.run(open ? 1.0 : 0.0, 400, Easing.EXPO_OUT);

        float openAnim = (float) openAnimation.getValue();
        if (!open && openAnim < 0.1) close();

        float windowHeight = mc.getWindow().getScaledHeight();
        float windowWidth = mc.getWindow().getScaledWidth();

        MatrixStack matrixStack = context.getMatrices();

        // Рисуем затемненный фон как в Essence
        int bgAlpha = (int) (openAnim * 100);
        RenderUtil.RECT.draw(matrixStack, 0, 0, windowWidth, windowHeight, 0f, new Color(0, 0, 0, bgAlpha));

        // Центрируем панели и применяем масштабирование
        float centerX = windowWidth / 2f;
        float centerY = windowHeight / 2f;
        float scale = openAnim;

        // Трансформируем координаты мыши
        double transformedMouseX = (mouseX - centerX) / scale + centerX;
        double transformedMouseY = (mouseY - centerY) / scale + centerY;

        // Применяем масштабирование от центра
        matrixStack.push();
        matrixStack.translate(centerX, centerY, 0);
        matrixStack.scale(scale, scale, 1);
        matrixStack.translate(-centerX, -centerY, 0);

        float off = RenderService.getInstance().scaled(7f); // Отступ между панелями
        float panelWidth = RenderService.getInstance().scaled(105f); // Обновленная ширина - чуть шире
        float totalWidth = panels.size() * panelWidth + (panels.size() - 1) * off;

        float panelY = centerY - RenderService.getInstance().scaled(240f) / 2f; // Обновленная высота
        float firstX = (windowWidth - totalWidth) / 2f;

        if (themeEditor.isOpen()) {
            themeEditor.setAnim(openAnim);
            // Правый нижний угол
            themeEditor.setX(windowWidth - themeEditor.getWidth() - RenderService.getInstance().scaled(10f));
            themeEditor.setY(windowHeight - themeEditor.getHeight() - RenderService.getInstance().scaled(10f));
        }

        for (Panel panel : panels) {
            panel.setAlpha(openAnim);
            panel.setY(panelY);
            panel.setX(firstX + panels.indexOf(panel) * (panelWidth + off));

            panel.render(context, (int)transformedMouseX, (int)transformedMouseY, delta);
        }

        // Рисуем поиск внизу
        searchComponent.setAlpha(openAnim);
        searchComponent.setX(centerX - searchComponent.getWidth() / 2f);
        searchComponent.setY(panelY + RenderService.getInstance().scaled(270f)); // Обновленная позиция
        searchComponent.render(context, (int)transformedMouseX, (int)transformedMouseY, delta);

        matrixStack.pop();

        themeEditor.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            open = false;
            mc.mouse.lockCursor();
            return true;
        }

        // Ctrl+F активирует поиск
        if (keyCode == GLFW.GLFW_KEY_F && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            searchComponent.activate();
            return true;
        }

        searchComponent.keyPressed(keyCode, scanCode, modifiers);
        panels.forEach(panel -> panel.keyPressed(keyCode, scanCode, modifiers));
        themeEditor.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;
        float scale = (float) openAnimation.getValue();

        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;

        searchComponent.mouseClicked(transformedX, transformedY, button);
        panels.forEach(panel -> panel.mouseClicked(transformedX, transformedY, button));
        themeEditor.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;
        float scale = (float) openAnimation.getValue();

        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;

        panels.forEach(panel -> panel.mouseReleased(transformedX, transformedY, button));
        themeEditor.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;
        float scale = (float) openAnimation.getValue();

        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;

        // Проверяем скролл внутри панелей
        for (Panel panel : panels) {
            panel.mouseScrolled(transformedX, transformedY, horizontalAmount, verticalAmount);
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        searchComponent.charTyped(chr, modifiers);
        return themeEditor.charTyped(chr, modifiers);
    }

    @Override public void blur() {}
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
    @Override public boolean shouldPause() { return false; }
    @Override protected void applyBlur() {}
}
