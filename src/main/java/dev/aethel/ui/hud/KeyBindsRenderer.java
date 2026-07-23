package dev.aethel.ui.hud;

import dev.aethel.Aethel;
import dev.aethel.module.Module;
import dev.aethel.module.list.render.Interface;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.keyboard.KeyStorage;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.List;

public class KeyBindsRenderer implements IMinecraft {
    private final Interface interfaceModule;
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private final Animation widthAnim = new Animation(Easing.EXPO_OUT, 200);
    public KeyBindsRenderer(Interface interfaceModule) {
        this.interfaceModule = interfaceModule;
    }

    public void render(DrawContext context) {
        if (mc.player == null) return;

        renderCelestial(context);
    }

    private void renderCelestial(DrawContext context) {
        if (mc.player == null) return;

        final boolean chatOpen = mc.currentScreen instanceof ChatScreen;

        List<Module> activeModules = Aethel.getInstance().getModuleStorage().getModules().stream()
                .filter(m -> m.getKey() != -1 && m.getAnimation().getValue() > 0.01f)
                .toList();

        alpha.run((activeModules.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);

        final float fontSize = 7f;
        final float keyFontSize = 6.5f;
        final float headerH = 13f;
        final float rowH = 10f;
        final float minRadius = 4f;

        float targetWidth = 70f;
        for (Module m : activeModules) {
            String key = KeyStorage.getKey(m.getKey());
            float rowWidth = Fonts.SFBOLD.get().getWidth(m.getName(), fontSize) + Fonts.SFBOLD.get().getWidth(key, keyFontSize) + 14f;
            targetWidth = Math.max(targetWidth, rowWidth);
        }
        widthAnim.run(targetWidth);
        float curW = Math.max(70f, (float) widthAnim.getValue());

        float rowsHeight = (float) activeModules.stream()
                .mapToDouble(m -> rowH * MathHelper.clamp((float) m.getAnimation().getValue(), 0f, 1f))
                .sum();
        float totalH = headerH + rowsHeight + (rowsHeight > 0f ? 3f : 1f);

        float x = interfaceModule.getKeyBindsDrag().getX();
        float y = interfaceModule.getKeyBindsDrag().getY();

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();

        // Фон - тема на 35% прозрачности
        int bgColor = ColorProvider.rgba(
                ((t1 >> 16) & 0xFF) >> 2,
                ((t1 >> 8) & 0xFF) >> 2,
                (t1 & 0xFF) >> 2,
                Math.min(135, aInt)
        );

        // Глоу/обводка - градиентная, от темы (медленнее, другой сдвиг фаз)
        int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
        int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp; // меняем углы

        Matrix4f m2 = context.getMatrices().peek().getPositionMatrix();

        DrawUtil.drawShadow(m2, x, y, curW, totalH, minRadius, 8f, ColorProvider.rgba(0, 0, 0, 80));
        interfaceModule.drawGlow(m2, x, y, curW, totalH, minRadius, globalAlpha);
        DrawUtil.drawRound(x - 0.5f, y - 0.5f, curW + 1f, totalH + 1f, minRadius, glow[0], glow[1], glow[2], glow[3]);

        DrawUtil.drawRound(x, y, curW, totalH, minRadius, bgColor);

        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "C", x + 4f, y + 2.5f, ColorProvider.rgba(255, 255, 255, aInt), 8f);
        DrawUtil.drawRound(x + 14f, y + 3f, 0.5f, headerH - 6f, 0.2f, ColorProvider.rgba(120, 120, 120, aInt / 2));

        float headerTextX = x + (curW - Fonts.SFREGULAR.get().getWidth("Key Binds", 7f)) / 2f;
        DrawUtil.drawText(Fonts.SFBOLD.get(), "Key Binds", headerTextX, y + 2.5f, ColorProvider.rgba(255, 255, 255, aInt), 7f);

        int sepColor = ColorProvider.setAlpha(t1, aInt / 3);
        DrawUtil.drawRound(x + 3f, y + headerH, curW - 6f, 0.5f, 0.25f, sepColor);

        float curY = y + headerH +1;

        for (Module m : activeModules) {
            float rowAnim = MathHelper.clamp((float) m.getAnimation().getValue(), 0f, 1f);
            if (rowAnim <= 0.001f) continue;

            float itemHeight = rowH * rowAnim;
            int itemAlpha = MathHelper.clamp((int) (aInt * rowAnim), 0, 255);

            if (itemAlpha >= 4) {
                float textY = curY + (itemHeight / 2f) - (fontSize / 2f) - 0.5f;
                String key = KeyStorage.getKey(m.getKey());

                DrawUtil.drawText(Fonts.SFBOLD.get(), m.getName(), x + 5f, textY, ColorProvider.rgba(255, 255, 255, itemAlpha), fontSize);

                float keyX = x + curW - Fonts.SFBOLD.get().getWidth(key, keyFontSize) - 4f;
                DrawUtil.drawText(Fonts.SFBOLD.get(), key, keyX, textY, ColorProvider.rgba(255, 255, 255, itemAlpha), keyFontSize);
            }
            curY += itemHeight;
        }

        interfaceModule.getKeyBindsDrag().setWidth(curW);
        interfaceModule.getKeyBindsDrag().setHeight(totalH);
    }
}
