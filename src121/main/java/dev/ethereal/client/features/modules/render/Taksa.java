package dev.ethereal.client.features.modules.render;

import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.render.taksa.TaksaBrain;
import dev.ethereal.api.utils.render.taksa.TaksaModel;
import lombok.Getter;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

@ModuleRegister(name = "Taksa", category = Category.RENDER)
public class Taksa extends Module {
    @Getter private static final Taksa instance = new Taksa();

    private static final Identifier TEXTURE = Identifier.of("ethereal", "textures/taksa.png");
    
    private TaksaModel model;
    private final TaksaBrain brain = new TaksaBrain();
    private int ticksExisted = 0;

    @Override
    public void onEnable() {
        if (model == null) {
            model = new TaksaModel(TaksaModel.getTexturedModelData().createModel());
        }
        ticksExisted = 0;
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        brain.setEntity(mc.player);
        brain.update();
        ticksExisted++;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null || model == null) return;

        MatrixStack matrices = event.matrixStack();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        float partialTicks = mc.getRenderTickCounter().getTickDelta(false);

        renderTaksa(matrices, camera, brain, partialTicks);
    }
    
    private void renderTaksa(MatrixStack matrices, Vec3d camera, TaksaBrain taksaBrain, float partialTicks) {
        Vec3d taksaPos = taksaBrain.getPos(partialTicks);
        if (taksaPos == null || (taksaPos.x == 0 && taksaPos.y == 0 && taksaPos.z == 0)) return;
        
        matrices.push();
        matrices.translate(
            taksaPos.x - camera.x,
            taksaPos.y - camera.y,
            taksaPos.z - camera.z
        );
        
        model.setAngles(ticksExisted, taksaBrain);
        
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vertices = immediate.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        
        int light = LightmapTextureManager.pack(15, 15);
        int overlay = OverlayTexture.DEFAULT_UV;
        
        model.render(matrices, vertices, light, overlay, taksaBrain);
        
        immediate.draw();
        
        matrices.pop();
    }
}
