package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.configs.FriendManager;
import dev.ethereal.api.utils.color.UIColors;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@ModuleRegister(name = "Chams", category = Category.RENDER)
public class ChamsModule extends Module {
    @Getter private static final ChamsModule instance = new ChamsModule();

    private final ModeSetting targets = new ModeSetting("Цели")
            .value("Все").values("Все", "Враги", "Друзья");

    private final BooleanSetting useThemeColor = new BooleanSetting("Цвет темы").value(true);
    private final SliderSetting colorR = new SliderSetting("R").value(255f).range(0f, 255f).step(1f)
            .setVisible(() -> !useThemeColor.getValue());
    private final SliderSetting colorG = new SliderSetting("G").value(100f).range(0f, 255f).step(1f)
            .setVisible(() -> !useThemeColor.getValue());
    private final SliderSetting colorB = new SliderSetting("B").value(100f).range(0f, 255f).step(1f)
            .setVisible(() -> !useThemeColor.getValue());
    private final SliderSetting alpha = new SliderSetting("Прозрачность").value(160f).range(10f, 255f).step(5f);
    private final SliderSetting fillAlpha = new SliderSetting("Прозрачность заливки").value(50f).range(5f, 200f).step(5f);
    private final SliderSetting lineWidth = new SliderSetting("Толщина линий").value(1.5f).range(0.5f, 5f).step(0.5f);
    private final SliderSetting layers = new SliderSetting("Слои").value(1f).range(1f, 3f).step(1f);
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены").value(true);

    public ChamsModule() {
        addSettings(targets, useThemeColor, colorR, colorG, colorB, alpha, fillAlpha, lineWidth, layers, throughWalls);
    }

    @Override
    public void onEvent() {
        addEvents(Render3DEvent.getInstance().subscribe(new Listener<>(this::onRender3D)));
    }

    public boolean shouldRender(PlayerEntity player) {
        if (!isEnabled()) return false;
        if (player == mc.player && mc.options.getPerspective().isFirstPerson()) return false;
        if (player == null || !player.isAlive()) return false;

        String name = player.getName().getString();
        boolean isFriend = FriendManager.getInstance().contains(name);

        if (targets.is("Враги") && isFriend) return false;
        if (targets.is("Друзья") && !isFriend) return false;

        return true;
    }

    private void onRender3D(Render3DEvent.Render3DEventData event) {
        if (!isEnabled() || mc.world == null) return;

        float partialTicks = event.partialTicks();
        int numLayers = layers.getValue().intValue();
        boolean depth = !throughWalls.getValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldRender(player)) continue;

            String name = player.getName().getString();
            boolean isFriend = FriendManager.getInstance().contains(name);

            double x = MathHelper.lerp(partialTicks, player.prevX, player.getX());
            double y = MathHelper.lerp(partialTicks, player.prevY, player.getY());
            double z = MathHelper.lerp(partialTicks, player.prevZ, player.getZ());

            double width = player.getWidth() * 0.5;
            double height = player.getHeight();

            float lw = lineWidth.getValue();

            for (int i = 0; i < numLayers; i++) {
                float expand = i * 0.02f;
                float w = (float) width + expand;
                float h = (float) height + expand;

                int layerAlpha = Math.max(10, (int) (alpha.getValue() / (1f + i * 1.5f)));
                int layerFillAlpha = Math.max(5, (int) (fillAlpha.getValue() / (1f + i * 1.5f)));

                int lineCol = getColor(isFriend, layerAlpha).getRGB();
                int fillCol = getColor(isFriend, layerFillAlpha).getRGB();

                renderPlayerBox(event.matrixStack(), x, y, z, w, h, lineCol, fillCol, depth, lw);
            }
        }
    }

    private void renderPlayerBox(MatrixStack ms, double x, double y, double z, float w, float h, int lineCol, int fillCol, boolean depth, float lw) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        double camX = camera.getPos().x;
        double camY = camera.getPos().y;
        double camZ = camera.getPos().z;

        ms.push();
        ms.translate(x - camX, y - camY, z - camZ);

        float x1 = -w;
        float y1 = 0;
        float z1 = -w;
        float x2 = w;
        float y2 = h;
        float z2 = w;

        int la = (lineCol >> 24) & 0xFF, lr = (lineCol >> 16) & 0xFF,
                lg = (lineCol >> 8) & 0xFF, lb = lineCol & 0xFF;
        int fa = (fillCol >> 24) & 0xFF, fr = (fillCol >> 16) & 0xFF,
                fg = (fillCol >> 8) & 0xFF, fb = fillCol & 0xFF;

        Matrix4f mat = ms.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        if (!depth) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        }

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0f, -1.0f);
        RenderSystem.depthMask(false);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        fill.vertex(mat, x1, y1, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y1, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y1, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y1, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y2, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y2, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y2, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y2, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y1, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y2, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y2, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y1, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y1, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y1, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y2, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y2, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y1, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y1, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y2, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x1, y2, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y1, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y2, z1).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y2, z2).color(fr, fg, fb, fa);
        fill.vertex(mat, x2, y1, z2).color(fr, fg, fb, fa);
        BufferRenderer.drawWithGlobalProgram(fill.end());
        RenderSystem.depthMask(true);

        GL11.glPolygonOffset(-2.0f, -2.0f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(lw);

        BufferBuilder line = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        drawEdge(line, mat, x1,y1,z1, x2,y1,z1, lr,lg,lb,la);
        drawEdge(line, mat, x2,y1,z1, x2,y1,z2, lr,lg,lb,la);
        drawEdge(line, mat, x2,y1,z2, x1,y1,z2, lr,lg,lb,la);
        drawEdge(line, mat, x1,y1,z2, x1,y1,z1, lr,lg,lb,la);
        drawEdge(line, mat, x1,y2,z1, x2,y2,z1, lr,lg,lb,la);
        drawEdge(line, mat, x2,y2,z1, x2,y2,z2, lr,lg,lb,la);
        drawEdge(line, mat, x2,y2,z2, x1,y2,z2, lr,lg,lb,la);
        drawEdge(line, mat, x1,y2,z2, x1,y2,z1, lr,lg,lb,la);
        drawEdge(line, mat, x1,y1,z1, x1,y2,z1, lr,lg,lb,la);
        drawEdge(line, mat, x2,y1,z1, x2,y2,z1, lr,lg,lb,la);
        drawEdge(line, mat, x2,y1,z2, x2,y2,z2, lr,lg,lb,la);
        drawEdge(line, mat, x1,y1,z2, x1,y2,z2, lr,lg,lb,la);
        BufferRenderer.drawWithGlobalProgram(line.end());
        RenderSystem.lineWidth(1f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(0.0f, 0.0f);

        if (depth) RenderSystem.depthFunc(GL11.GL_LEQUAL);
        if (!depth) RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        ms.pop();
    }

    private void drawEdge(BufferBuilder buf, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a);
    }

    private Color getColor(boolean isFriend, int a) {
        if (isFriend) return new Color(0, 220, 80, a);
        if (useThemeColor.getValue())
            return UIColors.primary(a);
        return new Color(colorR.getValue().intValue(), colorG.getValue().intValue(), colorB.getValue().intValue(), a);
    }
}
