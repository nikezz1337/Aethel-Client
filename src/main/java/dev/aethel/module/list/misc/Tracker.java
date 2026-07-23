package dev.aethel.module.list.misc;

import com.google.common.eventbus.Subscribe;
import dev.aethel.config.ChatUtils;
import dev.aethel.event.list.EventPacket;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.MultiBooleanSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.text.Text;

import java.util.*;

@ModuleInformation(
    moduleName = "Tracker",
    moduleCategory = ModuleCategory.MISC,
    moduleDesc = "Трекер тотемов, зелий и предметов"
)
public class Tracker extends Module {

    private final MultiBooleanSetting notifyMode = new MultiBooleanSetting("Трекать", "",
            new BooleanSetting("Полученные зелья", true),
            new BooleanSetting("Съеденный предмет", true)
    );

    private final Map<UUID, Map<String, StatusEffectInstance>> playerEffects = new HashMap<>();
    private static final Map<UUID, Boolean> enchantedTotems = new HashMap<>();
    private final Map<UUID, ItemStack> activeUseItem = new HashMap<>();
    private final Map<UUID, Integer> useStartTick = new HashMap<>();

    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 500L;

    @Override
    public void onEnable() {
        super.onEnable();
        playerEffects.clear();
        enchantedTotems.clear();
        activeUseItem.clear();
        useStartTick.clear();
        lastUpdateTime = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        playerEffects.clear();
        enchantedTotems.clear();
        activeUseItem.clear();
        useStartTick.clear();
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (notifyMode.getValue("Съеденный предмет")) {
            trackConsumedItems();
        }

        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < UPDATE_INTERVAL_MS) return;
        lastUpdateTime = now;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player.getUuid().equals(mc.player.getUuid())) continue;
            UUID playerId = player.getUuid();

            Map<String, StatusEffectInstance> currentEffectsMap = new HashMap<>();
            for (StatusEffectInstance e : player.getStatusEffects()) {
                String key = e.getEffectType().value().getTranslationKey() + ":" + e.getAmplifier();
                currentEffectsMap.put(key, e);
            }
            playerEffects.put(playerId, currentEffectsMap);
        }
    }

    private void trackConsumedItems() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player.getUuid().equals(mc.player.getUuid())) continue;

            UUID id = player.getUuid();
            boolean using = player.isUsingItem();

            if (using) {
                if (!activeUseItem.containsKey(id)) {
                    activeUseItem.put(id, player.getActiveItem().copy());
                    useStartTick.put(id, player.age);
                }
            } else {
                ItemStack used = activeUseItem.remove(id);
                Integer startTick = useStartTick.remove(id);

                if (used != null && !used.isEmpty() && startTick != null) {
                    UseAction action = used.getUseAction();
                    String verb = switch (action) {
                        case DRINK -> "выпил";
                        case EAT -> "съел";
                        default -> null;
                    };

                    if (verb == null) continue;

                    int useDuration = player.age - startTick;
                    if (useDuration < 31) continue;

                    String itemName = used.getName().getString().replaceAll("§.", "");
                    String effectsStr = getPotionEffectsString(used);
                    String effectsPart = effectsStr.isEmpty() ? "" : " (§7" + effectsStr + "§r)";

                    ChatUtils.send("§R" + player.getName().getString() + "§7 " + verb + " §R" + itemName + effectsPart);
                }
            }
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.world == null || event.getType() != EventPacket.Type.RECEIVE) return;

        if (event.getPacket() instanceof EntityStatusS2CPacket statusPacket) {
            if (statusPacket.getStatus() == 35) {
                Entity entity = statusPacket.getEntity(mc.world);
                if (!(entity instanceof PlayerEntity player)) return;
                if (player.getUuid().equals(mc.player.getUuid())) return;

                boolean isEnchanted = EnchantmentHelper.hasEnchantments(player.getOffHandStack())
                        || EnchantmentHelper.hasEnchantments(player.getMainHandStack());
                enchantedTotems.put(player.getUuid(), isEnchanted);
            }
        }

        if (event.getPacket() instanceof EntityStatusEffectS2CPacket effectPacket) {
            if (!notifyMode.getValue("Полученные зелья")) return;

            Entity entity = mc.world.getEntityById(effectPacket.getEntityId());
            if (!(entity instanceof PlayerEntity player)) return;
            if (player.getUuid().equals(mc.player.getUuid())) return;

            UUID playerId = player.getUuid();
            Map<String, StatusEffectInstance> currentEffectsMap = playerEffects.getOrDefault(playerId, new HashMap<>());

            Map<String, StatusEffectInstance> updatedEffectsMap = new HashMap<>();
            for (StatusEffectInstance effect : player.getStatusEffects()) {
                String key = effect.getEffectType().value().getTranslationKey() + ":" + effect.getAmplifier();
                updatedEffectsMap.put(key, effect);
            }

            String newEffectKey = effectPacket.getEffectId().value().getTranslationKey() + ":" + effectPacket.getAmplifier();
            StatusEffectInstance newEffect = new StatusEffectInstance(
                    effectPacket.getEffectId(),
                    effectPacket.getDuration(),
                    effectPacket.getAmplifier(),
                    effectPacket.isAmbient(),
                    effectPacket.shouldShowParticles(),
                    effectPacket.shouldShowIcon()
            );
            updatedEffectsMap.put(newEffectKey, newEffect);

            List<SpecialPotionType> detectedPotions = new ArrayList<>();
            Set<String> newEffectKeys = new HashSet<>();
            newEffectKeys.add(newEffectKey);

            checkSpecialPotion(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.KILLER,
                    "effect.minecraft.strength:3", "effect.minecraft.resistance:0");
            checkSpecialPotion(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.URINE,
                    "effect.minecraft.jump_boost:0", "effect.minecraft.speed:2");
            checkSpecialPotion(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.MEDIC,
                    "effect.minecraft.health_boost:2", "effect.minecraft.regeneration:2");
            checkSpecialPotion(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.BURP,
                    "effect.minecraft.blindness:0", "effect.minecraft.glowing:0", "effect.minecraft.hunger:9",
                    "effect.minecraft.slowness:2", "effect.minecraft.wither:4");
            checkSpecialPotion(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.FLASH,
                    "effect.minecraft.blindness:0", "effect.minecraft.glowing:0");
            checkSpecialPotion(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.SULFURIC_ACID,
                    "effect.minecraft.poison:1", "effect.minecraft.slowness:3", "effect.minecraft.weakness:2",
                    "effect.minecraft.wither:4");
            checkSpecialPotion(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.WINNER,
                    "effect.minecraft.health_boost:1", "effect.minecraft.invisibility:0", "effect.minecraft.regeneration:1",
                    "effect.minecraft.resistance:0");

            if (!detectedPotions.isEmpty()) {
                for (SpecialPotionType type : detectedPotions) {
                    ChatUtils.send("§R" + player.getName().getString() + "§7 получил " + type.displayName);
                }
            } else {
                String localizedName = Text.translatable(effectPacket.getEffectId().value().getTranslationKey()).getString().replaceAll("§.", "");
                String duration = getEffectDuration(newEffect);
                int level = Math.max(0, effectPacket.getAmplifier()) + 1;

                ChatUtils.send("§R" + player.getName().getString() + "§7 получил §f" + localizedName + " " + level + "§R на §7" + duration + "§R");
            }

            playerEffects.put(playerId, updatedEffectsMap);
        }
    }

    private boolean checkSpecialPotion(Map<String, StatusEffectInstance> currentEffectsMap,
                                        Set<String> newOrRefreshedKeys,
                                        List<SpecialPotionType> detectedPotions,
                                        SpecialPotionType type, String... comboKeys) {
        Set<String> comboSet = new HashSet<>(Arrays.asList(comboKeys));
        boolean allPresent = comboSet.stream().allMatch(currentEffectsMap::containsKey);
        boolean atLeastOneNew = comboSet.stream().anyMatch(newOrRefreshedKeys::contains);

        if (allPresent && atLeastOneNew) {
            detectedPotions.add(type);
            newOrRefreshedKeys.removeAll(comboSet);
            return true;
        }
        return false;
    }

    private String getEffectDuration(StatusEffectInstance effect) {
        if (effect.isInfinite()) return "∞";
        int seconds = effect.getDuration() / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes > 0 ? minutes + " мин " + seconds + " сек" : seconds + " сек";
    }

    private String getPotionEffectsString(ItemStack stack) {
        PotionContentsComponent component = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (component == null) return "";

        StringBuilder sb = new StringBuilder();
        for (StatusEffectInstance eff : component.getEffects()) {
            if (!sb.isEmpty()) sb.append(", ");
            String name = Text.translatable(eff.getEffectType().value().getTranslationKey()).getString().replaceAll("§.", "");
            int level = eff.getAmplifier() + 1;
            sb.append(name).append(" ").append(level);
        }
        return sb.toString();
    }

    public enum SpecialPotionType {
        FLASH("§6[★] §eВспышка"),
        KILLER("§4[★] §cЗелье Киллера"),
        BURP("§c[★] §6Зелье Отрыжки"),
        SULFURIC_ACID("§2[★] §aСерная кислота"),
        MEDIC("§5[★] §dЗелье Медика"),
        WINNER("§2[★] §aЗелье Победителя"),
        URINE("§3[★] §bМоча Флеша");

        public final String displayName;

        SpecialPotionType(String displayName) {
            this.displayName = displayName;
        }
    }
}
