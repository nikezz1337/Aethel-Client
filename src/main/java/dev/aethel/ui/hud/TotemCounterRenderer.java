package dev.aethel.ui.hud;

import dev.aethel.module.list.render.Interface;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class TotemCounterRenderer implements IMinecraft {
    private final Interface interfaceModule;
    private final Animation widthAnim = new Animation(Easing.EXPO_OUT, 200);

    public TotemCounterRenderer(Interface interfaceModule) {
        this.interfaceModule = interfaceModule;
    }

    public void render(DrawContext context) {
        if (mc.player == null) return;

        float posX = interfaceModule.getTotemCounterDrag().getX();
        float posY = interfaceModule.getTotemCounterDrag().getY();

        int totemCount = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING) {
                totemCount += mc.player.getInventory().getStack(i).getCount();
            }
        }

        String text = totemCount + "x";
        float width = Fonts.SFREGULAR.get().getWidth(text, 9) + 18;
        float height = 14;

        widthAnim.run(width);
        float currentWidth = Math.max(20, (float) widthAnim.getValue());

        float iconW = Fonts.ICONS_NURIK.get().getWidth("H", 8);

        interfaceModule.drawBackground(posX, posY, currentWidth, height, 3, 255);

        DrawUtil.drawRound(posX + iconW + 4.5f, posY + 2, 0.5f, 10.5f, 0, ColorProvider.rgba(88, 88, 88, 255));
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "H", posX + 4, posY + 3.75f, ColorProvider.rgba(255, 255, 255, 255), 8);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), text, posX + iconW + 9, posY + 3.25f, ColorProvider.rgba(255, 255, 255, 255), 8.5f);

        interfaceModule.getTotemCounterDrag().setWidth(currentWidth);
        interfaceModule.getTotemCounterDrag().setHeight(height);
    }
}
