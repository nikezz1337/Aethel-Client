package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ethereal.api.system.files.FileUtil;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.*;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.combat.TargetManager;
import dev.ethereal.api.utils.math.MathUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

@ModuleRegister(name = "Arrows", category = Category.RENDER)
public class PointersModule extends Module {
    @Getter private static final PointersModule instance = new PointersModule();

    protected final MultiBooleanSetting targets = new MultiBooleanSetting("Цели").value(
            new BooleanSetting("Игроки").value(true),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Предметы").value(false)
    );

    private final SliderSetting pointerRadius = new SliderSetting("Радиус").value(40f).range(20f, 100f).step(1f);
    private final ColorSetting friendColor = new ColorSetting("Цвет друга").value(new Color(0, 255, 0));
    private final BooleanSetting animateRadius = new BooleanSetting("Анимация радиуса").value(true);
    private final BooleanSetting ignoreNaked = new BooleanSetting("Скрыть голых").value(false);

    private final TargetManager.EntityFilter entityFilter = new TargetManager.EntityFilter(targets.getList());

    private final HashSet<Entity> alive = new HashSet<>();
    private final HashMap<Entity, AnimationUtil> animations = new HashMap<>();
    private final AnimationUtil yawAnimation = new AnimationUtil();
    private final AnimationUtil radiusAnimation = new AnimationUtil();

    public PointersModule() {
        addSettings(targets, pointerRadius, friendColor, animateRadius, ignoreNaked);
    }

    @Override
    public void onDisable() {
        alive.clear();
        animations.clear();
        radiusAnimation.setValue(0);
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(2, event -> {
            alive.clear();

            yawAnimation.update();
            yawAnimation.run(mc.player.getYaw(), 200, Easing.EXPO_OUT);

            // анимация радиуса как в референсе
            radiusAnimation.update();
            if (animateRadius.getValue()) {
                float targetRadius = pointerRadius.getValue();
                if (mc.currentScreen != null) targetRadius += 50;
                if (mc.player.isSneaking()) targetRadius -= 10;
                else if (mc.player.getVelocity().horizontalLength() > 0.05) targetRadius += 15;
                radiusAnimation.run(targetRadius, 300, Easing.EXPO_OUT);
            } else {
                radiusAnimation.setValue(pointerRadius.getValue());
            }

            for (Entity entity : mc.world.getEntities()) {
                entityFilter.targetSettings = targets.getList();
                entityFilter.needFriends = true;

                if (mc.player == entity) continue;

                if (ignoreNaked.getValue() && entity instanceof net.minecraft.entity.player.PlayerEntity player) {
                    if (player.getArmor() <= 0) continue;
                }

                if ((entity instanceof ItemEntity && targets.isEnabled("Предметы")) ||
                        (entity instanceof LivingEntity le && entityFilter.isValid(le))) {
                    alive.add(entity);
                }
            }

            for (Entity entity : alive) {
                if (!animations.containsKey(entity)) {
                    animations.put(entity, new AnimationUtil());
                }
            }

            Iterator<Map.Entry<Entity, AnimationUtil>> iterator = animations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Entity, AnimationUtil> entry = iterator.next();
                Entity entity = entry.getKey();
                AnimationUtil anim = entry.getValue();

                boolean isAlive = alive.contains(entity);
                anim.update();
                anim.run(isAlive ? 1.0 : 0.0, 5 * 50, Easing.SINE_OUT);

                drawPointerToEntity(event, entity, anim);

                if (!isAlive && anim.getValue() <= 0.0) {
                    iterator.remove();
                }
            }
        }));

        addEvents(renderEvent);
    }

    private void drawPointerToEntity(Render2DEvent.Render2DEventData event, Entity entity, AnimationUtil spawn){
        DrawContext context = event.context();
        MatrixStack matrixStack = context.getMatrices();
        float centerX = getCenterX();
        float centerY = getCenterY();

        float spawnAnim = (float) spawn.getValue();
        if (spawnAnim <= 0.0) return;

        float radius = (float) radiusAnimation.getValue();
        float yaw = (float) (getEntityYaw(entity) - yawAnimation.getValue());

        // цвет: друг — friendColor, остальные — gradient
        Color baseColor;
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player &&
                dev.ethereal.api.system.configs.FriendManager.getInstance().contains(player.getName().getString())) {
            baseColor = friendColor.getValue();
        } else {
            baseColor = UIColors.primary(180);
        }
        Color color = ColorUtil.setAlpha(baseColor, (int) (spawnAnim * baseColor.getAlpha()));

        matrixStack.translate(centerX, centerY, 0.0F);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT ? -yaw : yaw));
        matrixStack.translate(-centerX, -centerY, 0.0F);

        drawPointer(context, centerX, centerY - radius, 0.5f * 20F, color, false);

        matrixStack.translate(centerX, centerY, 0.0F);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT ? yaw : -yaw));
        matrixStack.translate(-centerX, -centerY, 0.0F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public void drawPointer(DrawContext context, float x, float y, float size, Color color, boolean gps) {
        RenderSystem.setShaderTexture(0, FileUtil.getImage("pointers/" + (gps ? "arrow_gps" : "triangle")));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        float scaledSize = size + 8;

        int c1 = UIColors.primary(color.getAlpha()).getRGB();
        int c2 = UIColors.secondary(color.getAlpha()).getRGB();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, x - (scaledSize / 2f), y + scaledSize, 0).texture(0f, 1f).color(c1);
        buffer.vertex(matrix, x + scaledSize / 2f,   y + scaledSize, 0).texture(1f, 1f).color(c2);
        buffer.vertex(matrix, x + scaledSize / 2f,   y,              0).texture(1f, 0f).color(c1);
        buffer.vertex(matrix, x - (scaledSize / 2f), y,              0).texture(0f, 0f).color(c2);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
    }

    private float getCenterX() {
        return mc.getWindow().getScaledWidth() / 2f;
    }

    private float getCenterY() {
        return mc.getWindow().getScaledHeight() / 2f;
    }

    private float getEntityYaw(Entity entity) {
        if (mc.player == null) return 0;
        double xA = (MathUtil.interpolate(mc.player.prevX, mc.player.getPos().x));
        double zA = (MathUtil.interpolate(mc.player.prevZ, mc.player.getPos().z));
        double x = MathUtil.interpolate(entity.prevX, entity.getPos().x) - xA;
        double z = MathUtil.interpolate(entity.prevZ, entity.getPos().z) - zA;
        return (float) -(Math.atan2(x, z) * (180 / Math.PI));
    }

    private Color getEntityColor(Entity entity) {
        return UIColors.primary(120);
    }
}
