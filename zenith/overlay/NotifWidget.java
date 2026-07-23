package sweetie.evaware.client.ui.widget.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.ui.widget.Widget;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class NotifWidget extends Widget {
    private final Identifier star = FileUtil.getImage("particles/star1");
    private boolean wasChatOpen;
    private float fixedX, fixedY;
    private float hintAlpha;
    
    public boolean specRequest = false;
    public boolean moduleState = false;
    public boolean lowDurability = false;
    private float settingsAnim;
    private boolean settingsOpen;
    private float settingsX, settingsY;
    
    private boolean wasRightClick;
    private boolean wasLeftClick;
    private long lastDuraCheck;
    
    private final List<Notif> notifs = new CopyOnWriteArrayList<>();

    public NotifWidget() {
        super(0, 0);
        setEnabled(true);
    }

    @Override
    public String getName() {
        return "Notification";
    }

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
        
        boolean chatOpen = mc.currentScreen instanceof ChatScreen;

        if (!chatOpen) {
            settingsOpen = false;
            return;
        }

        if (chatOpen && !wasChatOpen) {
            fixedX = mc.getWindow().getScaledWidth() / 2f;
            fixedY = mc.getWindow().getScaledHeight() / 2f;
        }
        wasChatOpen = chatOpen;

        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();

        float pad = scaled(5);
        float icon = scaled(10);
        float gap = scaled(4);
        float font = scaled(7);
        
        String txt = "Это уведомление, кликни на меня для настройки";
        float txtW = getMediumFont().getWidth(txt, font);
        
        float w = pad + txtW + pad;
        float h = font + pad * 2;
        float r = scaled(3);

        float x = fixedX - w / 2;
        float y = fixedY + scaled(15);

        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;

        boolean rightClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (rightClick && !wasRightClick && hover) {
            settingsOpen = !settingsOpen;
            settingsX = (float) mx;
            settingsY = (float) my;
        }
        wasRightClick = rightClick;

        hintAlpha += (hover ? 0.15f : -0.15f);
        hintAlpha = Math.max(0, Math.min(1, hintAlpha));

        if (hintAlpha > 0) {
            String hint = "ПКМ - Дополнительные настройки";
            float hintW = getMediumFont().getWidth(hint, font);
            int alpha = (int) (255 * hintAlpha);
            getMediumFont().drawText(ms, hint, fixedX - hintW / 2, y - font - scaled(3), font, new Color(255, 255, 255, alpha));
        }

        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, r, new Color(20, 20, 30, 230));

        float ix = x + pad;
        
        getMediumFont().drawText(ms, txt, ix, y + h / 2 - font / 2, font, UIColors.textColor());
        
        renderSettings(ms, mx, my);
    }

    private void renderSettings(MatrixStack ms, double mx, double my) {
        settingsAnim += (settingsOpen ? 0.15f : -0.15f);
        settingsAnim = Math.max(0, Math.min(1, settingsAnim));

        if (settingsAnim <= 0.05f) return;

        float pad = scaled(6);
        float gap = scaled(5);
        float font = scaled(6);
        float toggle = scaled(7);

        String[] opts = {"Просьба о наблюдении", "Состояние модулей", "Низкая прочность предметов"};

        float maxW = 0;
        for (String opt : opts) {
            float w = getMediumFont().getWidth(opt, font);
            if (w > maxW) maxW = w;
        }

        float w = pad + toggle + gap + maxW + pad;
        float h = pad + (font + gap) * opts.length + pad;

        float x = settingsX + scaled(10);
        float y = settingsY;

        if (x + w > mc.getWindow().getScaledWidth()) x = settingsX - w - scaled(10);
        if (y + h > mc.getWindow().getScaledHeight()) y = mc.getWindow().getScaledHeight() - h - scaled(10);

        ms.push();
        float centX = x + w / 2;
        float centY = y + h / 2;
        ms.translate(centX, centY, 0);
        ms.scale(settingsAnim, settingsAnim, 1);
        ms.translate(-centX, -centY, 0);

        int bgAlpha = (int) (230 * settingsAnim);
        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, scaled(3), new Color(20, 20, 30, bgAlpha));

        float cy = y + pad;
        boolean[] states = {specRequest, moduleState, lowDurability};

        boolean leftClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        for (int i = 0; i < opts.length; i++) {
            float tx = x + pad + toggle + gap;
            int textAlpha = (int) (255 * settingsAnim);
            
            getMediumFont().drawText(ms, opts[i], tx, cy, font, new Color(200, 200, 200, textAlpha));

            float toggleX = x + pad;
            float toggleY = cy;
            
            boolean hoverToggle = mx >= toggleX && mx <= toggleX + toggle && my >= toggleY && my <= toggleY + toggle;
            
            if (hoverToggle && leftClick && !wasLeftClick) {
                switch (i) {
                    case 0 -> specRequest = !specRequest;
                    case 1 -> moduleState = !moduleState;
                    case 2 -> lowDurability = !lowDurability;
                }
                states[i] = !states[i];
            }
            
            Color toggleColor = states[i] ? new Color(100, 255, 100, textAlpha) : new Color(255, 100, 100, textAlpha);
            RenderUtil.RECT.draw(ms, toggleX, toggleY, toggle, toggle, toggle * 0.3f, toggleColor);

            cy += font + gap;
        }

        wasLeftClick = leftClick;
        ms.pop();
    }

    private void renderNotifs(MatrixStack ms) {
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;
        float yOff = 25f;

        for (Notif n : notifs) {
            float v = n.getAlpha();
            if (v <= 0.05f) continue;

            String txt = n.text.replace("§a", "").replace("§c", "").replace("§", "");
            float font = 5.5f;
            float pad = 3.5f;
            float r = 2.5f;
            
            float txtW = getMediumFont().getWidth(txt, font);
            float w = pad + txtW + pad;
            float h = font + pad * 2;
            
            float x = cx - w / 2;
            float y = cy + yOff;
            
            ms.push();
            float centX = cx;
            float centY = y + h / 2;
            ms.translate(centX, centY, 0);
            ms.scale(v, v, 1);
            ms.translate(-centX, -centY, 0);

            int bgAlpha = (int) (230 * v);
            RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, r, new Color(20, 20, 30, bgAlpha));

            float ix = x + pad;
            
            int textAlpha = (int) (255 * v);
            getMediumFont().drawText(ms, txt, ix, y + h / 2 - font / 2, font, new Color(255, 255, 255, textAlpha));

            ms.pop();
            yOff += (h + 2.5f) * v;
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
        long start = System.currentTimeMillis();
        long dur = 4000;
        boolean expired;
        
        Notif(String t) {
            text = t;
        }
        
        float getAlpha() {
            long e = System.currentTimeMillis() - start;
            if (e < 300) return e / 300f;
            if (e < dur - 300) return 1f;
            if (e < dur) return 1f - (e - (dur - 300)) / 300f;
            return 0f;
        }
        
        boolean shouldRemove() {
            if (!expired && System.currentTimeMillis() - start > dur) {
                expired = true;
            }
            return expired && getAlpha() <= 0.05f;
        }
    }
}
