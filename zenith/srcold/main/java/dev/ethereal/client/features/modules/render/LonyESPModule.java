package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ColorSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.interfaces.QuickImports;

import java.awt.*;
import java.util.*;
import java.util.List;

@ModuleRegister(name = "Lony ESP", category = Category.RENDER)
public class LonyESPModule extends Module implements QuickImports {
    @Getter private static final LonyESPModule instance = new LonyESPModule();

    private final ColorSetting color = new ColorSetting("Color").value(new Color(255, 100, 255));
    private final SliderSetting range = new SliderSetting("Range").value(50f).range(10f, 100f).step(5f);
    private final BooleanSetting fill = new BooleanSetting("Fill").value(true);
    private final SliderSetting fillAlpha = new SliderSetting("Fill Alpha").value(0.3f).range(0.1f, 1f).step(0.05f).setVisible(fill::getValue);
    private final BooleanSetting outline = new BooleanSetting("Outline").value(true);
    private final SliderSetting lineWidth = new SliderSetting("Line Width").value(2f).range(1f, 5f).step(0.5f).setVisible(outline::getValue);
    private final BooleanSetting chests = new BooleanSetting("Chests").value(true);

    private static final Set<Block> SHULKER_BOXES = new HashSet<>();
    private final List<BlockPos> cachedShulkers = new ArrayList<>();
    private final List<BlockPos> cachedChests = new ArrayList<>();
    private long lastScanTime = 0;
    private static final long SCAN_INTERVAL = 500; // Сканировать каждые 500ms

    static {
        SHULKER_BOXES.add(Blocks.SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.WHITE_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.LIGHT_GRAY_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.GRAY_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.BLACK_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.BROWN_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.RED_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.ORANGE_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.YELLOW_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.LIME_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.GREEN_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.CYAN_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.LIGHT_BLUE_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.BLUE_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.PURPLE_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.MAGENTA_SHULKER_BOX);
        SHULKER_BOXES.add(Blocks.PINK_SHULKER_BOX);
    }

    public LonyESPModule() {
        addSettings(color, range, fill, fillAlpha, outline, lineWidth, chests);
    }

    @Override
    public void onDisable() {
        cachedShulkers.clear();
        cachedChests.clear();
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScanTime > SCAN_INTERVAL) {
                scanForShulkers();
                if (chests.getValue()) {
                    scanForChests();
                }
                lastScanTime = currentTime;
            }

            if (cachedShulkers.isEmpty() && cachedChests.isEmpty()) return;

            MatrixStack ms = event.matrixStack();
            
            ms.push();
            
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

            for (BlockPos pos : cachedShulkers) {
                renderBox(ms, pos);
            }
            
            for (BlockPos pos : cachedChests) {
                renderBox(ms, pos);
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            
            ms.pop();
        }));

        addEvents(renderEvent);
    }

    private void scanForShulkers() {
        cachedShulkers.clear();
        if (mc.world == null || mc.player == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        float rangeFloat = range.getValue();
        int rangeInt = (int) rangeFloat;
        
        // Оптимизированный поиск: проверяем только чанки в радиусе
        int chunkRange = (rangeInt >> 4) + 1;
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        for (int cx = -chunkRange; cx <= chunkRange; cx++) {
            for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                int chunkX = playerChunkX + cx;
                int chunkZ = playerChunkZ + cz;
                
                // Проверяем загружен ли чанк
                if (!mc.world.isChunkLoaded(chunkX, chunkZ)) continue;
                
                // Получаем BlockEntity из чанка
                Iterable<BlockEntity> blockEntities = mc.world.getChunk(chunkX, chunkZ).getBlockEntities().values();
                
                for (BlockEntity blockEntity : blockEntities) {
                    if (!(blockEntity instanceof ShulkerBoxBlockEntity)) continue;
                    
                    BlockPos pos = blockEntity.getPos();
                    if (!pos.isWithinDistance(playerPos, rangeFloat)) continue;
                    
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (SHULKER_BOXES.contains(block)) {
                        cachedShulkers.add(pos.toImmutable());
                    }
                }
            }
        }
    }

    private void scanForChests() {
        cachedChests.clear();
        if (mc.world == null || mc.player == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        float rangeFloat = range.getValue();
        int rangeInt = (int) rangeFloat;
        
        int chunkRange = (rangeInt >> 4) + 1;
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        for (int cx = -chunkRange; cx <= chunkRange; cx++) {
            for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                int chunkX = playerChunkX + cx;
                int chunkZ = playerChunkZ + cz;
                
                if (!mc.world.isChunkLoaded(chunkX, chunkZ)) continue;
                
                Iterable<BlockEntity> blockEntities = mc.world.getChunk(chunkX, chunkZ).getBlockEntities().values();
                
                for (BlockEntity blockEntity : blockEntities) {
                    BlockPos pos = blockEntity.getPos();
                    if (!pos.isWithinDistance(playerPos, rangeFloat)) continue;
                    
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST) {
                        cachedChests.add(pos.toImmutable());
                    }
                }
            }
        }
    }

    private void renderBox(MatrixStack ms, BlockPos pos) {
        Box box = new Box(pos);
        
        double camX = mc.getEntityRenderDispatcher().camera.getPos().x;
        double camY = mc.getEntityRenderDispatcher().camera.getPos().y;
        double camZ = mc.getEntityRenderDispatcher().camera.getPos().z;

        ms.push();
        ms.translate(-camX, -camY, -camZ);

        Matrix4f matrix = ms.peek().getPositionMatrix();
        Color c = color.getValue();

        if (fill.getValue()) {
            renderFill(matrix, box, c);
        }

        if (outline.getValue()) {
            renderOutline(matrix, box, c);
        }

        ms.pop();
    }

    private void renderFill(Matrix4f matrix, Box box, Color c) {
        int alpha = (int) (fillAlpha.getValue() * 255);
        Color fillColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        int r = fillColor.getRed();
        int g = fillColor.getGreen();
        int b = fillColor.getBlue();
        int a = fillColor.getAlpha();

        // Bottom
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z2).color(r, g, b, a);

        // Top
        buf.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        // Front
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).color(r, g, b, a);

        // Back
        buf.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        // Left
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).color(r, g, b, a);

        // Right
        buf.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void renderOutline(Matrix4f matrix, Box box, Color c) {
        RenderSystem.lineWidth(lineWidth.getValue());

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        int a = 255;

        // Bottom edges
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        
        buf.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        
        buf.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        
        buf.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);

        // Top edges
        buf.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).color(r, g, b, a);
        
        buf.vertex(matrix, x2, y2, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).color(r, g, b, a);
        
        buf.vertex(matrix, x1, y2, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z1).color(r, g, b, a);

        // Vertical edges
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        
        buf.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z1).color(r, g, b, a);
        
        buf.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        
        buf.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        buf.vertex(matrix, x1, y2, z2).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}
