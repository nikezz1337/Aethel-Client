package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.ProjectionUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "Predictions", category = Category.RENDER)
public class PredictionsModule extends Module {
    @Getter private static final PredictionsModule instance = new PredictionsModule();

    private static final int MAX_SIM_TICKS = 100;
    private static final Identifier IMPACT_TEXTURE = Identifier.of("ethereal", "images/textures/target.png");
    private static final float IMPACT_MARKER_SIZE = 0.95f;
    private static final float IMPACT_GLOW_SCALE = 1.35f;
    private static final double IMPACT_BLOCK_OFFSET = 0.003;
    private static final double IMPACT_ENTITY_OFFSET = 0.02;
    private static final float IMPACT_FADE_IN_PER_SEC = 9.4f;
    private static final float IMPACT_FADE_OUT_PER_SEC = 10.5f;
    private static final float IMPACT_MOVE_LERP_PER_SEC = 23.0f;
    private static final float IMPACT_NORMAL_LERP_PER_SEC = 25.0f;
    private static final long IMPACT_ANIM_MAX_STEP_MS = 75L;
    private static final int MAX_PREVIEW_MARKERS = 3;
    private static final float TAG_FADE_IN_PER_SEC = 6.2f;
    private static final float TAG_FADE_OUT_PER_SEC = 4.0f;
    private static final long TAG_FADE_MAX_STEP_MS = 75L;
    private static final Color PANEL_BG = new Color(11, 11, 22, 170);
    private static final Color OWNER_TEXT_COLOR = new Color(240, 244, 255, 255);
    private static final Color TIMER_TEXT_COLOR = new Color(250, 252, 255, 245);
    private static final Color FALLBACK_HEAD_BG = new Color(30, 32, 45, 255);
    private static final Color FALLBACK_HEAD_TEXT = new Color(190, 200, 225, 230);
    private static final String UNKNOWN_OWNER = "Unknown";
    private static final Identifier TRAIL_TEXTURE = Identifier.of("ethereal", "images/particles/firefly.png");
    private static final int MAX_TRAIL_PARTICLES = 420;
    private static final long TRAIL_LIFETIME_MS = 760L;
    private static final long TRAIL_SPAWN_DELAY_MS = 18L;
    private static final long TRAIL_FORGET_MS = 1500L;
    private static final float TRAIL_SIZE_MULTIPLIER = 1.5f;

    private static final float OWNER_FONT_SIZE = 7.3f;
    private static final float TIMER_FONT_SIZE = 6.8f;

    private final BooleanSetting showOwnerPanel = new BooleanSetting("Отображать владельца").value(true);
    private final BooleanSetting showCurrentImpact = new BooleanSetting("Предикт попаданий").value(true);

    private final List<OwnerTagData> ownerTags = new ArrayList<>(32);
    private final Map<Integer, OwnerTagState> ownerTagStates = new HashMap<>(32);
    private final List<ImpactMarkerState> impactMarkerStates = new ArrayList<>(MAX_PREVIEW_MARKERS);
    private final List<TrailParticle> trailParticles = new ArrayList<>(MAX_TRAIL_PARTICLES);
    private final Map<Integer, Long> trailLastSpawn = new HashMap<>(32);
    private final Map<Integer, Long> trailSeen = new HashMap<>(32);

    public PredictionsModule() {
        addSettings(showOwnerPanel, showCurrentImpact);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clearAll();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearAll();
    }

    private void clearAll() {
        ownerTags.clear();
        ownerTagStates.clear();
        impactMarkerStates.clear();
        trailParticles.clear();
        trailLastSpawn.clear();
        trailSeen.clear();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) {
            clearAll();
            return;
        }

        ownerTags.clear();
        long now = System.currentTimeMillis();
        float tickDelta = event.partialTicks();
        boolean renderOwnerPanel = showOwnerPanel.getValue();
        boolean renderCurrentImpact = showCurrentImpact.getValue();

        for (OwnerTagState state : ownerTagStates.values()) {
            state.seen = false;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ProjectileEntity projectile)) continue;

            updateTrail(projectile, now, tickDelta);
            if (entity instanceof FireworkRocketEntity) continue;

            TrajectoryData trajectory = calcTrajectory(projectile);
            if (trajectory.path.size() < 2) continue;

            List<Vec3d> densePath = createDensePath(trajectory.path);
            renderBatchLine(event, densePath, 0.5f);

            if (renderOwnerPanel) {
                int projectileId = projectile.getId();
                OwnerTagState state = ownerTagStates.computeIfAbsent(projectileId, id -> new OwnerTagState());
                state.seen = true;
                state.impactPos = trajectory.impactPos;
                state.ticksToImpact = trajectory.ticksToImpact;
                state.owner = projectile.getOwner();
                state.ownerName = resolveOwnerName(state.owner);
                state.colorSeed = projectileId * 19;
            }
        }

        updateTrailParticles(now);
        cleanupTrailTrackers(now);
        renderTrailParticles(event, now);

        updateOwnerTagAnimations(renderOwnerPanel);

        if (renderCurrentImpact) {
            renderCurrentImpactMarker(event);
        } else {
            impactMarkerStates.clear();
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null || ownerTags.isEmpty()) return;

        for (OwnerTagData data : ownerTags) {
            renderOwnerTag(event, data);
        }
    }

    // ── Trail particles ──

    private void updateTrail(ProjectileEntity projectile, long now, float tickDelta) {
        if (!isFlyingProjectile(projectile)) return;

        int id = projectile.getId();
        trailSeen.put(id, now);

        long lastSpawn = trailLastSpawn.getOrDefault(id, 0L);
        if (now - lastSpawn < TRAIL_SPAWN_DELAY_MS) return;
        trailLastSpawn.put(id, now);

        Vec3d velocity = projectile.getVelocity();
        Vec3d direction = velocity.lengthSquared() > 1.0E-6 ? velocity.normalize() : Vec3d.ZERO;
        Vec3d basePos = projectile.getLerpedPos(tickDelta).subtract(direction.multiply(0.18));

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int burst = velocity.lengthSquared() > 0.35 ? 3 : 2;
        for (int i = 0; i < burst; i++) {
            double backOffset = random.nextDouble(0.02, 0.34);
            double spread = random.nextDouble(0.018, 0.075);
            Vec3d pos = basePos.subtract(direction.multiply(backOffset)).add(
                    random.nextDouble(-spread, spread),
                    random.nextDouble(-spread, spread),
                    random.nextDouble(-spread, spread)
            );

            Vec3d drift = direction.multiply(-random.nextDouble(0.008, 0.035)).add(
                    random.nextDouble(-0.010, 0.010),
                    random.nextDouble(-0.003, 0.016),
                    random.nextDouble(-0.010, 0.010)
            );

            trailParticles.add(new TrailParticle(
                    pos, drift, now,
                    TRAIL_LIFETIME_MS + random.nextLong(-120L, 180L),
                    0.12f + random.nextFloat() * 0.11f,
                    random.nextInt(220)
            ));
        }

        if (trailParticles.size() > MAX_TRAIL_PARTICLES) {
            int trim = trailParticles.size() - MAX_TRAIL_PARTICLES;
            trailParticles.subList(0, trim).clear();
        }
    }

    private boolean isFlyingProjectile(ProjectileEntity projectile) {
        return projectile.isAlive()
                && !projectile.isRemoved()
                && projectile.getVelocity().lengthSquared() > 0.0025
                && mc.player != null
                && projectile.squaredDistanceTo(mc.player) < 96.0 * 96.0;
    }

    private void updateTrailParticles(long now) {
        Iterator<TrailParticle> iterator = trailParticles.iterator();
        while (iterator.hasNext()) {
            TrailParticle particle = iterator.next();
            if (particle.isDead(now)) {
                iterator.remove();
                continue;
            }

            float frame = Math.min(2.5f, Math.max(0.25f, (now - particle.lastUpdateMs) / 16.666f));
            particle.lastUpdateMs = now;
            particle.pos = particle.pos.add(particle.velocity.multiply(frame));
            particle.velocity = particle.velocity.multiply(Math.pow(0.965, frame)).add(0.0, 0.0009 * frame, 0.0);
        }
    }

    private void cleanupTrailTrackers(long now) {
        trailSeen.entrySet().removeIf(entry -> now - entry.getValue() > TRAIL_FORGET_MS);
        trailLastSpawn.entrySet().removeIf(entry -> !trailSeen.containsKey(entry.getKey()));
    }

    private void renderTrailParticles(Render3DEvent event, long now) {
        if (trailParticles.isEmpty()) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        MatrixStack matrices = event.matrixStack();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, TRAIL_TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        int renderedQuads = 0;
        matrices.push();
        for (TrailParticle particle : trailParticles) {
            float life = particle.lifeProgress(now);
            float fade = 1.0f - life;
            if (fade <= 0.01f) continue;

            Vec3d pos = particle.pos;
            matrices.push();
            matrices.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
            matrices.multiply(camera.getRotation());

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float size = particle.size * TRAIL_SIZE_MULTIPLIER * (0.8f + fade * 0.55f);
            float half = size * 0.5f;
            Color color = UIColors.gradient(particle.paletteShift + (int) ((now - particle.spawnTimeMs) / 10L));
            int rgb = color.getRGB() & 0x00FFFFFF;
            int alpha = Math.max(0, Math.min(255, (int) (210f * fade * fade)));
            int rgba = (alpha << 24) | rgb;

            buffer.vertex(matrix, -half, -half, 0.0f).color(rgba).texture(0.0f, 1.0f);
            buffer.vertex(matrix, half, -half, 0.0f).color(rgba).texture(1.0f, 1.0f);
            buffer.vertex(matrix, half, half, 0.0f).color(rgba).texture(1.0f, 0.0f);
            buffer.vertex(matrix, -half, half, 0.0f).color(rgba).texture(0.0f, 0.0f);

            renderedQuads++;
            matrices.pop();
        }

        if (renderedQuads > 0) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // ── Trajectory line ──

    private void renderBatchLine(Render3DEvent event, List<Vec3d> points, float size) {
        if (points == null || points.isEmpty()) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        MatrixStack base = event.matrixStack();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, TRAIL_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        base.push();
        for (int i = 0; i < points.size(); i++) {
            Vec3d pos = points.get(i);
            float alpha = 1.0f - ((float) i / points.size());

            base.push();
            base.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
            base.multiply(camera.getRotation());

            Matrix4f matrix = base.peek().getPositionMatrix();
            Color color = UIColors.gradient(i * 5);
            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();
            int alphaed = ((int) (255 * alpha) << 24) | (r << 16) | (g << 8) | b;

            float hSize = size * 0.5f;
            buffer.vertex(matrix, -hSize, -hSize, 0).color(alphaed).texture(1, 1);
            buffer.vertex(matrix, hSize, -hSize, 0).color(alphaed).texture(0, 1);
            buffer.vertex(matrix, hSize, hSize, 0).color(alphaed).texture(0, 0);
            buffer.vertex(matrix, -hSize, hSize, 0).color(alphaed).texture(1, 0);

            base.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        base.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // ── Impact marker (preview) ──

    private void renderCurrentImpactMarker(Render3DEvent event) {
        List<ImpactRenderTarget> targets = new ArrayList<>(MAX_PREVIEW_MARKERS);
        for (ProjectileEntity previewProjectile : createPreviewProjectiles()) {
            TrajectoryData trajectory = calcTrajectory(previewProjectile);
            ImpactRenderTarget target = resolveImpactRenderTarget(trajectory);
            if (target != null) {
                targets.add(target);
            }
        }

        updateImpactMarkerAnimations(targets);

        for (ImpactMarkerState state : impactMarkerStates) {
            if (state.alpha <= 0.01f || state.center == null || state.normal == null) continue;
            renderImpactMarker(event.matrixStack(), state.center, state.normal, state.alpha);
        }
    }

    private List<ProjectileEntity> createPreviewProjectiles() {
        List<ProjectileEntity> result = new ArrayList<>(MAX_PREVIEW_MARKERS);
        PlayerEntity player = mc.player;
        if (player == null) return result;

        if (player.isUsingItem()) {
            createPreviewProjectilesForStack(player, player.getActiveItem(), true, result);
            if (!result.isEmpty()) return result;
        }

        createPreviewProjectilesForStack(player, player.getMainHandStack(), false, result);
        if (!result.isEmpty()) return result;

        createPreviewProjectilesForStack(player, player.getOffHandStack(), false, result);
        return result;
    }

    private void createPreviewProjectilesForStack(PlayerEntity player, ItemStack stack, boolean usingNow, List<ProjectileEntity> output) {
        if (stack.isEmpty() || output.size() >= MAX_PREVIEW_MARKERS) return;

        Item item = stack.getItem();
        if (item instanceof BowItem) {
            if (!usingNow) return;
            int useTicks = Math.max(1, player.getItemUseTime());
            float pull = BowItem.getPullProgress(useTicks);
            float speed = Math.max(0.2f, pull * 3.0f);

            ProjectileEntity bowProjectile = createBowProjectile(player, stack);
            if (bowProjectile == null) return;
            output.add(configurePreviewProjectile(player, bowProjectile, speed, 0.0f, 0.0f));
            return;
        }

        if (item instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged(stack) && !usingNow) return;

            float shotSpeed = getCrossbowShotSpeed(stack);
            List<ItemStack> projectiles = resolveCrossbowPreviewProjectiles(player, stack);
            int totalShots = Math.min(MAX_PREVIEW_MARKERS, Math.max(1, projectiles.size()));

            for (int i = 0; i < totalShots; i++) {
                ItemStack projectileStack = projectiles.get(Math.min(i, projectiles.size() - 1));
                ProjectileEntity crossbowProjectile = createCrossbowProjectileFromStack(player, stack, projectileStack);
                if (crossbowProjectile == null) continue;

                float yawOffset = resolveCrossbowYawOffset(i, totalShots);
                output.add(configurePreviewProjectile(player, crossbowProjectile, shotSpeed, 0.0f, yawOffset));
            }
            return;
        }

        if (item instanceof TridentItem) {
            if (!usingNow) return;
            ProjectileEntity tridentProjectile = new TridentEntity(mc.world, player, stack.copyWithCount(1));
            output.add(configurePreviewProjectile(player, tridentProjectile, TridentItem.THROW_SPEED, 0.0f, 0.0f));
            return;
        }

        if (item instanceof EnderPearlItem) {
            ProjectileEntity pearlProjectile = new EnderPearlEntity(mc.world, player, stack.copyWithCount(1));
            output.add(configurePreviewProjectile(player, pearlProjectile, EnderPearlItem.POWER, 0.0f, 0.0f));
            return;
        }

        if (item instanceof SnowballItem
                || item instanceof EggItem
                || item instanceof ThrowablePotionItem
                || item instanceof ExperienceBottleItem
                || item instanceof WindChargeItem) {
            ProjectileItem projectileItem = (ProjectileItem) item;
            ProjectileEntity projectile = projectileItem.createEntity(mc.world, player.getEyePos(), stack, player.getHorizontalFacing());
            if (projectile == null) return;

            float power = resolveProjectilePower(item, projectileItem);
            float pitchOffset = resolvePitchOffset(item);
            output.add(configurePreviewProjectile(player, projectile, power, pitchOffset, 0.0f));
        }
    }

    private ProjectileEntity createBowProjectile(PlayerEntity player, ItemStack bowStack) {
        ItemStack ammo = player.getProjectileType(bowStack);
        if (ammo.isEmpty()) {
            ammo = new ItemStack(Items.ARROW);
        }
        if (!(ammo.getItem() instanceof ArrowItem arrowItem)) return null;
        return arrowItem.createArrow(mc.world, ammo, player, bowStack);
    }

    private ProjectileEntity createCrossbowProjectileFromStack(PlayerEntity player, ItemStack crossbowStack, ItemStack projectileStack) {
        if (projectileStack.isOf(Items.FIREWORK_ROCKET)) {
            return new FireworkRocketEntity(mc.world, projectileStack, player,
                    player.getX(), player.getEyeY() - 0.15, player.getZ(), true);
        }
        if (projectileStack.getItem() instanceof ArrowItem arrowItem) {
            return arrowItem.createArrow(mc.world, projectileStack, player, crossbowStack);
        }
        if (projectileStack.getItem() instanceof ProjectileItem projectileItem) {
            return projectileItem.createEntity(mc.world, player.getEyePos(), projectileStack, player.getHorizontalFacing());
        }
        return null;
    }

    private List<ItemStack> resolveCrossbowPreviewProjectiles(PlayerEntity player, ItemStack crossbowStack) {
        ChargedProjectilesComponent charged = crossbowStack.get(DataComponentTypes.CHARGED_PROJECTILES);
        if (charged != null && !charged.isEmpty()) {
            List<ItemStack> loaded = charged.getProjectiles();
            if (!loaded.isEmpty()) return loaded;
        }

        ItemStack fallback = player.getProjectileType(crossbowStack);
        if (fallback.isEmpty()) fallback = new ItemStack(Items.ARROW);

        List<ItemStack> single = new ArrayList<>(1);
        single.add(fallback);
        return single;
    }

    private float resolveCrossbowYawOffset(int shotIndex, int totalShots) {
        if (totalShots == 3) {
            if (shotIndex == 0) return -10.0f;
            if (shotIndex == 2) return 10.0f;
            return 0.0f;
        }
        if (totalShots == 2) return shotIndex == 0 ? -5.0f : 5.0f;
        return 0.0f;
    }

    private ProjectileEntity configurePreviewProjectile(PlayerEntity player, ProjectileEntity projectile, float speed, float pitchOffset, float yawOffset) {
        projectile.setOwner(player);
        projectile.setPosition(player.getX(), player.getEyeY() - 0.1, player.getZ());
        projectile.setVelocity(player, player.getPitch() + pitchOffset, player.getYaw() + yawOffset, 0.0f, speed, 0.0f);
        return projectile;
    }

    private float getCrossbowShotSpeed(ItemStack crossbowStack) {
        ChargedProjectilesComponent charged = crossbowStack.get(DataComponentTypes.CHARGED_PROJECTILES);
        if (charged != null && charged.contains(Items.FIREWORK_ROCKET)) return 1.6f;
        return 3.15f;
    }

    private float resolveProjectilePower(Item item, ProjectileItem projectileItem) {
        if (item instanceof WindChargeItem) return WindChargeItem.POWER;
        if (item instanceof ThrowablePotionItem) return ThrowablePotionItem.POWER;
        if (item instanceof ExperienceBottleItem) return 0.7f;
        if (item instanceof SnowballItem) return SnowballItem.POWER;
        if (item instanceof EggItem) return EggItem.POWER;

        ProjectileItem.Settings settings = projectileItem.getProjectileSettings();
        return settings != null ? settings.power() : 1.5f;
    }

    private float resolvePitchOffset(Item item) {
        if (item instanceof ThrowablePotionItem || item instanceof ExperienceBottleItem) return -20.0f;
        return 0.0f;
    }

    private ImpactRenderTarget resolveImpactRenderTarget(TrajectoryData trajectory) {
        if (trajectory == null || trajectory.impactNormal == null) return null;

        Vec3d normal = trajectory.impactNormal;
        if (normal.lengthSquared() < 1.0E-6) return null;
        normal = normal.normalize();

        if (trajectory.entityHit != null) {
            Vec3d cameraToImpact = mc.gameRenderer.getCamera().getPos().subtract(trajectory.entityHit.getPos());
            if (cameraToImpact.lengthSquared() > 1.0E-6) {
                Vec3d cameraNormal = cameraToImpact.normalize();
                double dot = normal.dotProduct(cameraNormal);
                float blend = dot < 0.2 ? 0.58f : 0.28f;
                Vec3d correctedNormal = normal.lerp(cameraNormal, blend);
                if (correctedNormal.lengthSquared() > 1.0E-6) {
                    normal = correctedNormal.normalize();
                }
            }
            Vec3d center = trajectory.entityHit.getPos().add(normal.multiply(IMPACT_ENTITY_OFFSET));
            return new ImpactRenderTarget(center, normal);
        }
        if (trajectory.blockHit != null) {
            Vec3d center = trajectory.blockHit.getPos().add(normal.multiply(IMPACT_BLOCK_OFFSET));
            return new ImpactRenderTarget(center, normal);
        }
        return null;
    }

    private void updateImpactMarkerAnimations(List<ImpactRenderTarget> targets) {
        for (ImpactMarkerState s : impactMarkerStates) s.seen = false;

        int targetCount = Math.min(MAX_PREVIEW_MARKERS, targets.size());
        for (int i = 0; i < targetCount; i++) {
            ImpactRenderTarget target = targets.get(i);
            while (impactMarkerStates.size() <= i) impactMarkerStates.add(new ImpactMarkerState());

            ImpactMarkerState state = impactMarkerStates.get(i);
            state.seen = true;
            state.targetCenter = target.center;
            state.targetNormal = target.normal;
            if (state.center == null) state.center = target.center;
            if (state.normal == null) state.normal = target.normal;
        }

        long now = System.currentTimeMillis();
        for (Iterator<ImpactMarkerState> it = impactMarkerStates.iterator(); it.hasNext(); ) {
            ImpactMarkerState state = it.next();
            long elapsedMs = now - state.lastUpdateMs;
            if (elapsedMs <= 0L) elapsedMs = 16L;
            elapsedMs = Math.min(elapsedMs, IMPACT_ANIM_MAX_STEP_MS);
            state.lastUpdateMs = now;

            float deltaSeconds = elapsedMs / 1000f;
            float targetAlpha = state.seen ? 1f : 0f;
            float fadeSpeed = targetAlpha > state.alpha ? IMPACT_FADE_IN_PER_SEC : IMPACT_FADE_OUT_PER_SEC;
            state.alpha = approach(state.alpha, targetAlpha, fadeSpeed * deltaSeconds);

            if (state.center != null && state.targetCenter != null) {
                double gap = state.center.distanceTo(state.targetCenter);
                float moveSpeed = IMPACT_MOVE_LERP_PER_SEC;
                if (gap > 0.45) moveSpeed *= 1.35f;
                if (gap > 1.0) moveSpeed *= 1.55f;
                float moveFactor = Math.min(1f, moveSpeed * deltaSeconds);
                state.center = state.center.lerp(state.targetCenter, moveFactor);
            }
            if (state.normal != null && state.targetNormal != null) {
                float normalFactor = Math.min(1f, IMPACT_NORMAL_LERP_PER_SEC * deltaSeconds);
                Vec3d blendedNormal = state.normal.lerp(state.targetNormal, normalFactor);
                if (blendedNormal.lengthSquared() > 1.0E-6) {
                    state.normal = blendedNormal.normalize();
                }
            }

            if (!state.seen && state.alpha <= 0.01f) it.remove();
        }
    }

    private void renderImpactMarker(MatrixStack matrices, Vec3d center, Vec3d normal, float alpha) {
        if (center == null || normal == null || alpha <= 0.01f) return;

        Vec3d normalDir = normal.lengthSquared() > 1.0E-6 ? normal.normalize() : new Vec3d(0.0, 1.0, 0.0);
        Vec3d axisBase = Math.abs(normalDir.y) > 0.92 ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 1.0, 0.0);
        Vec3d axisU = normalDir.crossProduct(axisBase);
        if (axisU.lengthSquared() < 1.0E-6) {
            axisU = new Vec3d(0.0, 0.0, 1.0);
        } else {
            axisU = axisU.normalize();
        }
        Vec3d axisV = axisU.crossProduct(normalDir).normalize();

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vec3d centerRelative = center.subtract(cameraPos);
        double distance = cameraPos.distanceTo(center);
        float distanceBoost = (float) Math.min(1.0, distance / 96.0);

        Color themeColor = UIColors.primary();
        Color baseColor = alphaMul(ColorUtil.setAlpha(themeColor, 245), alpha);
        Color glowColor = alphaMul(ColorUtil.setAlpha(themeColor, (int) (130 + 90 * distanceBoost)), alpha);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, IMPACT_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        drawImpactQuad(matrix, centerRelative, axisU, axisV, IMPACT_MARKER_SIZE * IMPACT_GLOW_SCALE, glowColor.getRGB());

        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        drawImpactQuad(matrix, centerRelative, axisU, axisV, IMPACT_MARKER_SIZE, baseColor.getRGB());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawImpactQuad(Matrix4f matrix, Vec3d center, Vec3d axisU, Vec3d axisV, float size, int rgba) {
        double half = size * 0.5;
        Vec3d bottomLeft = center.subtract(axisU.multiply(half)).subtract(axisV.multiply(half));
        Vec3d bottomRight = center.add(axisU.multiply(half)).subtract(axisV.multiply(half));
        Vec3d topRight = center.add(axisU.multiply(half)).add(axisV.multiply(half));
        Vec3d topLeft = center.subtract(axisU.multiply(half)).add(axisV.multiply(half));

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, (float) bottomLeft.x, (float) bottomLeft.y, (float) bottomLeft.z).texture(0.0f, 1.0f).color(rgba);
        buffer.vertex(matrix, (float) bottomRight.x, (float) bottomRight.y, (float) bottomRight.z).texture(1.0f, 1.0f).color(rgba);
        buffer.vertex(matrix, (float) topRight.x, (float) topRight.y, (float) topRight.z).texture(1.0f, 0.0f).color(rgba);
        buffer.vertex(matrix, (float) topLeft.x, (float) topLeft.y, (float) topLeft.z).texture(0.0f, 0.0f).color(rgba);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    // ── Owner tag animations ──

    private void updateOwnerTagAnimations(boolean renderOwnerPanel) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, OwnerTagState>> iterator = ownerTagStates.entrySet().iterator();
        while (iterator.hasNext()) {
            OwnerTagState state = iterator.next().getValue();
            float targetAlpha = renderOwnerPanel && state.seen ? 1f : 0f;
            long elapsedMs = now - state.lastUpdateMs;
            if (elapsedMs <= 0L) elapsedMs = 16L;
            elapsedMs = Math.min(elapsedMs, TAG_FADE_MAX_STEP_MS);
            state.lastUpdateMs = now;

            float deltaSeconds = elapsedMs / 1000f;
            float speedPerSec = targetAlpha > state.alpha ? TAG_FADE_IN_PER_SEC : TAG_FADE_OUT_PER_SEC;
            state.alpha = approach(state.alpha, targetAlpha, speedPerSec * deltaSeconds);

            if (state.alpha <= 0.01f && targetAlpha == 0f) {
                iterator.remove();
                continue;
            }

            if (state.alpha > 0.01f && state.impactPos != null) {
                ownerTags.add(new OwnerTagData(
                        state.impactPos, state.ticksToImpact,
                        state.owner, state.ownerName,
                        state.colorSeed, state.alpha
                ));
            }
        }
    }

    // ── Owner tag 2D rendering ──

    private void renderOwnerTag(Render2DEvent event, OwnerTagData data) {
        float fade = data.alpha;
        if (fade <= 0.01f) return;

        Vector2f screenPos = ProjectionUtil.project(data.impactPos.add(0.0, 0.25, 0.0));
        if (screenPos.x == Float.MAX_VALUE && screenPos.y == Float.MAX_VALUE) return;

        String ownerName = shortenName(data.ownerName, 16);
        String etaText = formatDuration(data.ticksToImpact);

        float panelHeight = 15f;
        float panelRadius = 4f;
        float panelPad = 3f;
        float contentGap = 3f;
        float headSize = 10.5f;
        float timerPad = 2f;

        float ownerWidth = Fonts.PS_MEDIUM.getWidth(ownerName, OWNER_FONT_SIZE);
        float etaWidth = Fonts.PS_REGULAR.getWidth(etaText, TIMER_FONT_SIZE);
        float timerWidth = etaWidth + timerPad * 2f;
        float panelWidth = panelPad + headSize + contentGap + ownerWidth + contentGap + timerWidth + panelPad;
        panelWidth = Math.max(panelWidth, 68f);

        float x = screenPos.x - panelWidth * 0.5f;
        float y = screenPos.y - 17f;

        Color accent = UIColors.primary();
        Color timerBg = alphaMul(ColorUtil.setAlpha(accent, 95), fade);
        Color timerTextColor = alphaMul(TIMER_TEXT_COLOR, fade);

        MatrixStack matrices = event.matrixStack();

        Color panelBg = new Color(13, 11, 22, (int) (170 * fade));
        RenderUtil.BLUR_RECT.draw(matrices, x, y, panelWidth, panelHeight, panelRadius, panelBg);

        float headX = x + panelPad;
        float headY = y + (panelHeight - headSize) * 0.5f;
        renderOwnerHead(matrices, headX, headY, headSize, data.owner, fade);

        float ownerX = headX + headSize + contentGap;
        float ownerY = y + (panelHeight - Fonts.PS_MEDIUM.getHeight(OWNER_FONT_SIZE)) * 0.5f - 0.3f;
        Fonts.PS_MEDIUM.drawText(matrices, ownerName, ownerX, ownerY, OWNER_FONT_SIZE, alphaMul(OWNER_TEXT_COLOR, fade));

        float timerX = x + panelWidth - panelPad - timerWidth;
        float timerY = y + (panelHeight - 10f) * 0.5f;
        RenderUtil.RECT.draw(matrices, timerX, timerY, timerWidth, 10f, 2f, timerBg);
        float etaY = y + (panelHeight - Fonts.PS_REGULAR.getHeight(TIMER_FONT_SIZE)) * 0.5f;
        Fonts.PS_REGULAR.drawText(matrices, etaText, timerX + (timerWidth - etaWidth) * 0.5f, etaY, TIMER_FONT_SIZE, timerTextColor);
    }

    private void renderOwnerHead(MatrixStack matrices, float x, float y, float size, Entity owner, float fade) {
        if (owner instanceof AbstractClientPlayerEntity player) {
            RenderUtil.TEXTURE_RECT.drawRoundedHead(matrices, player, x, y, size, size, 2.2f, alphaMul(Color.WHITE, fade));
            return;
        }

        RenderUtil.RECT.draw(matrices, x, y, size, size, 2.2f, alphaMul(FALLBACK_HEAD_BG, fade));
        String fallback = "?";
        float textX = x + (size - Fonts.PS_REGULAR.getWidth(fallback, TIMER_FONT_SIZE)) * 0.5f;
        float textY = y + (size - Fonts.PS_REGULAR.getHeight(TIMER_FONT_SIZE)) * 0.5f;
        Fonts.PS_REGULAR.drawText(matrices, fallback, textX, textY, TIMER_FONT_SIZE, alphaMul(FALLBACK_HEAD_TEXT, fade));
    }

    // ── Trajectory calculation ──

    private TrajectoryData calcTrajectory(ProjectileEntity entity) {
        List<Vec3d> path = new ArrayList<>();
        Vec3d vel = entity.getVelocity();
        Vec3d pos = entity.getPos();
        double gravity = entity.getFinalGravity();
        double drag = entity.isTouchingWater() ? 0.8 : 0.99;

        if (vel.lengthSquared() < 0.01) {
            return new TrajectoryData(path, pos, 0, null, null, null);
        }

        Vec3d impactPos = pos;
        int ticksToImpact = MAX_SIM_TICKS;
        BlockHitResult impactHit = null;
        EntityHitResult impactEntityHit = null;
        Vec3d impactNormal = null;

        for (int i = 0; i < MAX_SIM_TICKS; i++) {
            path.add(pos);
            Vec3d next = pos.add(vel);

            EntityHitResult entityHit = raycastEntity(entity, pos, next);
            double entityDistanceSq = entityHit != null ? pos.squaredDistanceTo(entityHit.getPos()) : Double.POSITIVE_INFINITY;

            BlockHitResult blockHit = mc.world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
            double blockDistanceSq = blockHit.getType() != HitResult.Type.MISS ? pos.squaredDistanceTo(blockHit.getPos()) : Double.POSITIVE_INFINITY;

            if (entityHit != null && (blockHit.getType() == HitResult.Type.MISS || entityDistanceSq <= blockDistanceSq + 1.0E-4)) {
                impactPos = entityHit.getPos();
                impactEntityHit = entityHit;
                impactNormal = resolveEntityImpactNormal(entityHit.getEntity(), impactPos, vel);
                path.add(impactPos);
                ticksToImpact = i + 1;
                break;
            }

            if (blockHit.getType() != HitResult.Type.MISS) {
                impactPos = blockHit.getPos();
                impactHit = blockHit;
                impactNormal = Vec3d.of(blockHit.getSide().getVector());
                path.add(impactPos);
                ticksToImpact = i + 1;
                break;
            }
            pos = next;
            impactPos = pos;
            vel = vel.multiply(drag).subtract(0, gravity, 0);
        }

        if (path.isEmpty() && impactPos != null) path.add(impactPos);

        return new TrajectoryData(path, impactPos, ticksToImpact, impactHit, impactEntityHit, impactNormal);
    }

    private EntityHitResult raycastEntity(ProjectileEntity projectile, Vec3d start, Vec3d end) {
        Entity owner = projectile.getOwner();
        Vec3d movement = end.subtract(start);
        Box searchBox = projectile.getBoundingBox().stretch(movement).expand(1.25);

        Entity closestEntity = null;
        Vec3d closestPos = null;
        double closestDistanceSq = Double.POSITIVE_INFINITY;

        for (Entity candidate : mc.world.getOtherEntities(projectile, searchBox, entity ->
                entity.isAlive() && !entity.isSpectator() && entity != owner && entity != projectile)) {
            Box candidateBox = candidate.getBoundingBox().expand(0.45);
            Optional<Vec3d> hit = candidateBox.raycast(start, end);
            if (hit.isEmpty()) continue;

            Vec3d hitPos = hit.get();
            double distanceSq = start.squaredDistanceTo(hitPos);
            if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closestEntity = candidate;
                closestPos = hitPos;
            }
        }

        if (closestEntity == null || closestPos == null) return null;
        return new EntityHitResult(closestEntity, closestPos);
    }

    private Vec3d resolveEntityImpactNormal(Entity entity, Vec3d hitPos, Vec3d velocity) {
        Box box = entity.getBoundingBox();
        double epsilon = 0.03;

        if (Math.abs(hitPos.x - box.minX) <= epsilon) return new Vec3d(-1.0, 0.0, 0.0);
        if (Math.abs(hitPos.x - box.maxX) <= epsilon) return new Vec3d(1.0, 0.0, 0.0);
        if (Math.abs(hitPos.y - box.minY) <= epsilon) return new Vec3d(0.0, -1.0, 0.0);
        if (Math.abs(hitPos.y - box.maxY) <= epsilon) return new Vec3d(0.0, 1.0, 0.0);
        if (Math.abs(hitPos.z - box.minZ) <= epsilon) return new Vec3d(0.0, 0.0, -1.0);
        if (Math.abs(hitPos.z - box.maxZ) <= epsilon) return new Vec3d(0.0, 0.0, 1.0);

        if (velocity.lengthSquared() > 1.0E-6) return velocity.normalize().multiply(-1.0);
        return new Vec3d(0.0, 1.0, 0.0);
    }

    // ── Helpers ──

    private List<Vec3d> createDensePath(List<Vec3d> rawPoints) {
        List<Vec3d> densePath = new ArrayList<>(rawPoints.size() * 5);
        for (int i = 0; i < rawPoints.size() - 1; i++) {
            Vec3d start = rawPoints.get(i);
            Vec3d end = rawPoints.get(i + 1);
            for (double t = 0.0; t < 1.0; t += 0.2) {
                densePath.add(start.lerp(end, t));
            }
        }
        densePath.add(rawPoints.get(rawPoints.size() - 1));
        return densePath;
    }

    private String resolveOwnerName(Entity owner) {
        if (owner == null) return UNKNOWN_OWNER;
        String name = owner.getName().getString();
        return (name == null || name.isBlank()) ? UNKNOWN_OWNER : name;
    }

    private String shortenName(String input, int maxChars) {
        if (input == null || input.isEmpty()) return UNKNOWN_OWNER;
        if (input.length() <= maxChars) return input;
        return input.substring(0, Math.max(1, maxChars - 1)) + ".";
    }

    private String formatDuration(int ticks) {
        int clampedTicks = Math.max(0, ticks);
        if (clampedTicks == 0) return "0:00";
        int seconds = (clampedTicks + 19) / 20;
        int minutes = seconds / 60;
        int secondsPart = seconds % 60;
        return String.format("%d:%02d", minutes, secondsPart);
    }

    private Color alphaMul(Color color, float factor) {
        int alpha = (int) Math.max(0, Math.min(255, Math.round(color.getAlpha() * factor)));
        return ColorUtil.setAlpha(color, alpha);
    }

    private float approach(float current, float target, float speed) {
        if (current < target) return Math.min(target, current + speed);
        if (current > target) return Math.max(target, current - speed);
        return current;
    }

    // ── Data classes ──

    private static final class TrajectoryData {
        private final List<Vec3d> path;
        private final Vec3d impactPos;
        private final int ticksToImpact;
        private final BlockHitResult blockHit;
        private final EntityHitResult entityHit;
        private final Vec3d impactNormal;

        private TrajectoryData(List<Vec3d> path, Vec3d impactPos, int ticksToImpact, BlockHitResult blockHit, EntityHitResult entityHit, Vec3d impactNormal) {
            this.path = path;
            this.impactPos = impactPos;
            this.ticksToImpact = ticksToImpact;
            this.blockHit = blockHit;
            this.entityHit = entityHit;
            this.impactNormal = impactNormal;
        }
    }

    private static final class ImpactRenderTarget {
        private final Vec3d center;
        private final Vec3d normal;

        private ImpactRenderTarget(Vec3d center, Vec3d normal) {
            this.center = center;
            this.normal = normal;
        }
    }

    private static final class ImpactMarkerState {
        private Vec3d center;
        private Vec3d normal;
        private Vec3d targetCenter;
        private Vec3d targetNormal;
        private float alpha = 0f;
        private boolean seen = false;
        private long lastUpdateMs = System.currentTimeMillis();
    }

    private static final class OwnerTagData {
        private final Vec3d impactPos;
        private final int ticksToImpact;
        private final Entity owner;
        private final String ownerName;
        private final int colorSeed;
        private final float alpha;

        private OwnerTagData(Vec3d impactPos, int ticksToImpact, Entity owner, String ownerName, int colorSeed, float alpha) {
            this.impactPos = impactPos;
            this.ticksToImpact = ticksToImpact;
            this.owner = owner;
            this.ownerName = ownerName;
            this.colorSeed = colorSeed;
            this.alpha = alpha;
        }
    }

    private static final class TrailParticle {
        private Vec3d pos;
        private Vec3d velocity;
        private final long spawnTimeMs;
        private final long lifeTimeMs;
        private final float size;
        private final int paletteShift;
        private long lastUpdateMs;

        private TrailParticle(Vec3d pos, Vec3d velocity, long spawnTimeMs, long lifeTimeMs, float size, int paletteShift) {
            this.pos = pos;
            this.velocity = velocity;
            this.spawnTimeMs = spawnTimeMs;
            this.lifeTimeMs = Math.max(1L, lifeTimeMs);
            this.size = size;
            this.paletteShift = paletteShift;
            this.lastUpdateMs = spawnTimeMs;
        }

        private boolean isDead(long now) {
            return now - spawnTimeMs >= lifeTimeMs;
        }

        private float lifeProgress(long now) {
            return Math.max(0.0f, Math.min(1.0f, (float) (now - spawnTimeMs) / lifeTimeMs));
        }
    }

    private static final class OwnerTagState {
        private Vec3d impactPos;
        private int ticksToImpact;
        private Entity owner;
        private String ownerName = UNKNOWN_OWNER;
        private int colorSeed;
        private float alpha = 0f;
        private boolean seen = false;
        private long lastUpdateMs = System.currentTimeMillis();
    }
}
