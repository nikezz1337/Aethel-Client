package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.event.list.Event3DRender;
import dev.aethel.event.list.EventHUD;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.util.render.ShaderUtil;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.item.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(
        moduleName = "Prediction",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Predicts projectile paths and impact points"
)
public class Prediction extends Module {

    private final SliderSetting maxTicks = new SliderSetting("Max Ticks", 150, 20, 300, 10);
    private final SliderSetting maxDistance = new SliderSetting("Max Distance", 250, 50, 500, 10);
    private final SliderSetting lineWidth = new SliderSetting("Line Width", 4.0, 1.0, 8.0, 0.5);

    record ImpactPoint(Vec3d position, int ticks, Identifier texture) {}

    final List<ImpactPoint> impactPoints = new ArrayList<>();

    private final Quaternionf cachedCameraRotation = new Quaternionf();
    private final Vector3f tempVec = new Vector3f();
    private final Vector3f cachedCamPos = new Vector3f();
    private float cachedHalfScreenWidth;
    private float cachedHalfScreenHeight;
    private double cachedTanHalfFov;
    private long startMillis = -1L;

    @Override
    public void onDisable() {
        impactPoints.clear();
        startMillis = -1L;
        super.onDisable();
    }

    private void drawShaderSegment(Matrix4f matrix, Vec3d cam, Vec3d a, Vec3d b, float width) {
        if (a.distanceTo(b) < 0.01) return;
        Vec3d dir = b.subtract(a).normalize();
        Vec3d toCam = cam.subtract(a).normalize();
        Vec3d perp = dir.crossProduct(toCam).normalize();
        if (perp.lengthSquared() < 0.01) {
            perp = new Vec3d(0, 1, 0).crossProduct(dir).normalize();
        }
        float halfW = width * 0.02f;
        Vec3d off = perp.multiply(halfW);

        Vec3d a1 = a.add(off), a2 = a.subtract(off);
        Vec3d b1 = b.add(off), b2 = b.subtract(off);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(matrix, (float) (a1.x - cam.x), (float) (a1.y - cam.y), (float) (a1.z - cam.z)).texture(0, 0).color(255, 255, 255, 255);
        buf.vertex(matrix, (float) (a2.x - cam.x), (float) (a2.y - cam.y), (float) (a2.z - cam.z)).texture(0, 1).color(255, 255, 255, 255);
        buf.vertex(matrix, (float) (b2.x - cam.x), (float) (b2.y - cam.y), (float) (b2.z - cam.z)).texture(1, 1).color(255, 255, 255, 255);
        buf.vertex(matrix, (float) (b1.x - cam.x), (float) (b1.y - cam.y), (float) (b1.z - cam.z)).texture(1, 0).color(255, 255, 255, 255);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void setUniform(ShaderProgram shader, String name, float... values) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            if (values.length == 1) uniform.set(values[0]);
            else if (values.length == 2) uniform.set(values[0], values[1]);
            else if (values.length == 3) uniform.set(values[0], values[1], values[2]);
            else if (values.length == 4) uniform.set(values[0], values[1], values[2], values[3]);
        }
    }

    @Subscribe
    public void onRender3D(Event3DRender e) {
        if (mc.player == null || mc.world == null) return;

        impactPoints.clear();
        Vec3d cam = e.getCamera().getPos();
        Matrix4f matrix = e.getMatrixStack().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        int themeColor = ColorProvider.getThemeColor();
        float lw = lineWidth.getFloatValue();

        if (startMillis < 0L) startMillis = System.currentTimeMillis();
        float time = (System.currentTimeMillis() - startMillis) / 1000.0F;

        ShaderProgram shader = RenderSystem.setShader(ShaderUtil.blockOverlayEdge);
        if (shader != null) {
            float r = ((themeColor >> 16) & 0xFF) / 255.0F;
            float g = ((themeColor >> 8) & 0xFF) / 255.0F;
            float b = (themeColor & 0xFF) / 255.0F;
            float r2 = Math.max(0, Math.min(255, ((themeColor >> 16) & 0xFF) + 60)) / 255.0F;
            float g2 = Math.max(0, Math.min(255, ((themeColor >> 8) & 0xFF) - 20)) / 255.0F;
            float b2 = Math.max(0, Math.min(255, (themeColor & 0xFF) + 40)) / 255.0F;

            setUniform(shader, "color", r, g, b);
            setUniform(shader, "color2", r2, g2, b2);
            setUniform(shader, "time", time);
            setUniform(shader, "speed", 1.2f);
            setUniform(shader, "edgeWidth", 1.5f);
            setUniform(shader, "alpha", 0.65f);
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity.isRemoved()) continue;

            if (entity instanceof ProjectileEntity proj && proj.getVelocity().lengthSquared() > 0.01) {
                Identifier tex = getTextureForProjectile(proj);
                double gravity = getGravityFor(proj);

                Vec3d origin = proj.getPos();
                Vec3d mot = proj.getVelocity();
                Vec3d pos = origin;
                int ticks = 0;
                float maxDist = maxDistance.getFloatValue();

                for (int i = 0; i < maxTicks.getIntValue(); i++) {
                    Vec3d prev = pos;
                    pos = pos.add(mot);

                    boolean inWater = mc.world.getBlockState(net.minecraft.util.math.BlockPos.ofFloored(pos)).getFluidState().isIn(net.minecraft.registry.tag.FluidTags.WATER);
                    mot = mot.multiply(inWater ? 0.8 : 0.99);
                    if (!proj.hasNoGravity()) {
                        mot = mot.subtract(0, gravity, 0);
                    }

                    Vec3d end = pos;
                    Boolean hitBlock = false;

                    if (mc.world != null) {
                        RaycastContext ctx = new RaycastContext(prev, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, proj);
                        HitResult hit = mc.world.raycast(ctx);
                        if (hit.getType() == HitResult.Type.BLOCK) {
                            end = hit.getPos();
                            hitBlock = true;
                        }
                    }

                    drawShaderSegment(matrix, cam, prev, end, lw);

                    if (hitBlock || end.y < -128.0D || origin.distanceTo(pos) > maxDist) {
                        if (mot.lengthSquared() > 0.04) {
                            impactPoints.add(new ImpactPoint(end, ticks, tex));
                        }
                        break;
                    }
                    ticks++;
                }
            } else if (entity instanceof ItemEntity item && item.getVelocity().lengthSquared() > 0.01) {
                Identifier tex = getTextureForItem(item.getStack().getItem());

                Vec3d origin = item.getPos();
                Vec3d mot = item.getVelocity();
                Vec3d pos = origin;
                int ticks = 0;
                float maxDist = maxDistance.getFloatValue();

                for (int i = 0; i < maxTicks.getIntValue(); i++) {
                    Vec3d prev = pos;
                    pos = pos.add(mot);
                    mot = mot.multiply(0.99);
                    mot = mot.subtract(0, 0.04, 0);

                    Vec3d end = pos;
                    Boolean hitBlock = false;

                    if (mc.world != null) {
                        RaycastContext ctx = new RaycastContext(prev, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, item);
                        HitResult hit = mc.world.raycast(ctx);
                        if (hit.getType() == HitResult.Type.BLOCK) {
                            end = hit.getPos();
                            hitBlock = true;
                        }
                    }

                    drawShaderSegment(matrix, cam, prev, end, lw);

                    if (hitBlock || end.y < -128.0D || origin.distanceTo(pos) > maxDist) {
                        if (mot.lengthSquared() > 0.04) {
                            impactPoints.add(new ImpactPoint(end, ticks, tex));
                        }
                        break;
                    }
                    ticks++;
                }
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    @Subscribe
    public void onRenderHUD(EventHUD e) {
        if (impactPoints.isEmpty()) return;

        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        cachedCamPos.set((float) camPos.x, (float) camPos.y, (float) camPos.z);

        var camera = mc.getEntityRenderDispatcher().camera;
        var yawQuat = RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw());
        var pitchQuat = RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch());
        yawQuat.mul(pitchQuat, cachedCameraRotation).conjugate();

        cachedHalfScreenWidth = mc.getWindow().getScaledWidth() / 2f;
        cachedHalfScreenHeight = mc.getWindow().getScaledHeight() / 2f;
        float fov = mc.gameRenderer.getFov(camera, e.getRenderTickCounter().getTickDelta(true), !mc.player.isSubmergedInWater());
        cachedTanHalfFov = Math.tan(Math.toRadians(fov / 2.0));

        for (ImpactPoint pt : impactPoints) {
            Vector2f screen = projectToScreen(pt.position.x, pt.position.y - 0.3F, pt.position.z);
            if (screen.x == Float.MAX_VALUE) continue;

            double time = pt.ticks * 50.0 / 1000.0;
            String text = String.format("%.1f сек.", time);
            float textWidth = Fonts.SFREGULAR.get().getWidth(text, 5);
            float totalWidth = textWidth + 8 + 8;
            float x = screen.x - totalWidth / 2;
            float y = screen.y;

            DrawUtil.drawRound(x + 2, y + 2 - 3, totalWidth - 4, 12 - 3, 0, ColorProvider.rgba(24, 24, 24, 80));

            drawTexture(e.getDrawContext(), pt.texture, (int) x + 4, (int) y + 2 + 1 - 4, 8, 8);

            DrawUtil.drawText(Fonts.SFBOLD.get(), text, x + 14, y + 4.5F - 4, -1, 5);
        }
    }

    private void drawTexture(DrawContext ctx, Identifier texture, int x, int y, int w, int h) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(m, x, y + h, 0).texture(0, 1).color(255, 255, 255, 255);
        buf.vertex(m, x + w, y + h, 0).texture(1, 1).color(255, 255, 255, 255);
        buf.vertex(m, x + w, y, 0).texture(1, 0).color(255, 255, 255, 255);
        buf.vertex(m, x, y, 0).texture(0, 0).color(255, 255, 255, 255);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.disableBlend();
    }

    private Vector2f projectToScreen(double wx, double wy, double wz) {
        tempVec.set(
                (float) (cachedCamPos.x - wx),
                (float) (cachedCamPos.y - wy),
                (float) (cachedCamPos.z - wz)
        );
        tempVec.rotate(cachedCameraRotation);

        if (tempVec.z >= 0f) {
            return new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
        }

        float w = cachedHalfScreenWidth;
        float h = cachedHalfScreenHeight;
        double scale = h / (tempVec.z * cachedTanHalfFov);
        return new Vector2f(
                (float) (-tempVec.x * scale + w),
                (float) (h - tempVec.y * scale)
        );
    }

    private double getGravityFor(ProjectileEntity proj) {
        if (proj instanceof ExperienceBottleEntity) return 0.07;
        if (proj instanceof PotionEntity) return 0.05;
        if (proj instanceof PersistentProjectileEntity) return 0.05;
        if (proj instanceof FishingBobberEntity) return 0.04;
        if (proj instanceof FireworkRocketEntity) return 0.03;
        return 0.03;
    }

    private Identifier getTextureForProjectile(ProjectileEntity proj) {
        if (proj instanceof EnderPearlEntity) return Identifier.of("minecraft", "textures/item/ender_pearl.png");
        if (proj instanceof ExperienceBottleEntity) return Identifier.of("minecraft", "textures/item/experience_bottle.png");
        if (proj instanceof PotionEntity) return Identifier.of("minecraft", "textures/item/potion.png");
        if (proj instanceof SnowballEntity) return Identifier.of("minecraft", "textures/item/snowball.png");
        if (proj instanceof EggEntity) return Identifier.of("minecraft", "textures/item/egg.png");
        if (proj instanceof TridentEntity) return Identifier.of("minecraft", "textures/item/trident.png");
        if (proj instanceof ArrowEntity || proj instanceof SpectralArrowEntity) return Identifier.of("minecraft", "textures/item/arrow.png");
        if (proj instanceof FishingBobberEntity) return Identifier.of("minecraft", "textures/item/fishing_rod.png");
        if (proj instanceof FireworkRocketEntity) return Identifier.of("minecraft", "textures/item/firework_rocket.png");
        if (proj instanceof WindChargeEntity || proj instanceof BreezeWindChargeEntity) return Identifier.of("minecraft", "textures/item/wind_charge.png");
        String name = proj.getClass().getSimpleName().toLowerCase();
        if (name.contains("snowball")) return Identifier.of("minecraft", "textures/item/snowball.png");
        if (name.contains("egg")) return Identifier.of("minecraft", "textures/item/egg.png");
        return Identifier.of("minecraft", "textures/item/ender_pearl.png");
    }

    private Identifier getTextureForItem(Item item) {
        Identifier itemId = net.minecraft.registry.Registries.ITEM.getId(item);
        return Identifier.of(itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
    }
}
