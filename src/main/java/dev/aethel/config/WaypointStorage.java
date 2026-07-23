package dev.aethel.config;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.Aethel;
import dev.aethel.event.list.EventHUD;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import dev.aethel.util.render.msdf.Fonts;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class WaypointStorage {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Identifier GPS_ARROW_TEXTURE = Identifier.of("aethel", "textures/arrows/gps.png");

    private static Waypoint current;
    private static float animatedYaw;
    private static float animatedAlpha;
    private static boolean registered = false;

    public static class Waypoint {
        public final int x;
        public final int z;

        public Waypoint(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    public static void set(int x, int z) {
        current = new Waypoint(x, z);
        ensureRegistered();
    }

    public static void clear() {
        current = null;
    }

    public static Waypoint get() {
        return current;
    }

    public static boolean has() {
        return current != null;
    }

    private static void ensureRegistered() {
        if (!registered) {
            Aethel.getInstance().getEventBus().register(new Object() {
                @Subscribe
                public void onRender(EventHUD event) {
                    WaypointStorage.onRender(event);
                }
            });
            registered = true;
        }
    }

    private static void onRender(EventHUD event) {
        if (mc.player == null || mc.world == null) return;

        boolean hasWaypoint = current != null;
        float targetAlpha = hasWaypoint ? 1.0f : 0.0f;
        animatedAlpha = MathHelper.lerp(0.08f, animatedAlpha, targetAlpha);

        if (animatedAlpha <= 0.02f || !hasWaypoint) return;

        float centerX = mc.getWindow().getScaledWidth() * 0.5f;
        float centerY = mc.getWindow().getScaledHeight() * 0.25f;
        float size = 50.0f;

        double deltaX = current.x - mc.player.getX();
        double deltaZ = current.z - mc.player.getZ();
        int distance = (int) Math.round(Math.sqrt(deltaX * deltaX + deltaZ * deltaZ));

        float targetYaw = (float) -Math.toDegrees(Math.atan2(deltaX, deltaZ)) - mc.gameRenderer.getCamera().getYaw();
        animatedYaw = interpolateAngle(animatedYaw, targetYaw, 0.18f);

        int color = ColorProvider.setAlpha(ColorProvider.getThemeColor(), (int)(animatedAlpha * 255));

        MatrixStack matrices = event.getDrawContext().getMatrices();

        // Стрелка
        matrices.push();
        matrices.translate(centerX, centerY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(animatedYaw));
        matrices.translate(-centerX, -centerY, 0.0f);

        float drawX = centerX - size * 0.5f;
        float drawY = centerY - size * 0.5f;

        drawTexturedQuad(event.getDrawContext(), GPS_ARROW_TEXTURE, drawX, drawY, size, size, color);

        matrices.pop();

        // Расстояние
        String distanceText = distance + " m";
        int textColor = ColorProvider.setAlpha(0xFFFFFFFF, (int)(animatedAlpha * 255));
        DrawUtil.drawText(Fonts.SFBOLD.get(), distanceText, centerX - 15, centerY + 7.5f, textColor, 10);
    }

    private static void drawTexturedQuad(DrawContext ctx, Identifier texture, float x, float y, float w, float h, int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF, a = (color >> 24) & 0xFF;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(m, x, y + h, 0).texture(0, 1).color(r, g, b, a);
        buffer.vertex(m, x + w, y + h, 0).texture(1, 1).color(r, g, b, a);
        buffer.vertex(m, x + w, y, 0).texture(1, 0).color(r, g, b, a);
        buffer.vertex(m, x, y, 0).texture(0, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
    }

    private static float interpolateAngle(float current, float target, float factor) {
        float delta = MathHelper.wrapDegrees(target - current);
        return current + delta * factor;
    }
}
