package dev.ethereal.client.features.modules.render.nametags;

import dev.ethereal.api.system.configs.FriendManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import dev.ethereal.api.utils.other.TextUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NameTagsPotions {
    private final NameTagsModule module;

    public NameTagsPotions(NameTagsModule module) {
        this.module = module;
    }

    public void renderPotions(PlayerEntity player, float x, float y, DrawContext context) {
        MatrixStack matrixStack = context.getMatrices();

        if (player.getStatusEffects().isEmpty()) {
            return;
        }

        float scale = 0.9f;
        float gap = 2f * scale;
        float fontSize = 7f * scale;

        for (StatusEffectInstance effect : player.getStatusEffects()) {
            String effectText = getEffectName(effect) + TextUtil.getDurationText(effect.getDuration());
            Fonts.PS_MEDIUM.drawText(matrixStack, effectText, x + gap, y, fontSize, Color.lightGray);
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
}
