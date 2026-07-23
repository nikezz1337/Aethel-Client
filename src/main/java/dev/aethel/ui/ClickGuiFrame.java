package dev.aethel.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.list.misc.ClientSounds;
import dev.aethel.ui.component.SearchField;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.cursor.CursorManager;
import dev.aethel.util.render.helper.HoverUtil;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiFrame extends Screen implements IMinecraft {

    private final List<Panel> panels = new ArrayList<>();
    private final SearchField searchField;

    private final ThemeManagerWindow themeManager;

    // Zoom animation from Ethereal
    public boolean open;
    public final Animation openAnimation = new Animation(Easing.EXPO_OUT, 400);

    public ClickGuiFrame() {
        super(Text.of("Avalora Frame"));
        searchField = new SearchField("Search...");
        for (ModuleCategory category : ModuleCategory.values()) {
            panels.add(new Panel(category, this));
        }
        themeManager = new ThemeManagerWindow(this);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        CursorManager.reset();
        CursorManager.resetIBeam();
        CursorManager.resetClick();

        int windowWidth = mc.getWindow().getScaledWidth();
        int windowHeight = mc.getWindow().getScaledHeight();

        openAnimation.run(open);
        float openAnim = (float) Math.min(1, Math.max(0, openAnimation.getValue()));
        if (!open && openAnim < 0.1) close();

        // Dark overlay из Ethereal
        int bgAlpha = (int) (openAnim * 100);
        DrawUtil.drawRound(0, 0, windowWidth, windowHeight, 0f, ColorProvider.rgba(0, 0, 0, bgAlpha));

        float centerX = windowWidth / 2f;
        float centerY = windowHeight / 2f;
        float scale = openAnim;

        var matrixStack = context.getMatrices();

        // Трансформируем координаты мыши
        double transformedMouseX = scale > 0.01 ? (mouseX - centerX) / scale + centerX : centerX;
        double transformedMouseY = scale > 0.01 ? (mouseY - centerY) / scale + centerY : centerY;

        // Zoom animation from Ethereal
        matrixStack.push();
        matrixStack.translate(centerX, centerY, 0);
        matrixStack.scale(scale, scale, 1);
        matrixStack.translate(-centerX, -centerY, 0);

        float panelWidth = 120f; // Пошире
        float spacing = 2.9f;
        float panelHeight = 270f;
        float panelTotalWidth = panels.size() * (panelWidth + spacing) - spacing;

        float startX = (windowWidth - panelTotalWidth) / 2f;
        float panelY = (windowHeight - panelHeight) / 2f - 20;

        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            panel.getAnimationAlpha().setDuration(650);
            panel.getAnimationAlpha().run(1);
            panel.getAnimationAlpha().setEasing(Easing.QUINTIC_OUT);

            panel.setX(startX + i * (panelWidth + spacing));
            panel.setY(panelY);
            panel.setWidth(panelWidth);
            panel.setHeight(panelHeight);

            panel.render(matrixStack, (int)transformedMouseX, (int)transformedMouseY, delta);
        }

        float searchW = 140;
        float searchH = 18;
        float searchX = windowWidth / 2f - searchW / 2f;
        float searchY = panelY + panelHeight + 15;

        searchField.setBounds(searchX, searchY, searchW, searchH);
        searchField.render(context, (int)transformedMouseX, (int)transformedMouseY, delta);

        themeManager.setX(20);
        themeManager.setY(panelY);
        themeManager.setHeight(panelHeight);
        themeManager.render(matrixStack, (int)transformedMouseX, (int)transformedMouseY, delta);

        for (Panel panel : panels) {
            boolean isMouseInPanel = HoverUtil.isHovered(mouseX, mouseY, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight());
            for (ModuleComponent component : panel.getModuleComponents()) {
                if (component.isHovered() && isMouseInPanel && searchField.isEmpty()) {
                    String desc = component.getModule().getDesc();
                    if (desc != null && !desc.isEmpty()) {
                        float descCorner = 7f;
                        float descPadX = 8, descPadY = 5;
                        float descTextSize = 8f;
                        float descTextW = Fonts.SFREGULAR.get().getWidth(desc, descTextSize);
                        float descPanelW = descTextW + descPadX * 2;
                        float descPanelH = 10 + descPadY * 2;
                        float descPanelX = windowWidth / 2f - descPanelW / 2f;
                        float descPanelY = windowHeight / 2f - 180;

                        DrawUtil.drawRound(descPanelX, descPanelY, descPanelW, descPanelH, descCorner, ColorProvider.rgba(23, 23, 24, 255));
                        DrawUtil.drawRound(descPanelX + 3, descPanelY + 3, descPanelW - 6, descPanelH - 6, 5f, ColorProvider.rgba(17, 17, 17, 255));
                        DrawUtil.drawText(Fonts.SFREGULAR.get(), desc, descPanelX + descPadX, descPanelY + descPadY + 1, ColorProvider.rgba(255, 255, 255, 255), descTextSize);
                    }
                }
            }
        }

        matrixStack.pop();


        long window = mc.getWindow().getHandle();
        if (CursorManager.shouldBeHand()) GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
        else if (CursorManager.shouldIBeam()) GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR));
        else if (CursorManager.shouldClick()) GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR));
        else GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
    }

    public boolean searchCheck(String text) {
        return !searchField.isEmpty() && !text.replaceAll(" ", "").toLowerCase().contains(searchField.text.replaceAll(" ", "").toLowerCase());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;
        float scale = (float) Math.min(1, Math.max(0, openAnimation.getValue()));

        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;

        themeManager.mouseClicked(transformedX, transformedY, button);
        searchField.mouseClicked(transformedX, transformedY, button);

        if (searchField.isEmpty()) {
            for (Panel panel : panels) {
                if (HoverUtil.isHovered(transformedX, transformedY, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight())) {
                    panel.mouseClicked(transformedX, transformedY, button);
                }
            }
        } else {
            for (Panel panel : panels) {
                panel.mouseClicked(transformedX, transformedY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;
        float scale = (float) Math.min(1, Math.max(0, openAnimation.getValue()));

        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;

        themeManager.mouseReleased(transformedX, transformedY, button);
        for (Panel panel : panels) {
            panel.mouseReleased(transformedX, transformedY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;
        float scale = (float) Math.min(1, Math.max(0, openAnimation.getValue()));

        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;

        themeManager.mouseScrolled(transformedX, transformedY, horizontalAmount, verticalAmount);
        for (Panel panel : panels) {
            panel.mouseScrolled(transformedX, transformedY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            open = false;
            long window = mc.getWindow().getHandle();
            GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
        }

        // Ctrl+F for search (из Ethereal)
        if (keyCode == GLFW.GLFW_KEY_F && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            searchField.setFocused(true);
            return true;
        }

        searchField.keyPressed(keyCode, scanCode, modifiers);
        for (Panel panel : panels) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                panel.getAnimationAlpha().setValue(0);
                panel.getAnimationAlpha().reset();
            }
            panel.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        searchField.charTyped(chr, modifiers);
        for (Panel panel : panels) {
            panel.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        if (ClientSounds.INSTANCE != null && ClientSounds.INSTANCE.isEnabled()) {
            ClientSounds.INSTANCE.playCloseGui();
        }
    }

    @Override
    public void close() {
        ThemeManagerWindow window = themeManager;
        if (window != null) {
            window.saveThemes();
        }
        open = false;
        super.close();
    }

    public List<Panel> getPanels() {
        return panels;
    }

    public SearchField getSearchField() {
        return searchField;
    }

    public ThemeManagerWindow getThemeManager() {
        return themeManager;
    }
}