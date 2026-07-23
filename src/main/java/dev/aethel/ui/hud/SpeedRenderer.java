package dev.aethel.ui.hud;

import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;

public class SpeedRenderer implements IMinecraft {

    public void render(DrawContext context) {
        if (mc.player == null) return;

        double deltaX = mc.player.getX() - mc.player.prevX;
        double deltaY = mc.player.getY() - mc.player.prevY;
        double deltaZ = mc.player.getZ() - mc.player.prevZ;
        double speedBps = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 20;

        String text = String.format(java.util.Locale.US, "%.2f", speedBps);
        float fontSize = 11f;
        float textWidth = Fonts.SFBOLD.get().getWidth(text, fontSize);

        float x = mc.getWindow().getScaledWidth() / 2f - (textWidth / 2f);
        float y = mc.getWindow().getScaledHeight() / 2f + 12f;

        DrawUtil.drawText(Fonts.SFBOLD.get(), text, x, y, -1, fontSize);
    }
}
