package dev.aethel.module.list.render;

import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.msdf.MsdfFont;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.Color;

public class NameTagsPotions implements IMinecraft {

    private final NameTags module;

    public NameTagsPotions(NameTags module) {
        this.module = module;
    }

    public void renderPotions(PlayerEntity player, float x, float y, DrawContext context) {
        if (player.getStatusEffects().isEmpty()) return;

        float scale = 1.0f;
        float gap = 3f;
        float fontSize = 9f;

        for (StatusEffectInstance effect : player.getStatusEffects()) {
            String effectText = getEffectName(effect) + getDurationText(effect.getDuration());
            DrawUtil.drawText(Fonts.SFBOLD.get(), effectText, x + gap, y, ColorProvider.rgba(170, 170, 170, 255), fontSize);
            y += fontSize + gap;
        }
    }

    private String getEffectName(StatusEffectInstance effect) {
        String translationKey = effect.getTranslationKey();
        String name = translationKey.substring(translationKey.lastIndexOf(".") + 1);
        name = name.substring(0, 1).toUpperCase() + name.substring(1);

        if (effect.getAmplifier() > 0) {
            name += " " + (effect.getAmplifier() + 1);
        }

        return name.replace('_', ' ');
    }

    private String getDurationText(int duration) {
        if (duration == -1) return " §b\u221E";

        int totalSeconds = duration / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return " §7" + String.format("%d:%02d", minutes, seconds);
    }
}
