package ru.zenith.implement.features.draggables;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.implement.events.packet.PacketEvent;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.effect.StatusEffect;

import java.util.*;

public class Potions extends AbstractDraggable {
    private final List<Potion> list = new ArrayList<>();
    public Potions() {
        super("Potions", 210, 10, 80, 23,true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(p -> p.anim.isFinished(Direction.BACKWARDS));
        list.forEach(p -> p.effect.update(mc.player,null));
    }

    @Override
    public void packet(PacketEvent e) {
        switch (e.getPacket()) {
            case EntityStatusEffectS2CPacket effect -> {
                if (!PlayerIntersectionUtil.nullCheck() && effect.getEntityId() == Objects.requireNonNull(mc.player).getId()) {
                    RegistryEntry<StatusEffect> effectId = effect.getEffectId();
                    list.stream().filter(p -> p.effect.getEffectType().getIdAsString().equals(effectId.getIdAsString())).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
                    list.add(new Potion(new StatusEffectInstance(effectId, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()), new DecelerateAnimation().setMs(150).setValue(1.0F)));
                }
            }
            case RemoveEntityStatusEffectS2CPacket effect -> list.stream().filter(s -> s.effect.getEffectType().getIdAsString().equals(effect.effect().getIdAsString())).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
            case PlayerRespawnS2CPacket p -> list.clear();
            case GameJoinS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT);
        FontRenderer fontIcons = Fonts.getSize(16, Fonts.Type.ICONS);
        FontRenderer fontPotion = Fonts.getSize(13, Fonts.Type.DEFAULT);

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 13.5F)
                .round(4).color(ColorUtil.getClientColor()).build());

        float centerX = getX() + getWidth() / 2.0F;
        int offset = 23 - 6, maxWidth = 80;

        fontIcons.drawString(matrix, "E", getX() + 3.5F, getY() + 6, ColorUtil.getText());
        font.drawString(matrix, getName(),(int) (centerX - font.getStringWidth(getName()) / 2.0F), getY() + 5.5f, ColorUtil.getText());
        for (Potion potion : list) {
            StatusEffectInstance effect = potion.effect;
            float animation = potion.anim.getOutput().floatValue();
            float centerY = getY() + offset;
            int amplifier = effect.getAmplifier();

            String name = effect.getEffectType().value().getName().getString();
            String duration = Formatting.GRAY + "| " + Formatting.RESET + getDuration(effect);
            String lvl = amplifier > 0 ? Formatting.RED + " " + (amplifier + 1) + Formatting.RESET : "";

            MathUtil.scale(matrix, centerX, centerY, 1, animation, () -> {
                float animRed = effect.getDuration() != -1 && effect.getDuration() <= 120 ? MathUtil.blinking(1000, 8) : 1;
                blur.render(ShapeProperties.create(matrix, getX(), centerY - 2, getWidth(), 10).round(4).color(ColorUtil.getClientColor()).build());
                Render2DUtil.drawSprite(matrix, mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType()), getX() + 2.5f, centerY - 1.5f, 8, 8);
                fontPotion.drawString(matrix, name + lvl, getX() + 11.5f, centerY + 1, ColorUtil.getText());
                fontPotion.drawString(matrix, duration, getX() + getWidth() - 5 - fontPotion.getStringWidth(duration), centerY + 1, ColorUtil.multRed(ColorUtil.getText(), animRed));
            });

            int width = (int) fontPotion.getStringWidth(name + lvl + duration) + 30;
            maxWidth = Math.max(width, maxWidth);
            offset += (int) (11 * animation);
        }

        setWidth(maxWidth);
        setHeight(offset);
    }

    private String getDuration(StatusEffectInstance pe) {
        int var1 = pe.getDuration();
        int mins = var1 / 1200;
        return pe.isInfinite() || mins > 60 ? "**:**": mins + ":" + String.format("%02d", (var1 % 1200) / 20);
    }

    private record Potion(StatusEffectInstance effect, Animation anim) {}
}