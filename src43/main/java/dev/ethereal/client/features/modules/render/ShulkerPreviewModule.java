package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.render.HandledScreenEvent;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.utils.math.ProjectionUtil;
import lombok.Getter;
import net.minecraft.block.MapColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Shulker Preview", category = Category.RENDER)
public class ShulkerPreviewModule extends Module {
    @Getter
    private static final ShulkerPreviewModule instance = new ShulkerPreviewModule();

    private static final Identifier TEXTURE = Identifier.of("ethereal", "textures/container.png");
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 67;
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 6;
    private static final int SLOT_SPACING = 18;
    private static final float INVENTORY_PREVIEW_SCALE = 0.6F;

    private final BooleanSetting showGroundItems = new BooleanSetting("Отображать на земле").value(true);

    public ShulkerPreviewModule() {
        addSettings(showGroundItems);
    }

    @Override
    public void onEvent() {
        EventListener drawEvent = Render2DEvent.getInstance().subscribe(new Listener<>(this::onDraw));
        EventListener handledScreenEvent = HandledScreenEvent.getInstance().subscribe(new Listener<>(this::onHandledScreen));
        addEvents(drawEvent, handledScreenEvent);
    }

    private void onDraw(Render2DEvent.Render2DEventData event) {
        if (!showGroundItems.getValue() || mc.world == null) return;

        DrawContext context = event.context();
        MatrixStack matrix = context.getMatrices();
        RenderSystem.disableDepthTest();
        matrix.push();
        matrix.translate(0, 0, -1000);

        List<Entity> entities = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            entities.add(entity);
        }
        entities.sort((first, second) -> {
            boolean firstEmpty = first instanceof ItemEntity item
                    && "empty".equals(item.getStack().getName().getContent().toString());
            boolean secondEmpty = second instanceof ItemEntity item
                    && "empty".equals(item.getStack().getName().getContent().toString());
            return Boolean.compare(firstEmpty, secondEmpty);
        });

        for (Entity entity : entities) {
            if (!(entity instanceof ItemEntity item)) continue;

            ItemStack stack = item.getStack();
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            List<ItemStack> stacks = container != null ? container.stream().toList() : List.of();
            if (stacks.isEmpty()) continue;

            double x = MathHelper.lerp(event.partialTicks(), item.prevX, item.getX());
            double y = MathHelper.lerp(event.partialTicks(), item.prevY, item.getY()) + item.getHeight() + 0.15;
            double z = MathHelper.lerp(event.partialTicks(), item.prevZ, item.getZ());

            Vector2f projected = ProjectionUtil.project(new Vec3d(x, y, z));
            if (projected == null || projected.x == Float.MAX_VALUE || projected.y == Float.MAX_VALUE) continue;

            drawShulkerBox(context, stack, stacks, projected.x, projected.y);
        }

        matrix.pop();
        RenderSystem.enableDepthTest();
    }

    private void onHandledScreen(HandledScreenEvent event) {
        Slot hoverSlot = event.getSlotHover();
        if (hoverSlot == null || !hoverSlot.hasStack()) return;

        ItemStack stack = hoverSlot.getStack();
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) return;

        List<ItemStack> stacks = container.stream().toList();
        if (stacks.isEmpty()) return;

        DrawContext context = event.getDrawContext();
        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

        drawShulkerBoxAtPosition(context, stack, stacks, (int) mouseX, (int) mouseY);
    }

    private void drawShulkerBox(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, float screenX, float screenY) {
        MatrixStack matrix = context.getMatrices();
        int color = getShulkerColor(itemStack);

        RenderSystem.disableDepthTest();
        matrix.push();
        matrix.translate(screenX - (double) TEXTURE_WIDTH / 4, screenY + 2, 0);
        matrix.scale(0.5F, 0.5F, 1F);

        drawBackground(context, color);
        drawItems(context, stacks);

        matrix.pop();
    }

    private void drawShulkerBoxAtPosition(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, int mouseX, int mouseY) {
        MatrixStack matrix = context.getMatrices();
        int color = getShulkerColor(itemStack);

        RenderSystem.disableDepthTest();
        matrix.push();

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float scale = INVENTORY_PREVIEW_SCALE;
        int scaledWidth = (int) (TEXTURE_WIDTH * scale);
        int scaledHeight = (int) (TEXTURE_HEIGHT * scale);

        int tooltipHeight = 20;
        int posX = mouseX + 10;
        int posY = mouseY + tooltipHeight + 5;

        if (posX + scaledWidth > screenWidth) {
            posX = mouseX - scaledWidth - 10;
        }
        if (posY + scaledHeight > screenHeight) {
            posY = mouseY - scaledHeight - tooltipHeight - 15;
        }
        if (posX < 0) posX = 10;
        if (posY < 0) posY = 10;
        if (posX + scaledWidth > screenWidth) posX = Math.max(10, screenWidth - scaledWidth - 10);
        if (posY + scaledHeight > screenHeight) posY = Math.max(10, screenHeight - scaledHeight - 10);

        matrix.translate(posX, posY - 100, 300);
        matrix.scale(scale, scale, 1F);

        drawBackground(context, color);
        drawItems(context, stacks);

        matrix.pop();
    }

    private void drawBackground(DrawContext context, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RenderSystem.setShaderColor(r, g, b, a);
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private void drawItems(DrawContext context, List<ItemStack> stacks) {
        int posX = SLOT_START_X;
        int posY = SLOT_START_Y;

        for (ItemStack stack : stacks) {
            context.drawItem(stack, posX, posY);
            context.drawStackOverlay(mc.textRenderer, stack, posX, posY);
            posX += SLOT_SPACING;
            if (posX >= SLOT_START_X + SLOT_SPACING * 9) {
                posY += SLOT_SPACING;
                posX = SLOT_START_X;
            }
        }
    }

    private int getShulkerColor(ItemStack itemStack) {
        if (itemStack.getItem() instanceof BlockItem blockItem) {
            MapColor mapColor = blockItem.getBlock().getDefaultMapColor();
            return 0xFF000000 | mapColor.color;
        }
        return 0xFFFFFFFF;
    }
}
