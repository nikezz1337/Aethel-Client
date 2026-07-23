package dev.aethel.ui.hud;

import dev.aethel.module.list.render.Interface;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.math.Scissor;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class PotionsRenderer implements IMinecraft {
    private final Interface interfaceModule;
    private final Animation alpha3 = new Animation(Easing.EXPO_OUT, 200);
    private final Animation widthAnim3 = new Animation(Easing.EXPO_OUT, 200);
    private final Animation xLine2 = new Animation(Easing.EXPO_OUT, 170);
    private final List<PotionItem> potionItems = new CopyOnWriteArrayList<>();

    public PotionsRenderer(Interface interfaceModule) {
        this.interfaceModule = interfaceModule;
    }

    public void update() {
        if (mc.player == null) return;
        java.util.Map<String, StatusEffectInstance> currentEffects = mc.player.getStatusEffects().stream()
                .collect(Collectors.toMap(
                        e -> net.minecraft.text.Text.translatable(e.getTranslationKey()).getString() + ":" + e.getAmplifier(),
                        e -> e,
                        (e1, e2) -> e1
                ));

        potionItems.forEach(item -> {
            String key = item.name + ":" + item.amplifier;
            StatusEffectInstance effect = currentEffects.get(key);

            if (effect != null) {
                item.durationTicks = effect.getDuration();
                if (!item.active) {
                    item.animation.setValue(1.0f);
                }
                item.active = true;
                currentEffects.remove(key);
            } else {
                item.active = false;
            }
        });

        currentEffects.forEach((key, effect) -> {
            potionItems.add(new PotionItem(
                    net.minecraft.text.Text.translatable(effect.getTranslationKey()).getString(),
                    effect.getAmplifier(),
                    effect.getDuration(),
                    effect.getEffectType()
            ));
        });

        potionItems.removeIf(item -> !item.active && item.animation.getValue() == 0);
    }

    public void render(DrawContext context) {
        if (mc.player == null) return;

        renderCelestial(context);
    }

    private void renderCelestial(DrawContext context) {
        if (mc.player == null) return;

        final boolean chatOpen = mc.currentScreen instanceof ChatScreen;

        potionItems.sort(Comparator.comparing(pi -> pi.name));

        List<PotionItem> visible = new ArrayList<>();
        for (PotionItem item : potionItems) {
            item.animation.run(item.active ? 1f : 0f);
            if (item.animation.getValue() > 0.01f) visible.add(item);
        }

        alpha3.run((visible.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha3.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);

        final String headerText = "Potions";

        final float fontSize = 7f;
        final float headerH = 13f;
        final float rowH = 11f;
        final float minRadius = 4f;

        float targetWidth = 70f;

        for (PotionItem item : visible) {
            int totalSec = Math.max(0, item.durationTicks / 20);
            int minutes = totalSec / 60;
            int sec = totalSec % 60;
            String time = String.format("%d:%02d", minutes, sec);

            int lvl = item.amplifier + 1;
            String lvlText = "   " + lvl;

            float nameW = Fonts.SFBOLD.get().getWidth(item.name, fontSize);
            float lvlW = Fonts.SFBOLD.get().getWidth(lvlText, fontSize);
            float timeW = Fonts.SFBOLD.get().getWidth(time, fontSize);

            float rowW = 5f + nameW + lvlW + 10f + timeW + 4f;
            targetWidth = Math.max(targetWidth, rowW);
        }

        widthAnim3.run(targetWidth);
        float curW = Math.max(70f, (float) widthAnim3.getValue());

        float rowsHeight = 0f;
        for (PotionItem item : visible) {
            rowsHeight += rowH * MathHelper.clamp((float) item.animation.getValue(), 0f, 1f);
        }

        float totalH = headerH + rowsHeight + (rowsHeight > 0f ? 3f : 1f);

        float x = interfaceModule.getPotionsDrag().getX();
        float y = interfaceModule.getPotionsDrag().getY();

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();

        // Фон - приглушённый оттенок темы
        int bgColor = ColorProvider.rgba(
                ((t1 >> 16) & 0xFF) >> 2,
                ((t1 >> 8) & 0xFF) >> 2,
                (t1 & 0xFF) >> 2,
                Math.min(135, aInt)
        );

        // Глоу/обводка
        int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
        int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp;

        Matrix4f m = context.getMatrices().peek().getPositionMatrix();

        interfaceModule.drawGlow(m, x, y, curW, totalH, minRadius, globalAlpha);
        DrawUtil.drawRound(x - 0.5f, y - 0.5f, curW + 1f, totalH + 1f, minRadius, glow[0], glow[1], glow[2], glow[3]);
        DrawUtil.drawRound(x, y, curW, totalH, minRadius, bgColor);

        // Иконка слева
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "E", x + 4f, y + 2.5f, ColorProvider.rgba(255, 255, 255, aInt), 8f);
        DrawUtil.drawRound(x + 14f, y + 3f, 0.5f, headerH - 6f, 0.2f, ColorProvider.rgba(120, 120, 120, aInt / 2));

        // Текст заголовка по центру
        float headerTextX = x + (curW - Fonts.SFBOLD.get().getWidth(headerText, 7f)) / 2f;
        DrawUtil.drawText(Fonts.SFBOLD.get(), headerText, headerTextX, y + 2.5f, ColorProvider.rgba(255, 255, 255, aInt), 7f);

        // Разделитель под заголовком
        int sepColor = ColorProvider.setAlpha(t1, aInt / 3);
        DrawUtil.drawRound(x + 3f, y + headerH, curW - 6f, 0.5f, 0.25f, sepColor);

        float curY = y + headerH + 1f;

        for (PotionItem item : visible) {
            float rowAnim = MathHelper.clamp((float) item.animation.getValue(), 0f, 1f);
            if (rowAnim <= 0.001f) continue;

            float itemH = rowH * rowAnim;
            int itemA = MathHelper.clamp((int) (aInt * rowAnim), 0, 255);

            if (itemA >= 4) {
                int totalSec = Math.max(0, item.durationTicks / 20);
                int minutes = totalSec / 60;
                int sec = totalSec % 60;
                String time = String.format("%d:%02d", minutes, sec);

                int lvl = item.amplifier + 1;
                String lvlText = "   " + lvl;

                float timeW = Fonts.SFBOLD.get().getWidth(time, fontSize);
                float timeX = x + curW - timeW - 4f;

                float textY = curY + (itemH / 2f) - (fontSize / 2f) - 0.5f;

                // Клип для названия (чтобы не налезало на время)
                float leftX = x + 5f;
                float clipW = Math.max(0f, (timeX - 6f) - leftX);
                Scissor.push();
                Scissor.setFromComponentCoordinates(leftX, curY, clipW, itemH);

                DrawUtil.drawText(Fonts.SFBOLD.get(), item.name, leftX, textY,
                        ColorProvider.rgba(233, 233, 233, itemA), fontSize);

                float nameW = Fonts.SFBOLD.get().getWidth(item.name, fontSize);

                int lvlColor = (lvl >= 2)
                        ? ColorProvider.rgba(192, 100, 106, itemA)
                        : ColorProvider.rgba(200, 200, 200, itemA);
                if(lvl > 1){
                    DrawUtil.drawText(Fonts.SFBOLD.get(), lvlText, leftX + nameW, textY, lvlColor, fontSize);
                }

                Scissor.unset();
                Scissor.pop();

                DrawUtil.drawText(Fonts.SFBOLD.get(), time, timeX, textY,
                        ColorProvider.rgba(200, 200, 200, itemA), fontSize);
            }

            curY += itemH;
        }

        interfaceModule.getPotionsDrag().setWidth(curW);
        interfaceModule.getPotionsDrag().setHeight(totalH);
    }

    private static class PotionItem {
        String name;
        int amplifier;
        int durationTicks;
        boolean active;
        net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect;
        Animation animation = new Animation(Easing.EXPO_OUT, 233);

        PotionItem(String name, int amplifier, int durationTicks, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
            this.name = name;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
            this.effect = effect;
            this.active = true;
        }
    }
}
