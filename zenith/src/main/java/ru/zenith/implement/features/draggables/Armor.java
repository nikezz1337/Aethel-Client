package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.render.Render2DUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Armor extends AbstractDraggable {
    private final List<ItemStack> items = new ArrayList<>();

    public Armor() {
        super("Armor", 0, 0, 82, 22, false);
    }

    @Override
    public boolean visible() {
        return mc.player != null && mc.player.getInventory().armor.stream().anyMatch(s -> !s.isEmpty());
    }

    @Override
    public void tick() {
        items.clear();
        if (mc.player == null) return;
        PlayerEntity player = mc.player;
        items.add(player.getMainHandStack());
        items.add(player.getOffHandStack());
        List<ItemStack> armor = player.getInventory().armor;
        for (int i = armor.size() - 1; i >= 0; i--) items.add(armor.get(i));
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        if (items.isEmpty()) return;

        float itemSize = 13f;
        float gap = 3f;
        int screenWidth = mc.getWindow().getScaledWidth();

        float longSide = (itemSize + gap) * 6f + gap;
        float shortSide = itemSize + gap * 2f;
        float threshold = longSide + gap;

        boolean isVertical = getX() < threshold || getX() > screenWidth - threshold;

        float w = isVertical ? shortSide : longSide;
        float h = isVertical ? longSide : shortSide;

        setWidth((int) w);
        setHeight((int) h);

        blur.render(ShapeProperties.create(matrix, getX(), getY(), w, h)
                .round(5).softness(1).thickness(2)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7f)).build());

        float cx = getX() + gap;
        float cy = getY() + gap;
        float scaleFactor = itemSize / 16f;

        for (ItemStack item : items) {
            matrix.push();
            matrix.translate(cx, cy, 0f);
            matrix.scale(scaleFactor, scaleFactor, 1f);
            context.drawItem(item, 0, 0);
            matrix.pop();

            // durability bar
            if (item.isDamageable()) {
                float maxDmg = item.getMaxDamage();
                float curDmg = item.getDamage();
                float progress = (maxDmg - curDmg) / maxDmg;
                float barH = scaleFactor * 2f;
                float barOffset = barH * 0.7f;
                int color = ColorUtil.overCol(
                        ColorUtil.getColor(130, 255, 130),
                        ColorUtil.getColor(255, 80, 80),
                        progress);
                rectangle.render(ShapeProperties.create(matrix, cx + barOffset, cy + itemSize - barH,
                        (itemSize - barOffset * 2f) * progress, barH)
                        .round(barH * 0.2f).color(color).build());
            }

            if (isVertical) cy += itemSize + gap;
            else cx += itemSize + gap;
        }
    }
}
