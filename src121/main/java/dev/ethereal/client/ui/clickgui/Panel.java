package dev.ethereal.client.ui.clickgui;

import dev.ethereal.api.utils.color.ColorUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.other.WindowResizeEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleManager;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.ScissorUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.UIComponent;
import dev.ethereal.client.ui.clickgui.module.ModuleComponent;
import dev.ethereal.client.ui.clickgui.module.SettingComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Panel extends UIComponent {
    private final Category category;
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();

    @Setter private int categoryIndex;
    @Setter private String searchText = "";

    private float scroll = 0f;
    private float smoothedScroll = 0f;
    private final AnimationUtil scrollAnimation = new AnimationUtil();

    // Фиксированная высота панели - оптимальная
    private static final float FIXED_PANEL_HEIGHT = 245f;

    public Panel(Category category) {
        this.category = category;

        ModuleManager.getInstance().getModules().stream()
                .filter(module -> module.getCategory() == category)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .forEach(module -> {
                    ModuleComponent moduleComponent = new ModuleComponent(module);
                    moduleComponent.setRound(getRound() * 2f);
                    moduleComponents.add(moduleComponent);
                });

        if (!moduleComponents.isEmpty()) {
            moduleComponents.getLast().setLast(true);
        }

        int index = categoryIndex;
        for (ModuleComponent module : moduleComponents) {
            module.setIndex(index);
            index += 45;
        }

        Events.subscribe(this);
    }

    @EventHandler(priority = -1)
    public void onWindowResize(WindowResizeEvent event) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateThings();
        renderThings(context, mouseX, mouseY, delta);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleComponent module : moduleComponents) {
            if (shouldRenderModule(module)) {
                module.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (inPanel(mouseX, mouseY)) {
            for (ModuleComponent module : moduleComponents) {
                if (!shouldRenderModule(module)) continue;

                if (MouseUtil.isHovered(mouseX, mouseY, module.getX(), module.getY(), module.getWidth(), module.getHeight())) {
                    module.mouseClicked(mouseX, mouseY, button);
                    return;
                }
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (inPanel(mouseX, mouseY)) {
            for (ModuleComponent module : moduleComponents) {
                if (!shouldRenderModule(module)) continue;

                if (MouseUtil.isHovered(mouseX, mouseY, module.getX(), module.getY(), module.getWidth(), module.getHeight())) {
                    module.mouseReleased(mouseX, mouseY, button);
                }
            }
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            scroll += (float) (verticalAmount * 40.0);
        }
    }

    private void updateThings() {
        scrollAnimation.update();
        scrollAnimation.run(scroll, 300, Easing.EXPO_OUT);
        smoothedScroll = (float) scrollAnimation.getValue();

        // Размеры панели - чуть шире
        float w = 105f; // Ширина панели - увеличена
        setWidth(scaled(w));
        setHeight(scaled(FIXED_PANEL_HEIGHT));
        moduleComponents.forEach(m -> m.setRound(getPanelRound() * 1.5f));
    }

    private void renderThings(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        int fullAlpha = (int) (getAlpha() * 255f);

        float panelRound = getPanelRound();
        float headerHeight = getHeaderHeight();

        // Тонкая обводка панели (рисуется до фона — чуть больше)
        RenderUtil.RECT.drawBorder(matrixStack, getX(), getY(), getWidth(), getHeight(), panelRound, scaled(0.5f),
                new Color(255, 255, 255, (int)(fullAlpha * 0.12f)));

        // Единый фон для всей панели — тёмный, альфа 230, блюр через ползунок
        Color panelBg = new Color(8, 8, 10, (int)(230 * getAlpha()));
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), panelRound, panelBg);

        // Иконка + название категории по центру заголовка
        float fontSize = headerHeight * 0.42f;
        float iconSize = headerHeight * 0.42f;
        String categoryName = category.getLabel();

        String iconChar = switch (category) {
            case COMBAT -> "u";
            case MOVEMENT -> "g";
            case RENDER -> "s";
            case PLAYER -> "b";
            case OTHER -> "t";
        };

        float iconWidth = Fonts.ICONS2.getWidth(iconChar, iconSize);
        float textWidth = Fonts.MEDIUM.getWidth(categoryName, fontSize);
        float iconGap = scaled(3f);
        float totalW = iconWidth + iconGap + textWidth;
        float startX = getX() + (getWidth() - totalW) / 2f;

        Fonts.ICONS2.drawText(matrixStack, iconChar,
                startX, getY() + headerHeight / 2f - iconSize / 2f,
                iconSize, UIColors.textColor(fullAlpha));

        Fonts.MEDIUM.drawText(matrixStack, categoryName,
                startX + iconWidth + iconGap, getY() + headerHeight / 2f - fontSize / 2f - scaled(0.3f),
                fontSize, UIColors.textColor(fullAlpha));

        // Модули начинаются сразу после заголовка
        float modulesY = getY() + headerHeight;
        float modulesH = getHeight() - headerHeight;
        renderModulesWithScissor(context, mouseX, mouseY, delta, modulesY, modulesH);
    }

    private void renderScrollIndicator(MatrixStack matrixStack, int fullAlpha, float panelY, float panelHeight) {
        float totalHeight = calculateTotalHeight();
        float visibleHeight = panelHeight - gap() * 2;

        if (totalHeight > visibleHeight) {
            float scrollbarHeight = (visibleHeight / totalHeight) * visibleHeight;
            scrollbarHeight = Math.max(scrollbarHeight, 20f);

            float scrollbarY = panelY + gap();
            float scrollProgress = Math.abs(smoothedScroll) / (totalHeight - visibleHeight);
            scrollbarY += scrollProgress * (visibleHeight - scrollbarHeight);

            float scrollbarX = getX() + getWidth() - 3f;

            RenderUtil.RECT.draw(matrixStack, scrollbarX, scrollbarY, 2f, scrollbarHeight, 1f,
                    ColorUtil.setAlpha(UIColors.gradient(categoryIndex), fullAlpha / 2));
        }
    }

    private void renderModulesWithScissor(DrawContext context, int mouseX, int mouseY, float delta, float panelY, float panelHeight) {
        MatrixStack matrixStack = context.getMatrices();

        float totalModuleHeight = calculateTotalHeight();

        // Внутренние отступы панели
        float padding = gap();
        float moduleStartY = panelY + padding;
        float visibleHeight = panelHeight - padding;

        // Ограничиваем скролл
        if (totalModuleHeight > visibleHeight) {
            float maxScroll = -(totalModuleHeight - visibleHeight);
            scroll = MathHelper.clamp(scroll, maxScroll, 0);
            smoothedScroll = MathHelper.clamp(smoothedScroll, maxScroll, 0);
        } else {
            scroll = 0;
            smoothedScroll = 0;
        }

        Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
        float scissorPad = scaled(1f); // минимальный отступ — обводка модулей влезает
        ScissorUtil.push(positionMatrix, getX() + scissorPad, panelY, getWidth() - scissorPad * 2f, panelHeight);

        float moduleY = moduleStartY + smoothedScroll;

        for (ModuleComponent module : moduleComponents) {
            if (!shouldRenderModule(module)) continue;

            float moduleHeight = calculateModuleHeight(module);
            float modPad = padding + scaled(0.5f); // отступ с учётом обводки

            module.setX(getX() + modPad);
            module.setY(moduleY);
            module.setWidth(getWidth() - modPad * 2);
            module.setHeight(moduleHeight);
            module.setAlpha(getAlpha());

            // Рендерим модуль - scissor обрежет все что выходит за границы
            module.render(context, mouseX, mouseY, delta);

            moduleY += moduleHeight + gap();
        }

        // Заканчиваем scissor ПОСЛЕ рендеринга всех модулей
        ScissorUtil.pop();
    }

    private float calculateTotalHeight() {
        float total = 0;
        for (ModuleComponent module : moduleComponents) {
            if (!shouldRenderModule(module)) continue;
            total += calculateModuleHeight(module) + gap();
        }
        return total;
    }

    private float calculateModuleHeight(ModuleComponent module) {
        float moduleHeight = module.getDefaultHeight();
        float openAnim = module.getAnim();

        if (openAnim > 0f) {
            float settingOffset = 0.0f;
            List<SettingComponent> visible = module.getSettings().stream()
                    .filter(s -> s.getVisibleAnimation().getValue() > 0.0)
                    .toList();
            for (int i = 0; i < visible.size(); i++) {
                SettingComponent setting = visible.get(i);
                float visibleAnim = (float) setting.getVisibleAnimation().getValue();
                float itemH = setting.getHeight() * visibleAnim;
                // gap между настройками, но не после последней
                float itemGap = (i < visible.size() - 1) ? gap() * visibleAnim : 0f;
                settingOffset += itemH + itemGap;
            }
            moduleHeight += settingOffset * openAnim;
        }

        return moduleHeight;
    }

    private boolean shouldRenderModule(ModuleComponent component) {
        if (!searchText.isEmpty()) {
            return component.getModule().getName().toLowerCase().contains(searchText);
        }
        return true;
    }

    public float getHeaderHeight() {
        return scaled(19f); // Увеличена высота заголовка
    }

    public float getRound() {
        return scaled(5f); // Закругление для заголовка
    }

    public float getPanelRound() {
        return scaled(5f);
    }

    @Override
    public float gap() {
        return scaled(3f);
    }

    public boolean inPanel(double mouseX, double mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight());
    }
}
