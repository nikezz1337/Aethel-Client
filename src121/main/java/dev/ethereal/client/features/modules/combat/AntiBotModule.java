package dev.ethereal.client.features.modules.combat;

import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ModuleRegister(name = "Anti Bot", category = Category.COMBAT)
public class AntiBotModule extends Module {
    @Getter private static final AntiBotModule instance = new AntiBotModule();
    private static final Set<UUID> botSet = new HashSet<>();

    private final ModeSetting mode = new ModeSetting("Режим").value("Matrix").values("Matrix", "ReallyWorld", "UniAC");

    public AntiBotModule() {
        addSettings(mode);
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            boolean isBot = switch (mode.getValue()) {
                case "Matrix" -> checkMatrix(player);
                case "ReallyWorld" -> checkReallyWorld(player);
                case "UniAC" -> checkUniAC(player);
                default -> false;
            };

            if (isBot) {
                botSet.add(player.getUuid());
            } else {
                botSet.remove(player.getUuid());
            }
        }
    }

    private boolean checkMatrix(PlayerEntity player) {
        var inv = player.getInventory();
        for (int i = 0; i < 4; i++) {
            var stack = inv.getArmorStack(i);
            if (stack.isEmpty()) return false;
            if (!stack.isEnchantable()) return false;
            if (stack.isDamaged()) return false;
        }

        if (!player.getOffHandStack().isEmpty()) return false;
        if (player.getMainHandStack().isEmpty()) return false;

        boolean hasLeatherOrIron = false;
        for (int i = 0; i < 4; i++) {
            var stack = inv.getArmorStack(i);
            var item = stack.getItem();
            if (item == Items.LEATHER_HELMET || item == Items.LEATHER_CHESTPLATE ||
                item == Items.LEATHER_LEGGINGS || item == Items.LEATHER_BOOTS ||
                item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE ||
                item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS) {
                hasLeatherOrIron = true;
                break;
            }
        }
        if (!hasLeatherOrIron) return false;

        return player.getHungerManager().getFoodLevel() == 20;
    }

    private boolean checkReallyWorld(PlayerEntity player) {
        String name = player.getName().getString();
        UUID expected = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
        return !player.getUuid().equals(expected) && !name.contains("NPC") && !name.startsWith("[ZNPC]");
    }

    private boolean checkUniAC(PlayerEntity player) {
        var inv = player.getInventory();
        boolean[] nullSlot = new boolean[4];
        boolean[] nonNull = new boolean[4];
        boolean[] enchanted = new boolean[4];

        for (int i = 0; i < 4; i++) {
            var stack = inv.getArmorStack(i);
            nullSlot[i] = stack.isEmpty();
            nonNull[i] = !stack.isEmpty();
            enchanted[i] = stack.hasEnchantments();
        }

        boolean boots = nullSlot[0] || (nonNull[0] && !enchanted[0]);
        boolean leggings = nullSlot[1] || (nonNull[1] && !enchanted[1]);
        boolean chestplate = nullSlot[2] || (nonNull[2] && !enchanted[2]);
        boolean helmet = nullSlot[3] || (nonNull[3] && !enchanted[3]);

        boolean notNaked = player.getArmor() != 0;
        boolean damaged = inv.getArmorStack(0).isDamaged() &&
                inv.getArmorStack(1).isDamaged() &&
                inv.getArmorStack(2).isDamaged() &&
                inv.getArmorStack(3).isDamaged();
        boolean nameWidth = player.getName().getString().length() == 6;
        boolean fullArmor = boots && leggings && chestplate && helmet;

        return nameWidth && notNaked && !damaged && fullArmor;
    }

    public static boolean isBot(PlayerEntity entity) {
        return botSet.contains(entity.getUuid());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        botSet.clear();
    }
}
