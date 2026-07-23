package antileak.base.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import antileak.base.elysium;
import antileak.base.api.utils.color.ColorUtils;
import antileak.base.api.utils.math.HoveringUtils;
import antileak.base.api.utils.render.RenderUtils;
import antileak.base.api.utils.render.fonts.msdf.Font;
import antileak.base.api.utils.render.fonts.msdf.Fonts;
import antileak.base.api.utils.scissor.ScissorUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.Setting;
import antileak.base.client.ui.MenuPanel;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiRenderer {
    private final ClickGuiState state;
    private final ClickGuiSettingRenderer settingRenderer;
    private final ClickGuiThemeSelector themeSelector;
    private final MenuPanel menuPanel;

    public ClickGuiRenderer(ClickGuiState state, ClickGuiSettingRenderer settingRenderer, ClickGuiThemeSelector themeSelector, MenuPanel menuPanel) {
        this.state = state;
        this.settingRenderer = settingRenderer;
        this.themeSelector = themeSelector;
        this.menuPanel = menuPanel;
    }

    public void render(DrawContext context, int mouseX, int mouseY, Window window, float animationProgress) {
        if (window == null) return;

        float alphaMul = MathHelper.clamp(animationProgress, 0.0f, 1.0f);
        int shadeColor = getFadeShadeColor(alphaMul, 120);
        int colorTheme = getThemeColor();
        Module hoveredModule = null;

        Module.ModuleCategory[] categories = Module.ModuleCategory.values();
        for (int i = 0; i < categories.length; i++) {
            Module.ModuleCategory category = categories[i];
            float panelX = ClickGuiLayout.getCategoryPanelX(state.getX(), i);
            Module categoryHoveredModule = renderCategoryPanel(context, mouseX, mouseY, panelX, category, colorTheme, alphaMul, shadeColor);
            if (categoryHoveredModule != null) {
                hoveredModule = categoryHoveredModule;
            }
        }

        renderSearch(context, categories.length, colorTheme, alphaMul, getFadeShadeColor(alphaMul, 95));
        renderLanguageButton(context, categories.length, colorTheme, alphaMul, getFadeShadeColor(alphaMul, 95));
        themeSelector.render(context, window, state.getRenderOffsetY(), alphaMul, getFadeShadeColor(alphaMul, 95));
        renderDescription(context, window, hoveredModule, colorTheme, animationProgress);
    }

    private Module renderCategoryPanel(DrawContext context, int mouseX, int mouseY, float panelX, Module.ModuleCategory category, int colorTheme, float alphaMul, int shadeColor) {
        float slideOffX = menuPanel != null ? menuPanel.getPanelSlideOffsetX(category) : 0f;
        float slideOffY = menuPanel != null ? menuPanel.getPanelSlideOffsetY(category) : 0f;
        float panelAlphaScale = menuPanel != null ? menuPanel.getPanelAlpha(category) : 1f;

        float actualPanelX = panelX + slideOffX;
        float panelY = state.getY() + state.getRenderOffsetY() + slideOffY;
        float effectiveAlpha = alphaMul * panelAlphaScale;

        int iconTop;
        if (!elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            iconTop = elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        } else {
            iconTop = ColorUtils.getThemeColor();
        }

        RenderUtils.drawBlur(context.getMatrices(), actualPanelX, panelY, ClickGuiLayout.WIDTH, ClickGuiLayout.HEIGHT, 5, ColorUtils.rgba(255, 255, 255, 255));
        RenderUtils.drawRoundedRect(context.getMatrices(), actualPanelX, panelY, ClickGuiLayout.WIDTH, ClickGuiLayout.HEIGHT, 5, ColorUtils.rgba(0, 0, 0, 150));

        String categoryName = elysium.INSTANCE.localizationStorage.translateCategory(category);
        icons(14).drawCenteredString(context.getMatrices(), category.getIcons(), actualPanelX + 50 - (issue(15).getWidth(categoryName) / 2F) - 4, panelY + 13, alpha(colorTheme, effectiveAlpha));
        issue(15).drawCenteredString(context.getMatrices(), categoryName, actualPanelX + 52, panelY + 12, alpha(-1, effectiveAlpha));

        float contentY = ClickGuiLayout.getContentY(panelY);
        float contentHeight = ClickGuiLayout.getContentHeight();
        state.clampScroll(category, contentHeight);
        float moduleY = contentY + state.getScroll(category);
        Module hoveredModule = null;

        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(actualPanelX, contentY, ClickGuiLayout.WIDTH, contentHeight + 1);

        for (Module module : state.getModules(category)) {
            float openProgress = state.getOpenProgress(module);
            float moduleHeight = ClickGuiLayout.getModuleHeight(module, openProgress);

            if (moduleY + moduleHeight + ClickGuiLayout.MODULE_GAP >= contentY && moduleY <= contentY + contentHeight) {
                Module moduleHover = renderModule(context, mouseX, mouseY, actualPanelX, moduleY, module, openProgress, moduleHeight, colorTheme, effectiveAlpha, shadeColor);
                if (moduleHover != null) {
                    hoveredModule = moduleHover;
                }
            }

            moduleY += ClickGuiLayout.MODULE_GAP + moduleHeight;
        }

        ScissorUtils.pop();
        return hoveredModule;
    }

    private Module renderModule(DrawContext context, int mouseX, int mouseY, float panelX, float moduleY, Module module, float openProgress, float moduleHeight, int colorTheme, float alphaMul, int shadeColor) {
        List<Setting> settings = module.getSettings();
        renderModuleBackground(context, panelX, moduleY, moduleHeight, module.isEnable(), colorTheme, shadeColor);

        String moduleName = elysium.INSTANCE.localizationStorage.translate(module.getName());
        String bindText = "";
        if (state.getBindingModule() == module) {
            bindText = " [...]";
        }

        int nameColor = module.isEnable() ? alpha(-1, alphaMul) : alpha(ColorUtils.rgba(255, 255, 255, 170), alphaMul);
        int bindColor = module.isEnable() ? alpha(ColorUtils.rgba(255, 255, 255, 150), alphaMul) : alpha(ColorUtils.rgba(255, 255, 255, 100), alphaMul);

        issue(15).draw(context.getMatrices(), moduleName, panelX + ClickGuiLayout.SETTING_LEFT, moduleY + 6.5f, nameColor);
        if (!bindText.isEmpty()) {
            float nameWidth = issue(15).getWidth(moduleName);
            issue(11).draw(context.getMatrices(), bindText, panelX + ClickGuiLayout.SETTING_LEFT + nameWidth, moduleY + 9, bindColor);
        }

        if (settings != null && !settings.isEmpty() && ClickGuiLayout.hasVisibleSettings(settings)) {
            renderModuleDivineIcon(context, panelX, moduleY, module, module.isEnable(), alphaMul, -1);
        }

        if (settings != null && !settings.isEmpty()) {
            settingRenderer.render(context, module, panelX, moduleY, openProgress, colorTheme, mouseX, mouseY, state);
        }

        if (HoveringUtils.isHovered(mouseX, mouseY, panelX + ClickGuiLayout.MODULE_PADDING, moduleY, ClickGuiLayout.MODULE_INNER_WIDTH, moduleHeight)) {
            return module;
        }
        return null;
    }

    private void renderModuleBackground(DrawContext context, float panelX, float moduleY, float moduleHeight, boolean enabled, int colorTheme, int shadeColor) {
        RenderUtils.drawBlur(context.getMatrices(), panelX + ClickGuiLayout.MODULE_PADDING, moduleY - 0.5f, ClickGuiLayout.MODULE_INNER_WIDTH, moduleHeight + 1, 3f, 8.0f, ColorUtils.rgba(0, 0, 0, 150));

        int iconTop;
        if (!elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            iconTop = elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        } else {
            iconTop = ColorUtils.getThemeColor();
        }

        if (enabled) {
            RenderUtils.drawRoundedRect(context.getMatrices(), panelX + ClickGuiLayout.MODULE_PADDING, moduleY - 0.5f, ClickGuiLayout.MODULE_INNER_WIDTH, moduleHeight + 1, 2, ColorUtils.rgba(0, 0, 0, 10));
            return;
        }

        RenderUtils.drawRoundedRect(context.getMatrices(), panelX + ClickGuiLayout.MODULE_PADDING, moduleY - 0.5f, ClickGuiLayout.MODULE_INNER_WIDTH, moduleHeight + 1, 2, ColorUtils.rgba(0, 0, 0, 10));
    }

    private void renderModuleDots(DrawContext context, float panelX, float moduleY, Module module, boolean enabled, float alphaMul) {
        int dotsColor = enabled ? alpha(ColorUtils.rgba(255, 255, 255, 220), alphaMul) : alpha(ColorUtils.rgba(255, 255, 255, 100), alphaMul);
        float dotsX = panelX + 87.5f;
        float baseY = moduleY + 10f;
        float spacing = 2f;
        float radius = 2.1f;
        float bottomXOffset = 2.1f;
        float angle = state.updateDotsRotation(module, module.isOpen() ? (float) (Math.PI / 2f) : 0f);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float[][] offsets = {
                {0f, -spacing},
                {-bottomXOffset, spacing},
                {bottomXOffset, spacing}
        };

        for (float[] offset : offsets) {
            float rx = offset[0] * cos - offset[1] * sin;
            float ry = offset[0] * sin + offset[1] * cos;
            RenderUtils.drawRoundCircle(context.getMatrices(), dotsX + rx, baseY + ry, radius, dotsColor);
        }
    }

    private void renderModuleDivineIcon(DrawContext context, float panelX, float moduleY, Module module, boolean enabled, float alphaMul, int colorTheme) {
        antileak.base.api.utils.render.fonts.ttf.MCFontRenderer divineFont = divine(18);
        if (divineFont == null) {
            renderModuleDots(context, panelX, moduleY, module, enabled, alphaMul);
            return;
        }

        String icon = "h";
        float anchorX = panelX + 87.5f;
        float anchorY = moduleY + 10f;
        float drawX = anchorX - (divineFont.getStringWidth(icon) / 2.0f) ;
        float drawY = anchorY - (divineFont.getFontHeight() / 2.0f) - 1;
        int iconColor = enabled
                ? alpha(colorTheme, alphaMul)
                : alpha(ColorUtils.setAlphaColor(ColorUtils.darken(colorTheme, 0.9f), 110), alphaMul);
        divineFont.drawString(icon, drawX, drawY, iconColor);
    }

    private int getThemeColor() {
        if (!elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }

    private void renderSearch(DrawContext context, int categoryCount, int colorTheme, float alphaMul, int shadeColor) {
        float searchY = ClickGuiLayout.getSearchY(state.getY() + state.getRenderOffsetY());
        float searchW = getSearchWidth();
        float searchX = ClickGuiLayout.getSearchX(state.getX(), categoryCount, searchW) - 10;
        float searchH = ClickGuiLayout.SEARCH_HEIGHT;
        float selectionPaddingLeft = 3.0f;
        float selectionPaddingRight = 1.5f;
        int borderColor = ColorUtils.darken(colorTheme, 0.12f);

        RenderUtils.drawBlur(context.getMatrices(), searchX - 0.5f, searchY - 0.5f, searchW + 1f, searchH + 1f, 4, ColorUtils.rgba(255,255,255,255));
        RenderUtils.drawRoundedRect(context.getMatrices(), searchX - 0.5f, searchY - 0.5f, searchW + 1f, searchH + 1f, 4, ColorUtils.rgba(0,0,0,150));

        String query = state.getSearchText();
        String searchPlaceholder = elysium.INSTANCE.localizationStorage.translate("Search...");
        String text = query.isEmpty() ? searchPlaceholder : query;
        int textColor = query.isEmpty()
                ? alpha(ColorUtils.rgba(255, 255, 255, 110), alphaMul)
                : alpha(ColorUtils.rgba(255, 255, 255, 230), alphaMul);

        float iconX = searchX + ClickGuiLayout.SEARCH_ICON_X;
        float textX = searchX + ClickGuiLayout.SEARCH_TEXT_X;
        float textY = searchY + 6.2f;
        iconsNew(18).drawGradientStringHorizontal(context.getMatrices(), "l", iconX + 2, searchY + 6.5f, alpha(colorTheme, alphaMul), alpha(colorTheme, alphaMul));

        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(textX - selectionPaddingLeft, searchY, searchW - ClickGuiLayout.SEARCH_TEXT_X - ClickGuiLayout.SEARCH_RIGHT_PADDING + selectionPaddingLeft, searchH);
        if (!query.isEmpty() && state.hasSearchSelection()) {
            int selectionStart = state.getSearchSelectionStart();
            int selectionEnd = state.getSearchSelectionEnd();
            float selectedX = textX + issue(15).getWidth(query.substring(0, selectionStart)) - selectionPaddingLeft;
            float selectedW = issue(15).getWidth(query.substring(selectionStart, selectionEnd)) + selectionPaddingLeft + selectionPaddingRight;
            RenderUtils.drawRoundedRect(context.getMatrices(), selectedX, searchY + 3.8f, selectedW, 10.5f, 1.5f, alpha(ColorUtils.rgba(42, 115, 255, 155), alphaMul));
        }

        issue(15).draw(context.getMatrices(), text, textX, textY + 1, textColor);
        if (state.isSearchActive() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float cursorX = textX + issue(15).getWidth(query.substring(0, Math.min(state.getSearchCursor(), query.length())));
            RenderUtils.drawRoundedRect(context.getMatrices(), cursorX + 1f, searchY + 4.5f, 0.8f, 9f, 0f, alpha(ColorUtils.applyAlpha(colorTheme, 0.9f), alphaMul));
        }
        ScissorUtils.pop();
    }

    private void renderLanguageButton(DrawContext context, int categoryCount, int colorTheme, float alphaMul, int shadeColor) {
        float searchY = ClickGuiLayout.getSearchY(state.getY() + state.getRenderOffsetY());
        float searchW = getSearchWidth();
        float searchX = ClickGuiLayout.getSearchX(state.getX(), categoryCount, searchW) - 10;

        String langLabel = switch (elysium.INSTANCE.localizationStorage.getLanguage()) {
            case RUSSIAN -> "RU";
            case ENGLISH -> "EN";
            case UKRAINIAN -> "UA";
        };

        float btnW = issue(15).getWidth(langLabel) + 10f;
        float btnH = ClickGuiLayout.SEARCH_HEIGHT;
        float btnX = searchX + searchW + 4f;

        RenderUtils.drawBlur(context.getMatrices(), btnX - 0.5f, searchY - 0.5f, btnW + 1f, btnH + 1f, 4,  ColorUtils.rgba(255,255,255,255));
        RenderUtils.drawRoundedRect(context.getMatrices(), btnX - 0.5f, searchY - 0.5f, btnW + 1f, btnH + 1f, 4,  ColorUtils.rgba(0,0,0,150));

        issue(15).drawCenteredString(context.getMatrices(), langLabel, btnX + btnW / 2f, searchY + 7f, alpha(-1, alphaMul));
    }

    private void renderDescription(DrawContext context, Window window, Module hoveredModule, int colorTheme, float alphaMul) {
        if (hoveredModule == null) return;

        String description = hoveredModule.getDisplayDescription();
        if (description == null || description.isBlank() || "NULLABLE".equalsIgnoreCase(description) || "desc".equalsIgnoreCase(description)) return;

        String translatedDescription = elysium.INSTANCE.localizationStorage.translate(description);

        Font descriptionFont = issue(16);
        float maxWidth = window.getScaledWidth() - 40.0f;
        List<String> lines = wrapDescription(descriptionFont, translatedDescription, maxWidth);
        if (lines.isEmpty()) return;

        float lineHeight = descriptionFont.getHeight() - 2.0f;
        float boxHeight = lines.size() * lineHeight;
        float centerX = window.getScaledWidth() * 0.5f;
        float startY = ClickGuiLayout.THEME_PANEL_Y - boxHeight - 6.0f;

        for (int i = 0; i < lines.size(); i++) {
            descriptionFont.drawCenteredString(context.getMatrices(), lines.get(i), centerX, startY + i * lineHeight, ColorUtils.applyAlpha(-1, alphaMul));
        }
    }

    private List<String> wrapDescription(Font font, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.trim().split("\\s+");
        if (words.length == 0) return lines;

        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.getWidth(candidate) <= maxWidth || currentLine.isEmpty()) {
                currentLine.setLength(0);
                currentLine.append(candidate);
                continue;
            }
            lines.add(currentLine.toString());
            currentLine.setLength(0);
            currentLine.append(word);
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private float getSearchWidth() {
        String query = state.getSearchText();
        String text = query.isEmpty() ? "Search..." : query;
        float contentWidth = ClickGuiLayout.SEARCH_TEXT_X + issue(15).getWidth(text) + ClickGuiLayout.SEARCH_RIGHT_PADDING;
        return Math.max(ClickGuiLayout.SEARCH_WIDTH, contentWidth);
    }

    private Font issue(int size) {
        return Fonts.getFont("moe3", size);
    }

    private Font icons(int size) {
        return Fonts.getFont("icon", size);
    }

    private Font iconsNew(int size) {
        return Fonts.getFont("icon1", size);
    }

    private antileak.base.api.utils.render.fonts.ttf.MCFontRenderer divine(int size) {
        return antileak.base.api.utils.render.fonts.ttf.Fonts.getFont("divine.ttf", size);
    }

    private int alpha(int color, float alphaMul) {
        return ColorUtils.applyAlpha(color, alphaMul);
    }

    private int getFadeShadeColor(float alphaMul, int maxAlpha) {
        int alpha = MathHelper.clamp((int) ((1.0f - alphaMul) * maxAlpha), 0, 255);
        return ColorUtils.rgba(0, 0, 0, alpha);
    }
}
