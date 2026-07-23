package dev.ethereal.client.features.modules.player;

import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.color.UIColors;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ModuleRegister(name = "ItemTracker", category = Category.PLAYER)
@Environment(EnvType.CLIENT)
public class ItemsTracker extends Module {
    @Getter private static final ItemsTracker instance = new ItemsTracker();

    private final BooleanSetting trackTotem = new BooleanSetting("Снос тотема").value(true);
    private final BooleanSetting trackPotions = new BooleanSetting("Полученные зелья").value(true);
    private final BooleanSetting trackConsume = new BooleanSetting("Съеденный предмет").value(true);
    private final SliderSetting radius = new SliderSetting("Радиус зелий").value(100f).range(10f, 100f).step(1f);

    private static class PotionData {
        ItemStack stack;
        double lastX, lastY, lastZ;
        PotionData(ItemStack stack, double x, double y, double z) {
            this.stack = stack; this.lastX = x; this.lastY = y; this.lastZ = z;
        }
    }
    private final Map<Integer, PotionData> trackedPotions = new HashMap<>();
    private final Map<UUID, ItemStack> activeUseItem = new HashMap<>();
    private final Map<UUID, Integer> useStartTick = new HashMap<>();

    public ItemsTracker() {
        addSettings(trackTotem, trackPotions, trackConsume, radius);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        handleConsume();
        handlePotions();
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!event.isReceive() || mc.world == null || mc.player == null) return;
        if (trackTotem.getValue() && event.packet() instanceof EntityStatusS2CPacket pkt && pkt.getStatus() == 35) {
            handleTotemPop(pkt);
        }
    }

    private void handleConsume() {
        if (!trackConsume.getValue()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getUuid().equals(mc.player.getUuid())) continue;
            UUID id = player.getUuid();

            if (player.isUsingItem()) {
                if (!activeUseItem.containsKey(id)) {
                    activeUseItem.put(id, player.getActiveItem().copy());
                    useStartTick.put(id, player.age);
                }
            } else {
                ItemStack used = activeUseItem.remove(id);
                Integer startTick = useStartTick.remove(id);
                if (used == null || used.isEmpty() || startTick == null) continue;
                if (player.age - startTick < 31) continue;

                UseAction action = used.getUseAction();
                String verb = null;
                if (action == UseAction.DRINK) verb = "выпил";
                else if (action == UseAction.EAT) verb = "съел";
                if (verb == null) continue;

                String name = used.getName().getString().replaceAll("§.", "");
                sendMsg(Text.literal(player.getName().getString()).formatted(Formatting.WHITE)
                        .append(Text.literal(" " + verb + " ").formatted(Formatting.GRAY))
                        .append(Text.literal(name).formatted(Formatting.WHITE)));
            }
        }
    }

    private void handlePotions() {
        if (!trackPotions.getValue()) return;

        Set<Integer> current = new HashSet<>();
        float r = radius.getValue();

        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof PotionEntity potion)) continue;
            if (mc.player.distanceTo(potion) > r) continue;

            int eid = potion.getId();
            current.add(eid);

            if (!trackedPotions.containsKey(eid)) {
                trackedPotions.put(eid, new PotionData(potion.getStack().copy(), potion.getX(), potion.getY(), potion.getZ()));
            } else {
                PotionData d = trackedPotions.get(eid);
                d.lastX = potion.getX();
                d.lastY = potion.getY();
                d.lastZ = potion.getZ();
            }
        }

        Set<Integer> removed = new HashSet<>(trackedPotions.keySet());
        removed.removeAll(current);

        for (int eid : removed) {
            PotionData data = trackedPotions.remove(eid);
            if (data == null) continue;

            Box hitBox = new Box(
                    data.lastX - 4, data.lastY - 2, data.lastZ - 4,
                    data.lastX + 4, data.lastY + 2, data.lastZ + 4
            );

            for (LivingEntity hit : mc.world.getEntitiesByClass(LivingEntity.class, hitBox, e2 -> true)) {
                if (!(hit instanceof PlayerEntity player)) continue;
                double dx = player.getX() - data.lastX;
                double dz = player.getZ() - data.lastZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 4.0) continue;

                String playerName = player.getName().getString();
                double hitChance = Math.max(0, 1.0 - dist / 4.0) * 100.0;
                Formatting hitColor = hitChance >= 65 ? Formatting.GREEN
                        : hitChance >= 35 ? Formatting.YELLOW
                        : Formatting.RED;

                sendMsg(Text.literal(playerName).formatted(Formatting.WHITE)
                        .append(Text.literal(" получил ").formatted(Formatting.GRAY))
                        .append(data.stack.getName())
                        .append(Text.literal(" (").formatted(Formatting.GRAY))
                        .append(Text.literal(String.format("%.0f%%", hitChance)).formatted(hitColor))
                        .append(Text.literal(")").formatted(Formatting.GRAY)));
            }
        }
    }

    private void handleTotemPop(EntityStatusS2CPacket pkt) {
        var entity = pkt.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player)) return;
        if (player.getUuid().equals(mc.player.getUuid())) return;

        boolean isEnchanted = EnchantmentHelper.hasEnchantments(player.getOffHandStack())
                || EnchantmentHelper.hasEnchantments(player.getMainHandStack());

        ItemStack totemStack = player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                ? player.getOffHandStack()
                : player.getMainHandStack();

        Text totemName = totemStack.isEmpty()
                ? Text.literal("Тотем бессмертия")
                : totemStack.getName();

        sendMsg(Text.literal(player.getName().getString()).formatted(Formatting.WHITE)
                .append(Text.literal(" потерял ").formatted(Formatting.GRAY))
                .append(totemName)
                .append(Text.literal(" (").formatted(Formatting.GRAY))
                .append(Text.literal(isEnchanted ? "зачарованный" : "незачарованный")
                        .formatted(isEnchanted ? Formatting.GREEN : Formatting.RED))
                .append(Text.literal(")").formatted(Formatting.GRAY)));
    }

    private void sendMsg(Text content) {
        if (mc.player == null) return;
        int rgb = UIColors.primary().getRGB() & 0xFFFFFF;

        MutableText msg = Text.empty()
                .append(Text.literal("Ethereal").styled(s -> s.withBold(true).withColor(rgb)))
                .append(Text.literal(" » ").formatted(Formatting.GRAY))
                .append(content);

        mc.player.sendMessage(msg, false);
    }

    @Override
    public void onDisable() {
        trackedPotions.clear();
        activeUseItem.clear();
        useStartTick.clear();
        super.onDisable();
    }
}
