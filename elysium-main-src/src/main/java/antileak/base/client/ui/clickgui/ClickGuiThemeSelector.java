package antileak.base.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import antileak.base.elysium;
import antileak.base.api.utils.color.ColorUtils;
import antileak.base.api.utils.math.HoveringUtils;
import antileak.base.api.utils.render.RenderUtils;

import java.util.List;

public class ClickGuiThemeSelector {
    public void render(DrawContext context, Window window, float offsetY, float alphaMul, int shadeColor) {
        if (context == null || window == null) {
            return;
        }

        List<antileak.base.api.storages.implement.ThemeStorage.Themes> themes = elysium.INSTANCE.themeStorage.getThemeList();
        if (themes == null || themes.isEmpty()) {
            return;
        }

        float totalWidth = themes.size() * ClickGuiLayout.THEME_BOX_SIZE + (themes.size() - 1) * ClickGuiLayout.THEME_BOX_GAP;
        float panelWidth = totalWidth + ClickGuiLayout.THEME_SIDE_PADDING * 2f;
        float panelX = getThemePanelX(window, panelWidth);
        float panelY = ClickGuiLayout.THEME_PANEL_Y + offsetY;
        float startX = panelX + ClickGuiLayout.THEME_SIDE_PADDING;
        float startY = panelY + (ClickGuiLayout.THEME_PANEL_H - ClickGuiLayout.THEME_BOX_SIZE) / 2f;

        RenderUtils.drawBlur(context.getMatrices(), panelX, panelY, panelWidth, ClickGuiLayout.THEME_PANEL_H, 3.5f, ColorUtils.rgba(255,255,255,255));
        RenderUtils.drawRoundedRect(context.getMatrices(), panelX, panelY, panelWidth, ClickGuiLayout.THEME_PANEL_H, 3.5f, ColorUtils.rgba(0,0,0,150));
        antileak.base.api.storages.implement.ThemeStorage.Themes selected = elysium.INSTANCE.themeStorage.getThemes();
        for (int i = 0; i < themes.size(); i++) {
            antileak.base.api.storages.implement.ThemeStorage.Themes theme = themes.get(i);
            float boxX = startX + i * (ClickGuiLayout.THEME_BOX_SIZE + ClickGuiLayout.THEME_BOX_GAP);
            float boxY = startY;
            if (theme == selected) {
                RenderUtils.drawRoundedRect(
                        context.getMatrices(),
                        boxX - 0.5f,
                        boxY - 0.5f,
                        ClickGuiLayout.THEME_BOX_SIZE + 1,
                        ClickGuiLayout.THEME_BOX_SIZE + 1,
                        ClickGuiLayout.THEME_BOX_RADIUS + 0.5f,
                        ColorUtils.setAlphaColor(-1, Math.max(1, (int) (200 * alphaMul)))
                );
            }
            RenderUtils.drawRoundedRect(
                    context.getMatrices(),
                    boxX,
                    boxY,
                    ClickGuiLayout.THEME_BOX_SIZE,
                    ClickGuiLayout.THEME_BOX_SIZE,
                    ClickGuiLayout.THEME_BOX_RADIUS,
                    ColorUtils.applyAlpha(getThemeDisplayColor(theme), Math.max(0.55f, alphaMul))
            );
        }
    }

    public boolean handleClick(Window window, double mouseX, double mouseY, int button, float offsetY) {
        if (window == null || button != 0) {
            return false;
        }

        List<antileak.base.api.storages.implement.ThemeStorage.Themes> themes = elysium.INSTANCE.themeStorage.getThemeList();
        if (themes == null || themes.isEmpty()) {
            return false;
        }

        float totalWidth = themes.size() * ClickGuiLayout.THEME_BOX_SIZE + (themes.size() - 1) * ClickGuiLayout.THEME_BOX_GAP;
        float panelWidth = totalWidth + ClickGuiLayout.THEME_SIDE_PADDING * 2f;
        float panelX = getThemePanelX(window, panelWidth);
        float panelY = ClickGuiLayout.THEME_PANEL_Y + offsetY;
        float startX = panelX + ClickGuiLayout.THEME_SIDE_PADDING;
        float startY = panelY + (ClickGuiLayout.THEME_PANEL_H - ClickGuiLayout.THEME_BOX_SIZE) / 2f;

        if (!HoveringUtils.isHovered(mouseX, mouseY, panelX, panelY, panelWidth, ClickGuiLayout.THEME_PANEL_H)) {
            return false;
        }

        for (int i = 0; i < themes.size(); i++) {
            float boxX = startX + i * (ClickGuiLayout.THEME_BOX_SIZE + ClickGuiLayout.THEME_BOX_GAP);
            float boxY = startY;
            if (HoveringUtils.isHovered(mouseX, mouseY, boxX, boxY, ClickGuiLayout.THEME_BOX_SIZE, ClickGuiLayout.THEME_BOX_SIZE)) {
                elysium.INSTANCE.themeStorage.setThemes(themes.get(i));
                return true;
            }
        }
        return false;
    }

    private int getThemeDisplayColor(antileak.base.api.storages.implement.ThemeStorage.Themes theme) {
        int color = theme.getTheme().getColor(0);
        if (ColorUtils.alpha(color) == 0) {
            return ColorUtils.rgba(220, 220, 220, 180);
        }
        return color;
    }

    private float getThemePanelX(Window window, float panelWidth) {
        return (window.getScaledWidth() / 2F) - (panelWidth / 2F);
    }
}
