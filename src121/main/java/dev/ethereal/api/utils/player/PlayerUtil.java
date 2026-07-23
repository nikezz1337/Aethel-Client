package dev.ethereal.api.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import dev.ethereal.api.system.interfaces.QuickImports;
import net.minecraft.world.World;

import java.util.*;
import java.util.regex.Pattern;

@UtilityClass
public class PlayerUtil implements QuickImports {

    public float ftAn;

    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");

    public boolean isEating() {
        return mc.player.isUsingItem();
    }

    public boolean canSee(Vec3d to) {
        HitResult hitResult = mc.world.raycast(new RaycastContext(mc.getCameraEntity().getEyePos(), to, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.getCameraEntity()));
        return hitResult == null || hitResult.getType() == HitResult.Type.MISS;
    }

    public boolean isAboveWater() {
        if (mc.player == null) return false;
        if (mc.world == null) return false;

        return mc.player.isSubmergedInWater() || mc.world.getBlockState(mc.player.getBlockPos().add(0, (int) (-0.5), 0)).getBlock() == Blocks.WATER;
    }

    public boolean isInWeb() {
        if (mc.player == null) return false;
        Box playerBox = mc.player.getBoundingBox();
        BlockPos playerPosition = mc.player.getBlockPos();

        return getNearbyBlockPositions(playerPosition).stream().anyMatch(pos -> isBlockCobweb(playerBox, pos));
    }

    private boolean isBlockCobweb(Box playerBox, BlockPos blockPos) {
        return playerBox.intersects(new Box(blockPos)) && mc.world != null && mc.world.getBlockState(blockPos).getBlock() == Blocks.COBWEB;
    }

    public List<BlockPos> getNearbyBlockPositions(BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = (center.getX() - 2); x <= (center.getX() + 2); x++) {
            for (int y = (center.getY() - 1); y <= (center.getY() + 4); y++) {
                for (int z = (center.getZ() - 2); z <= (center.getZ() + 2); z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }

    public Block getBlock(float x, float y, float z) {
        Vec3d pos = mc.player.getPos();
        return mc.world.getBlockState(new BlockPos(new Vec3i((int) (pos.x + x), (int) (pos.y + y), (int) (pos.z + z)))).getBlock();
    }

    public boolean hasCollisionWith(Entity entity) {
        return hasCollisionWith(entity, 0f);
    }

    public boolean hasCollisionWith(Entity entity, float expand) {
        Box box = mc.player.getBoundingBox();
        Box targetbox = entity.getBoundingBox().expand(expand, 0, expand);

        return box.maxX > targetbox.minX
                && box.maxY > targetbox.minY
                && box.maxZ > targetbox.minZ
                && box.minX < targetbox.maxX
                && box.minY < targetbox.maxY
                && box.minZ < targetbox.maxZ;
    }

    public boolean isValidName(String name) {
        return namePattern.matcher(name).matches();
    }

    public boolean isHW() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
        var serverInfo = mc.getNetworkHandler().getServerInfo();
        String brand = mc.getNetworkHandler().getBrand();
        String serverIp = serverInfo != null ? serverInfo.address.toLowerCase() : "";
        if (brand == null) return false;
        brand = brand.toLowerCase();
        return serverIp.contains("holyworld") && !serverIp.contains("funtime") || brand.contains("holyworld") && !serverIp.contains("funtime");
    }

    public boolean isFT() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
        var serverInfo = mc.getNetworkHandler().getServerInfo();
        String brand = mc.getNetworkHandler().getBrand();
        if (brand == null) return false;
        brand = brand.toLowerCase();
        String serverIp = serverInfo != null ? serverInfo.address.toLowerCase() : "";
        return brand.contains("botfilter") || serverIp.contains("funtime");
    }

    public boolean isST() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
        var serverInfo = mc.getNetworkHandler().getServerInfo();
        String serverIp = serverInfo != null ? serverInfo.address.toLowerCase() : "";
        return serverIp.contains("spookytime");
    }

    public boolean isRW() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
        var serverInfo = mc.getNetworkHandler().getServerInfo();
        String serverIp = serverInfo != null ? serverInfo.address.toLowerCase() : "";
        return serverIp.contains("reallyworld");
    }

    public boolean isFS() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
        var serverInfo = mc.getNetworkHandler().getServerInfo();
        String serverIp = serverInfo != null ? serverInfo.address.toLowerCase() : "";
        return serverIp.contains("funsky");
    }

    public boolean isCopy() {
        return PlayerUtil.isFT() || PlayerUtil.isST() || PlayerUtil.isFS();
    }

    public boolean isFTRepl() {
        if (mc == null || mc.inGameHud == null) return false;

        BossBarHud bossOverlayGui = mc.inGameHud.getBossBarHud();
        Map<UUID, ClientBossBar> bossBars = bossOverlayGui.bossBars;

        for (ClientBossBar bossInfo : bossBars.values()) {
            String name = bossInfo.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains("fun") || name.contains("time") || name.contains("funtime") || name.contains("фан") || name.contains("тайм") || name.contains("фантайм")) {
                name = name.replaceAll("(?i)fun", "Von").replaceAll("(?i)time", "Tam");
                bossInfo.setName(Text.of(name));
                return true;
            }

        }
        return false;
    }
}
