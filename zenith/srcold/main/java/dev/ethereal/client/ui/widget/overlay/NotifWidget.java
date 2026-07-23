package dev.ethereal.client.ui.widget.overlay;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.client.ui.widget.Widget;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class NotifWidget extends Widget {
    private boolean wasChatOpen;
    private long lastDuraCheck;

    public boolean lowDurability = false;
    public boolean moduleState = false;
    public boolean specRequest = false;

    private final List<Notif> notifs = new CopyOnWriteArrayList<>();

    public NotifWidget() {
        super(0, 0);
        setEnabled(true);
    }

    @Override
    public String getName() { return "Notification"; }

    public void addNotif(String text) {
        notifs.add(new Notif(text));
    }

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        MatrixStack ms = event.matrixStack();

        notifs.removeIf(Notif::shouldRemove);
        renderNotifs(ms);

        if (lowDurability && System.currentTimeMillis() - lastDuraCheck > 5000) {
            checkDura();
            lastDuraCheck = System.currentTimeMillis();
        }
    }

    private void renderNotifs(MatrixStack ms) {
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float startY = mc.getWindow().getScaledHeight() / 2f + scaled(25);
        float fS = scaled(5.5f), p = scaled(2.5f), pV = scaled(1.5f);

        float yOff = 0;
        for (Notif n : notifs) {
            float v = n.getAlpha();
            if (v <= 0.01f) continue;

            String txt = n.text.replaceAll("§.", "");
            float txtW = getMediumFont().getWidth(txt, fS);
            float w = txtW + p * 2f;
            float h = fS + pV * 2f;

            float x = cx - w / 2f;
            float y = startY + yOff;

            ms.push();
            ms.translate(cx, y + h / 2f, 0);
            ms.scale(v, v, 1);
            ms.translate(-cx, -(y + h / 2f), 0);

            int alpha = (int)(245 * v);
            RenderUtil.RECT.draw(ms, x, y, w, h, 2.5f, new Color(8, 8, 8, (int)(225 * v)));
            getMediumFont().drawText(ms, txt, x + p, y + h / 2f - fS / 2f, fS, new Color(255, 255, 255, alpha), 0f);

            ms.pop();
            yOff += (h + scaled(1.5f)) * v;
        }
    }

    private void checkDura() {
        if (mc.player == null) return;
        for (ItemStack stack : mc.player.getInventory().main) {
            if (stack.isEmpty() || !stack.isDamageable()) continue;
            int left = stack.getMaxDamage() - stack.getDamage();
            if (left > 0 && left < 100) {
                addNotif("Низкая прочность: " + stack.getName().getString());
                break;
            }
        }
    }

    @Override
    public void render(MatrixStack matrixStack) {}

    private static class Notif {
        String text;
        final long start = System.currentTimeMillis();
        final long dur = 4000;
        boolean expired;

        Notif(String t) { text = t; }

        float getAlpha() {
            long e = System.currentTimeMillis() - start;
            if (e < 300) return e / 300f;
            if (e < dur - 300) return 1f;
            if (e < dur) return 1f - (e - (dur - 300)) / 300f;
            return 0f;
        }

        boolean shouldRemove() {
            if (!expired && System.currentTimeMillis() - start > dur) expired = true;
            return expired && getAlpha() <= 0.01f;
        }
    }
}
