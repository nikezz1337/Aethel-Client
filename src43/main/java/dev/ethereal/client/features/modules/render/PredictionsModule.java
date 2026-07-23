package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.api.utils.rotation.RotationUtil;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector2f;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ColorSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.system.configs.FriendManager;
import dev.ethereal.api.utils.math.ProjectionUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ModuleRegister(name = "Predictions", category = Category.RENDER)
public class PredictionsModule extends Module {
    @Getter private static final PredictionsModule instance = new PredictionsModule();

    private final MultiBooleanSetting render = new MultiBooleanSetting("Render").value(
            new BooleanSetting("Ender pearl").value(true),
            new BooleanSetting("Trident").value(false),
            new BooleanSetting("Arrow").value(false)
    );
    private final BooleanSetting walls = new BooleanSetting("Through walls").value(true);
    private final BooleanSetting friend = new BooleanSetting("Friendly indicator").value(false);
    private final ColorSetting friendColor = new ColorSetting("Color").value(new Color(0, 255, 0));

    private final List<Points> points = new ArrayList<>();
    private final String UNKNOWN = "Неизвестный";
    private long lastRenderTime = 0;

    public PredictionsModule() {
        addSettings(render, walls, friend, friendColor);
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            handleRender3D(event);
        }));

        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            handleRender2D(event);
        }));

        addEvents(render3DEvent, render2DEvent);
    }

    private void handleRender2D(Render2DEvent.Render2DEventData event) {
        DrawContext context = event.context();
        MatrixStack ms = event.matrixStack();
        long now = System.currentTimeMillis();
        float anim = ((now - lastRenderTime) % 2000) / 2000f;

        for (Points point : points) {
            Vector2f project = ProjectionUtil.project((float)point.position.x, (float)point.position.y, (float)point.position.z);
            if (project.x == Float.MAX_VALUE && project.y == Float.MAX_VALUE) continue;

            float cx = project.x;
            float cy = project.y;
            float seconds = point.ticks * 50 / 1000.0f;
            float pulse = 1f + 0.15f * MathHelper.sin(anim * (float)Math.PI * 2f);
            Color baseColor = point.isFriend && friend.getValue() ? friendColor.getValue() : UIColors.gradient(point.age % 360);

            drawLandingGlow(ms, cx, cy, pulse, baseColor);
            drawLandingDot(ms, cx, cy, baseColor);
            drawItemBadge(context, ms, cx, cy, point.stack);
            drawTimerPill(ms, cx, cy, seconds, baseColor);
            drawImpactMark(ms, cx, cy, anim, baseColor);
        }
        lastRenderTime = now;
    }

    private void drawLandingGlow(MatrixStack ms, float cx, float cy, float pulse, Color color) {
        int alpha = Math.min(40, color.getAlpha());
        Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        float glowSize = 18f * pulse;
        RenderUtil.RECT.draw(ms, cx - glowSize / 2f, cy - glowSize / 2f, glowSize, glowSize, glowSize / 2f, glowColor);
        float midSize = 13f * pulse;
        Color midGlow = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 1.5f));
        RenderUtil.RECT.draw(ms, cx - midSize / 2f, cy - midSize / 2f, midSize, midSize, midSize / 2f, midGlow);
    }

    private void drawLandingDot(MatrixStack ms, float cx, float cy, Color color) {
        float dotSize = 3.5f;
        RenderUtil.RECT.draw(ms, cx - dotSize / 2f, cy - dotSize / 2f, dotSize, dotSize, dotSize / 2f, new Color(255, 255, 255, 180));
        RenderUtil.RECT.draw(ms, cx - dotSize / 2f, cy - dotSize / 2f, dotSize, dotSize, dotSize / 2f, new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
    }

    private void drawItemBadge(DrawContext context, MatrixStack ms, float cx, float cy, ItemStack stack) {
        if (stack.isEmpty()) return;
        float badgeSize = 12f;
        float bx = cx - badgeSize / 2f;
        float by = cy - badgeSize / 2f;
        Color badgeBg = new Color(0, 0, 0, 80);
        RenderUtil.RECT.draw(ms, bx - 1, by - 1, badgeSize + 2, badgeSize + 2, 2.5f, badgeBg);

        ms.push();
        ms.translate(cx, cy, 0f);
        float scale = 0.7f;
        ms.scale(scale, scale, 1f);
        context.drawItem(stack, -8, -8);
        ms.pop();
    }

    private void drawTimerPill(MatrixStack ms, float cx, float cy, float seconds, Color accentColor) {
        var font = Fonts.PS_BOLD;
        float fontSize = 6.5f;
        String timeText = String.format("%.1fs", seconds);
        float timeWidth = font.getWidth(timeText, fontSize);
        float pillPad = 2.5f;
        float pillH = fontSize + pillPad * 2f;
        float pillW = timeWidth + pillPad * 3f + 7f;
        float timeY = cy + 8f;
        Color bg = new Color(5, 5, 10, 180);
        RenderUtil.BLUR_RECT.draw(ms, cx - pillW / 2f, timeY - pillPad, pillW, pillH, pillH / 2f, bg);
        RenderUtil.GRADIENT_RECT.draw(ms, cx - pillW / 2f, timeY - pillPad, pillW, pillH, pillH / 2f,
                new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 30),
                new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 3),
                new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 3),
                new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 30));

        float iconX = cx - pillW / 2f + pillPad;
        float iconW = Fonts.ICONS.getWidth("Y", 5f);
        Fonts.ICONS.drawText(ms, "Y", iconX, timeY - 1, 5f, new Color(180, 180, 180));

        font.drawText(ms, timeText, iconX + iconW + 4f, timeY, fontSize, new Color(230, 230, 190));
    }

    private void drawImpactMark(MatrixStack ms, float cx, float cy, float anim, Color color) {
        float markSize = 2.5f + 1f * MathHelper.sin(anim * (float)Math.PI * 2f);
        int alpha = Math.min(80, color.getAlpha());
        Color markColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        RenderUtil.RECT.draw(ms, cx - markSize, cy - 0.5f, markSize * 2f, 1f, 0f, markColor);
        RenderUtil.RECT.draw(ms, cx - 0.5f, cy - markSize, 1f, markSize * 2f, 0f, markColor);
    }

    private void handleRender3D(Render3DEvent.Render3DEventData event) {
        MatrixStack matrixStack = event.matrixStack();
        Vec3d renderOffset = mc.getEntityRenderDispatcher().camera.getPos();
        points.clear();

        matrixStack.push();
        matrixStack.translate(-renderOffset.x, -renderOffset.y, -renderOffset.z);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (var entity : mc.world.getEntities()) {
            ItemStack stack = ItemStack.EMPTY;
            if (entity instanceof EnderPearlEntity pearl) {
                stack = pearl.getStack();
            } else if (entity instanceof TridentEntity) {
                stack = new ItemStack(Items.TRIDENT);
            } else if (entity instanceof ArrowEntity) {
                stack = new ItemStack(Items.ARROW);
            }

            String name = entity instanceof EnderPearlEntity ? ((ThrownItemEntity) entity).getStack().getName().getString() : entity.getName().getString();

            boolean isPearl = entity instanceof EnderPearlEntity;
            boolean isTrident = entity instanceof TridentEntity trident && !trident.isNoClip() && !trident.groundCollision;
            boolean isArrow = entity instanceof ArrowEntity;

            if ((isPearl && render.isEnabled("Ender pearl")) || (isTrident && render.isEnabled("Trident")) || (isArrow && render.isEnabled("Arrow"))) {

                if ((isArrow || isTrident) && (entity.getVelocity().lengthSquared() < 0.001)) continue;

                UUID ownerUuid = ((ProjectileEntity) entity).ownerUuid != null ? ((ProjectileEntity) entity).ownerUuid : null;
                var owner = ownerUuid != null ? mc.world.getPlayerByUuid(ownerUuid) : null;
                boolean isFriend = owner != null && (FriendManager.getInstance().contains(owner.getName().getString()) || owner.getName().getString().equals(mc.getSession().getUsername()));
                String ownerName = owner != null ? mc.getSession().getUsername().equals(owner.getName().getString()) ? "Вас" : owner.getName().getString() : UNKNOWN;

                predictTrajectory(entity, isFriend, name, ownerName, stack, matrixStack);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrixStack.pop();
    }

    private void predictTrajectory(Entity entity, boolean isFriend, String itemName, String ownerName, ItemStack stack, MatrixStack matrixStack) {
        Color color = isFriend && friend.getValue() ? Color.GREEN : UIColors.gradient(entity.age % 360);

        Vec3d motion = entity.getVelocity();
        Vec3d pos = entity.getPos();
        Vec3d prevPos;
        int ticks = 0;

        for (int i = 0; i <= 149; i++) {
            prevPos = pos;
            pos = pos.add(motion);
            motion = getMotion(entity, motion);

            boolean canSee = walls.getValue() || PlayerUtil.canSee(pos);

            if (canSee) {
                var matrix = matrixStack.peek().getPositionMatrix();
                var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                int alpha = Math.max(80, color.getAlpha() - i);
                Color segColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                buffer.vertex(matrix, (float) prevPos.x, (float) prevPos.y, (float) prevPos.z)
                        .color(segColor.getRed(), segColor.getGreen(), segColor.getBlue(), segColor.getAlpha());

                var hit = mc.world.raycast(new RaycastContext(prevPos, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
                if (hit.getType() == HitResult.Type.BLOCK) pos = hit.getPos();

                buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                        .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

                BufferRenderer.drawWithGlobalProgram(buffer.end());

                if (hit.getType() == HitResult.Type.BLOCK || pos.y < -128) {
                    points.add(new Points(pos, ticks, isFriend, itemName, ownerName, stack));
                    break;
                }
            } else {
                var hit = mc.world.raycast(new RaycastContext(prevPos, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    pos = hit.getPos();
                    points.add(new Points(pos, ticks, isFriend, itemName, ownerName, stack));
                    break;
                }
            }
            ticks++;
        }
    }

    private Vec3d getMotion(Entity entity, Vec3d motion) {
        Vec3d motion2 = motion;
        motion2 = entity.isTouchingWater() ? motion2.multiply(0.8) : motion2.multiply(0.99);

        if (!entity.hasNoGravity()) {
            motion2 = motion2.subtract(0, 0.03, 0);
        }

        return motion2;
    }

    private record Points(Vec3d position, int ticks, boolean isFriend, String itemName, String ownerName, ItemStack stack, int age) {
        Points(Vec3d position, int ticks, boolean isFriend, String itemName, String ownerName, ItemStack stack) {
            this(position, ticks, isFriend, itemName, ownerName, stack, (int)(System.currentTimeMillis() % 100000));
        }
    }
}
