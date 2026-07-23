package dev.ethereal.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ColorSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.math.ProjectionUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Font;
import dev.ethereal.api.utils.render.fonts.Fonts;

import java.awt.*;

@ModuleRegister(name = "ItemESP", category = Category.RENDER)
public class ItemESP extends Module implements QuickImports {
    @Getter private static final ItemESP instance = new ItemESP();

    private final ColorSetting color = new ColorSetting("Color").value(new Color(30, 30, 40, 200));
    private final ColorSetting textColor = new ColorSetting("Text Color").value(new Color(255, 255, 255));
    private final SliderSetting scale = new SliderSetting("Scale").value(1f).range(0.5f, 2f).step(0.1f);
    private final BooleanSetting showCount = new BooleanSetting("Show Count").value(true);
    private final BooleanSetting showDistance = new BooleanSetting("Show Distance").value(true);
    private final SliderSetting glassy = new SliderSetting("Glassy").value(0.3f).range(0f, 1f).step(0.05f);

    public ItemESP() {
        addSettings(color, textColor, scale, showCount, showDistance, glassy);
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(2, event -> {
            if (mc.world == null || mc.player == null) return;

            for (ItemEntity itemEntity : mc.world.getEntitiesByClass(ItemEntity.class, 
                    mc.player.getBoundingBox().expand(200), e -> true)) {
                renderItemTag(itemEntity, event.context(), event.partialTicks());
            }
        }));

        addEvents(renderEvent);
    }

    private void renderItemTag(ItemEntity entity, DrawContext context, float partialTicks) {
        double xI = MathHelper.lerp(partialTicks, entity.prevX, entity.getX());
        double yI = MathHelper.lerp(partialTicks, entity.prevY, entity.getY());
        double zI = MathHelper.lerp(partialTicks, entity.prevZ, entity.getZ());

        Vec3d pos = new Vec3d(xI, yI + 0.5, zI);
        Vector2f projected = ProjectionUtil.project(pos);

        if (projected == null) return;

        float x = projected.x;
        float y = projected.y;

        if (x < 0 || x > mc.getWindow().getScaledWidth() || 
            y < 0 || y > mc.getWindow().getScaledHeight()) {
            return;
        }

        MatrixStack matrixStack = context.getMatrices();
        Font font = Fonts.SF_MEDIUM;

        ItemStack stack = entity.getStack();
        String name = stack.getName().getString();
        
        if (showCount.getValue() && stack.getCount() > 1) {
            name += " x" + stack.getCount();
        }

        if (showDistance.getValue()) {
            double distance = Math.sqrt(mc.player.squaredDistanceTo(xI, yI, zI));
            name += " §7[" + String.format("%.1f", distance) + "м]";
        }

        float scale = this.scale.getValue();
        float size = 8f * scale;
        float gap = 2f * scale;
        float nameWidth = font.getWidth(name, size);

        x -= nameWidth / 2f + gap;

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, nameWidth + gap * 2f, size + gap * 2f, 
            scale, color.getValue(), 1f - glassy.getValue());

        font.drawText(matrixStack, name, x + gap, y + gap, size, textColor.getValue());
    }
}
