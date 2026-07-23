package dev.ethereal.client.features.modules.render.particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.events.player.world.AttackEvent;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.files.FileUtil;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.player.MoveUtil;
import dev.ethereal.api.utils.player.PlayerUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleRegister(name = "Particles", category = Category.RENDER)
public class ParticlesModule extends Module {
    @Getter private static final ParticlesModule instance = new ParticlesModule();

    // --- Settings ---
    private final MultiBooleanSetting events = new MultiBooleanSetting("Спавнить при").value(
            new BooleanSetting("Атаке").value(true),
            new BooleanSetting("Бросок").value(true),
            new BooleanSetting("Тотем").value(true),
            new BooleanSetting("Движении").value(false),
            new BooleanSetting("Бездействии").value(true)
    );

    private final ModeSetting particleMode = new ModeSetting("Тип частиц").value("Бубенец").values("Бубенец", "Звездачка", "Сердечко", "Доллар", "Снежок");
    private final SliderSetting speed = new SliderSetting("Скорость").value(1.5f).range(0.1f, 3f).step(0.1f);
    private final SliderSetting size = new SliderSetting("Размер").value(0.2f).range(0f, 1f).step(0.1f);
    private final SliderSetting attackCount = new SliderSetting("Кол-в атаки").value(30f).range(5f, 50f).step(1f)
            .setVisible(() -> events.isEnabled("Атаке"));
    private final SliderSetting totemCount = new SliderSetting("Кол-в тотеме").value(8f).range(2f, 16f).step(1f)
            .setVisible(() -> events.isEnabled("Тотем"));
    private final SliderSetting moveCount = new SliderSetting("Кол-в движении").value(2f).range(1f, 6f).step(1f)
            .setVisible(() -> events.isEnabled("Движении"));
    private final SliderSetting brosokCount = new SliderSetting("Кол-в броске").value(6f).range(1f, 16f).step(1f)
            .setVisible(() -> events.isEnabled("Бросок"));
    private final SliderSetting countAFK = new SliderSetting("Кол-во при бездействии").value(5f).range(1f, 25f).step(1f)
            .setVisible(() -> events.isEnabled("Бездействии"));
    private final SliderSetting range = new SliderSetting("Дистанция при бездействии").value(16f).range(4f, 32f).step(1f)
            .setVisible(() -> events.isEnabled("Бездействии"));
    private final BooleanSetting glowEffect = new BooleanSetting("Эффект свечения").value(true);
    private final BooleanSetting seeThrough = new BooleanSetting("Видеть через стену").value(false);

    // --- Particle lists ---
    private final List<Particle> targetParticles = new ArrayList<>();
    private final List<Particle> worldParticles = new ArrayList<>();
    private final List<Particle> flameParticles = new ArrayList<>();

    private long lastUpdateTime = System.nanoTime();

    // --- Totem state ---
    private long totemStartTime = 0;
    private final long totemDuration = 2500;
    private boolean spawningTotem = false;
    private Entity totemTarget = null;

    public ParticlesModule() {
        addSettings(events, particleMode, speed, size,
                attackCount, totemCount, moveCount, brosokCount, countAFK, range,
                glowEffect, seeThrough);
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
        if (!events.isEnabled("Тотем")) return;
        spawningTotem = true;
        totemStartTime = System.currentTimeMillis();
        totemTarget = target;
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

            // Totem particles tick
            if (spawningTotem && totemTarget != null) {
                long elapsed = System.currentTimeMillis() - totemStartTime;
                if (elapsed > totemDuration) {
                    spawningTotem = false;
                    totemTarget = null;
                } else {
                    int[] colors = {
                            new Color(221, 218, 127).getRGB(),
                            new Color(127, 221, 144).getRGB()
                    };
                    for (int i = 0; i < totemCount.getValue().intValue(); i++) {
                        int col = colors[MathUtil.random(0, 1)];
                        spawnParticleColored(flameParticles,
                                new Vec3d(totemTarget.getX() + MathUtil.random(-0.4f, 0.4f),
                                        totemTarget.getY() + MathUtil.random(0f, 2f),
                                        totemTarget.getZ() + MathUtil.random(-0.4f, 0.4f)),
                                new Vec3d(MathUtil.random(-0.8f, 0.8f), MathUtil.random(-0.6f, 0.1f), MathUtil.random(-0.8f, 0.8f)),
                                col, 2.0f);
                    }
                }
            }

            // AFK / world particles
            if (events.isEnabled("Бездействии") && worldParticles.size() < countAFK.getValue().intValue() * 4) {
                int r = range.getValue().intValue();
                for (int i = 0; i < Math.min(2, countAFK.getValue().intValue()); i++) {
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
            if (events.isEnabled("Движении") && MoveUtil.isMoving()) {
                for (int i = 0; i < moveCount.getValue().intValue(); i++) {
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
            if (events.isEnabled("Бросок")) {
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
                        for (int i = 0; i < brosokCount.getValue().intValue(); i++) {
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
        targetParticles.removeIf(p -> p.timer.finished(1000));
        worldParticles.removeIf(p -> !PlayerUtil.canSee(p.position) || p.timer.finished(2000));
        flameParticles.removeIf(p -> p.timer.finished(2000));
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (!events.isEnabled("Атаке")) return;
        Entity target = event.entity();
        float motion = 1.35f;
        for (int i = 0; i < attackCount.getValue().intValue(); i++) {
            spawnParticle(targetParticles,
                    new Vec3d(target.getX() + MathUtil.random(-0.4f, 0.4f),
                            target.getY() + MathUtil.random(0, target.getHeight()),
                            target.getZ() + MathUtil.random(-0.4f, 0.4f)),
                    new Vec3d(MathUtil.random(-motion, motion),
                            MathUtil.random(-1.25f, 1.25f),
                            MathUtil.random(-motion, motion)));
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        MatrixStack matrix = event.matrixStack();

        long now = System.nanoTime();
        double dt = (now - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = now;

        setupRenderState();
        renderParticles(matrix, targetParticles, 400, 600, dt);
        renderParticles(matrix, worldParticles, 800, 1500, dt);
        renderParticles(matrix, flameParticles, 700, 1200, dt);
        resetRenderState();
    }

    // --- Spawn helpers ---

    private ParticleType resolveType() {
        return switch (particleMode.getValue()) {
            case "Доллар" -> ParticleType.DOLLAR;
            case "Сердечко" -> ParticleType.HEART;
            case "Звездачка" -> ParticleType.STAR;
            case "Бубенец" -> ParticleType.BLOOM;
            case "Снежок" -> ParticleType.SNOWFLAKE;
            default -> ParticleType.getRandom();
        };
    }

    private void spawnParticle(List<Particle> list, Vec3d pos, Vec3d vel) {
        float s = 0.05f + size.getValue() * 0.2f;
        int col = UIColors.gradient(list.size() * 100).getRGB();
        list.add(new Particle(resolveType(), pos.add(0, s, 0), vel, list.size(), col, s, speed.getValue()));
    }

    private void spawnParticleColored(List<Particle> list, Vec3d pos, Vec3d vel, int col, float spd) {
        float s = 0.05f + size.getValue() * 0.2f;
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
            p.anim.update();
            if (p.anim.getValue() != 1 && !p.timer.finished(fadeIn))
                p.anim.run(1.0, 500, Easing.CUBIC_OUT);
            if (p.anim.getValue() != 0 && p.timer.finished(fadeOut))
                p.anim.run(0.0, 500, Easing.CUBIC_OUT);
        }

        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
        net.minecraft.util.math.RotationAxis yawAxis = net.minecraft.util.math.RotationAxis.POSITIVE_Y;
        net.minecraft.util.math.RotationAxis pitchAxis = net.minecraft.util.math.RotationAxis.POSITIVE_X;
        float yaw = mc.gameRenderer.getCamera().getYaw();
        float pitch = mc.gameRenderer.getCamera().getPitch();

        if (seeThrough.getValue()) RenderSystem.disableDepthTest();
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);

        ParticleType lastType = null;
        BufferBuilder bb = null;

        try {
            for (Particle p : snapshot) {
                float alpha = (float) p.anim.getValue();
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

                // строим billboard матрицу для этого партикла
                matrix.push();
                matrix.translate(p.position.x - cam.x, p.position.y - cam.y, p.position.z - cam.z);
                matrix.multiply(yawAxis.rotationDegrees(-yaw));
                matrix.multiply(pitchAxis.rotationDegrees(pitch));
                Matrix4f m = matrix.peek().getPositionMatrix();
                float half = p.size;

                if (glowEffect.getValue()) {
                    int ga = Math.max(1, (int) (a * 0.15f));
                    float gh = half * 2f;
                    bb.vertex(m, -gh, -gh, 0).texture(0f, 1f).color(r, g, b, ga);
                    bb.vertex(m,  gh, -gh, 0).texture(1f, 1f).color(r, g, b, ga);
                    bb.vertex(m,  gh,  gh, 0).texture(1f, 0f).color(r, g, b, ga);
                    bb.vertex(m, -gh,  gh, 0).texture(0f, 0f).color(r, g, b, ga);
                }

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
        BLOOM("glow"),       // glow.png используется как bloom-эффект
        SNOWFLAKE("snowflake"),
        STARNEW("star");     // отдельного starnew нет, используем star

        public final Identifier texture;

        ParticleType(String name) {
            texture = FileUtil.getImage("particles/" + name);
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
        public final AnimationUtil anim = new AnimationUtil();

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
            // bounce off blocks
            Vec3d vel = velocity;
            if (isBlockSolid(position.x, position.y, position.z + vel.z))
                velocity = new Vec3d(vel.x, vel.y, -vel.z);
            if (isBlockSolid(position.x, position.y + vel.y, position.z))
                velocity = new Vec3d(vel.x, -vel.y, vel.z);
            if (isBlockSolid(position.x + vel.x, position.y, position.z))
                velocity = new Vec3d(-vel.x, vel.y, vel.z);

            // friction
            double friction = Math.pow(0.999, dt * 60);
            velocity = velocity.multiply(friction);
            position = position.add(velocity.multiply(dt * 60 * speedMultiplier));
        }

        private boolean isBlockSolid(double x, double y, double z) {
            if (MinecraftClient.mc.world == null) return false;
            return !MinecraftClient.mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).isAir();
        }
    }

    // tiny helper to access mc from inner static class
    private static final class MinecraftClient {
        static final net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
    }
}
