package dev.ethereal.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.utils.math.ProjectionUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Font;
import dev.ethereal.api.utils.render.fonts.Fonts;

import java.awt.*;

@ModuleRegister(name = "ItemESP", category = Category.RENDER)
public class ItemESP extends Module {
    @Getter private static final ItemESP instance = new ItemESP();

    private static final int alph = 255;
    private static final int bgalpha = 140;
    private static final float tagHeight = 13.5f;
    private static final float tagRadius = 3f;

    private static final Color colorbg = new Color(15, 15, 15, bgalpha);
    private static final Color colorbox = new Color(20, 20, 20, alph / 2);
    private static final Color COUNT_X_COLOR = new Color(170, 170, 170, 220);
    private static final Color COUNT_COLOR = new Color(255, 85, 85, 220);
    private static final Color ACCENT_ITEM = new Color(180, 140, 60, alph);

    private final BooleanSetting box = new BooleanSetting("Квадрат").value(false);
    private final BooleanSetting blur = new BooleanSetting("Размытие").value(false);

    private int screenWidth;
    private int screenHeight;

    public ItemESP() {
        addSettings(box, blur);
    }

    @EventHandler(priority = 2)
    public void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        final float tickDelta = event.partialTicks();
        final boolean renderBox = box.getValue();
        final boolean useBlur = blur.getValue();
        final DrawContext context = event.context();
        final MatrixStack matrices = event.matrixStack();

        screenWidth = mc.getWindow().getScaledWidth();
        screenHeight = mc.getWindow().getScaledHeight();

        Font font7 = Fonts.SF_REGULAR;
        Font font10 = Fonts.SF_MEDIUM;
        float size7 = 7f;
        float size10 = 10f;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity item)) continue;

            ItemStack stack = item.getStack();
            if (stack.isEmpty()) continue;

            double x = MathHelper.lerp(tickDelta, item.prevX, item.getX());
            double y = MathHelper.lerp(tickDelta, item.prevY, item.getY()) + item.getHeight() * 1.3f;
            double z = MathHelper.lerp(tickDelta, item.prevZ, item.getZ());

            Vector2f projected = ProjectionUtil.project(x, y, z);
            if (projected == null || projected.x == Float.MAX_VALUE) continue;

            float centerX = projected.x;
            float centerY = projected.y;
            if (!isOnScreen(centerX, centerY, 50f)) continue;

            Text itemName = stack.getName();
            String itemNameString = itemName.getString();
            if (itemNameString.contains("ТГ") && isServer("holyworld")) {
                itemName = item.getName();
                itemNameString = itemName.getString();
            }

            boolean donate = false;
            if (itemNameString.contains("Сфера") || itemNameString.contains("Шар") || itemNameString.contains("Талисман")
                    || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE || stack.getItem() == Items.ELYTRA
                    || stack.getItem() == Items.DRAGON_HEAD || itemNameString.contains("Трапка")
                    || itemNameString.contains("Дезориентация") || itemNameString.contains("Божья аура")
                    || itemNameString.contains("Пласт") || itemNameString.contains("Явная пыль")
                    || itemNameString.contains("Крушителя") || itemNameString.contains("Тсс")
                    || itemNameString.contains("Осколок") || itemNameString.contains("Проклятая душа")
                    || itemNameString.contains("Ассасина") || itemNameString.contains("Гнева")
                    || itemNameString.contains("Радиации") || itemNameString.contains("Палладина")) {
                donate = true;
            }

            Font font = donate ? font10 : font7;
            float fontSize = donate ? size10 : size7;
            String stackCountString = String.valueOf(stack.getCount());

            float nameWidth = font.getWidth(itemNameString, fontSize);
            float xWidth = font.getWidth("x", fontSize);
            float countWidth = xWidth + font.getWidth(stackCountString, fontSize);
            float totalWidth = 6f + nameWidth + 4f + countWidth;
            float rectX = centerX - totalWidth * 0.5f;
            float currentHeight = donate ? 15.5f : tagHeight;

            if (renderBox) {
                renderBox(matrices, item, tickDelta);
            }

            if (useBlur) {
                RenderUtil.BLUR_RECT.draw(matrices, rectX, centerY, totalWidth, currentHeight, tagRadius, colorbg, 1f);
            } else {
                RenderUtil.RECT.draw(matrices, rectX, centerY, totalWidth, currentHeight, tagRadius, colorbg);
            }

            RenderUtil.drawRect(matrices, rectX + 2f, centerY + currentHeight - 1.5f, totalWidth - 4f, 1f, ACCENT_ITEM);

            float textY = centerY + (currentHeight / 2f - fontSize / 2f);
            float currentX = rectX + 3f;

            font.drawText(matrices, itemNameString, currentX, textY, fontSize, new Color(255, 255, 255, alph));
            currentX += nameWidth + 4f;

            font.drawText(matrices, "x", currentX, textY, fontSize, COUNT_X_COLOR);
            currentX += xWidth;

            font.drawText(matrices, stackCountString, currentX, textY, fontSize, COUNT_COLOR);
        }
    }

    private boolean isOnScreen(float x, float y, float margin) {
        return x >= -margin && x <= screenWidth + margin
                && y >= -margin && y <= screenHeight + margin;
    }

    private void renderBox(MatrixStack matrices, Entity item, float delta) {
        double ix = MathHelper.lerp(delta, item.prevX, item.getX());
        double iy = MathHelper.lerp(delta, item.prevY, item.getY());
        double iz = MathHelper.lerp(delta, item.prevZ, item.getZ());

        Vector2f bottom = ProjectionUtil.project(ix, iy, iz);
        if (bottom == null || bottom.x == Float.MAX_VALUE) return;
        Vector2f top = ProjectionUtil.project(ix, iy + item.getHeight(), iz);
        if (top == null || top.x == Float.MAX_VALUE) return;

        float h = Math.abs(bottom.y - top.y);
        float w = h * 0.5f;

        if (w < 4f) w = 4f;
        if (h < 4f) h = 4f;

        RenderUtil.RECT.draw(matrices, bottom.x - w * 0.5f, top.y, w, h, tagRadius, colorbox);
    }

    private boolean isServer(String name) {
        if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getConnection() == null) return false;
        if (mc.isInSingleplayer()) return false;
        String addr = mc.getNetworkHandler().getConnection().getAddress().toString();
        return addr.toLowerCase().contains(name.toLowerCase());
    }
}
