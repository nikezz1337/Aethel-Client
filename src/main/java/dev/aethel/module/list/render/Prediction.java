package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.event.list.Event3DRender;
import dev.aethel.event.list.EventHUD;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.ModeListSetting;
import dev.aethel.module.list.render.Interface;
import dev.aethel.util.base.Instance;
import dev.aethel.util.render.ShaderUtil;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.item.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
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

    public final ModeListSetting targets = new ModeListSetting("Цели",
            new BooleanSetting("Жемчуг", true),
            new BooleanSetting("Стрелы", true),
            new BooleanSetting("Зелья", true),
            new BooleanSetting("Снежки/яйца", true),
            new BooleanSetting("Трезубцы", true),
            new BooleanSetting("Фейерверки", true),
            new BooleanSetting("Удочки", true),
            new BooleanSetting("Опыт/з.заряды", true),
            new BooleanSetting("Предметы", true)
    );

    record ImpactPoint(Vec3d position, int ticks, ItemStack stack) {}

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

    private ItemStack getItemStackForProjectile(ProjectileEntity proj) {
        if (proj instanceof EnderPearlEntity) return new ItemStack(Items.ENDER_PEARL);
        if (proj instanceof ExperienceBottleEntity) return new ItemStack(Items.EXPERIENCE_BOTTLE);
        if (proj instanceof PotionEntity) return new ItemStack(Items.SPLASH_POTION);
        if (proj instanceof SnowballEntity) return new ItemStack(Items.SNOWBALL);
        if (proj instanceof EggEntity) return new ItemStack(Items.EGG);
        if (proj instanceof TridentEntity) return new ItemStack(Items.TRIDENT);
        if (proj instanceof ArrowEntity) return new ItemStack(Items.ARROW);
        if (proj instanceof SpectralArrowEntity) return new ItemStack(Items.SPECTRAL_ARROW);
        if (proj instanceof FishingBobberEntity) return new ItemStack(Items.FISHING_ROD);
        if (proj instanceof FireworkRocketEntity) return new ItemStack(Items.FIREWORK_ROCKET);
        if (proj instanceof WindChargeEntity || proj instanceof BreezeWindChargeEntity) return new ItemStack(Items.WIND_CHARGE);
        return new ItemStack(Items.ENDER_PEARL);
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
        float lw = 11.0f;

        if (startMillis < 0L) startMillis = System.currentTimeMillis();
        float time = (System.currentTimeMillis() - startMillis) / 1000.0F;

        ShaderProgram shader = RenderSystem.setShader(ShaderUtil.blockOverlayEdge);
        if (shader != null) {
            float r = ((themeColor >> 16) & 0xFF) / 255.0F;
            float g = ((themeColor >> 8) & 0xFF) / 255.0F;
            float b = (themeColor & 0xFF) / 255.0F;
            float r2 = Math.min(1.0f, r + 0.6f);
            float g2 = Math.min(1.0f, g + 0.6f);
            float b2 = Math.min(1.0f, b + 0.6f);

            setUniform(shader, "color", r, g, b);
            setUniform(shader, "color2", r2, g2, b2);
            setUniform(shader, "time", time);
            setUniform(shader, "speed", 3.0f);
            setUniform(shader, "edgeWidth", 6.0f);
            setUniform(shader, "alpha", 1.0f);
        }

        boolean enderPearls = targets.isEnabled("Жемчуг");
        boolean arrows = targets.isEnabled("Стрелы");
        boolean potions = targets.isEnabled("Зелья");
        boolean snowballs = targets.isEnabled("Снежки/яйца");
        boolean tridents = targets.isEnabled("Трезубцы");
        boolean fireworks = targets.isEnabled("Фейерверки");
        boolean fishing = targets.isEnabled("Удочки");
        boolean expWind = targets.isEnabled("Опыт/з.заряды");
        boolean renderItems = targets.isEnabled("Предметы");

        int maxTicks = 200;
        float maxDist = 300f;

        for (Entity entity : mc.world.getEntities()) {
            if (entity.isRemoved()) continue;

            if (entity instanceof ProjectileEntity proj && proj.getVelocity().lengthSquared() > 0.01) {
                boolean shouldRender = false;
                double gravity = 0.03;

                if (proj instanceof EnderPearlEntity && enderPearls) {
                    shouldRender = true;
                } else if (proj instanceof ExperienceBottleEntity && expWind) {
                    shouldRender = true;
                    gravity = 0.07;
                } else if (proj instanceof PotionEntity && potions) {
                    shouldRender = true;
                    gravity = 0.05;
                } else if ((proj instanceof SnowballEntity || proj instanceof EggEntity) && snowballs) {
                    shouldRender = true;
                } else if (proj instanceof TridentEntity && tridents) {
                    shouldRender = true;
                } else if ((proj instanceof ArrowEntity || proj instanceof SpectralArrowEntity) && arrows) {
                    shouldRender = true;
                    gravity = 0.05;
                } else if (proj instanceof FishingBobberEntity && fishing) {
                    shouldRender = true;
                    gravity = 0.04;
                } else if (proj instanceof FireworkRocketEntity && fireworks) {
                    shouldRender = true;
                    gravity = 0.03;
                } else if ((proj instanceof WindChargeEntity || proj instanceof BreezeWindChargeEntity) && expWind) {
                    shouldRender = true;
                }

                if (!shouldRender) continue;

                ItemStack stack = getItemStackForProjectile(proj);
                Vec3d origin = proj.getPos();
                Vec3d mot = proj.getVelocity();
                Vec3d pos = origin;
                int ticks = 0;

                for (int i = 0; i < maxTicks; i++) {
                    Vec3d prev = pos;
                    pos = pos.add(mot);

                    boolean inWater = mc.world.getBlockState(net.minecraft.util.math.BlockPos.ofFloored(pos)).getFluidState().isIn(net.minecraft.registry.tag.FluidTags.WATER);
                    mot = mot.multiply(inWater ? 0.8 : 0.99);
                    if (!proj.hasNoGravity()) {
                        mot = mot.subtract(0, gravity, 0);
                    }

                    Vec3d end = pos;
                    boolean hitBlock = false;

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
                            impactPoints.add(new ImpactPoint(end, ticks, stack));
                        }
                        break;
                    }
                    ticks++;
                }
            } else if (renderItems && entity instanceof ItemEntity item && item.getVelocity().lengthSquared() > 0.01) {
                ItemStack stack = item.getStack();

                Vec3d origin = item.getPos();
                Vec3d mot = item.getVelocity();
                Vec3d pos = origin;
                int ticks = 0;

                for (int i = 0; i < maxTicks; i++) {
                    Vec3d prev = pos;
                    pos = pos.add(mot);
                    mot = mot.multiply(0.99);
                    mot = mot.subtract(0, 0.04, 0);

                    Vec3d end = pos;
                    boolean hitBlock = false;

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
                            impactPoints.add(new ImpactPoint(end, ticks, stack));
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

        Interface iface = Instance.get(Interface.class);
        if (iface == null) return;

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

        DrawContext ctx = e.getDrawContext();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();

        // Фон как у всех HUD элементов — тема на 25% яркости, альфа 135
        int bgColor = ColorProvider.rgba(
                ((t1 >> 16) & 0xFF) >> 2,
                ((t1 >> 8) & 0xFF) >> 2,
                (t1 & 0xFF) >> 2,
                135
        );

        // Орбитальный градиент для обводки
        int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, 255);
        int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp;

        float borderRadius = 3f;
        float textSize = 6f;
        float iconRenderSize = 8;
        float padX = 3;
        float padY = 2;
        float sep = 2;

        for (ImpactPoint pt : impactPoints) {
            Vector2f screen = projectToScreen(pt.position.x, pt.position.y - 0.3F, pt.position.z);
            if (screen.x == Float.MAX_VALUE) continue;

            double timeSec = pt.ticks * 50.0 / 1000.0;
            String text = String.format("%.1f сек.", timeSec);

            float textWidth = Fonts.SFBOLD.get().getWidth(text, textSize);
            float totalWidth = iconRenderSize + sep + textWidth + padX * 2;
            float totalHeight = iconRenderSize + padY * 2;
            float x = screen.x - totalWidth / 2;
            float y = screen.y;

            // Тень (самый нижний слой)
            DrawUtil.drawShadow(m, x, y, totalWidth, totalHeight, borderRadius, 10f, ColorProvider.rgba(0, 0, 0, 80));
            // Глоу (орбитальное свечение)
            iface.drawGlow(m, x, y, totalWidth, totalHeight, borderRadius, 1.0f);
            // Орбитальная обводка
            DrawUtil.drawRound(x - 0.5f, y - 0.5f, totalWidth + 1f, totalHeight + 1f, borderRadius, glow[0], glow[1], glow[2], glow[3]);
            // Тёмный фон
            DrawUtil.drawRound(x, y, totalWidth, totalHeight, borderRadius, bgColor);
            // Разделитель
            float sepX = x + iconRenderSize + padX + 0.5f;
            DrawUtil.drawRound(sepX, y + 2f, 0.5f, totalHeight - 4f, 0.2f, ColorProvider.rgba(120, 120, 120, 120));
            // Иконка предмета
            MatrixStack matrices = ctx.getMatrices();
            matrices.push();
            matrices.translate(x + padX, y + padY, 0);
            matrices.scale(0.7f, 0.7f, 1.0f);
            ctx.drawItem(pt.stack, 0, 0);
            matrices.pop();
            // Текст
            float textX = sepX + sep;
            float textY = y + (totalHeight - textSize) * 0.5f;
            DrawUtil.drawText(Fonts.SFBOLD.get(), text, textX, textY, ColorProvider.rgba(233, 233, 233, 255), textSize);
        }
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
}
