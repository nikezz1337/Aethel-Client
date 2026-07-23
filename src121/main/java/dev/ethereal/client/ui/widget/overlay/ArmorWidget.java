package dev.ethereal.client.ui.widget.overlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.client.ui.widget.Widget;

import java.util.ArrayList;
import java.util.List;

public class ArmorWidget extends Widget {
    public ArmorWidget() {
        super(30f, 100f);
    }

    @Override
    public String getName() {
        return "Armor";
    }

    private final List<ItemStack> ITEMS = new ArrayList<>();

    private static final Identifier HOTBAR_TEXTURE = Identifier.ofVanilla("hud/hotbar");

    private static final int SLOT_PX = 20;
    private static final int STRIP_W = 182;
    private static final int STRIP_H = 22;
    private static final float WIDGET_SCALE = 1.0f;

    @Override
    public void render(Render2DEvent event) {
        MatrixStack matrixStack = event.matrixStack();
        DrawContext context = event.context();

        updateItems();
        int slots = ITEMS.size();
        if (slots == 0) return;

        float uiScale = WIDGET_SCALE;
        float x = getDraggable().getX();
        float y = getDraggable().getY();

        // Ширина текстуры: кол-во слотов по 20px + 1px финальной правой рамки
        int pixelWidth = slots * SLOT_PX + 1;

        updateDraggable(STRIP_H * uiScale, pixelWidth * uiScale);

        matrixStack.push();
        matrixStack.translate(x, y, 0f);
        matrixStack.scale(uiScale, uiScale, 1f);

        // 1. Рендерим текстуру хотбара сплошным куском без разрезов под каждый слот
        context.drawGuiTexture(RenderLayer::getGuiTextured, HOTBAR_TEXTURE,
                STRIP_W, STRIP_H,
                0, 0,
                0, 0,
                slots * SLOT_PX, STRIP_H);

        // 2. Дорисовываем последний правый пиксель-рамку, чтобы красиво закрыть сетку
        context.drawGuiTexture(RenderLayer::getGuiTextured, HOTBAR_TEXTURE,
                STRIP_W, STRIP_H,
                STRIP_W - 1, 0,
                slots * SLOT_PX, 0,
                1, STRIP_H);

        // 3. Выводим предметы по стандартной ванильной сетке с отступом +2
        for (int i = 0; i < slots; i++) {
            ItemStack stack = ITEMS.get(i);
            if (!stack.isEmpty()) {
                int itemX = i * SLOT_PX + 2;
                context.drawItem(stack, itemX, 3);
                context.drawStackOverlay(mc.textRenderer, stack, itemX, 3);
            }
        }

        matrixStack.pop();
    }

    private void updateDraggable(float height, float width) {
        getDraggable().setHeight(height);
        getDraggable().setWidth(width);
    }

    private void updateItems() {
        ITEMS.clear();
        if (mc.player == null) return;
        PlayerEntity player = mc.player;

        ITEMS.add(player.getMainHandStack());
        ITEMS.add(player.getOffHandStack());

        List<ItemStack> armor = player.getInventory().armor;
        for (int i = armor.size() - 1; i >= 0; i--) {
            ITEMS.add(armor.get(i));
        }
    }

    @Override
    public void render(MatrixStack matrixStack) {}
}