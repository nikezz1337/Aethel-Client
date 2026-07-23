package dev.ethereal.client.ui.widget.overlay;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.combat.TpsCalculator;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.widget.Widget;

import java.awt.Color;

public class WatermarkWidget extends Widget {

    private float animatedFps = 0;

    public WatermarkWidget() { super(5f, 5f); }

    @Override public String getName() { return "Watermark"; }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        final float fS      = scaled(5.5f);
        final float iconS   = scaled(8f);
        final float p       = scaled(4f);
        final float gap     = scaled(3f);
        final float rowH    = scaled(12.7f);
        final float round   = 2.5f;
        final float bGap    = scaled(1f);

        // fixed top-left position, ignore draggable
        final float x = scaled(5f);
        final float y = scaled(5f);

        animatedFps = MathHelper.lerp(0.1f, animatedFps, mc.getCurrentFps());

        Color bg   = new Color(8, 8, 8, 200);
        Color text = Color.WHITE;

        String clientName = "Ethereal";
        String fpsStr  = Math.round(animatedFps) + " fps";
        String pingStr = getPing() + " ms";
        String tpsStr  = String.format("%.1f", TpsCalculator.getInstance().getTps()) + " tps";

        String clientIcon = "g";
        String fpsIcon    = "c";
        String pingIcon   = "i";
        String tpsIcon    = "j";

        float clientIconW = Fonts.ICONS2.getWidth(clientIcon, iconS);
        float fpsIconW    = Fonts.ICONS2.getWidth(fpsIcon,    iconS);
        float pingIconW   = Fonts.ICONS2.getWidth(pingIcon,   iconS);
        float tpsIconW    = Fonts.ICONS2.getWidth(tpsIcon,    iconS);

        float nameW = getMediumFont().getWidth(clientName, fS);
        float fpsW  = getMediumFont().getWidth(fpsStr,     fS);
        float pingW = getMediumFont().getWidth(pingStr,    fS);
        float tpsW  = getMediumFont().getWidth(tpsStr,     fS);

        // block 1: icon + name
        float nameBlockW = p + clientIconW + gap + nameW + p;
        // block 2: fps + ping + tps each with icon
        float infoBlockW = p
                + fpsIconW  + gap + fpsW  + gap
                + pingIconW + gap + pingW + gap
                + tpsIconW  + gap + tpsW  + p;

        float cy     = y + rowH / 2f;
        float iconY  = cy - iconS / 2f;
        float textY  = cy - getMediumFont().getHeight(fS) / 2f;

        // name block
        RenderUtil.BLUR_RECT.draw(ms, x, y, nameBlockW, rowH, round, bg);
        float cx = x + p;
        Fonts.ICONS2.drawGradientText(ms, clientIcon, cx, iconY, iconS,
                UIColors.primary(), UIColors.secondary(), iconS);
        cx += clientIconW + gap;
        getMediumFont().drawText(ms, clientName, cx, textY, fS, text, 0f);

        // info block
        float infoX = x + nameBlockW + bGap;
        RenderUtil.BLUR_RECT.draw(ms, infoX, y, infoBlockW, rowH, round, bg);
        cx = infoX + p;

        Fonts.ICONS2.drawGradientText(ms, fpsIcon, cx, iconY, iconS,
                UIColors.primary(), UIColors.secondary(), iconS);
        cx += fpsIconW + gap;
        getMediumFont().drawText(ms, fpsStr, cx, textY, fS, text, 0f);
        cx += fpsW + gap;

        Fonts.ICONS2.drawGradientText(ms, pingIcon, cx, iconY, iconS,
                UIColors.primary(), UIColors.secondary(), iconS);
        cx += pingIconW + gap;
        getMediumFont().drawText(ms, pingStr, cx, textY, fS, text, 0f);
        cx += pingW + gap;

        Fonts.ICONS2.drawGradientText(ms, tpsIcon, cx, iconY, iconS,
                UIColors.primary(), UIColors.secondary(), iconS);
        cx += tpsIconW + gap;
        getMediumFont().drawText(ms, tpsStr, cx, textY, fS, text, 0f);
    }

    private int getPing() {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
}
