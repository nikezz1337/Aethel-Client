package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleManager;
import sweetie.evaware.api.system.backend.KeyStorage;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.widget.ContainerWidget;
import java.awt.Color;
import java.util.*;

public class KeybindsWidget extends ContainerWidget {
    private final Map<String, Float> animMap = new HashMap<>();
    private final Map<String, Integer> orderMap = new HashMap<>();
    private int orderCounter = 0;

    public KeybindsWidget() { super(3f, 120f); }
    @Override public String getName() { return "Keybinds"; }
    @Override protected Map<String, ContainerElement.ColoredString> getCurrentData() { return null; }

    @Override
    public void render(MatrixStack ms) {
        List<Module> enabledWithBinds = new ArrayList<>(ModuleManager.getInstance().getModules().stream()
                .filter(m -> m.isEnabled() && m.hasBind())
                .toList());

        // Анимация появления
        enabledWithBinds.forEach(m -> {
            String k = m.getName();
            if (!orderMap.containsKey(k)) {
                orderMap.put(k, orderCounter++);
            }
            float current = animMap.getOrDefault(k, 0f);
            animMap.put(k, current + (1f - current) * 0.25f);
        });
        
        // Анимация исчезновения
        List<String> toRemove = new ArrayList<>();
        animMap.forEach((k, v) -> {
            if (enabledWithBinds.stream().noneMatch(m -> m.getName().equals(k))) {
                float newVal = v - v * 0.2f;
                if (newVal < 0.01f) {
                    toRemove.add(k);
                } else {
                    animMap.put(k, newVal);
                }
            }
        });
        toRemove.forEach(k -> {
            animMap.remove(k);
            orderMap.remove(k);
        });

        float x = getDraggable().getX(), y = getDraggable().getY(), width = getDraggable().getWidth();
        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float h = scaled(11), p = scaled(3.5f), fS = scaled(6f), iconS = scaled(8f);
        float arrowW = getMediumFont().getWidth(">", fS);
        Color bg = new Color(12, 12, 18, 240);
        Color graySeparator = new Color(160, 160, 160, 180);

        String title = "Keybinds";
        String iconChar = "t";
        float titleW = getMediumFont().getWidth(title, fS);
        float iconW = Fonts.ICONS2.getWidth(iconChar, iconS);

        Map<String, Float> widthMap = new HashMap<>();
        float maxModuleW = 0;

        for (Module m : enabledWithBinds) {
            String keyName = KeyStorage.getBind(m.getBind());
            float nameW = getMediumFont().getWidth(m.getName(), fS);
            float keyW = getMediumFont().getWidth(keyName, fS);
            float w = p + nameW + p + arrowW + p + (keyW + scaled(4)) + p;
            widthMap.put(m.getName(), w);
            if (w > maxModuleW) maxModuleW = w;
        }

        float headerMinW = titleW + iconW + p * 4;
        float headerW = Math.max(headerMinW, maxModuleW);

        enabledWithBinds.sort((m1, m2) -> Float.compare(widthMap.getOrDefault(m2.getName(), 0f), widthMap.getOrDefault(m1.getName(), 0f)));

        float headerX = isRightSide ? (x + width - headerW) : x;
        float currentY = y;

        RenderUtil.RECT.draw(ms, headerX, currentY, headerW, h, 3f, bg);
        getMediumFont().drawText(ms, title, headerX + p, currentY + h/2f - fS/2f, fS, Color.WHITE, 0f);
        Fonts.ICONS2.drawGradientText(ms, iconChar, headerX + headerW - p - iconW, currentY + h/2f - iconS/2f, iconS,
                UIColors.primary(), UIColors.secondary(), 1.1f);

        currentY += h + 1.5f;

        // Собираем все ключи (включая исчезающие модули)
        List<String> allKeys = new ArrayList<>();
        
        // Добавляем активные модули
        enabledWithBinds.forEach(m -> {
            if (!allKeys.contains(m.getName())) {
                allKeys.add(m.getName());
            }
        });
        
        // Добавляем исчезающие модули
        animMap.keySet().forEach(k -> {
            if (!allKeys.contains(k)) {
                allKeys.add(k);
            }
        });
        
        // Сортируем по порядку появления (а не по ширине)
        allKeys.sort((k1, k2) -> {
            int order1 = orderMap.getOrDefault(k1, 0);
            int order2 = orderMap.getOrDefault(k2, 0);
            return Integer.compare(order1, order2);
        });
        
        for (String k : allKeys) {
            Float animValue = animMap.get(k);
            if (animValue == null || animValue <= 0.01f) continue;
            float anim = animValue;
            
            Module m = enabledWithBinds.stream().filter(mod -> mod.getName().equals(k)).findFirst().orElse(null);
            
            // Для исчезающих модулей нужно получить данные из кэша
            String keyName = "";
            float itemW = headerMinW;
            
            if (m != null) {
                keyName = KeyStorage.getBind(m.getBind());
                itemW = widthMap.getOrDefault(k, headerMinW);
            } else {
                // Модуль исчезает, используем последние известные данные
                itemW = widthMap.getOrDefault(k, headerMinW);
                // Пытаемся найти модуль в общем списке для получения бинда
                Module cachedModule = ModuleManager.getInstance().getModules().stream()
                    .filter(mod -> mod.getName().equals(k))
                    .findFirst().orElse(null);
                if (cachedModule != null) {
                    keyName = KeyStorage.getBind(cachedModule.getBind());
                }
            }

            float xOffset = isRightSide ? (25f * (1f - anim)) : (-25f * (1f - anim));
            float itemX = (isRightSide ? (x + width - itemW) : x) + xOffset;

            float rowH = h * anim;
            int alpha = (int)(255 * anim);

            Color dynamicBg = new Color(16, 16, 24, (int)(245 * anim));
            Color dynamicText = new Color(180, 180, 180, alpha);
            Color dynamicArrow = new Color(120, 120, 120, alpha);
            Color dynamicKeyBg = new Color(50, 50, 50, (int)(220 * anim));

            ms.push();
            RenderUtil.BLUR_RECT.draw(ms, itemX, currentY, itemW, rowH, 3f, dynamicBg);

            float tY = currentY + (rowH / 2f) - fS/2f;

            getMediumFont().drawText(ms, k, itemX + p, tY, fS, dynamicText, 0f);

            float nW = getMediumFont().getWidth(k, fS);
            getMediumFont().drawText(ms, " >", itemX + p + nW + (p * 0.5f), tY, fS, dynamicArrow, 0f);

            if (!keyName.isEmpty()) {
                float kW = getMediumFont().getWidth(keyName, fS);
                float kRectW = kW + scaled(4);
                float kRectH = (fS + scaled(2)) * anim;
                float kRectX = itemX + itemW - p - kRectW;
                float kRectY = currentY + (rowH / 2f) - (kRectH / 2f);

                if (anim > 0.1f) {
                    RenderUtil.RECT.draw(ms, kRectX, kRectY, kRectW, kRectH, 1.7f, dynamicKeyBg);
                    float keyTextY = kRectY + (kRectH / 2f) - (fS / 2f);
                    getMediumFont().drawText(ms, keyName, kRectX + (kRectW / 2f) - (kW / 2f), keyTextY, fS, Color.WHITE, 0f);
                }
            }
            
            ms.pop();

            currentY += rowH + 1.5f;
        }

        getDraggable().setWidth(headerW);
        getDraggable().setHeight(currentY - y);
    }
}
