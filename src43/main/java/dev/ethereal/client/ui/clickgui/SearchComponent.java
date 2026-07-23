package dev.ethereal.client.ui.clickgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
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
    @Getter private String text = "";
    @Setter private Runnable onTextChanged;
    private boolean typing = false;
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private long lastClickTime = 0;
    private String suggestion = ""; // Автодополнение
    private dev.ethereal.api.module.Module suggestedModule = null; // Модуль для автодополнения
    
    // Анимации
    private float typingAnim = 0f;
    private float suggestionAnim = 0f;

    public void setText(String text) {
        this.text = text;
        cursorPosition = Math.min(cursorPosition, text.length());
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        
        // Обновляем анимации
        typingAnim += (typing ? 0.15f : -0.15f);
        typingAnim = Math.max(0, Math.min(1, typingAnim));
        
        // Обновляем автодополнение
        updateSuggestion();
        
        suggestionAnim += (!suggestion.isEmpty() ? 0.1f : -0.1f);
        suggestionAnim = Math.max(0, Math.min(1, suggestionAnim));
        
        // Динамическая ширина по тексту (учитываем автодополнение)
        String displayText = text.isEmpty() && !typing ? "Поиск: Ctrl + F" : text;
        String fullText = displayText + (typing && !suggestion.isEmpty() ? suggestion : "");
        float fontSize = scaled(7f);
        float textWidth = Fonts.MEDIUM.getWidth(fullText, fontSize);
        float padding = scaled(4f);
        
        setWidth(textWidth + padding * 2);
        setHeight(scaled(16f));

        // Фон поиска
        RenderUtil.BLUR_RECT.draw(matrix, getX(), getY(), getWidth(), getHeight(), 
            scaled(1f),
            UIColors.blur((int) (getAlpha() * 255f)));

        float textX = getX() + (getWidth() - textWidth) / 2f;
        float textY = getY() + getHeight() / 2f - fontSize / 2f;
        
        // Рисуем выделение (синий фон)
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

        // Основной текст
        Color textColor = typing ? UIColors.textColor((int) (getAlpha() * 255f)) : UIColors.inactiveTextColor((int) (getAlpha() * 255f));
        Fonts.MEDIUM.drawText(matrix, displayText, textX, textY, fontSize, textColor);
        
        // Автодополнение (прозрачный текст с анимацией)
        if (typing && !suggestion.isEmpty() && !hasSelection() && suggestionAnim > 0.01f) {
            float mainTextWidth = Fonts.MEDIUM.getWidth(text, fontSize);
            int suggestionAlpha = (int) (getAlpha() * 80 * suggestionAnim);
            Color suggestionColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), suggestionAlpha);
            Fonts.MEDIUM.drawText(matrix, suggestion, textX + mainTextWidth, textY, fontSize, suggestionColor);
        }
        
        // Курсор
        if (typing && !hasSelection() && (System.currentTimeMillis() % 1000 < 500)) {
            float cursorOffset = Fonts.MEDIUM.getWidth(text.substring(0, cursorPosition), fontSize);
            float cursorX = textX + cursorOffset;
            RenderUtil.RECT.draw(matrix, cursorX, textY, 1f, fontSize, 0f, 
                ColorUtil.setAlpha(Color.WHITE, (int) (getAlpha() * 255f)));
        }
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
        // Не требуется для поиска
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Не требуется для поиска
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!typing) return;

        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_A) {
            // Select all - выделяем весь текст
            selectionStart = 0;
            selectionEnd = text.length();
            cursorPosition = text.length();
            return;
        }
        
        // Tab - автодополнение и открытие настроек
        if (keyCode == GLFW.GLFW_KEY_TAB && !suggestion.isEmpty() && suggestedModule != null) {
            text += suggestion;
            cursorPosition = text.length();
            clearSelection();
            notifyTextChanged();
            
            // Открываем настройки модуля
            openModuleSettings(suggestedModule);
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
                // Enter - включаем/выключаем модуль если он найден
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
            text = text.substring(0, cursorPosition) + chr + text.substring(cursorPosition);
            cursorPosition++;
            notifyTextChanged();
        }
    }
    
    private void updateSuggestion() {
        suggestion = "";
        suggestedModule = null;
        if (text.isEmpty() || !typing) return;
        
        // Ищем модули для автодополнения
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
        float textWidth = Fonts.MEDIUM.getWidth(displayText, fontSize);
        float textStartX = getX() + (getWidth() - textWidth) / 2f;
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
        // Находим ModuleComponent для этого модуля
        var panels = ScreenClickGUI.getInstance().getPanels();
        for (var panel : panels) {
            for (var moduleComponent : panel.getModuleComponents()) {
                if (moduleComponent.getModule() == module) {
                    // Открываем настройки модуля если они закрыты
                    if (!moduleComponent.isOpen()) {
                        moduleComponent.toggleOpen();
                    }
                    break;
                }
            }
        }
    }
    
    private void toggleModuleByName(String name) {
        // Ищем модуль по имени и переключаем его
        var modules = dev.ethereal.api.module.ModuleManager.getInstance().getModules();
        for (var module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                module.toggle();
                break;
            }
        }
    }
}
