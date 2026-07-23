package dev.aethel.ui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.module.list.render.Interface;
import dev.aethel.util.base.Instance;
import dev.aethel.util.draggable.Draggable;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationRenderer {
    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();
    private static boolean wasChatOpen = false;
    private static boolean chatTestPosted = false;
    private static Notification chatTestNotification = null;

    private static final Identifier INFO_TEXTURE = Identifier.of("aethel", "textures/notifications/info.png");
    private static final float FONT_SIZE = 8f;
    private static final float ICON_SIZE = 8f;
    private static final float HEIGHT = 16f;
    private static final float RADIUS = 3f;
    private static final float PAD_L = 5f;
    private static final float SG = 3f;

    public static void post(String name, boolean enabled) {
        notifications.add(0, new Notification(name, enabled));
    }

    public static void postWarning(String text) {
        notifications.add(0, new Notification(text, true, true));
    }

    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

        if (chatOpen && !wasChatOpen) {
            chatTestPosted = false;
        }

        if (chatOpen && !chatTestPosted) {
            chatTestNotification = new Notification("Тестовое уведомление", true, true);
            chatTestNotification.isChatTest = true;
            notifications.add(0, chatTestNotification);
            chatTestPosted = true;
        }

        if (!chatOpen && wasChatOpen) {
            notifications.removeIf(n -> n.isChatTest);
            chatTestNotification = null;
            chatTestPosted = false;
        }

        wasChatOpen = chatOpen;

        Interface interfaceModule = Instance.get(Interface.class);
        if (interfaceModule == null) return;

        Draggable drag = interfaceModule.getNotificationsDrag();
        float centerX = drag.getX();
        float startY = drag.getY();

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();

        List<Notification> visible = new ArrayList<>();
        for (Notification n : notifications) {
            boolean shouldRemove;
            if (n.isChatTest) {
                shouldRemove = false;
            } else {
                shouldRemove = System.currentTimeMillis() - n.time > n.duration && n.anim.getValue() <= 0.01;
            }

            if (shouldRemove) {
                notifications.remove(n);
                continue;
            }

            boolean expiring;
            if (n.isChatTest) {
                expiring = false;
            } else {
                expiring = System.currentTimeMillis() - n.time > n.duration;
            }

            n.anim.run(expiring ? 0 : 1);

            if (n.anim.getValue() <= 0.01) continue;

            visible.add(n);
        }

        if (visible.isEmpty()) {
            drag.setWidth(120);
            drag.setHeight(HEIGHT);
            return;
        }

        float offset = 0;
        float maxW = 0;

        for (Notification n : visible) {
            double animValue = n.anim.getValue();
            float clampedAlpha = (float) Math.max(0.0, Math.min(1.0, animValue));
            int aInt = (int) (255 * clampedAlpha);

            String moduleName = getModuleName(n);
            boolean isEnabled = n.enabled;
            boolean isWarn = n.isChatTest || n.isWarning;

            String textPart1;
            String textPart2;
            int textPart2Color;

            if (isWarn) {
                textPart1 = n.customText;
                textPart2 = "";
                textPart2Color = ColorProvider.rgba(255, 70, 70, aInt);
            } else if (isEnabled) {
                textPart1 = moduleName + " was ";
                textPart2 = " enabled";
                textPart2Color = ColorProvider.rgba(80, 230, 80, aInt);
            } else {
                textPart1 = moduleName + " was ";
                textPart2 = " disabled";
                textPart2Color = ColorProvider.rgba(230, 80, 80, aInt);
            }

            float w1 = Fonts.SFREGULAR.get().getWidth(textPart1, FONT_SIZE);
            float w2 = Fonts.SFREGULAR.get().getWidth(textPart2, FONT_SIZE);
            float textWidth = w1 + w2;
            float width = PAD_L + ICON_SIZE + SG + textWidth + PAD_L;
            float height = HEIGHT;

            float x = centerX - width / 2f;
            float y = startY + offset;

            if (width > maxW) maxW = width;

            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(x + width / 2f, y + height / 2f, 0);
            matrices.scale((float) animValue, (float) animValue, 1f);
            matrices.translate(-(x + width / 2f), -(y + height / 2f), 0);

            int bgColor = ColorProvider.rgba(
                    ((t1 >> 16) & 0xFF) >> 2,
                    ((t1 >> 8) & 0xFF) >> 2,
                    (t1 & 0xFF) >> 2,
                    Math.min(160, aInt)
            );

            int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
            int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp;

            Matrix4f m = matrices.peek().getPositionMatrix();

            DrawUtil.drawShadow(m, x, y, width, height, RADIUS, 8f, ColorProvider.rgba(0, 0, 0, 80));
            interfaceModule.drawGlow(m, x, y, width, height, RADIUS, clampedAlpha);

            DrawUtil.drawRound(x - 0.5f, y - 0.5f, width + 1f, height + 1f, RADIUS, glow[0], glow[1], glow[2], glow[3]);
            DrawUtil.drawRound(x, y, width, height, RADIUS, bgColor);

            float textX = x + PAD_L + ICON_SIZE + SG;
            float textY = y + (height - FONT_SIZE) / 2f - 0.5f;

            int baseColor = ColorProvider.rgba(230, 230, 235, aInt);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), textPart1, textX, textY, baseColor, FONT_SIZE);
            if (!textPart2.isEmpty()) {
                DrawUtil.drawText(Fonts.SFREGULAR.get(), textPart2, textX + w1, textY, textPart2Color, FONT_SIZE);
            }

            matrices.pop();

            drawTexture(context.getMatrices(), INFO_TEXTURE, x + PAD_L, y + (height - ICON_SIZE) / 2f, ICON_SIZE, ICON_SIZE, aInt);

            offset += (height + 3) * clampedAlpha;
        }

        drag.setWidth(maxW);
        drag.setHeight(offset > 0 ? offset : HEIGHT);
    }

    private static void drawTexture(MatrixStack matrices, Identifier texture, float x, float y, float w, float h, int alpha) {
        int color = ColorProvider.setAlpha(0xFFFFFFFF, alpha);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(m, x, y + h, 0).texture(0, 1).color(r, g, b, a);
        buffer.vertex(m, x + w, y + h, 0).texture(1, 1).color(r, g, b, a);
        buffer.vertex(m, x + w, y, 0).texture(1, 0).color(r, g, b, a);
        buffer.vertex(m, x, y, 0).texture(0, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
    }

    private static String getModuleName(Notification n) {
        if (n.name != null) return n.name;
        return "Module";
    }

    private static class Notification {
        String name;
        boolean enabled;
        long time;
        long duration = 1200;
        Animation anim = new Animation(Easing.BACK_OUT, 300);

        boolean isWarning = false;
        boolean isChatTest = false;
        String customText;

        public Notification(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
            this.time = System.currentTimeMillis();
        }

        public Notification(String customText, boolean enabled, boolean isWarning) {
            this.customText = customText;
            this.enabled = enabled;
            this.isWarning = isWarning;
            this.time = System.currentTimeMillis();
            this.duration = 2200;
        }
    }
}
