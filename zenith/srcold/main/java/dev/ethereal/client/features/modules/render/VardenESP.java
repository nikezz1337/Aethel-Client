package dev.ethereal.client.features.modules.render;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.math.ProjectionUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Font;
import dev.ethereal.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "WardenESP", category = Category.RENDER)
public class VardenESP extends Module implements QuickImports {
    @Getter private static final VardenESP instance = new VardenESP();

    private final SliderSetting scale = new SliderSetting("Scale").value(1f).range(0.5f, 2f).step(0.1f);

    private final Map<BlockPos, ChestTimer> trackedChests = new HashMap<>();
    private long lastScanTime = 0;
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})");

    private static class ChestTimer {
        BlockPos pos;
        long endTime;
        boolean isOpen;
        
        ChestTimer(BlockPos pos, long endTime) {
            this.pos = pos;
            this.endTime = endTime;
            this.isOpen = false;
        }
        
        void updateTimer(long newEndTime) {
            this.endTime = newEndTime;
            this.isOpen = false;
        }
        
        void markOpen() {
            this.isOpen = true;
        }
        
        long getRemainingTime() {
            if (isOpen) return 0;
            long remaining = endTime - System.currentTimeMillis();
            return Math.max(0, remaining);
        }
        
        String getDisplayText() {
            if (isOpen && getRemainingTime() == 0) {
                return "Открыт!";
            }
            
            long remaining = getRemainingTime();
            if (remaining == 0) {
                return "Открыт!";
            }
            
            long seconds = remaining / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public VardenESP() {
        addSettings(scale);
    }

    @Override
    public void onDisable() {
        trackedChests.clear();
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScanTime > 100) {
                scanForChestsAndHolograms();
                lastScanTime = currentTime;
            }

            renderChests(event.context());
        }));

        addEvents(renderEvent);
    }

    private void scanForChestsAndHolograms() {
        if (mc.world == null || mc.player == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        float range = 200f;
        int rangeInt = (int) range;
        
        int chunkRange = (rangeInt >> 4) + 1;
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        trackedChests.entrySet().removeIf(entry -> !entry.getKey().isWithinDistance(playerPos, range));

        Set<BlockPos> foundChests = new HashSet<>();

        for (int cx = -chunkRange; cx <= chunkRange; cx++) {
            for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                int chunkX = playerChunkX + cx;
                int chunkZ = playerChunkZ + cz;
                
                if (!mc.world.isChunkLoaded(chunkX, chunkZ)) continue;
                
                Iterable<BlockEntity> blockEntities = mc.world.getChunk(chunkX, chunkZ).getBlockEntities().values();
                
                for (BlockEntity blockEntity : blockEntities) {
                    BlockPos pos = blockEntity.getPos();
                    if (!pos.isWithinDistance(playerPos, range)) continue;
                    
                    Block block = mc.world.getBlockState(pos).getBlock();
                    
                    if (block == Blocks.CHEST && blockEntity instanceof ChestBlockEntity) {
                        BlockPos immutable = pos.toImmutable();
                        foundChests.add(immutable);
                        
                        Long timerValue = findHologramTimer(immutable);
                        
                        if (timerValue != null) {
                            if (trackedChests.containsKey(immutable)) {
                                trackedChests.get(immutable).updateTimer(timerValue);
                            } else {
                                trackedChests.put(immutable, new ChestTimer(immutable, timerValue));
                            }
                        }
                    }
                }
            }
        }
    }

    private Long findHologramTimer(BlockPos chestPos) {
        if (mc.world == null) return null;
        
        Box searchBox = new Box(
            chestPos.getX() - 1, chestPos.getY(), chestPos.getZ() - 1,
            chestPos.getX() + 2, chestPos.getY() + 3, chestPos.getZ() + 2
        );
        
        for (Entity entity : mc.world.getEntitiesByClass(ArmorStandEntity.class, searchBox, e -> true)) {
            if (entity instanceof ArmorStandEntity armorStand) {
                if (!armorStand.hasCustomName()) continue;
                
                Text customName = armorStand.getCustomName();
                if (customName == null) continue;
                
                String nameText = customName.getString();
                Matcher matcher = TIME_PATTERN.matcher(nameText);
                
                if (matcher.find()) {
                    try {
                        int minutes = Integer.parseInt(matcher.group(1));
                        int seconds = Integer.parseInt(matcher.group(2));
                        long totalMillis = (minutes * 60L + seconds) * 1000L;
                        return System.currentTimeMillis() + totalMillis;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        
        return null;
    }

    private void renderChests(DrawContext context) {
        MatrixStack matrixStack = context.getMatrices();
        Font font = Fonts.SF_MEDIUM;
        float scale = this.scale.getValue();
        float size = 8f * scale;
        float gap = 2f * scale;

        for (ChestTimer timer : trackedChests.values()) {
            BlockPos pos = timer.pos;
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.5;
            double z = pos.getZ() + 0.5;

            Vector2f projected = ProjectionUtil.project(new Vec3d(x, y, z));
            
            if (projected == null) continue;

            float screenX = projected.x;
            float screenY = projected.y;

            if (screenX < 0 || screenX > mc.getWindow().getScaledWidth() || 
                screenY < 0 || screenY > mc.getWindow().getScaledHeight()) {
                continue;
            }

            String displayText = timer.getDisplayText();

            float textWidth = font.getWidth(displayText, size);
            float textX = screenX - textWidth / 2f;
            float textY = screenY;

            Color bgColor = timer.getRemainingTime() == 0 ? 
                new Color(50, 255, 50) : new Color(255, 100, 100);

            RenderUtil.BLUR_RECT.draw(matrixStack, textX - gap, textY - gap, 
                textWidth + gap * 2f, size + gap * 2f, scale, bgColor, 0.7f);

            font.drawText(matrixStack, displayText, textX, textY, size, new Color(255, 255, 255));
        }
    }
}
