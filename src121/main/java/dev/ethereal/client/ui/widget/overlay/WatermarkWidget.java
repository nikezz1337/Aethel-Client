package dev.ethereal.client.ui.widget.overlay;

import dev.ethereal.paste.xweb.Profile;
import dev.ethereal.paste.xweb.Role;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.api.utils.render.fonts.Icons;
import dev.ethereal.client.features.modules.render.InterfaceModule;
import dev.ethereal.client.ui.widget.Widget;

import java.awt.*;

public class WatermarkWidget extends Widget {

    public WatermarkWidget() {
        super(3f, 3f);
    }

    @Override
    public String getName() {
        return "Watermark";
    }

    @Override
    public void render(MatrixStack matrixStack) {
        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float padding = scaled(2.0f);
        float fontSize = scaled(6.0f);
        float headSize = scaled(9.0f);

        String username = Profile.getUsername();
        String clientName = "Ethereal";
        String fpsInfo = mc.getCurrentFps() + " fps";
        String bpsInfo = String.format("%.1f", MathUtil.getEntityBPS(mc.player)) + " bps";
        String serverInfo = getServerAddress();

        String fpsIcon = Icons.PERFORMANCE.getLetter();
        String bpsIcon = Icons.SPEED.getLetter();
        String ipIcon = Icons.WLAN.getLetter();

        InterfaceModule m = InterfaceModule.getInstance();
        Color wmBg = new Color(8, 8, 8, 200);
        float round = scaled(2.7f);
        float gap = scaled(2.5f);
        float sep = scaled(4f);

        float totalHeight = Math.max(headSize, getMediumFont().getHeight(fontSize)) + padding * 2f;
        float iconY = y + (totalHeight - Fonts.ICONS.getHeight(fontSize)) / 2f;
        float textY = y + (totalHeight - getMediumFont().getHeight(fontSize)) / 2f;
        float headY = y + (totalHeight - headSize) / 2f;

        boolean showIcon = m.watermarkElements.isEnabled("Icon");
        float logoDrawSize = scaled(11f);
        float logoBlockW;
        if (showIcon) {
            logoBlockW = padding + logoDrawSize + scaled(2f) + getMediumFont().getWidth(clientName, fontSize) + padding;
        } else {
            logoBlockW = padding + getMediumFont().getWidth(clientName, fontSize) + padding;
        }

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, logoBlockW, totalHeight, round, wmBg);

        float lx = x + padding;
        if (showIcon) {
            Identifier iconId = Identifier.of("ethereal", "icon.png");
            int iconTexture = mc.getTextureManager().getTexture(iconId).getGlId();
            float iconDrawY = y + (totalHeight - logoDrawSize) / 2f;
            RenderUtil.TEXTURE_RECT.draw(matrixStack, lx-1.2f, iconDrawY, logoDrawSize, logoDrawSize, scaled(1.2f), Color.WHITE, 0f, 0f, 1f, 1f, iconTexture);
            lx += logoDrawSize + scaled(2f);
        }
        getMediumFont().drawText(matrixStack, clientName, lx-1.2f, textY, fontSize, Color.WHITE);

        float infoWidth = padding * 2;

        if (m.watermarkElements.isEnabled("Name")) {
            infoWidth += headSize + scaled(2f) + getMediumFont().getWidth(username, fontSize);
        }
        if (m.watermarkElements.isEnabled("IP")) {
            if (infoWidth > padding * 2) infoWidth += sep + Fonts.ICONS.getWidth(ipIcon, fontSize) + sep;
            else infoWidth += Fonts.ICONS.getWidth(ipIcon, fontSize) + sep;
            infoWidth += getMediumFont().getWidth(serverInfo, fontSize);
        }
        if (m.watermarkElements.isEnabled("FPS")) {
            if (infoWidth > padding * 2) infoWidth += sep + Fonts.ICONS.getWidth(fpsIcon, fontSize) + sep;
            else infoWidth += Fonts.ICONS.getWidth(fpsIcon, fontSize) + sep;
            infoWidth += getMediumFont().getWidth(fpsInfo, fontSize);
        }
        if (m.watermarkElements.isEnabled("BPS")) {
            if (infoWidth > padding * 2) infoWidth += sep + Fonts.ICONS.getWidth(bpsIcon, fontSize) + sep;
            else infoWidth += Fonts.ICONS.getWidth(bpsIcon, fontSize) + sep;
            infoWidth += getMediumFont().getWidth(bpsInfo, fontSize);
        }

        float infoX = x + logoBlockW + gap;
        boolean hasInfo = infoWidth > padding * 2;

        if (hasInfo) {
            RenderUtil.BLUR_RECT.draw(matrixStack, infoX, y, infoWidth, totalHeight, round, wmBg);
        }

        float cx = infoX + padding;

        if (m.watermarkElements.isEnabled("Name")) {
            if (mc.player != null) {
                RenderUtil.TEXTURE_RECT.drawHead(matrixStack, mc.player, cx , headY, headSize, headSize, 0f, headSize * 0.5f, Color.WHITE);
            }
            cx += headSize + scaled(2f);
            getMediumFont().drawText(matrixStack, username, cx, textY, fontSize, Color.WHITE);
            cx += getMediumFont().getWidth(username, fontSize);
        }

        if (m.watermarkElements.isEnabled("IP")) {
            if (cx > infoX + padding) cx += sep;
            Fonts.ICONS.drawText(matrixStack, ipIcon, cx, iconY, fontSize, new Color(180, 180, 180));
            cx += Fonts.ICONS.getWidth(ipIcon, fontSize) + sep;
            getMediumFont().drawText(matrixStack, serverInfo, cx, textY, fontSize, Color.WHITE);
            cx += getMediumFont().getWidth(serverInfo, fontSize);
        }

        if (m.watermarkElements.isEnabled("FPS")) {
            if (cx > infoX + padding) cx += sep;
            Fonts.ICONS.drawText(matrixStack, fpsIcon, cx, iconY, fontSize, new Color(180, 180, 180));
            cx += Fonts.ICONS.getWidth(fpsIcon, fontSize) + sep;
            getMediumFont().drawText(matrixStack, fpsInfo, cx, textY, fontSize, Color.WHITE);
            cx += getMediumFont().getWidth(fpsInfo, fontSize);
        }

        if (m.watermarkElements.isEnabled("BPS")) {
            if (cx > infoX + padding) cx += sep;
            Fonts.ICONS.drawText(matrixStack, bpsIcon, cx, iconY, fontSize, new Color(180, 180, 180));
            cx += Fonts.ICONS.getWidth(bpsIcon, fontSize) + sep;
            getMediumFont().drawText(matrixStack, bpsInfo, cx, textY, fontSize, Color.WHITE);
        }

        float totalWidth = hasInfo ? infoX + infoWidth - x : logoBlockW;
        getDraggable().setWidth(totalWidth);
        getDraggable().setHeight(totalHeight);
    }

    private String getServerAddress() {
        try {
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
                if (mc.isInSingleplayer()) return "Singleplayer";
                String addr = mc.getNetworkHandler().getConnection().getAddress().toString();
                addr = addr.replaceAll("/.*", "")
                        .replaceAll(":.*", "")
                        .replaceAll("\\.+$", "")
                        .trim();
                return addr.isEmpty() ? "localhost" : addr;
            }
        } catch (Exception ignored) {}
        return "localhost";
    }
}