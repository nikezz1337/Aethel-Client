package dev.ethereal.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleManager;
import dev.ethereal.api.system.backend.KeyStorage;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.widget.ContainerWidget;

import java.awt.Color;
import java.util.*;

public class KeybindsWidget extends ContainerWidget {
    private final Map<String, AnimationUtil> animMap = new HashMap<>();
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

        // Добавляем новые модули
        enabledWithBinds.forEach(m -> {
            String k = m.getName();
            if (!orderMap.containsKey(k)) orderMap.put(k, orderCounter++);
            animMap.computeIfAbsent(k, key -> new AnimationUtil());
        });

        // Обновляем анимации
        animMap.forEach((k, anim) -> {
            anim.update();
            boolean active = enabledWithBinds.stream().anyMatch(m -> m.getName().equals(k));
            anim.run(active ? 1.0 : 0.0, 250, Easing.EXPO_OUT);
        });

        // Удаляем полностью исчезнувшие
        animMap.entrySet().removeIf(e -> {
            if (e.getValue().getValue() < 0.01 &&
                enabledWithBinds.stream().noneMatch(m -> m.getName().equals(e.getKey()))) {
                orderMap.remove(e.getKey());
                return true;
            }
            return false;
        });

        float x = getDraggable().getX(), y = getDraggable().getY(), width = getDraggable().getWidth();
        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float headerH = scaled(12.7f), rowH0 = scaled(8.5f), p = scaled(3.5f), fS = scaled(5.5f), iconS = scaled(8f);
        float keyBoxSize = rowH0 + 2f;

        String title = "Keybinds";
        String iconChar = "g";
        float titleW = getMediumFont().getWidth(title, fS);
        float iconW = Fonts.ICONS2.getWidth(iconChar, iconS);

        Map<String, Float> widthMap = new HashMap<>();
        float maxModuleW = 0;

        for (Module m : enabledWithBinds) {
            float nameW = getMediumFont().getWidth(m.getName(), fS);
            float w = p + nameW + p + scaled(2f) + keyBoxSize + p;
            widthMap.put(m.getName(), w);
            if (w > maxModuleW) maxModuleW = w;
        }

        float headerMinW = titleW + iconW + p * 4;
        float headerW = Math.max(headerMinW, maxModuleW);
        float headerX = isRightSide ? (x + width - headerW) : x;
        float currentY = y;

        Color headerBg = new Color(8, 8, 8, 200);
        RenderUtil.BLUR_RECT.draw(ms, headerX, currentY, headerW, headerH, 2.5f, headerBg);
        getMediumFont().drawText(ms, title, headerX + p, currentY + headerH / 2f - fS / 2f, fS, Color.WHITE, 0f);
        Fonts.ICONS2.drawGradientText(ms, iconChar, headerX + headerW - p - iconW, currentY + headerH / 2f - iconS / 2f, iconS,
                UIColors.primary(), UIColors.secondary(), 1.1f);

        currentY += headerH + scaled(1f);

        List<String> allKeys = new ArrayList<>(animMap.keySet());
        allKeys.sort(Comparator.comparingInt(k -> orderMap.getOrDefault(k, 0)));

        for (String k : allKeys) {
            AnimationUtil animUtil = animMap.get(k);
            if (animUtil == null) continue;
            float anim = (float) animUtil.getValue();
            if (anim <= 0.01f) continue;

            Module m = enabledWithBinds.stream().filter(mod -> mod.getName().equals(k)).findFirst().orElse(null);

            String keyName = "";
            float itemW = widthMap.getOrDefault(k, headerMinW);

            if (m != null) {
                keyName = KeyStorage.getBind(m.getBind());
            } else {
                Module cached = ModuleManager.getInstance().getModules().stream()
                        .filter(mod -> mod.getName().equals(k)).findFirst().orElse(null);
                if (cached != null) keyName = KeyStorage.getBind(cached.getBind());
            }

            float xOffset = isRightSide ? (scaled(15f) * (1f - anim)) : (-scaled(15f) * (1f - anim));
            float itemX = (isRightSide ? (x + width - itemW) : x) + xOffset;

            float rowH = rowH0 * anim + 2.5f;
            int alpha = (int)(255 * anim);

            float gap = scaled(1.5f);
            float nameBlockW = itemW - gap - keyBoxSize - p;
            float keyBlockW = keyBoxSize;

            Color blurColor = new Color(8, 8, 8, (int)(200 * anim));

            ms.push();

            RenderUtil.BLUR_RECT.draw(ms, itemX, currentY, nameBlockW, rowH, 2f, blurColor);
            getMediumFont().drawText(ms, k, itemX + p, currentY + rowH / 2f - fS / 2f, fS, new Color(255, 255, 255, alpha), 0f);

            if (!keyName.isEmpty()) {
                float kBoxX = itemX + nameBlockW + gap;
                RenderUtil.BLUR_RECT.draw(ms, kBoxX, currentY, keyBlockW, rowH, 2.5f, blurColor);
                float kW = getMediumFont().getWidth(keyName, fS);
                getMediumFont().drawText(ms, keyName,
                        kBoxX + keyBlockW / 2f - kW / 2f,
                        currentY + rowH / 2f - fS / 2f,
                        fS, new Color(255, 255, 255, alpha), 0f);
            }

            ms.pop();

            currentY += rowH + scaled(1f);
        }

        getDraggable().setWidth(headerW);
        getDraggable().setHeight(currentY - y);
    }
}
