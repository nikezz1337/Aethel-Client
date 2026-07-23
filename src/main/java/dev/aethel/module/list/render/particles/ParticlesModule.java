package dev.aethel.module.list.render.particles;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import dev.aethel.event.list.EventAttack;
import dev.aethel.event.list.EventPlayerUpdate;
import dev.aethel.event.list.Event3DRender;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.module.settings.MultiBooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.module.list.combat.aura.util.time.TimerUtil;
import dev.aethel.util.render.math.MathUtil;
import dev.aethel.util.render.providers.ColorProvider;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInformation(
        moduleName = "Particles",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Красивые частицы при атаке, тотеме и т.д."
)
public class ParticlesModule extends Module {

    // --- Settings ---
    private final MultiBooleanSetting events = new MultiBooleanSetting("Спавнить при", "",
            new BooleanSetting("Атаке", true),
            new BooleanSetting("Бросок", true),
            new BooleanSetting("Тотем", true),
            new BooleanSetting("Движении", false),
            new BooleanSetting("Бездействии", true)
    );

    private final ModeSetting particleMode = new ModeSetting("Тип частиц", "Бубенец", "Бубенец", "Звездачка", "Сердечко", "Доллар", "Снежок");
    private final SliderSetting speed = new SliderSetting("Скорость", 1.5, 0.1, 3, 0.1);
    private final SliderSetting size = new SliderSetting("Размер", 0.2, 0, 1, 0.1);
    private final SliderSetting attackCount = new SliderSetting("Кол-во атаки", 30, 5, 50, 1)
            .setVisible(() -> events.getValue("Атаке"));
    private final SliderSetting totemCount = new SliderSetting("Кол-во тотем", 8, 2, 16, 1)
            .setVisible(() -> events.getValue("Тотем"));
    private final SliderSetting moveCount = new SliderSetting("Кол-во движения", 2, 1, 6, 1)
            .setVisible(() -> events.getValue("Движении"));
    private final SliderSetting brosokCount = new SliderSetting("Кол-во броска", 6, 1, 16, 1)
            .setVisible(() -> events.getValue("Бросок"));
    private final SliderSetting countAFK = new SliderSetting("Кол-во бездействия", 5, 1, 25, 1)
            .setVisible(() -> events.getValue("Бездействии"));
    private final SliderSetting range = new SliderSetting("Дистанция бездействия", 16, 4, 32, 1)
            .setVisible(() -> events.getValue("Бездействии"));
    private final BooleanSetting glowEffect = new BooleanSetting("Свечение", true);
    private final BooleanSetting seeThrough = new BooleanSetting("Сквозь стены", false);

    // --- Particle lists ---
    private final List<Particle> targetParticles = new ArrayList<>();
    private final List<Particle> worldParticles = new ArrayList<>();
    private final List<Particle> flameParticles = new ArrayList<>();

    private long lastUpdateTime = System.nanoTime();

    // --- Totem state ---
    private long totemStartTime = 0;
    private static final long totemDuration = 2500;
    private boolean spawningTotem = false;
    private Entity totemTarget = null;

    public ParticlesModule() {
        // Settings are auto-detected via reflection in Module.getSettings()
    }

    @Override
    public void toggle() {
        super.toggle();
        clearAll();
    }

    @Override
    public void onDisable() {
        clearAll();
    }

    private void clearAll() {
        targetParticles.clear();
        worldParticles.clear();
        flameParticles.clear();
        spawningTotem = false;
        totemTarget = null;
    }

    public void spawnTotemParticles(Entity target) {
        if (!events.getValue("Тотем")) return;
        spawningTotem = true;
        totemStartTime = System.currentTimeMillis();
        totemTarget = target;
    }

    @Subscribe
    public void onUpdate(EventPlayerUpdate event) {
        if (mc.player == null || mc.world == null) return;

        // Totem particles tick
        if (spawningTotem && totemTarget != null) {
            long elapsed = System.currentTimeMillis() - totemStartTime;
            if (elapsed > totemDuration) {
                spawningTotem = false;
                totemTarget = null;
            } else {
                int[] colors = {
                        ColorProvider.interpolateColor(ColorProvider.getThemeColor(), 0xFFFFFFFF, 0.3f),
                        ColorProvider.interpolateColor(ColorProvider.getThemeColorTwo(), 0xFFFFFFFF, 0.3f)
                };
                for (int i = 0; i < (int) totemCount.getValue(); i++) {
                    int col = colors[(int) MathUtil.random(0, 1.99)];
                    spawnParticleColored(flameParticles,
                            new Vec3d(totemTarget.getX() + MathUtil.random(-0.4, 0.4),
                                    totemTarget.getY() + MathUtil.random(0.0, 2.0),
                                    totemTarget.getZ() + MathUtil.random(-0.4, 0.4)),
                            new Vec3d(MathUtil.random(-0.8, 0.8), MathUtil.random(-0.6, 0.1), MathUtil.random(-0.8, 0.8)),
                            col, 2.0f);
                }
            }
        }

        // AFK / world particles
        if (events.getValue("Бездействии") && worldParticles.size() < (int) countAFK.getValue() * 4) {
            int r = (int) range.getValue();
            for (int i = 0; i < Math.min(2, (int) countAFK.getValue()); i++) {
                double spawnX = mc.player.getX() + MathUtil.random(-r, r);
                double spawnZ = mc.player.getZ() + MathUtil.random(-r, r);
                double spawnY = mc.player.getY() + MathUtil.random(mc.player.getHeight(), r);
                Vec3d spawnPos = new Vec3d(spawnX, spawnY, spawnZ);
                while (!mc.world.getBlockState(BlockPos.ofFloored(spawnPos)).isAir()
                        && spawnPos.y < mc.world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, (int) spawnPos.x, (int) spawnPos.z)) {
                    spawnPos = spawnPos.add(0, 1, 0);
                }
                spawnParticle(worldParticles, spawnPos,
                        new Vec3d(mc.player.getVelocity().x + MathUtil.random(-0.5, 0.5),
                                MathUtil.random(-0.06, 0.06),
                                mc.player.getVelocity().z + MathUtil.random(-0.5, 0.5)));
            }
        }

        // Movement particles
        if (events.getValue("Движении") && isMoving()) {
            for (int i = 0; i < (int) moveCount.getValue(); i++) {
                spawnParticle(flameParticles,
                        new Vec3d(mc.player.getX() + MathUtil.random(-0.45, 0.45),
                                mc.player.getY() + MathUtil.random(0, mc.player.getHeight()),
                                mc.player.getZ() + MathUtil.random(-0.45, 0.45)),
                        new Vec3d(
                                (mc.player.getVelocity().x + MathUtil.random(-0.1, 0.1)) * 0.2,
                                MathUtil.random(-0.1, 0.1) * 0.2,
                                (mc.player.getVelocity().z + MathUtil.random(-0.1, 0.1)) * 0.2));
            }
        }

        // Throw particles
        if (events.getValue("Бросок")) {
            List<Entity> entitySnapshot = new ArrayList<>();
            mc.world.getEntities().forEach(entitySnapshot::add);
            for (Entity entity : entitySnapshot) {
                try {
                    boolean isProjectile = entity instanceof net.minecraft.entity.projectile.thrown.EnderPearlEntity
                            || entity instanceof ArrowEntity
                            || entity instanceof TridentEntity;
                    if (!isProjectile) continue;
                    if (entity instanceof TridentEntity trident && trident.groundCollision) continue;
                    boolean moving = entity.prevX != entity.getX()
                            || entity.prevY != entity.getY()
                            || entity.prevZ != entity.getZ();
                    if (!moving) continue;
                    Vec3d pos = entity.getPos();
                    for (int i = 0; i < (int) brosokCount.getValue(); i++) {
                        spawnParticle(flameParticles,
                                new Vec3d(pos.x + MathUtil.random(-0.5, 0.5),
                                        pos.y + MathUtil.random(-0.5, 0.5),
                                        pos.z + MathUtil.random(-0.5, 0.5)),
                                new Vec3d(MathUtil.random(-0.06, 0.06),
                                        MathUtil.random(-0.06, 0.06),
                                        MathUtil.random(-0.06, 0.06)));
                    }
                } catch (Exception ignored) {}
            }
        }

        // Cleanup
        targetParticles.removeIf(p -> p.timer.hasReached(1000));
        worldParticles.removeIf(p -> !canSee(p.position) || p.timer.hasReached(2000));
        flameParticles.removeIf(p -> p.timer.hasReached(2000));
    }

    @Subscribe
    public void onAttack(EventAttack event) {
        if (!events.getValue("Атаке")) return;
        Entity target = event.getEntity();
        float motion = 1.35f;
        for (int i = 0; i < (int) attackCount.getValue(); i++) {
            spawnParticle(targetParticles,
                    new Vec3d(target.getX() + MathUtil.random(-0.4, 0.4),
                            target.getY() + MathUtil.random(0, target.getHeight()),
                            target.getZ() + MathUtil.random(-0.4, 0.4)),
                    new Vec3d(MathUtil.random(-motion, motion),
                            MathUtil.random(-1.25, 1.25),
                            MathUtil.random(-motion, motion)));
        }
    }

    @Subscribe
    public void onRender3D(Event3DRender event) {
        MatrixStack matrix = event.getMatrixStack();

        long now = System.nanoTime();
        double dt = (now - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = now;

        setupRenderState();
        renderParticles(matrix, targetParticles, 400, 600, dt);
        renderParticles(matrix, worldParticles, 800, 1500, dt);
        renderParticles(matrix, flameParticles, 700, 1200, dt);
        resetRenderState();
    }

    // --- Helpers ---

    private boolean isMoving() {
        if (mc.player == null) return false;
        Vec3d vel = mc.player.getVelocity();
        return (vel.x * vel.x + vel.z * vel.z) > 1.0E-6;
    }

    private boolean canSee(Vec3d pos) {
        if (mc.player == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        double dist = eyePos.distanceTo(pos);
        return mc.world != null && mc.world.raycast(new net.minecraft.world.RaycastContext(
                eyePos, pos,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    // --- Spawn helpers ---

    private ParticleType resolveType() {
        String mode = particleMode.getValue();
        if (mode.equals("Доллар")) return ParticleType.DOLLAR;
        if (mode.equals("Сердечко")) return ParticleType.HEART;
        if (mode.equals("Звездачка")) return ParticleType.STAR;
        if (mode.equals("Бубенец")) return ParticleType.BLOOM;
        if (mode.equals("Снежок")) return ParticleType.SNOWFLAKE;
        return ParticleType.getRandom();
    }

    private void spawnParticle(List<Particle> list, Vec3d pos, Vec3d vel) {
        float s = 0.05f + (float) size.getValue() * 0.2f;
        int col = ColorProvider.interpolateColor(ColorProvider.getThemeColor(), 0xFFFFFFFF, 0.3f);
        list.add(new Particle(resolveType(), pos.add(0, s, 0), vel, list.size(), col, s, (float) speed.getValue()));
    }

    private void spawnParticleColored(List<Particle> list, Vec3d pos, Vec3d vel, int col, float spd) {
        float s = 0.05f + (float) size.getValue() * 0.2f;
        list.add(new Particle(resolveType(), pos.add(0, s, 0), vel, list.size(), col, s, spd));
    }

    // --- Render ---

    private void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
    }

    private void resetRenderState() {
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void renderParticles(MatrixStack matrix, List<Particle> particles, long fadeIn, long fadeOut, double dt) {
        if (particles.isEmpty()) return;

        Particle[] snapshot = particles.toArray(new Particle[0]);
        if (snapshot.length == 0) return;

        for (Particle p : snapshot) {
            p.update(dt);
            p.animUpdate();
            if (p.animValue != 1 && !p.timer.hasReached(fadeIn))
                p.animTarget = 1.0;
            if (p.animValue != 0 && p.timer.hasReached(fadeOut))
                p.animTarget = 0.0;
        }

        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
        float yaw = mc.gameRenderer.getCamera().getYaw();
        float pitch = mc.gameRenderer.getCamera().getPitch();

        if (seeThrough.getValue()) RenderSystem.disableDepthTest();
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);

        // --- Main pass (particle textures) ---
        ParticleType lastType = null;
        BufferBuilder bb = null;

        try {
            for (Particle p : snapshot) {
                float alpha = (float) p.animValue;
                if (alpha <= 0f) continue;
                int baseAlpha = (p.color >>> 24) & 0xFF;
                int a = (int) (baseAlpha * alpha);
                if (a <= 0) continue;
                int r = (p.color >> 16) & 0xFF;
                int g = (p.color >> 8) & 0xFF;
                int b = p.color & 0xFF;

                if (p.type != lastType) {
                    if (bb != null) { BufferRenderer.drawWithGlobalProgram(bb.end()); bb = null; }
                    RenderSystem.setShaderTexture(0, p.type.texture);
                    bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    lastType = p.type;
                }

                matrix.push();
                matrix.translate(p.position.x - cam.x, p.position.y - cam.y, p.position.z - cam.z);
                matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f m = matrix.peek().getPositionMatrix();
                float half = p.size;

                bb.vertex(m, -half, -half, 0).texture(0f, 1f).color(r, g, b, a);
                bb.vertex(m,  half, -half, 0).texture(1f, 1f).color(r, g, b, a);
                bb.vertex(m,  half,  half, 0).texture(1f, 0f).color(r, g, b, a);
                bb.vertex(m, -half,  half, 0).texture(0f, 0f).color(r, g, b, a);

                matrix.pop();
            }

            if (bb != null) { BufferRenderer.drawWithGlobalProgram(bb.end()); bb = null; }
        } catch (Exception ignored) {}

        if (seeThrough.getValue()) RenderSystem.enableDepthTest();
    }

    // --- ParticleType ---

    public enum ParticleType {
        DOLLAR("dollar"),
        HEART("heart"),
        STAR("star"),
        BLOOM("glow"),
        SNOWFLAKE("snowflake");

        public final Identifier texture;

        ParticleType(String name) {
            texture = Identifier.of("aethel", "textures/particles/" + name + ".png");
        }

        private static final ParticleType[] VALUES = values();
        private static final Random RNG = new Random();

        public static ParticleType getRandom() {
            return VALUES[RNG.nextInt(VALUES.length)];
        }
    }

    // --- Particle ---

    public static class Particle {
        public final ParticleType type;
        public Vec3d position;
        public Vec3d velocity;
        public final int index;
        public final int color;
        public final float size;
        public final double speedMultiplier;
        public final TimerUtil timer = new TimerUtil();
        public double animValue = 0.0;
        public double animTarget = 1.0;
        private static final long ANIM_SPEED = 200; // ms for fade

        private static final double BASE_VELOCITY = 0.05;

        public Particle(ParticleType type, Vec3d position, Vec3d velocity,
                        int index, int color, float size, float speedMultiplier) {
            this.type = type;
            this.position = position;
            this.velocity = velocity.multiply(BASE_VELOCITY);
            this.index = index;
            this.color = color;
            this.size = size;
            this.speedMultiplier = speedMultiplier;
        }

        public void update(double dt) {
            Vec3d vel = velocity;
            if (isBlockSolid(position.x, position.y, position.z + vel.z))
                velocity = new Vec3d(vel.x, vel.y, -vel.z);
            if (isBlockSolid(position.x, position.y + vel.y, position.z))
                velocity = new Vec3d(vel.x, -vel.y, vel.z);
            if (isBlockSolid(position.x + vel.x, position.y, position.z))
                velocity = new Vec3d(-vel.x, vel.y, vel.z);

            double friction = Math.pow(0.999, dt * 60);
            velocity = velocity.multiply(friction);
            position = position.add(velocity.multiply(dt * 60 * speedMultiplier));
        }

        public void animUpdate() {
            if (Math.abs(animValue - animTarget) < 0.001) {
                animValue = animTarget;
                return;
            }
            double step = (animTarget - animValue) * 0.1;
            animValue += step;
        }

        private boolean isBlockSolid(double x, double y, double z) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.world == null) return false;
            return !mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).isAir();
        }
    }
}
