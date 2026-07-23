package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.config.FriendManager;
import dev.aethel.event.list.EventHUD;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.util.render.providers.ColorProvider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ModuleInformation(
        moduleName = "Arrows",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Стрелки на игроков"
)
public class Arrows extends Module {

    public static Arrows INSTANCE = new Arrows();

    private static final Identifier perviy = Identifier.of("aethel", "textures/arrows/arrow.png");
    private static final Identifier vtoroy = Identifier.of("aethel", "textures/arrows/arr.png");

    private final ModeSetting type = new ModeSetting("Вид", "Первый", "Первый", "Второй");
    private final SliderSetting radius = new SliderSetting("Радиус", 44.0, 30.0, 80.0, 1.0);
    private final SliderSetting size = new SliderSetting("Размер", 19.5, 16.0, 28.0, 0.5);
    private final BooleanSetting ignoreNaked = new BooleanSetting("Скрыть голых", false);

    private final Map<UUID, ArrowState> states = new HashMap<>();
    private final Set<UUID> seenPlayers = new HashSet<>();

    public Arrows() {
        INSTANCE = this;
    }

    @Subscribe
    public void onRender(EventHUD event) {
        if (mc.player == null || mc.world == null || mc.options.hudHidden) {
            states.clear();
            return;
        }
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) {
            fadeAllStates();
            return;
        }

        float centerX = mc.getWindow().getScaledWidth() * 0.5f;
        float centerY = mc.getWindow().getScaledHeight() * 0.5f;
        float arrowSize = (float) size.getValue();
        float y = centerY - (float) radius.getValue();
        float playerYaw = mc.player.getYaw();
        Vec3d selfPos = mc.player.getPos();

        seenPlayers.clear();
        for (var player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive() || player.isSpectator() || isGhostPlayer(player)) {
                continue;
            }

            if (ignoreNaked.getValue() && player.getArmor() <= 0) {
                continue;
            }

            UUID uuid = player.getUuid();
            ArrowState state = states.computeIfAbsent(uuid, id -> new ArrowState());
            seenPlayers.add(uuid);

            int color = getPlayerColor(player);
            float targetYaw = getRelativeYaw(player, playerYaw, selfPos);
            state.rotation = interpolateAngle(state.rotation, targetYaw, 0.18f);
            state.alpha = approach(state.alpha, 1.0f, 0.12f);
            float alpha = MathHelper.clamp(state.alpha, 0.0f, 1.0f);
            if (alpha <= 0.01f) {
                continue;
            }

            int drawColor = ColorProvider.setAlpha(color, (int)(alpha * 255));
            int shadowColor = ColorProvider.setAlpha(ColorProvider.brighter(color, 0.55f), (int)(alpha * 0.65f * 255));
            renderArrow(event.getDrawContext(), centerX, centerY, y, arrowSize, state.rotation, drawColor, shadowColor);
        }

        states.entrySet().removeIf(entry -> {
            if (seenPlayers.contains(entry.getKey())) {
                return false;
            }
            ArrowState state = entry.getValue();
            state.alpha = approach(state.alpha, 0.0f, 0.10f);
            return state.alpha <= 0.02f;
        });
    }

    private void renderArrow(DrawContext context, float centerX, float centerY, float y, float size, float rotation, int color, int shadowColor) {
        Identifier arrow;
        if (type.getIndex() == 0) {
            arrow = perviy;
        } else {
            arrow = vtoroy;
        }

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, centerY, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        matrices.translate(-centerX, -centerY, 0.0f);

        float x = centerX - size * 0.5f;

        drawTexturedQuad(context, arrow, x, y + size * 0.08f, size, size, shadowColor);
        drawTexturedQuad(context, arrow, x, y, size, size, color);

        matrices.pop();
    }

    private void drawTexturedQuad(DrawContext ctx, Identifier texture, float x, float y, float w, float h, int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF, a = (color >> 24) & 0xFF;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(m, x, y + h, 0).texture(0, 1).color(r, g, b, a);
        buf.vertex(m, x + w, y + h, 0).texture(1, 1).color(r, g, b, a);
        buf.vertex(m, x + w, y, 0).texture(1, 0).color(r, g, b, a);
        buf.vertex(m, x, y, 0).texture(0, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.disableBlend();
    }

    private void fadeAllStates() {
        states.entrySet().removeIf(entry -> {
            ArrowState state = entry.getValue();
            state.alpha = approach(state.alpha, 0.0f, 0.10f);
            return state.alpha <= 0.02f;
        });
    }

    private float approach(float current, float target, float factor) {
        factor = MathHelper.clamp(factor, 0.0f, 1.0f);
        return MathHelper.lerp(factor, current, target);
    }

    private int getPlayerColor(net.minecraft.client.network.AbstractClientPlayerEntity player) {
        String name = player.getName().getString();
        boolean isFriend = FriendManager.isFriend(name);
        return isFriend ? ColorProvider.rgb(0, 255, 0) : ColorProvider.getThemeColor();
    }

    private float getRelativeYaw(Entity entity, float playerYaw, Vec3d selfPos) {
        Vec3d entityPos = entity.getPos();
        double dx = entityPos.x - selfPos.x;
        double dz = entityPos.z - selfPos.z;
        float yaw = (float) -Math.toDegrees(Math.atan2(dx, dz));
        return MathHelper.wrapDegrees(yaw - playerYaw);
    }

    private float interpolateAngle(float current, float target, float factor) {
        float delta = MathHelper.wrapDegrees(target - current);
        return current + delta * factor;
    }

    private boolean isGhostPlayer(net.minecraft.client.network.AbstractClientPlayerEntity player) {
        if (player.getCustomName() != null) {
            String name = player.getCustomName().getString();
            if (name != null && name.startsWith("Ghost_")) {
                return true;
            }
        }
        return "OtherClientPlayerEntity".equals(player.getClass().getSimpleName()) && player.getPitch() == -30.0f;
    }

    private static final class ArrowState {
        private float alpha;
        private float rotation;
    }
}
