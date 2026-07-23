package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.Registries;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.implement.events.packet.PacketEvent;

import java.util.*;

public class CoolDowns extends AbstractDraggable {
    public static CoolDowns getInstance() {
        return Instance.getDraggable(CoolDowns.class);
    }

    public final List<CoolDown> list = new ArrayList<>();

    public CoolDowns() {
        super("Cool Downs", 120, 10, 80, 23, true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(c -> c.anim.isFinished(Direction.BACKWARDS));
        if (mc.player != null) {
            list.stream()
                .filter(c -> !mc.player.getItemCooldownManager().isCoolingDown(c.item.getDefaultStack()))
                .forEach(c -> c.anim.setDirection(Direction.BACKWARDS));
        }
    }

    @Override
    public void packet(PacketEvent e) {
        if (PlayerIntersectionUtil.nullCheck()) return;
        switch (e.getPacket()) {
            case CooldownUpdateS2CPacket c -> {
                Item item = Registries.ITEM.get(c.cooldownGroup());
                list.stream().filter(cd -> cd.item.equals(item)).forEach(cd -> cd.anim.setDirection(Direction.BACKWARDS));
                if (c.cooldown() != 0) {
                    list.add(new CoolDown(item, new StopWatch().setMs(-c.cooldown() * 50L), new DecelerateAnimation().setMs(150).setValue(1.0f)));
                }
            }
            case PlayerRespawnS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font         = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontCoolDown = Fonts.getSize(13, Fonts.Type.DEFAULT);

        float headerH = 17.5f;

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), headerH)
                .round(4, 0, 4, 0).softness(1).thickness(2)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9f)).build());

        blur.render(ShapeProperties.create(matrix, getX(), getY() + headerH, getWidth(), getHeight() - headerH)
                .round(0, 4, 0, 4).softness(1).thickness(2)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7f)).build());

        float centerX = getX() + getWidth() / 2f;
        font.drawCenteredString(matrix, getName(), centerX, getY() + 7, ColorUtil.getText());

        int offset = (int) (headerH + 5);
        int maxWidth = 80;

        for (CoolDown coolDown : list) {
            float animation = coolDown.anim.getOutput().floatValue();
            float centerY = getY() + offset;
            int time = -coolDown.time.elapsedTime() / 1000;
            String name = coolDown.item.getDefaultStack().getName().getString();
            String duration = StringUtil.getDuration(time);

            MathUtil.scale(matrix, centerX, centerY, 1, animation, () -> {
                float green = time <= 5 ? MathUtil.blinking(1000, 8) : 1f;
                Render2DUtil.defaultDrawStack(context, coolDown.item.getDefaultStack(), getX() + 4, centerY - 3, false, false, 0.5f);
                rectangle.render(ShapeProperties.create(matrix, getX() + 15, centerY - 1, 0.5f, 6)
                        .color(ColorUtil.getOutline(1, 0.5f)).build());
                fontCoolDown.drawString(matrix, name, getX() + 18, centerY + 1, ColorUtil.getText());
                fontCoolDown.drawString(matrix, duration,
                        getX() + getWidth() - 5 - fontCoolDown.getStringWidth(duration),
                        centerY + 1, ColorUtil.multGreen(ColorUtil.getText(), green));
            });

            int width = (int) fontCoolDown.getStringWidth(name + duration) + 30;
            maxWidth = Math.max(width, maxWidth);
            offset += (int) (11 * animation);
        }

        setWidth(maxWidth);
        setHeight(offset);
    }

    public record CoolDown(Item item, StopWatch time, Animation anim) {}
}