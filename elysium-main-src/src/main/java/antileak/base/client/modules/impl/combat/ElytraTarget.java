package antileak.base.client.modules.impl.combat;

import com.adl.nativeprotect.Native;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.Event3DRender;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.api.utils.combat.PredictUtils;
import antileak.base.api.utils.color.ColorUtils;
import antileak.base.api.utils.math.TimerUtils;
import antileak.base.api.utils.player.InventoryUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.impl.movement.ElytraBoost;
import antileak.base.client.modules.settings.implement.BooleanSetting;
import antileak.base.client.modules.settings.implement.FloatSetting;
import antileak.base.client.modules.settings.implement.ModeSetting;
import antileak.base.mixin.FireworkRocketEntityAccessor;

public class ElytraTarget extends Module {
    public static ElytraTarget INSTANCE = new ElytraTarget();

    public final FloatSetting elytraDistance = new FloatSetting("Дистанция", 30.0f, 5.0f, 100.0f, 5.0f);
    public final FloatSetting forward = new FloatSetting("Перегон", 3.0f, 0.0f, 10.0f, 0.1f);
    public final FloatSetting forwardValue = forward;
    private final ModeSetting predictMode = new ModeSetting("Mode", "ReallyWorld", "ReallyWorld", "ReallyWorld - 2", "Default");
    private final ModeSetting useFirework = new ModeSetting("Фейерверк", "Никогда", "Никогда", "По таймеру");
    private final FloatSetting fireworkTiming = new FloatSetting("Задержка фейерверка", 1.0f, 0.1f, 5.0f, 0.1f)
            .visible(() -> useFirework.is("По таймеру"));
    private final ModeSetting fireworkMode = new ModeSetting("Режим фейерверка", "Только с целью", "Только с целью", "Без цели", "Всегда")
            .visible(() -> useFirework.is("По таймеру"));
    private final ModeSetting handMode = new ModeSetting("Рука", "Основная", "Основная", "Вспомогательная")
            .visible(() -> useFirework.is("По таймеру"));
    private final BooleanSetting blockOnUse = new BooleanSetting("Блок при использовании", true)
            .visible(() -> useFirework.is("По таймеру"));

    private final BooleanSetting target = new BooleanSetting("Target", false);
    private final BooleanSetting visualReverse = new BooleanSetting("Visual Reverse", false);
    private final FloatSetting distance = new FloatSetting("Predict Distance", 2.7f, 1.0f, 5.0f, 0.1f);
    private static boolean predictCondition = false;
    private static int movementTicks = 0;
    private static boolean prePredictCondition = false;
    private static final TimerUtils tickStopWatch = new TimerUtils();

    private final TimerUtils fireworkTimer = new TimerUtils();
    private Vec3d chasePos = Vec3d.ZERO;
    private boolean chaseBlockPos = false;
    private double predictedDistance = 0.0D;
    private boolean visualReverseWasActive = false;
    private final BooleanSetting renderPredictCube = new BooleanSetting("Render Predict Cube", true);
    private final BooleanSetting predictFromTheme = new BooleanSetting("Theme Color", true);
    private final FloatSetting predictFillAlpha = new FloatSetting("Fill Alpha", 40.0f, 0.0f, 255.0f, 1.0f);
    private final ModeSetting predictBoxMode = new ModeSetting("Box Mode", "Normal", "Normal", "Diagonal");

    public ElytraTarget() {
        super("Elytra Target", "Элитра перегон", ModuleCategory.COMBAT);
        addSettings(predictMode, target, visualReverse, distance, elytraDistance, forward, useFirework, fireworkTiming, fireworkMode, handMode, blockOnUse,
                renderPredictCube, predictFromTheme, predictFillAlpha, predictBoxMode);
    }

    public boolean isPredictionActive() {
        return mc.player != null && isEnable() && mc.player.isGliding();
    }

    public boolean isAuraActive() {
        return mc.player != null && isEnable() && mc.player.isGliding();
    }

    public int getForwardTicks() {
        return Math.max(0, Math.round(forward.getValue().floatValue()));
    }
    @Native
    public boolean shouldSyncTargetFlight(LivingEntity target) {
        if (!isAuraActive() || target == null || !target.isAlive() || !target.isGliding()) {
            return false;
        }

        Vec3d playerEye = mc.player.getEyePos();
        Vec3d targetEye = target.getEyePos();
        double horizontalGap = Math.hypot(targetEye.x - playerEye.x, targetEye.z - playerEye.z);
        double verticalGap = targetEye.y - playerEye.y;
        double maxSyncRange = MathHelper.clamp(forward.getValue().doubleValue() * 1.8D, 4.0D, 7.5D);

        if (horizontalGap > maxSyncRange) {
            return false;
        }

        if (verticalGap > 2.25D && horizontalGap > 2.0D) {
            return false;
        }

        return Math.abs(verticalGap) <= 4.5D;
    }
    @Native
    public Vec3d getPredictedPoint(LivingEntity target, Vec3d point) {
        if (target == null) {
            return point;
        }

        Vec3d predictedCenter;

        if (shouldSyncTargetFlight(target)) {
            Vec3d vel = target.getVelocity();
            int ticks = getForwardTicks();
            predictedCenter = target.getBoundingBox().getCenter().add(vel.multiply(ticks));
        } else if (hasChasePosition()) {
            Vec3d vel = target.getVelocity();
            int ticks = getForwardTicks();
            Vec3d rawPredicted = target.getBoundingBox().getCenter().add(vel.multiply(ticks));

            predictedCenter = clampToExpandedBox(target, rawPredicted, 0.6D);
        } else {
            return PredictUtils.predict(target, point, getForwardTicks());
        }

        return predictedCenter;
    }
    @Native
    private Vec3d clampToExpandedBox(LivingEntity target, Vec3d pos, double margin) {
        Box box = target.getBoundingBox().expand(margin);
        double x = MathHelper.clamp(pos.x, box.minX, box.maxX);
        double z = MathHelper.clamp(pos.z, box.minZ, box.maxZ);
        double boxHeight = box.maxY - box.minY;
        double critY = box.minY + boxHeight * 0.75D;
        double y = MathHelper.clamp(pos.y, critY - 0.15D, box.maxY - 0.05D);
        return new Vec3d(x, y, z);
    }
    @Native
    public Vec3d getPredictedCenter(LivingEntity target) {
        if (target == null) {
            return null;
        }

        Vec3d base = getPredictedPoint(target, target.getBoundingBox().getCenter());

        if (mc.player != null && base != null) {
            Vec3d playerEye = mc.player.getEyePos();
            double targetEyeY = target.getEyePos().y;
            if (playerEye.y > targetEyeY - 0.3D) {
                double critOffset = MathHelper.clamp(playerEye.y - targetEyeY, 0.0D, 1.2D) * 0.35D;
                base = new Vec3d(base.x, targetEyeY - 0.25D - critOffset, base.z);
            }
        }

        return base;
    }
    @Native
    public Vec3d getAimPoint(LivingEntity target) {
        if (mc.player == null || target == null) {
            return null;
        }

        if (!hasChasePosition()) {
            return getFollowAnchor(target);
        }

        Vec3d playerEye = mc.player.getEyePos();
        Vec3d targetEye = target.getEyePos();
        Vec3d aimPoint = chasePos;
        Vec3d forwardVec = getChaseForward(target, playerEye, targetEye);

        if (forwardVec.lengthSquared() > 1.0E-4D) {
            aimPoint = aimPoint.add(forwardVec.normalize().multiply(2.0D));
        }

        if (playerEye.y > targetEye.y + 0.35D) {
            aimPoint = new Vec3d(aimPoint.x, Math.min(aimPoint.y, targetEye.y + 0.25D), aimPoint.z);
        }

        if (shouldAimAtTargetXZ(playerEye, targetEye, forwardVec)) {
            double aimY = MathHelper.clamp(aimPoint.y, targetEye.y - 0.35D, targetEye.y + 0.25D);
            aimPoint = new Vec3d(targetEye.x, aimY, targetEye.z);
        }

        if (!hasActiveFireworkBoost() && aimPoint.y < playerEye.y && mc.player.getVelocity().y < 0.03D) {
            double maxDrop = MathHelper.clamp(
                    0.45D + Math.hypot(aimPoint.x - playerEye.x, aimPoint.z - playerEye.z) * 0.08D,
                    0.45D,
                    0.95D
            );
            double heldY = playerEye.y - maxDrop;
            if (aimPoint.y < heldY) {
                aimPoint = new Vec3d(aimPoint.x, MathHelper.lerp(0.2D, aimPoint.y, heldY), aimPoint.z);
            }
        }

        return aimPoint;
    }

    public Vec3d getAimVector(LivingEntity target) {
        if (mc.player == null) {
            return Vec3d.ZERO;
        }

        Vec3d aimPoint = getAimPoint(target);
        if (aimPoint == null) {
            return Vec3d.ZERO;
        }

        return aimPoint.subtract(mc.player.getEyePos());
    }

    public boolean shouldTarget(LivingEntity livingEntity) {
        if (!isEnable() || livingEntity == null || mc.player == null) {
            return false;
        }

        if (!target.isState() || !mc.player.isGliding()) {
            return false;
        }

        return livingEntity.isGliding();
    }

    public boolean isReverseActive() {
        if (!isEnable() || !target.isState() || !visualReverse.isState() || mc.player == null) {
            visualReverseWasActive = false;
            return false;
        }

        Aura aura = ModuleClass.aura;
        LivingEntity auraTarget = aura != null && aura.isEnable() ? aura.getTarget() : null;
        if (auraTarget == null || !shouldTarget(auraTarget)) {
            visualReverseWasActive = false;
            return false;
        }

        float distanceToTarget = mc.player.distanceTo(auraTarget);
        float enableDistance = 2.5F;
        float predictDistance = Math.max(enableDistance, distance.getValue().floatValue());
        float disableDistance = predictDistance + 3.0F;

        if (!visualReverseWasActive && distanceToTarget <= enableDistance) {
            visualReverseWasActive = true;
        }

        if (visualReverseWasActive && distanceToTarget >= disableDistance) {
            visualReverseWasActive = false;
        }

        return visualReverseWasActive;
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        if (mc.player == null || mc.world == null || !renderPredictCube.isState()) {
            return;
        }

        Aura aura = ModuleClass.aura;
        LivingEntity auraTarget = aura != null && aura.isEnable() ? aura.getTarget() : null;
        if (auraTarget == null || !auraTarget.isGliding() || !this.target.isState() || !shouldTarget(auraTarget)) {
            return;
        }

        Vec3d predictedCenter = getPredictedCenter(auraTarget);
        if (predictedCenter == null) {
            return;
        }

        Vec3d cam = event.getCamera().getPos();
        Box box = new Box(
                predictedCenter.x - 0.35D - cam.x,
                predictedCenter.y - 0.35D - cam.y,
                predictedCenter.z - 0.35D - cam.z,
                predictedCenter.x + 0.35D - cam.x,
                predictedCenter.y + 0.35D - cam.y,
                predictedCenter.z + 0.35D - cam.z
        );

        int baseColor = predictFromTheme.isState() ? ColorUtils.getThemeColor() : ColorUtils.rgb(255, 255, 255);
        int fillColor = ColorUtils.setAlphaColor(baseColor, (int) predictFillAlpha.getValue().floatValue());
        int lineColor = ColorUtils.setAlphaColor(baseColor, 255);

        renderPredictCube(event, box, fillColor, lineColor, predictBoxMode.is("Diagonal"));
    }

    private void renderPredictCube(Event3DRender event, Box box, int fillColor, int lineColor) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5f);

        Tessellator tessellator = Tessellator.getInstance();
        if (((fillColor >>> 24) & 0xFF) > 0) {
            drawFilledBox(tessellator, event, box, fillColor);
        }

        drawBoxOutline(tessellator, event, box, lineColor);
        if (predictBoxMode.is("Diagonal")) {
            drawBodyDiagonals(tessellator, event, box, lineColor);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawFilledBox(Tessellator tessellator, Event3DRender event, Box box, int color) {
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);

        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);

        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);

        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);

        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);

        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawBoxOutline(Tessellator tessellator, Event3DRender event, Box box, int color) {
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);

        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);

        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawBodyDiagonals(Tessellator tessellator, Event3DRender event, Box box, int color) {
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    public static boolean isPredictCondition() {
        return predictCondition;
    }

    public static void resetPredictState() {
        predictCondition = false;
        movementTicks = 0;
        prePredictCondition = false;
    }

    public void resetChase() {
        chaseBlockPos = false;
        chasePos = Vec3d.ZERO;
        predictedDistance = 0.0D;
    }
    @Native
    public void updateChase(LivingEntity target, boolean shouldPredict) {
        resetChase();

        if (!isAuraActive() || target == null) {
            return;
        }

        chaseBlockPos = shouldPredict && target.isGliding();
        if (!chaseBlockPos) {
            return;
        }

        double power = forward.getValue().doubleValue();
        double distOffset = power < 4.0D ? 0.0D : power - 3.0D;
        Vec3d playerEye = mc.player.getEyePos();
        Vec3d targetEye = target.getEyePos();
        Vec3d forwardVec = getChaseForward(target, playerEye, targetEye);

        if (forwardVec.lengthSquared() <= 1.0E-4D) {
            chaseBlockPos = false;
            return;
        }

        chasePos = targetEye.add(forwardVec.normalize().multiply(power));
        Vec3d checkPos = targetEye.add(forwardVec.normalize().multiply(distOffset));
        predictedDistance = checkPos.distanceTo(playerEye);
    }

    public boolean hasChasePosition() {
        return chaseBlockPos && chasePos != null && chasePos.lengthSquared() > 1.0E-4D;
    }

    public double getPredictedDistance() {
        return predictedDistance;
    }
    @Native
    public Vec3d getFollowAnchor(LivingEntity target) {
        if (target == null) {
            return Vec3d.ZERO;
        }

        if (hasChasePosition()) {
            return chasePos;
        }

        Vec3d targetEye = target.getEyePos();
        Vec3d guide = resolveTargetForward(target);
        if (guide.lengthSquared() < 1.0E-4D) {
            return targetEye;
        }

        double followDistance = MathHelper.clamp(forward.getValue().doubleValue() * 0.75D, 1.5D, 4.0D);
        return targetEye.add(guide.normalize().multiply(followDistance));
    }
    @Native
    public void syncTargetFlightSpeed(LivingEntity target) {
        if (!shouldSyncTargetFlight(target) || !hasActiveFireworkBoost()) {
            return;
        }

        Vec3d anchor = getFollowAnchor(target);
        if (anchor == null || anchor.lengthSquared() < 1.0E-4D) {
            return;
        }

        Vec3d playerEye = mc.player.getEyePos();
        Vec3d playerMotion = mc.player.getVelocity();
        Vec3d targetMotion = target.getVelocity();
        Vec3d toAnchor = anchor.subtract(playerEye);
        Vec3d horizontalDirection = new Vec3d(toAnchor.x, 0.0D, toAnchor.z);
        double verticalOffset = toAnchor.y;
        double horizontalGap = Math.hypot(toAnchor.x, toAnchor.z);

        if (horizontalGap > MathHelper.clamp(forward.getValue().doubleValue() * 1.8D, 4.0D, 7.0D)) {
            return;
        }

        if (verticalOffset > 2.0D && horizontalGap > 2.0D) {
            return;
        }

        if (Math.abs(verticalOffset) > 4.5D) {
            return;
        }

        if (horizontalDirection.lengthSquared() < 1.0E-4D) {
            horizontalDirection = new Vec3d(targetMotion.x, 0.0D, targetMotion.z);
        }

        if (horizontalDirection.lengthSquared() < 1.0E-4D) {
            Vec3d forwardVec = resolveTargetForward(target);
            horizontalDirection = new Vec3d(forwardVec.x, 0.0D, forwardVec.z);
        }

        if (horizontalDirection.lengthSquared() < 1.0E-4D) {
            return;
        }

        horizontalDirection = horizontalDirection.normalize();

        double targetHorizontalSpeed = Math.hypot(targetMotion.x, targetMotion.z);
        double playerHorizontalSpeed = Math.hypot(playerMotion.x, playerMotion.z);

        ElytraBoost elytraBoost = ElytraBoost.INSTANCE;
        boolean boosterActive = elytraBoost != null && elytraBoost.isEnable();

        double catchUpBoost = MathHelper.clamp((horizontalGap - 1.8D) * 0.04D, 0.0D, boosterActive ? 0.28D : 0.18D);
        double desiredHorizontalSpeed = Math.max(targetHorizontalSpeed, 0.05D) + catchUpBoost;

        if (boosterActive && playerHorizontalSpeed > desiredHorizontalSpeed && horizontalGap > 1.5D) {
            desiredHorizontalSpeed += (playerHorizontalSpeed - desiredHorizontalSpeed) * 0.2D;
        }

        double desiredY = targetMotion.y + MathHelper.clamp(verticalOffset * 0.045D, -0.12D, 0.12D);
        double blend = boosterActive ? 0.12D : 0.18D;
        double targetX = horizontalDirection.x * desiredHorizontalSpeed;
        double targetZ = horizontalDirection.z * desiredHorizontalSpeed;

        mc.player.setVelocity(
                playerMotion.x + (targetX - playerMotion.x) * blend,
                playerMotion.y + (desiredY - playerMotion.y) * blend,
                playerMotion.z + (targetZ - playerMotion.z) * blend
        );
    }
    @Native
    @EventLink
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            resetPredictState();
            resetChase();
            return;
        }

        Aura aura = ModuleClass.aura;
        LivingEntity target = aura != null && aura.isEnable() ? aura.getTarget() : null;

        if (target != null) {
            PredictUtils.updateEntity(target);
        }

        smartPredict(target);
        updateChase(target, predictCondition);
        syncTargetFlightSpeed(target);
        updateFireworks(target);
    }

    private void updateFireworks(LivingEntity target) {
        if (!useFirework.is("По таймеру") || !isAuraActive()) {
            return;
        }

        if (blockOnUse.isState() && mc.player.isUsingItem()) {
            return;
        }

        if (mc.player.getItemCooldownManager().isCoolingDown(Items.FIREWORK_ROCKET.getDefaultStack())) {
            return;
        }

        boolean useFireworkNow = switch (fireworkMode.getCurrent()) {
            case "Только с целью" -> target != null;
            case "Без цели" -> target == null;
            default -> true;
        };

        if (!useFireworkNow) {
            return;
        }

        long delay = (long) (MathHelper.clamp(fireworkTiming.getValue().floatValue(), 0.1f, 5.0f) * 1000L);
        if (!fireworkTimer.finished(delay)) {
            return;
        }

        if (handMode.is("Основная")) {
            InventoryUtils.inventorySwapClick(Items.FIREWORK_ROCKET, true);
        } else {
            InventoryUtils.swapDef(Items.FIREWORK_ROCKET);
        }
        fireworkTimer.reset();
    }
    @Native
    public static void smartPredict(LivingEntity target) {
        if (!tickStopWatch.finished(50L)) {
            return;
        }

        if (target != null) {
            double dx = target.getX() - target.prevX;
            double dy = target.getY() - target.prevY;
            double dz = target.getZ() - target.prevZ;
            float speed = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0f;

            if (speed > 20.0f) {
                movementTicks = 0;
                prePredictCondition = false;
                predictCondition = true;
            } else {
                movementTicks++;
                if (predictCondition) {
                    prePredictCondition = true;
                }

                if (movementTicks >= 3) {
                    predictCondition = false;
                    movementTicks = 0;
                    prePredictCondition = false;
                } else {
                    predictCondition = prePredictCondition;
                }
            }
        }

        tickStopWatch.reset();
    }
    @Native
    private boolean shouldAimAtTargetXZ(Vec3d playerEye, Vec3d targetEye, Vec3d forwardVec) {
        double verticalGap = playerEye.y - targetEye.y;
        if (verticalGap < 3.0D) {
            return false;
        }

        Vec3d horizontalForward = new Vec3d(forwardVec.x, 0.0D, forwardVec.z);
        Vec3d targetToPlayer = new Vec3d(playerEye.x - targetEye.x, 0.0D, playerEye.z - targetEye.z);
        if (horizontalForward.lengthSquared() <= 1.0E-4D || targetToPlayer.lengthSquared() <= 1.0E-4D) {
            return false;
        }

        double behindDot = horizontalForward.normalize().dotProduct(targetToPlayer.normalize());
        return behindDot < -0.15D;
    }

    private Vec3d getChaseForward(LivingEntity target, Vec3d playerEye, Vec3d targetEye) {
        Vec3d forwardVec = resolveTargetForward(target);
        if (forwardVec.lengthSquared() <= 1.0E-4D) {
            return Vec3d.ZERO;
        }

        forwardVec = forwardVec.normalize();
        if (playerEye.y > targetEye.y + 0.35D && forwardVec.y > 0.0D) {
            Vec3d horizontalForward = new Vec3d(forwardVec.x, 0.0D, forwardVec.z);
            if (horizontalForward.lengthSquared() > 1.0E-4D) {
                return horizontalForward.normalize();
            }
        }

        return forwardVec;
    }

    private Vec3d resolveTargetForward(LivingEntity target) {
        PredictUtils.PositionData data = PredictUtils.getData(target);
        if (data != null) {
            Vec3d resolvedForward = data.getResolvedForward();
            if (resolvedForward.lengthSquared() > 1.0E-4D) {
                return resolvedForward;
            }
        }

        Vec3d motion = target.getVelocity();
        Vec3d horizontalMotion = new Vec3d(motion.x, 0.0D, motion.z);
        if (horizontalMotion.lengthSquared() > 1.0E-4D) {
            return motion;
        }

        return target.getRotationVector();
    }

    private boolean hasActiveFireworkBoost() {
        if (mc.world == null || mc.player == null) {
            return false;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof FireworkRocketEntity rocket) || !rocket.isAlive()) {
                continue;
            }

            LivingEntity shooter = ((FireworkRocketEntityAccessor) rocket).elysium$getShooter();
            if (shooter == mc.player) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onDisable() {
        resetPredictState();
        resetChase();
        super.onDisable();
    }
}
