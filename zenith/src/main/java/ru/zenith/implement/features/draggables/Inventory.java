package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render2DUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Inventory extends AbstractDraggable {
    List<ItemStack> stacks = new ArrayList<>();

    public Inventory() {
        super("Inventory", 390, 10, 123, 60,true);
    }

    @Override
    public boolean visible() {
        return !stacks.stream().filter(stack -> !stack.isEmpty()).toList().isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        stacks = IntStream.range(9, 36).mapToObj(i -> mc.player.inventory.getStack(i)).toList();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 17.5F)
                .round(5,0,5,0).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());
        blur.render(ShapeProperties.create(matrix, getX(), getY() + 17, getWidth(), getHeight() - 17)
                .quality(25).round(0,5,0,5).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        Fonts.getSize(15, Fonts.Type.DEFAULT).drawCenteredString(matrix, getName(), getX() + getWidth() / 2F, getY() + 7, ColorUtil.getText());

        int offsetY = 20;
        int offsetX = 4;
        for (ItemStack stack : stacks) {
            Render2DUtil.defaultDrawStack(context, stack, getX() + offsetX, getY() + offsetY, false, true, 0.5F);

            offsetX += 13;
            if (offsetX > getWidth() - 13) {
                offsetY += 13;
                offsetX = 4;
            }
        }
    }
}
