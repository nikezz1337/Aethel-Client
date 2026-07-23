package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.event.list.Event3DRender;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInformation(
    moduleName = "BlockESP",
    moduleCategory = ModuleCategory.RENDER,
    moduleDesc = "Подсветка блоков"
)
public class BlockESP extends Module {

    private static final float BOX_LINE_WIDTH = 2.0f;
    private static final float FILL_ALPHA = 0.18f;

    private static final long SCAN_INTERVAL_MS = 50L;
    private static final int MAX_CHUNKS_PER_PASS = 2;

    private static class BlockInfo {
        final BlockPos pos;
        final String name;
        final int color;

        public BlockInfo(BlockPos pos, String name, int color) {
            this.pos = pos;
            this.name = name;
            this.color = color;
        }
    }

    private int getBlockColor(BlockState state, BlockPos pos) {
        if (state == null || mc.world == null) return 0xFF00FF00;
        net.minecraft.block.MapColor mapColor = state.getMapColor(mc.world, pos);
        if (mapColor == null) return 0xFF00FF00;
        int r = mapColor.color >> 16 & 0xFF;
        int g = mapColor.color >> 8 & 0xFF;
        int b = mapColor.color & 0xFF;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private boolean blocksLoaded;

    public final SliderSetting distance = new SliderSetting("Дистанция", 60, 10, 120, 1);

    private final Set<String> trackedBlocks = ConcurrentHashMap.newKeySet();
    private final Map<BlockPos, BlockInfo> foundBlocks = new ConcurrentHashMap<>();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();

    private ChunkPos lastPlayerChunk;
    private int lastScanRadius = -1;
    private long lastScanTime;

    @Override
    public void onEnable() {
        super.onEnable();
        if (!blocksLoaded) {
            loadBlocks();
            blocksLoaded = true;
        }
    }

    @Subscribe
    public void onRender3D(Event3DRender event) {
        if (mc.world == null || mc.player == null || trackedBlocks.isEmpty()) return;

        int scanRadius = (int) distance.getValue();
        ChunkPos currentChunk = new ChunkPos(mc.player.getBlockPos());

        if (scanRadius != lastScanRadius) {
            resetScanState();
            lastScanRadius = scanRadius;
        }

        if (lastPlayerChunk == null || !lastPlayerChunk.equals(currentChunk)) {
            scannedChunks.clear();
            lastPlayerChunk = currentChunk;
        }

        long now = System.currentTimeMillis();
        if (now - lastScanTime >= SCAN_INTERVAL_MS) {
            scanNearbyBlocks(scanRadius);
            lastScanTime = now;
        }

        cleanupInvalidAndDistantBlocks(mc.player.getPos(), scanRadius);
        renderFoundBlocks(event);
    }

    private void scanNearbyBlocks(int scanRadius) {
        if (mc.world == null || mc.player == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        int chunkRange = (scanRadius >> 4) + 2;

        List<ChunkPos> candidates = new ArrayList<>();
        for (int cx = -chunkRange; cx <= chunkRange; cx++) {
            for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                ChunkPos chunkPos = new ChunkPos(playerChunkX + cx, playerChunkZ + cz);
                if (!scannedChunks.contains(chunkPos)) candidates.add(chunkPos);
            }
        }

        candidates.sort((a, b) -> {
            long da = chunkDistanceSq(a, playerChunkX, playerChunkZ);
            long db = chunkDistanceSq(b, playerChunkX, playerChunkZ);
            return Long.compare(da, db);
        });

        int scannedThisPass = 0;
        for (ChunkPos chunkPos : candidates) {
            if (scannedThisPass >= MAX_CHUNKS_PER_PASS) break;
            WorldChunk chunk = mc.world.getChunk(chunkPos.x, chunkPos.z);
            if (chunk == null) continue;
            scanChunk(chunk, playerPos, scanRadius);
            scannedChunks.add(chunkPos);
            scannedThisPass++;
        }
    }

    private void scanChunk(WorldChunk chunk, BlockPos playerPos, int scanRadius) {
        int minX = chunk.getPos().getStartX();
        int minZ = chunk.getPos().getStartZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int minY = Math.max(mc.world.getBottomY(), playerPos.getY() - scanRadius);
        int maxY = Math.min(mc.world.getTopYInclusive(), playerPos.getY() + scanRadius);
        int radiusSq = scanRadius * scanRadius;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    mutable.set(x, y, z);
                    if (mutable.getSquaredDistance(playerPos) > radiusSq) continue;
                    BlockState state = chunk.getBlockState(mutable);
                    if (state.isAir()) continue;
                    String blockName = Registries.BLOCK.getId(state.getBlock()).getPath().toLowerCase();
                    if (trackedBlocks.contains(blockName)) {
                        int color = getBlockColor(state, mutable);
                        foundBlocks.put(mutable.toImmutable(), new BlockInfo(mutable.toImmutable(), blockName, color));
                    }
                }
            }
        }
    }

    private void cleanupInvalidAndDistantBlocks(Vec3d playerPos, int renderDistance) {
        if (mc.world == null) {
            foundBlocks.clear();
            return;
        }
        int renderDistanceSq = renderDistance * renderDistance;
        foundBlocks.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            BlockState currentState = mc.world.getBlockState(pos);
            if (currentState.isAir()) return true;
            String currentBlockName = Registries.BLOCK.getId(currentState.getBlock()).getPath().toLowerCase();
            return !trackedBlocks.contains(currentBlockName) || pos.getSquaredDistance(playerPos) > renderDistanceSq;
        });
    }

    private void renderFoundBlocks(Event3DRender event) {
        if (foundBlocks.isEmpty()) return;

        Vec3d cam = event.getCamera().getPos();
        Matrix4f matrix = event.getMatrixStack().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (BlockInfo blockInfo : foundBlocks.values()) {
            int color = blockInfo.color;
            float r = (color >> 16) & 0xFF;
            float g = (color >> 8) & 0xFF;
            float b = color & 0xFF;
            addFilledBox(buffer, matrix, blockInfo.pos, cam, r / 255.0f, g / 255.0f, b / 255.0f, FILL_ALPHA);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        BufferBuilder lineBuffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (BlockInfo blockInfo : foundBlocks.values()) {
            int color = blockInfo.color;
            float r = (color >> 16) & 0xFF;
            float g = (color >> 8) & 0xFF;
            float b = color & 0xFF;
            addOutlinedBox(lineBuffer, matrix, blockInfo.pos, cam, r / 255.0f, g / 255.0f, b / 255.0f, 1.0f);
        }
        BufferRenderer.drawWithGlobalProgram(lineBuffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void addFilledBox(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, Vec3d cam, float r, float g, float b, float a) {
        float minX = (float) (pos.getX() - cam.x);
        float minY = (float) (pos.getY() - cam.y);
        float minZ = (float) (pos.getZ() - cam.z);
        float maxX = minX + 1.0f;
        float maxY = minY + 1.0f;
        float maxZ = minZ + 1.0f;

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
    }

    private void addOutlinedBox(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, Vec3d cam, float r, float g, float b, float a) {
        float minX = (float) (pos.getX() - cam.x);
        float minY = (float) (pos.getY() - cam.y);
        float minZ = (float) (pos.getZ() - cam.z);
        float maxX = minX + 1.0f;
        float maxY = minY + 1.0f;
        float maxZ = minZ + 1.0f;

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
    }

    private static final File BLOCKS_FILE = new File("Aethel/blocks.json");

    public void loadBlocks() {
        if (!BLOCKS_FILE.exists()) return;
        try (FileReader reader = new FileReader(BLOCKS_FILE)) {
            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseReader(reader).getAsJsonArray();
            trackedBlocks.clear();
            for (var el : arr) {
                trackedBlocks.add(el.getAsString());
            }
        } catch (Exception ignored) {}
    }

    public void saveBlocks() {
        try {
            BLOCKS_FILE.getParentFile().mkdirs();
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (String block : trackedBlocks) {
                arr.add(block);
            }
            try (FileWriter writer = new FileWriter(BLOCKS_FILE)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(arr, writer);
            }
        } catch (Exception ignored) {}
    }

    public void addBlock(String blockName) {
        trackedBlocks.add(blockName.toLowerCase());
        scannedChunks.clear();
        foundBlocks.clear();
        saveBlocks();
    }

    public void removeBlock(String blockName) {
        trackedBlocks.remove(blockName.toLowerCase());
        foundBlocks.entrySet().removeIf(entry -> entry.getValue().name.equalsIgnoreCase(blockName));
        saveBlocks();
    }

    public void clearBlocks() {
        trackedBlocks.clear();
        resetScanState();
        saveBlocks();
    }

    public Set<String> getTrackedBlocks() {
        return new HashSet<>(trackedBlocks);
    }

    public boolean isTracking(String blockName) {
        return trackedBlocks.contains(blockName.toLowerCase());
    }

    private long chunkDistanceSq(ChunkPos chunkPos, int playerChunkX, int playerChunkZ) {
        long dx = chunkPos.x - playerChunkX;
        long dz = chunkPos.z - playerChunkZ;
        return dx * dx + dz * dz;
    }

    private void resetScanState() {
        foundBlocks.clear();
        scannedChunks.clear();
        lastPlayerChunk = null;
        lastScanTime = 0L;
        lastScanRadius = -1;
    }
}
