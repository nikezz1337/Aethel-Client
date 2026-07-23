package dev.ethereal.client.ui.clickgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.ScissorUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.UIComponent;

import java.awt.*;

public class SearchComponent extends UIComponent {
    private static final int MAX_LENGTH = 28;

    @Getter private String text = "";
    @Setter private Runnable onTextChanged;
    private boolean typing = false;
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private String suggestion = "";
    private dev.ethereal.api.module.Module suggestedModule = null;

    private float typingAnim = 0f;
    private float suggestionAnim = 0f;

    public void setText(String text) {
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH);
        }
        this.text = text;
        cursorPosition = Math.min(cursorPosition, text.length());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        typingAnim += (typing ? 0.15f : -0.15f);
        typingAnim = Math.max(0, Math.min(1, typingAnim));

        updateSuggestion();

        suggestionAnim += (!suggestion.isEmpty() ? 0.1f : -0.1f);
        suggestionAnim = Math.max(0, Math.min(1, suggestionAnim));

        String displayText = text.isEmpty() && !typing ? "Поиск: Ctrl + F" : text;
        String fullText = displayText + (typing && !suggestion.isEmpty() ? suggestion : "");
        float fontSize = scaled(7f);
        float textWidth = Fonts.MEDIUM.getWidth(fullText, fontSize);
        float padding = scaled(6f);
        float minWidth = scaled(64f);
        float maxWidth = scaled(150f);

        setWidth(Math.max(minWidth, Math.min(maxWidth, textWidth + padding * 2)));
        setHeight(scaled(16f));

        float round = scaled(6f);
        RenderUtil.BLUR_RECT.draw(matrix, getX(), getY(), getWidth(), getHeight(), round,
            UIColors.blur((int) (getAlpha() * 255f)));

        float innerWidth = getWidth() - padding * 2;
        float textX = getX() + (getWidth() - Math.min(textWidth, innerWidth)) / 2f;
        float textY = getY() + getHeight() / 2f - fontSize / 2f;

        ScissorUtil.start(matrix, getX() + padding, getY(), innerWidth, getHeight());

        if (hasSelection()) {
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);
            String beforeSelection = text.substring(0, start);
            String selectedText = text.substring(start, end);

            float beforeWidth = Fonts.MEDIUM.getWidth(beforeSelection, fontSize);
            float selectedWidth = Fonts.MEDIUM.getWidth(selectedText, fontSize);

            Color selectionColor = new Color(0, 120, 215, (int) (getAlpha() * 200));
            RenderUtil.RECT.draw(matrix, textX + beforeWidth, textY, selectedWidth, fontSize, 0f, selectionColor);
        }

        Color textColor = typing ? UIColors.textColor((int) (getAlpha() * 255f)) : UIColors.inactiveTextColor((int) (getAlpha() * 255f));
        Fonts.MEDIUM.drawText(matrix, displayText, textX, textY, fontSize, textColor);

        if (typing && !suggestion.isEmpty() && !hasSelection() && suggestionAnim > 0.01f) {
            float mainTextWidth = Fonts.MEDIUM.getWidth(text, fontSize);
            int suggestionAlpha = (int) (getAlpha() * 80 * suggestionAnim);
            Color suggestionColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), suggestionAlpha);
            Fonts.MEDIUM.drawText(matrix, suggestion, textX + mainTextWidth, textY, fontSize, suggestionColor);
        }

        if (typing && !hasSelection() && (System.currentTimeMillis() % 1000 < 500)) {
            float cursorOffset = Fonts.MEDIUM.getWidth(text.substring(0, cursorPosition), fontSize);
            float cursorX = textX + cursorOffset;
            RenderUtil.RECT.draw(matrix, cursorX, textY, 1f, fontSize, 0f,
                ColorUtil.setAlpha(Color.WHITE, (int) (getAlpha() * 255f)));
        }

        ScissorUtil.stop(matrix);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight()) && button == 0) {
            typing = true;
            cursorPosition = getCursorIndexAt(mouseX);
        } else {
            typing = false;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!typing) return;

        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_A) {
            selectionStart = 0;
            selectionEnd = text.length();
            cursorPosition = text.length();
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB && !suggestion.isEmpty() && suggestedModule != null) {
            String candidate = text + suggestion;
            if (candidate.length() <= MAX_LENGTH) {
                text = candidate;
                cursorPosition = text.length();
                clearSelection();
                notifyTextChanged();
                openModuleSettings(suggestedModule);
            }
            return;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition > 0) {
                    text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                    cursorPosition--;
                    notifyTextChanged();
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition < text.length()) {
                    text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
                    notifyTextChanged();
                }
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (cursorPosition > 0) {
                    cursorPosition--;
                    clearSelection();
                }
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (cursorPosition < text.length()) {
                    cursorPosition++;
                    clearSelection();
                }
            }
            case GLFW.GLFW_KEY_ENTER -> {
                if (!text.isEmpty()) {
                    toggleModuleByName(text);
                }
                typing = false;
            }
            case GLFW.GLFW_KEY_ESCAPE -> typing = false;
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (typing && chr >= 32 && chr != 127) {
            if (hasSelection()) {
                deleteSelection();
            }
            if (text.length() >= MAX_LENGTH) {
                return;
            }
            text = text.substring(0, cursorPosition) + chr + text.substring(cursorPosition);
            cursorPosition++;
            notifyTextChanged();
        }
    }

    private void updateSuggestion() {
        suggestion = "";
        suggestedModule = null;
        if (text.isEmpty() || !typing) return;

        String lowerText = text.toLowerCase();
        var modules = dev.ethereal.api.module.ModuleManager.getInstance().getModules();

        for (var module : modules) {
            String moduleName = module.getName().toLowerCase();
            if (moduleName.startsWith(lowerText) && !moduleName.equals(lowerText)) {
                suggestion = module.getName().substring(text.length());
                suggestedModule = module;
                break;
            }
        }
    }

    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    private void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    private void deleteSelection() {
        if (!hasSelection()) return;

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        text = text.substring(0, start) + text.substring(end);
        cursorPosition = start;
        clearSelection();
        notifyTextChanged();
    }

    private int getCursorIndexAt(double mouseX) {
        String displayText = text.isEmpty() && !typing ? "Поиск: Ctrl + F" : text;
        float fontSize = scaled(7f);
        float padding = scaled(6f);
        float innerWidth = getWidth() - padding * 2;
        float textWidth = Fonts.MEDIUM.getWidth(displayText, fontSize);
        float textStartX = getX() + (getWidth() - Math.min(textWidth, innerWidth)) / 2f;
        float relativeX = (float) mouseX - textStartX;

        int position = 0;
        while (position < text.length()) {
            float charWidth = Fonts.MEDIUM.getWidth(text.substring(0, position + 1), fontSize);
            if (charWidth > relativeX) {
                break;
            }
            position++;
        }
        return position;
    }

    private void notifyTextChanged() {
        if (onTextChanged != null) {
            onTextChanged.run();
        }
    }

    public void clearText() {
        text = "";
        cursorPosition = 0;
        clearSelection();
        suggestion = "";
        notifyTextChanged();
    }

    public void activate() {
        typing = true;
        cursorPosition = text.length();
    }

    public void reset() {
        clearText();
        typing = false;
    }

    private void openModuleSettings(dev.ethereal.api.module.Module module) {
        var panels = ScreenClickGUI.getInstance().getPanels();
        for (var panel : panels) {
            for (var moduleComponent : panel.getModuleComponents()) {
                if (moduleComponent.getModule() == module) {
                    if (!moduleComponent.isOpen()) {
                        moduleComponent.toggleOpen();
                    }
                    break;
                }
            }
        }
    }

    private void toggleModuleByName(String name) {
        var modules = dev.ethereal.api.module.ModuleManager.getInstance().getModules();
        for (var module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                module.toggle();
                break;
            }
        }
    }
}
