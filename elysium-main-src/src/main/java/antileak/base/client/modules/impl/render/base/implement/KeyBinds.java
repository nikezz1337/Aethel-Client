package antileak.base.client.modules.impl.render.base.implement;

import net.minecraft.client.util.math.MatrixStack;
import antileak.base.elysium;
import antileak.base.api.events.implement.EventRender;
import antileak.base.api.utils.animation.AnimationUtils;
import antileak.base.api.utils.animation.Easings;
import antileak.base.api.utils.color.ColorUtils;
import antileak.base.api.utils.draggable.Draggable;
import antileak.base.api.utils.input.KeyBoardUtils;
import antileak.base.api.utils.render.RenderUtils;
import antileak.base.api.utils.render.fonts.msdf.Font;
import antileak.base.api.utils.render.fonts.msdf.Fonts;
import antileak.base.api.utils.scissor.ScissorUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.impl.render.base.InterfaceProcessing;

import java.util.HashMap;
import java.util.Map;

public class KeyBinds extends InterfaceProcessing {
    private static final float BASE_MIN_WIDTH   = 64f;
    private static final float EXTRA_WIDTH      = 0f;
    private static final float ROW_RIGHT_MARGIN = 25f;
    private static final float ROW_HEIGHT       = 10f;

    private final Map<Module, AnimationUtils> animations = new HashMap<>();
    private final AnimationUtils widthAnimation  = new AnimationUtils(60, 10.5f, Easings.QUAD_OUT);
    private final AnimationUtils heightAnimation = new AnimationUtils(16, 10.5f, Easings.QUAD_OUT);

    private static final Map<Character, Character> RU_TO_EN = new HashMap<>();
    static {
        String ru = "йцукенгшщзхъфывапролджэячсмитьбюЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ";
        String en = "qwertyuiop[]asdfghjkl;'zxcvbnm,.QWERTYUIOP[]ASDFGHJKL;'ZXCVBNM,.";
        for (int i = 0; i < ru.length(); i++) {
            RU_TO_EN.put(ru.charAt(i), en.charAt(i));
        }
    }
    private Font issue(int size) { return Fonts.getFont("suisse", size); }
    private Font icons(int size) { return Fonts.getFont("icon", size); }
    private antileak.base.api.utils.render.fonts.ttf.MCFontRenderer sf_regular(int size) {
        return antileak.base.api.utils.render.fonts.ttf.Fonts.getFont("sf_regular.ttf", size);
    }
    private antileak.base.api.utils.render.fonts.ttf.MCFontRenderer myyyy(int size) {
        return antileak.base.api.utils.render.fonts.ttf.Fonts.getFont("myyyy.ttf", size);
    }

    public KeyBinds(Draggable draggable) {
        super(draggable);
    }

    private AnimationUtils getAnimation(Module module) {
        return animations.computeIfAbsent(module, m -> new AnimationUtils(0, 10.5f, Easings.QUAD_OUT));
    }

    private String toEnglish(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(RU_TO_EN.getOrDefault(c, c));
        }
        return result.toString();
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        float baseX = draggable.getX(), y = draggable.getY();
        int colorTheme = getStableThemeColor();

        float targetWidth = BASE_MIN_WIDTH;
        int enabledCount = 0;

        for (Module module : antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass.INSTANCE.getObject()) {
            if (module.getKey() != -1) {
                getAnimation(module).update(module.isEnable() ? 1 : 0);
            }
        }

        for (Module module : antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass.INSTANCE.getObject()) {
            if (module.getKey() != -1 && module.isEnable()) {
                enabledCount++;
                String keyName = toEnglish(KeyBoardUtils.getKeyName(module.getKey()));
                Font iconFont = icons(11);
                float iconWidth = iconFont != null ? iconFont.getWidth(module.getCategory().getIcons()) : 0f;
                float moduleWidth = iconWidth + 4f + issue(12).getWidth(module.getDisplayName())
                        + issue(10).getWidth(keyName) + ROW_RIGHT_MARGIN;
                if (moduleWidth > targetWidth) targetWidth = moduleWidth;
            }
        }

        float targetHeight = 16 + enabledCount * ROW_HEIGHT;

        widthAnimation.update(targetWidth);
        heightAnimation.update(targetHeight);

        float width  = widthAnimation.getValue() + EXTRA_WIDTH;
        float height = heightAnimation.getValue();
        float rightEdge = baseX + width;
        float x = baseX;

        RenderUtils.drawDefaultHudElementRects(eventRender.getContext().getMatrices(), x, y, width, height, colorTheme, isUnusualRectType());
        issue(14).draw(eventRender.getContext().getMatrices(),"Keybinds", x + 5.2f, y + 6.5f, -1);
        myyyy(15).drawString("A", rightEdge - 13f, y + 7.5f, colorTheme);

        float offsetY = 18;
        for (Module module : antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass.INSTANCE.getObject()) {
            if (module.getKey() != -1) {
                AnimationUtils anim = getAnimation(module);
                float animValue = anim.getValue();
                if (animValue <= 0.01f) continue;

                ScissorUtils.push();
                ScissorUtils.setFromComponentCoordinates(x, y, width, height);

                String keyName = toEnglish(KeyBoardUtils.getBindName(module.getKey()));
                float keyBoxWidth = Math.max(issue(10).getStringWidth(keyName) + 4, 9f);

                int alpha = (int) (255 * animValue);
                int textColor = ColorUtils.rgba(255, 255, 255, alpha);
                Font iconFont = icons(13);

                float textX = x + 5.2f;
                if (iconFont != null) {
                    String categoryIcon = module.getCategory().getIcons();
                    float iconY = y + offsetY - 0.5f;
                    iconFont.draw(eventRender.getContext().getMatrices(), categoryIcon, textX, iconY, colorTheme);
                    textX += iconFont.getWidth(categoryIcon) + 4f;
                }

                issue(12).draw(eventRender.getContext().getMatrices(), module.getDisplayName(), textX, y + offsetY - 1, textColor);
                float keyBoxX = rightEdge - keyBoxWidth - 5;
                issue(12).drawCenteredString(eventRender.getContext().getMatrices(), keyName, keyBoxX + keyBoxWidth / 2, y + offsetY + 0.8f - 1, colorTheme);

                offsetY += ROW_HEIGHT * animValue;
                ScissorUtils.pop();
                ScissorUtils.unset();
            }
        }

        draggable.setWidth(width);
        draggable.setHeight(height);
    }

    private int getStableThemeColor() {
        if (!elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }
}
