package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.event.list.Event3DRender;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.module.settings.MultiBooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.util.render.ShaderUtil;
import dev.aethel.util.render.providers.ColorProvider;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(
        moduleName = "Block Overlay",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Renders a custom block overlay"
)
public class BlockOverlay extends Module {

    private final SliderSetting lineWidth = new SliderSetting("Line Width", 2.5F, 0.5F, 5.0F, 0.1F);
    private final SliderSetting fillAlpha = new SliderSetting("Fill Alpha", 0.6F, 0.0F, 1.0F, 0.01F);
    private final SliderSetting smooth = new SliderSetting("Smooth", 0.6F, 0.05F, 1.0F, 0.01F);
    private final SliderSetting shaderSpeed = new SliderSetting("Shader Speed", 1.2F, 0.1F, 5.0F, 0.1F);
    private final SliderSetting shaderScale = new SliderSetting("Shader Scale", 8.0F, 1.0F, 40.0F, 0.5F);

    private BlockPos currentBlock;
    private List<Box> renderBoxes;
    private float overlayAlpha;
    private long startMillis = -1L;

    @Override
    public void onDisable() {
        currentBlock = null;
        renderBoxes = null;
        overlayAlpha = 0.0F;
        startMillis = -1L;
        super.onDisable();
    }

    @Subscribe
    public void onRender(Event3DRender e) {
        if (mc == null || mc.world == null || mc.player == null) return;

        BlockHitResult result = getOutlineHitResult();
        List<Box> worldBoxes = null;

        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = result.getBlockPos();
            if (pos != null && !mc.world.getBlockState(pos).isAir()) {
                var state = mc.world.getBlockState(pos);
                VoxelShape shape = state.getOutlineShape(mc.world, pos);
                if (shape == null || shape.isEmpty()) {
                    shape = state.getCollisionShape(mc.world, pos);
                }
                if (shape != null && !shape.isEmpty()) {
                    List<Box> boxes = shape.getBoundingBoxes();
                    if (!boxes.isEmpty()) {
                        worldBoxes = new ArrayList<>();
                        double exp = 0.002;
                        for (Box b : boxes) {
                            worldBoxes.add(b.offset(pos).expand(exp));
                        }
                    }
                }
            }

            if (worldBoxes != null) {
                if (currentBlock == null || !pos.equals(currentBlock)) {
                    currentBlock = pos;
                }
            }
        }

        if (worldBoxes != null) {
            renderBoxes = worldBoxes;
            overlayAlpha = lerpValue(overlayAlpha, 1.0F, Math.min(1.0F, smooth.getFloatValue() * 1.35F));
        } else {
            overlayAlpha = lerpValue(overlayAlpha, 0.0F, 0.18F);
            if (overlayAlpha <= 0.02F || renderBoxes == null) {
                currentBlock = null;
                renderBoxes = null;
                overlayAlpha = 0.0F;
                return;
            }
        }

        Vec3d cam = e.getCamera().getPos();
        int color = ColorProvider.getThemeColor();
        float a = fillAlpha.getFloatValue() * overlayAlpha;
        if (a <= 0.01F) return;

        for (Box box : renderBoxes) {
            Box localBox = box.offset(-cam.x, -cam.y, -cam.z);
            drawShaderEdgeBox(e.getMatrixStack(), localBox, color, a);
        }
    }

    private void drawFillBox(MatrixStack ms, Box box, int color, float alpha) {
        if (startMillis < 0L) startMillis = System.currentTimeMillis();
        float time = (System.currentTimeMillis() - startMillis) / 1000.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        float r2 = (float) Math.max(0, Math.min(255, ((color >> 16) & 0xFF) + 60)) / 255.0F;
        float g2 = (float) Math.max(0, Math.min(255, ((color >> 8) & 0xFF) - 20)) / 255.0F;
        float b2 = (float) Math.max(0, Math.min(255, (color & 0xFF) + 40)) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        ShaderProgram shader = RenderSystem.setShader(ShaderUtil.blockOverlayFill);
        if (shader != null) {
            setUniform(shader, "color", r, g, b);
            setUniform(shader, "color2", r2, g2, b2);
            setUniform(shader, "time", time);
            setUniform(shader, "speed", shaderSpeed.getFloatValue());
            setUniform(shader, "alpha", alpha);
        }

        Matrix4f matrix = ms.peek().getPositionMatrix();
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
        int ia = (int) (alpha * 255.0F);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        buf.vertex(matrix, x1, y1, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1, z1).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y1, z2).texture(0, 1).color(r, g, b, ia);

        buf.vertex(matrix, x1, y2, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2, z1).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2, z2).texture(0, 1).color(r, g, b, ia);

        buf.vertex(matrix, x1, y1, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2, z1).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y1, z2).texture(0, 1).color(r, g, b, ia);

        buf.vertex(matrix, x2, y1, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2, z1).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1, z2).texture(0, 1).color(r, g, b, ia);

        buf.vertex(matrix, x1, y1, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1, z1).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2, z1).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2, z1).texture(0, 1).color(r, g, b, ia);

        buf.vertex(matrix, x1, y1, z2).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1, z2).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2, z2).texture(0, 1).color(r, g, b, ia);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawShaderEdgeBox(MatrixStack ms, Box box, int color, float alpha) {
        if (startMillis < 0L) startMillis = System.currentTimeMillis();
        float time = (System.currentTimeMillis() - startMillis) / 1000.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        float r2 = (float) Math.max(0, Math.min(255, ((color >> 16) & 0xFF) + 60)) / 255.0F;
        float g2 = (float) Math.max(0, Math.min(255, ((color >> 8) & 0xFF) - 20)) / 255.0F;
        float b2 = (float) Math.max(0, Math.min(255, (color & 0xFF) + 40)) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        ShaderProgram shader = RenderSystem.setShader(ShaderUtil.blockOverlayEdge);
        if (shader != null) {
            setUniform(shader, "color", r, g, b);
            setUniform(shader, "color2", r2, g2, b2);
            setUniform(shader, "time", time);
            setUniform(shader, "speed", shaderSpeed.getFloatValue());
            setUniform(shader, "edgeWidth", lineWidth.getFloatValue());
            setUniform(shader, "alpha", alpha);
        }

        Matrix4f matrix = ms.peek().getPositionMatrix();
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
        float w = lineWidth.getFloatValue() * 0.02F;
        int ia = (int) (alpha * 255.0F);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        buf.vertex(matrix, x1, y1 - w, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1 - w, z1).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1 + w, z1).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y1 + w, z1).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y1 - w, z2).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1 - w, z2).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1 + w, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y1 + w, z2).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y1 - w, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x1, y1 - w, z2).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x1, y1 + w, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y1 + w, z1).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1 - w, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1 - w, z2).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1 + w, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x2, y1 + w, z1).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2 - w, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2 - w, z1).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2 + w, z1).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2 + w, z1).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2 - w, z2).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2 - w, z2).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2 + w, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2 + w, z2).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2 - w, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2 - w, z2).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2 + w, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1, y2 + w, z1).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2 - w, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2 - w, z2).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2 + w, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x2, y2 + w, z1).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1 - w, y1, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x1 - w, y2, z1).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x1 + w, y2, z1).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1 + w, y1, z1).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x2 - w, y1, z1).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2 - w, y2, z1).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2 + w, y2, z1).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x2 + w, y1, z1).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1 - w, y1, z2).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x1 - w, y2, z2).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x1 + w, y2, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x1 + w, y1, z2).texture(0, 1).color(r, g, b, ia);
        buf.vertex(matrix, x2 - w, y1, z2).texture(0, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2 - w, y2, z2).texture(1, 0).color(r, g, b, ia);
        buf.vertex(matrix, x2 + w, y2, z2).texture(1, 1).color(r, g, b, ia);
        buf.vertex(matrix, x2 + w, y1, z2).texture(0, 1).color(r, g, b, ia);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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

    private BlockHitResult getOutlineHitResult() {
        if (mc.player == null || mc.world == null) return null;

        Vec3d start = mc.player.getCameraPosVec(1.0F);
        Vec3d direction = mc.player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(6.0));

        HitResult outlineHit = mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return outlineHit instanceof BlockHitResult blockHit ? blockHit : null;
    }

    private float lerpValue(float current, float target, float speed) {
        return current + (target - current) * speed;
    }
}
