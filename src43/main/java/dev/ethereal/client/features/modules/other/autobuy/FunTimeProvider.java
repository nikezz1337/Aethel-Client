package dev.ethereal.client.features.modules.other.autobuy;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class FunTimeProvider {
    private static List<AutoBuyableItem> items = null;

    public static List<AutoBuyableItem> getItems() {
        if (items == null) { items = new ArrayList<>(); addItems(); }
        return items;
    }

    public static void reload() { items = null; }

    private static String stripStar(String name) {
        return name.replace("[★] ", "").replace("[★]", "").trim();
    }

    private static void addItems() {
        // ============== KRUSH (Крушителя) ==============
        List<EnchantmentData> krushHelmet = Arrays.asList(
            new EnchantmentData(Enchantments.AQUA_AFFINITY, -1),
            new EnchantmentData(Enchantments.BLAST_PROTECTION, 5),
            new EnchantmentData(Enchantments.FIRE_PROTECTION, 5),
            new EnchantmentData(Enchantments.MENDING, -1),
            new EnchantmentData(Enchantments.PROJECTILE_PROTECTION, 5),
            new EnchantmentData(Enchantments.PROTECTION, 5),
            new EnchantmentData(Enchantments.RESPIRATION, 3),
            new EnchantmentData(Enchantments.UNBREAKING, 5)
        );
        List<EnchantmentData> krushChest = Arrays.asList(
            new EnchantmentData(Enchantments.BLAST_PROTECTION, 5),
            new EnchantmentData(Enchantments.FIRE_PROTECTION, 5),
            new EnchantmentData(Enchantments.MENDING, -1),
            new EnchantmentData(Enchantments.PROJECTILE_PROTECTION, 5),
            new EnchantmentData(Enchantments.PROTECTION, 5),
            new EnchantmentData(Enchantments.UNBREAKING, 5)
        );
        List<EnchantmentData> krushLegs = Arrays.asList(
            new EnchantmentData(Enchantments.BLAST_PROTECTION, 5),
            new EnchantmentData(Enchantments.FIRE_PROTECTION, 5),
            new EnchantmentData(Enchantments.MENDING, -1),
            new EnchantmentData(Enchantments.PROJECTILE_PROTECTION, 5),
            new EnchantmentData(Enchantments.PROTECTION, 5),
            new EnchantmentData(Enchantments.UNBREAKING, 5)
        );
        List<EnchantmentData> krushBoots = Arrays.asList(
            new EnchantmentData(Enchantments.BLAST_PROTECTION, 5),
            new EnchantmentData(Enchantments.DEPTH_STRIDER, 3),
            new EnchantmentData(Enchantments.FEATHER_FALLING, 4),
            new EnchantmentData(Enchantments.FIRE_PROTECTION, 5),
            new EnchantmentData(Enchantments.MENDING, -1),
            new EnchantmentData(Enchantments.PROJECTILE_PROTECTION, 5),
            new EnchantmentData(Enchantments.PROTECTION, 5),
            new EnchantmentData(Enchantments.SOUL_SPEED, 3),
            new EnchantmentData(Enchantments.UNBREAKING, 5)
        );
        List<EnchantmentData> krushSword = Arrays.asList(
            new EnchantmentData(Enchantments.BANE_OF_ARTHROPODS, 7),
            new EnchantmentData(Enchantments.FIRE_ASPECT, 2),
            new EnchantmentData(Enchantments.LOOTING, 5),
            new EnchantmentData(Enchantments.MENDING, -1),
            new EnchantmentData(Enchantments.SHARPNESS, 7),
            new EnchantmentData(Enchantments.SMITE, 7),
            new EnchantmentData(Enchantments.SWEEPING_EDGE, 3),
            new EnchantmentData(Enchantments.UNBREAKING, 5)
        );
        List<EnchantmentData> krushTrident = Arrays.asList(
            new EnchantmentData(Enchantments.CHANNELING, -1),
            new EnchantmentData(Enchantments.FIRE_ASPECT, 2),
            new EnchantmentData(Enchantments.IMPALING, 5),
            new EnchantmentData(Enchantments.LOYALTY, 3),
            new EnchantmentData(Enchantments.MENDING, -1),
            new EnchantmentData(Enchantments.SHARPNESS, 7),
            new EnchantmentData(Enchantments.UNBREAKING, 5)
        );
        List<EnchantmentData> krushCrossbow = Arrays.asList(
            new EnchantmentData(Enchantments.MENDING, -1),
            new EnchantmentData(Enchantments.MULTISHOT, -1),
            new EnchantmentData(Enchantments.PIERCING, 5),
            new EnchantmentData(Enchantments.QUICK_CHARGE, 3),
            new EnchantmentData(Enchantments.UNBREAKING, 3)
        );
        List<EnchantmentData> krushPick = Arrays.asList(
            new EnchantmentData(Enchantments.EFFICIENCY, 10),
            new EnchantmentData(Enchantments.FORTUNE, 5),
            new EnchantmentData(Enchantments.MENDING, -1),
            new EnchantmentData(Enchantments.UNBREAKING, 5)
        );
        List<EnchantmentData> krushElytra = Arrays.asList(
            new EnchantmentData(Enchantments.MENDING, -1),
            new EnchantmentData(Enchantments.UNBREAKING, 5)
        );
        List<EnchantmentData> krushMace = Arrays.asList(
            new EnchantmentData(Enchantments.BREACH, 4),
            new EnchantmentData(Enchantments.DENSITY, 5),
            new EnchantmentData(Enchantments.MENDING, -1),
            new EnchantmentData(Enchantments.UNBREAKING, 5),
            new EnchantmentData(Enchantments.WIND_BURST, 3)
        );

        KrushItems.add(items, krushHelmet, krushChest, krushLegs, krushBoots, krushSword,
                       krushTrident, krushCrossbow, krushPick, krushElytra, krushMace);

        // ============== TALISMANS ==============
        items.add(new FunTimeTalismanItem("[★] Талисман Раздора", Items.TOTEM_OF_UNDYING, 0,
            new AttributeData[]{
                createAttrs("minecraft:generic.attack_damage", 4.0, 0, "offhand"),
                createAttrs("minecraft:generic.max_health", 2.0, 0, "offhand"),
                createAttrs("minecraft:generic.movement_speed", 0.1, 1, "offhand"),
                createAttrs("minecraft:generic.attack_speed", 0.1, 1, "offhand"),
                createAttrs("minecraft:generic.armor", -3.0, 0, "offhand")
            }));
        items.add(new FunTimeTalismanItem("[★] Талисман Карателя", Items.TOTEM_OF_UNDYING, 0,
            new AttributeData[]{
                createAttrs("minecraft:generic.attack_damage", 7.0, 0, "offhand"),
                createAttrs("minecraft:generic.max_health", -4.0, 0, "offhand"),
                createAttrs("minecraft:generic.movement_speed", 0.1, 1, "offhand")
            }));
        items.add(new FunTimeTalismanItem("[★] Талисман Крушителя", Items.TOTEM_OF_UNDYING, 0,
            new AttributeData[]{
                createAttrs("minecraft:generic.max_health", 4.0, 0, "offhand"),
                createAttrs("minecraft:generic.attack_damage", 3.0, 0, "offhand"),
                createAttrs("minecraft:generic.armor_toughness", 2.0, 0, "offhand"),
                createAttrs("minecraft:generic.armor", 2.0, 0, "offhand")
            }));
        items.add(new FunTimeTalismanItem("[★] Талисман Тирана", Items.TOTEM_OF_UNDYING, 0,
            new AttributeData[]{
                createAttrs("minecraft:generic.attack_damage", 2.0, 0, "offhand"),
                createAttrs("minecraft:generic.armor", 2.0, 0, "offhand"),
                createAttrs("minecraft:generic.max_health", -4.0, 0, "offhand")
            }));
        items.add(new FunTimeTalismanItem("[★] Талисман Ярости", Items.TOTEM_OF_UNDYING, 0,
            new AttributeData[]{
                createAttrs("minecraft:generic.attack_damage", 5.0, 0, "offhand"),
                createAttrs("minecraft:generic.max_health", -4.0, 0, "offhand")
            }));
        items.add(new FunTimeTalismanItem("[★] Талисман Демона", Items.TOTEM_OF_UNDYING, 0,
            new AttributeData[]{
                createAttrs("minecraft:generic.attack_damage", 2.5, 0, "offhand"),
                createAttrs("minecraft:generic.attack_speed", 0.1, 1, "offhand")
            }));
        items.add(new FunTimeTalismanItem("[★] Талисман Вихря", Items.TOTEM_OF_UNDYING, 0,
            new AttributeData[]{
                createAttrs("minecraft:generic.max_health", 2.0, 0, "offhand"),
                createAttrs("minecraft:generic.movement_speed", 0.15, 1, "offhand"),
                createAttrs("minecraft:generic.attack_speed", 0.15, 1, "offhand")
            }));
        items.add(new FunTimeTalismanItem("[★] Талисман Мрака", Items.TOTEM_OF_UNDYING, 0,
            new AttributeData[]{
                createAttrs("minecraft:generic.armor", 1.5, 0, "offhand"),
                createAttrs("minecraft:generic.max_health", 1.5, 0, "offhand")
            }));

        // ============== SPHERES ==============
        items.add(new FunTimeSphereItem("[★] Сфера Хаоса", Items.PLAYER_HEAD, 0,
            "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODY0MTkwMCwKICAicHJvZmlsZUlkIiA6ICIxNzRjZmRiNGEzY2I0M2I1YmZjZGU0MjRjM2JiMmM2ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYXJhZWwxOCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lN2E3YWU3Y2RjZjYxNmU4YjdhNDIyMWE2MjFiMjQzNTc1M2M2MGVkNmEyNThlYTA2MGRhZTMwMDJmZmU5ZTI4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
            createAttrs("minecraft:generic.max_health", -4.0, 0, "offhand"),
            createAttrs("minecraft:generic.armor", 2.0, 0, "offhand"),
            createAttrs("minecraft:generic.attack_damage", 3.0, 0, "offhand"),
            createAttrs("minecraft:generic.movement_speed", 0.07, 1, "offhand"),
            createAttrs("minecraft:generic.attack_speed", 0.13, 1, "offhand")));
        items.add(new FunTimeSphereItem("[★] Сфера Сатира", Items.PLAYER_HEAD, 0,
            "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODYwODUyOCwKICAicHJvZmlsZUlkIiA6ICJkMTQ4NjFiM2UwZmM0Njk5OTFlMTcyNTllMzdiZjZhZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJyYXhpdG9jbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NzFhOWE0OThiNGZhNWVjNDkzNjJmOWJjODhlZGE0ZjUyYjA0ZGU0OWQ3NWFhM2NhMzMyYTFmZWExYWEwZTU3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
            createAttrs("minecraft:generic.attack_damage", 2.0, 0, "offhand"),
            createAttrs("minecraft:generic.attack_speed", 0.15, 1, "offhand")));
        items.add(new FunTimeSphereItem("[★] Сфера Бестии", Items.PLAYER_HEAD, 0,
            "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0MzgzNDkzMCwKICAicHJvZmlsZUlkIiA6ICI1MzUzNWIxN2M0ZDY0NWQ0YWUwY2U2ZjM4Zjk0NTFjYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJVYml2aXMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQxMWFjMTczODFiOWZjZTliYWIzYzcyYWZkYjdmMTk4NTcwZGFmNDczMmJkODExZDMxYzIyN2Q4MGZhMzliMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
            createAttrs("minecraft:generic.armor", 1.0, 0, "offhand"),
            createAttrs("minecraft:generic.max_health", 4.0, 0, "offhand"),
            createAttrs("minecraft:generic.movement_speed", 0.1, 1, "offhand"),
            createAttrs("minecraft:generic.attack_speed", 0.1, 1, "offhand")));
        items.add(new FunTimeSphereItem("[★] Сфера Ареса", Items.PLAYER_HEAD, 0,
            "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzc3NDI1NSwKICAicHJvZmlsZUlkIiA6ICJhYWMxYjA2OWNkMjE0NWE2ODNlNzQxNzE4MDcxMGU4MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJqdXNhbXUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE2YWRjNmJhZmNiNTdmZDcwN2RlZTdkZDZhNzM2ZmUxMjY3MTFkNTNhMWZkNmNlNzg5ZGE0MWIzYmUxM2YyYSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
            createAttrs("minecraft:generic.attack_damage", 6.0, 0, "offhand"),
            createAttrs("minecraft:generic.armor", -2.0, 0, "offhand"),
            createAttrs("minecraft:generic.max_health", -2.0, 0, "offhand")));
        items.add(new FunTimeSphereItem("[★] Сфера Гидры", Items.PLAYER_HEAD, 0,
            "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODUzMjE4MywKICAicHJvZmlsZUlkIiA6ICI1OGZmZWI5NTMxNGQ0ODcwYTQwYjVjYjQyZDRlYTU5OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTa2luREJuZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UzYzExOGQ2OTZkOTEwZTU0ZGUwMmNhNGQ4MDc1NDNmOWIxOGMwMDhjOTgzOGQyZmY2OTM3NzYyMmZiMWQzMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
            createAttrs("minecraft:generic.max_health", 4.0, 0, "offhand"),
            createAttrs("minecraft:generic.armor", 2.0, 0, "offhand")));
        items.add(new FunTimeSphereItem("[★] Сфера Икара", Items.PLAYER_HEAD, 0,
            "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODU4MjQ5MSwKICAicHJvZmlsZUlkIiA6ICJhZWNkODIxZTQyYzE0ZDJlOThmNTA1OTg1MWI5OWMzNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJSb2RyaVgyMDc1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M2ODAzZTZkNTY2N2EyZDYxMDYyOGJjM2IzMmY4NjNjZGE0OTVjNDY1NjE2ZGU2NTVjYjMyOTkzM2I2MWFmNzciLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
            createAttrs("minecraft:generic.attack_damage", 2.0, 0, "offhand"),
            createAttrs("minecraft:generic.max_health", 2.0, 0, "offhand")));
        items.add(new FunTimeSphereItem("[★] Сфера Эрида", Items.PLAYER_HEAD, 0,
            "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzZlNGUyZjEwNDdmM2VjNmU5ZTQ1OTE4NDczOWUzM2I3YzFmYzYzYWQ4MjAyYmRhYjlmMDI0NTA4YWRkMjNlNWIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
            createAttrs("minecraft:generic.luck", 1.0, 0, "offhand"),
            createAttrs("minecraft:generic.max_health", 2.0, 0, "offhand")));
        items.add(new FunTimeSphereItem("[★] Сфера Афины", Items.PLAYER_HEAD, 0,
            "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJTcGhlcmVBdGhlbmEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTNmOWVlZGEzYmEyM2ZlMTQyM2M0MDM2ZTdkZDBhNzQ0NjFkZmY5NmJhZGM1YjJmMmI5ZmFhN2NjMTZmMzgyZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
            createAttrs("minecraft:generic.attack_speed", 0.15, 1, "offhand"),
            createAttrs("minecraft:generic.movement_speed", 0.15, 1, "offhand"),
            createAttrs("minecraft:generic.attack_damage", 3.0, 0, "offhand"),
            createAttrs("minecraft:generic.max_health", -2.0, 0, "offhand")));

        // ============== MISC (basic items) ==============
        items.add(new FunTimeItem("Золотое яблоко", Items.GOLDEN_APPLE, 0, null, null));
        items.add(new FunTimeItem("Зачарованное золотое яблоко", Items.ENCHANTED_GOLDEN_APPLE, 0, null, null));
        items.add(new FunTimeItem("Порох", Items.GUNPOWDER, 0, null, null));
        items.add(new FunTimeItem("Перка", Items.ENDER_PEARL, 0, null, null));
        items.add(new FunTimeItem("Незеритовый слиток", Items.NETHERITE_INGOT, 0, null, null));
        items.add(new FunTimeItem("Алмаз", Items.DIAMOND, 0, null, null));
        items.add(new FunTimeItem("Алмазный блок", Items.DIAMOND_BLOCK, 0, null, null));
        items.add(new FunTimeItem("Золотой слиток", Items.GOLD_INGOT, 0, null, null));
        items.add(new FunTimeItem("Маяк", Items.BEACON, 0, null, null));
        items.add(new FunTimeItem("Пузырёк опыта", Items.EXPERIENCE_BOTTLE, 0, null, null));
        items.add(new FunTimeItem("Шалкер", Items.SHULKER_BOX, 0, null, null));
        items.add(new FunTimeItem("Обсидиан", Items.OBSIDIAN, 0, null, null));
        items.add(new FunTimeItem("Спавнер", Items.SPAWNER, 0, null, null));
        items.add(new FunTimeItem("Элитры", Items.ELYTRA, 0, null, null));
        items.add(new FunTimeItem("Тотем бессмертия", Items.TOTEM_OF_UNDYING, 0, null, null));
        items.add(new FunTimeItem("Динамит", Items.TNT, 0, null, null));
        items.add(new FunTimeItem("Голова дракона", Items.DRAGON_HEAD, 0, null, null));
        items.add(new FunTimeItem("Алмазная руда", Items.DIAMOND_ORE, 0, null, null));
        items.add(new FunTimeItem("Изумрудная руда", Items.EMERALD_ORE, 0, null, null));
        items.add(new FunTimeItem("Яблоко", Items.APPLE, 0, null, null));
        items.add(new FunTimeItem("Книга починки", Items.ENCHANTED_BOOK, 0,
            new EnchantmentData[]{new EnchantmentData(Enchantments.MENDING, 1)}, null));

        // ============== DONATOR (special/effect items) ==============
        items.add(new FunTimeSpecialItem("[★] Явная пыль", Items.SUGAR, 0, "effect-item-dust",
            Text.literal("Каст: Световая вспышка"),
            Text.literal("Радиус: 10 блоков"),
            Text.literal("Эффекты для противников:"),
            Text.literal(" • Свечение (00:30)"),
            Text.literal(" • Слепота (00:01)")));
        items.add(new FunTimeSpecialItem("[★] Дезориентация", Items.ENDER_EYE, 0, "effect-item-diz",
            Text.literal("Чем ближе цель, тем дольше длительность эффектов")));
        items.add(new FunTimeSpecialItem("[★] Трапка", Items.NETHERITE_SCRAP, 0, "schematic-item-trap",
            Text.literal("Каст: Нерушимая клетка"),
            Text.literal("Длительность: 15 секунд")));
        items.add(new FunTimeSpecialItem("[★] Пласт", Items.DRIED_KELP, 0, "schematic-item-plast",
            Text.literal("Каст: Нерушимая стена")));
        items.add(new FunTimeSpecialItem("[★] Божья аура", Items.PHANTOM_MEMBRANE, 0, "effect-item-god",
            Text.literal("● Каст: Божественная аура")));
        items.add(new FunTimeSpecialItem("[★] Снежок заморозка", Items.SNOWBALL, 0, "effect-item-snowball",
            Text.literal("● Каст: Ледяная сфера")));
        items.add(new FunTimeSpecialItem("[★] Проклятая душа", Items.SOUL_LANTERN, 0, "soul-currency"));
        items.add(new FunTimeSpecialItem("[★] Огненный смерч", Items.FIRE_CHARGE, 0, "effect-item-fire",
            Text.literal("● Каст: Огненная волна")));
        items.add(new FunTimeSpecialItem("[★] Серебро", Items.IRON_NUGGET, 0, "silver-currency"));
        items.add(new FunTimeSpecialItem("[★] Божье касание", Items.GOLDEN_PICKAXE, 0, "spawner-item-spawner-break",
            Text.literal("Божье касание")));
        items.add(new FunTimeSpecialItem("[★] Кирка мега-бульдозер", Items.NETHERITE_PICKAXE, 0, "radius-item-mega-buldozer",
            Text.literal("Вскапывает территорию размером 9x9x5 блоков")));
        items.add(new FunTimeSpecialItem("[★] Драконий скин", Items.PAPER, 0, "trap-skin-item-dragon"));
        items.add(new FunTimeSpecialItem("[★] Блок дамагер", Items.JIGSAW, 0, "executable-block-damager"));

        // lockpicks
        items.add(new FunTimeSpecialItem("Отмычка к Сферам", Items.TRIPWIRE_HOOK, 0, "spheres",
            Text.literal("Этой отмычкой можно"),
            Text.literal("Открыть хранилище"),
            Text.literal("С Сферами")));
        items.add(new FunTimeSpecialItem("Отмычка к броне", Items.TRIPWIRE_HOOK, 0, "armors"));
        items.add(new FunTimeSpecialItem("Отмычка к оружию", Items.TRIPWIRE_HOOK, 0, "weapons"));
        items.add(new FunTimeSpecialItem("Отмычка к инструментам", Items.TRIPWIRE_HOOK, 0, "tools"));
        items.add(new FunTimeSpecialItem("Отмычка к ресурсам", Items.TRIPWIRE_HOOK, 0, "resources"));

        // misti
        items.add(new FunTimeSpecialItem("Сигнальный огонь [Обычный]", Items.CAMPFIRE, 0, "MILD"));
        items.add(new FunTimeSpecialItem("Сигнальный огонь [Богатый]", Items.CAMPFIRE, 0, "WEAK"));
        items.add(new FunTimeSpecialItem("Сигнальный огонь [Легендарный]", Items.CAMPFIRE, 0, "MEDIUM"));

        // chunk loaders
        items.add(new FunTimeSpecialItem("Прогрузчик чанков [1x1]", Items.STRUCTURE_BLOCK, 0, "executable-block-chunker-1"));
        items.add(new FunTimeSpecialItem("Прогрузчик чанков [3x3]", Items.STRUCTURE_BLOCK, 0, "executable-block-chunker-3"));
        items.add(new FunTimeSpecialItem("Прогрузчик чанков [5x5]", Items.STRUCTURE_BLOCK, 0, "executable-block-chunker-5"));

        // TNT
        items.add(new FunTimeSpecialItem("[★] TNT - TIER WHITE", Items.TNT, 0, "tnt-item-white",
            Text.literal("Этот динамит взрывается в 10 раз сильнее обычного")));
        items.add(new FunTimeSpecialItem("[★] TNT - TIER BLACK", Items.TNT, 0, "tnt-item-black",
            Text.literal("Этот динамит взрывается в 10 раз сильнее обычного"),
            Text.literal("и способен взорвать обсидиан")));

        // ============== POTIONS ==============
        items.add(new FunTimePotionItem("[★] Святая вода", Items.SPLASH_POTION, 0,
            Arrays.asList(
                new StatusEffectInstance(StatusEffects.REGENERATION, 1200, 2),
                new StatusEffectInstance(StatusEffects.INVISIBILITY, 12000, 1),
                new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, 1)), 16777215));
        items.add(new FunTimePotionItem("[★] Зелье Гнева", Items.SPLASH_POTION, 0,
            Arrays.asList(
                new StatusEffectInstance(StatusEffects.STRENGTH, 600, 4),
                new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 3)), 10040115));
        items.add(new FunTimePotionItem("[★] Зелье Палладина", Items.SPLASH_POTION, 0,
            Arrays.asList(
                new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 0),
                new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 12000, 0),
                new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 1200, 2),
                new StatusEffectInstance(StatusEffects.INVISIBILITY, 18000, 2)), 65535));
        items.add(new FunTimePotionItem("[★] Зелье Ассасина", Items.SPLASH_POTION, 0,
            Arrays.asList(
                new StatusEffectInstance(StatusEffects.STRENGTH, 1200, 3),
                new StatusEffectInstance(StatusEffects.SPEED, 6000, 2),
                new StatusEffectInstance(StatusEffects.HASTE, 1200, 0),
                new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 1)), 3355443));
        items.add(new FunTimePotionItem("[★] Зелье Радиации", Items.SPLASH_POTION, 0,
            Arrays.asList(
                new StatusEffectInstance(StatusEffects.POISON, 400, 0),
                new StatusEffectInstance(StatusEffects.WITHER, 400, 0),
                new StatusEffectInstance(StatusEffects.SLOWNESS, 400, 2),
                new StatusEffectInstance(StatusEffects.HUNGER, 400, 4),
                new StatusEffectInstance(StatusEffects.GLOWING, 400, 0)), 4737096));
        items.add(new FunTimePotionItem("[★] Снотворное", Items.SPLASH_POTION, 0,
            Arrays.asList(
                new StatusEffectInstance(StatusEffects.WEAKNESS, 1800, 1),
                new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 200, 1),
                new StatusEffectInstance(StatusEffects.WITHER, 1800, 2),
                new StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0)), 3329330));
    }

    private static class KrushItems {
        static void add(List<AutoBuyableItem> list,
                        List<EnchantmentData> helm, List<EnchantmentData> chest,
                        List<EnchantmentData> legs, List<EnchantmentData> boots,
                        List<EnchantmentData> sword, List<EnchantmentData> trident,
                        List<EnchantmentData> crossbow, List<EnchantmentData> pick,
                        List<EnchantmentData> elytra, List<EnchantmentData> mace) {
            list.add(new FunTimeItem("Шлем Крушителя", Items.NETHERITE_HELMET, 0, helm.toArray(new EnchantmentData[0]), null));
            list.add(new FunTimeItem("Нагрудник Крушителя", Items.NETHERITE_CHESTPLATE, 0, chest.toArray(new EnchantmentData[0]), null));
            list.add(new FunTimeItem("Поножи Крушителя", Items.NETHERITE_LEGGINGS, 0, legs.toArray(new EnchantmentData[0]), null));
            list.add(new FunTimeItem("Ботинки Крушителя", Items.NETHERITE_BOOTS, 0, boots.toArray(new EnchantmentData[0]), null));
            list.add(new FunTimeItem("Меч Крушителя", Items.NETHERITE_SWORD, 0, sword.toArray(new EnchantmentData[0]), null));
            list.add(new FunTimeItem("Трезубец Крушителя", Items.TRIDENT, 0, trident.toArray(new EnchantmentData[0]), null));
            list.add(new FunTimeItem("Арбалет Крушителя", Items.CROSSBOW, 0, crossbow.toArray(new EnchantmentData[0]), null));
            list.add(new FunTimeItem("Кирка Крушителя", Items.NETHERITE_PICKAXE, 0, pick.toArray(new EnchantmentData[0]), null));
            list.add(new FunTimeItem("Элитры Крушителя", Items.ELYTRA, 0, elytra.toArray(new EnchantmentData[0]), null));
            list.add(new FunTimeItem("Булава Крушителя", Items.MACE, 0, mace.toArray(new EnchantmentData[0]), null));
        }
    }

    private static AttributeData createAttrs(String name, double amount, int op, String slot) {
        return new AttributeData(name, amount, op, slot);
    }

    public static class AttributeData {
        public final String attributeName;
        public final double amount;
        public final int operation;
        public final String slot;
        AttributeData(String name, double a, int op, String s) { this.attributeName = name; this.amount = a; this.operation = op; this.slot = s; }
    }

    public static class EnchantmentData {
        public final RegistryKey<Enchantment> enchantment;
        public final int level;
        public EnchantmentData(RegistryKey<Enchantment> e, int l) { this.enchantment = e; this.level = l; }
    }

    // ============== ITEM TYPES ==============

    public static class FunTimeItem implements AutoBuyableItem {
        private final String displayName;
        private final Item material;
        private final int price;
        private final EnchantmentData[] requiredEnchantments;
        private final List<Text> loreTexts;
        private final AutoBuyItemSettings settings;
        private boolean enabled = true;

        public FunTimeItem(String name, Item mat, int price, EnchantmentData[] ench, List<Text> lore) {
            this.displayName = name; this.material = mat; this.price = price;
            this.requiredEnchantments = ench; this.loreTexts = lore;
            this.settings = new AutoBuyItemSettings(price, mat, name);
        }

        public String getDisplayName() { return displayName; }
        public String getSearchName() { return stripStar(displayName); }

        public ItemStack createItemStack() {
            ItemStack stack = new ItemStack(material);
            if (requiredEnchantments != null && requiredEnchantments.length > 0)
                addEnchantments(stack, requiredEnchantments);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName).formatted(Formatting.BOLD, Formatting.GREEN));
            if (loreTexts != null && !loreTexts.isEmpty())
                stack.set(DataComponentTypes.LORE, new LoreComponent(loreTexts));
            NbtCompound nbt = new NbtCompound();
            nbt.putBoolean("FunTimeItem", true);
            nbt.putInt("HideFlags", 127);
            if (requiredEnchantments != null && requiredEnchantments.length > 0) {
                NbtList el = new NbtList();
                for (EnchantmentData d : requiredEnchantments) {
                    NbtCompound e = new NbtCompound();
                    e.putString("id", d.enchantment.getValue().toString());
                    e.putShort("lvl", (short) d.level);
                    el.add(e);
                }
                nbt.put("RequiredEnchantments", el);
            }
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            return stack;
        }

        public int getPrice() { return price; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { enabled = v; }
        public AutoBuyItemSettings getSettings() { return settings; }
    }

    public static class FunTimeTalismanItem implements AutoBuyableItem {
        private final String displayName;
        private final Item material;
        private final int price;
        private final AttributeData[] attributes;
        private final AutoBuyItemSettings settings;
        private boolean enabled = true;

        public FunTimeTalismanItem(String name, Item mat, int price, AttributeData[] attrs) {
            this.displayName = name; this.material = mat; this.price = price; this.attributes = attrs;
            this.settings = new AutoBuyItemSettings(price, mat, name);
        }

        public String getDisplayName() { return displayName; }
        public String getSearchName() { return stripStar(displayName); }

        public ItemStack createItemStack() {
            ItemStack stack = new ItemStack(material);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName).formatted(Formatting.BOLD, Formatting.YELLOW));
            NbtCompound nbt = new NbtCompound();
            nbt.putBoolean("FunTimeItem", true);
            nbt.putBoolean("FunTimeTalik", true);
            nbt.putInt("HideFlags", 127);
            if (attributes != null && attributes.length > 0) {
                NbtList al = new NbtList();
                for (AttributeData a : attributes) {
                    NbtCompound an = new NbtCompound();
                    an.putString("AttributeName", a.attributeName);
                    an.putDouble("Amount", a.amount);
                    an.putInt("Operation", a.operation);
                    an.putString("Slot", a.slot);
                    an.putString("Name", UUID.randomUUID().toString());
                    an.putIntArray("UUID", new int[]{
                        (int)(Math.random() * Integer.MAX_VALUE), (int)(Math.random() * Integer.MAX_VALUE),
                        (int)(Math.random() * Integer.MAX_VALUE), (int)(Math.random() * Integer.MAX_VALUE)
                    });
                    al.add(an);
                }
                nbt.put("AttributeModifiers", al);
            }
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            return stack;
        }

        public int getPrice() { return price; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { enabled = v; }
        public AutoBuyItemSettings getSettings() { return settings; }
    }

    public static class FunTimeSphereItem implements AutoBuyableItem {
        private final String displayName;
        private final Item material;
        private final int price;
        private final String texture;
        private final AttributeData[] attributes;
        private final AutoBuyItemSettings settings;
        private boolean enabled = true;

        public FunTimeSphereItem(String name, Item mat, int price, String tex, AttributeData... attrs) {
            this.displayName = name; this.material = mat; this.price = price;
            this.texture = tex; this.attributes = attrs;
            this.settings = new AutoBuyItemSettings(price, mat, name);
        }

        public String getDisplayName() { return displayName; }
        public String getSearchName() { return stripStar(displayName); }

        public ItemStack createItemStack() {
            ItemStack stack = new ItemStack(material);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName).formatted(Formatting.BOLD, Formatting.RED));
            NbtCompound nbt = new NbtCompound();
            nbt.putBoolean("FunTimeItem", true);
            nbt.putBoolean("FunTimeSphere", true);
            nbt.putInt("HideFlags", 127);
            if (texture != null && !texture.isEmpty() && material == Items.PLAYER_HEAD) {
                UUID skullId = UUID.fromString("9afca6b1-556f-3cf9-b349-3886d7d2c53b");
                NbtCompound so = new NbtCompound();
                so.putUuid("Id", skullId);
                NbtCompound props = new NbtCompound();
                NbtList texList = new NbtList();
                NbtCompound tn = new NbtCompound();
                tn.putString("Value", texture);
                texList.add(tn);
                props.put("textures", texList);
                so.put("Properties", props);
                nbt.put("SkullOwner", so);
                GameProfile profile = new GameProfile(skullId, "");
                profile.getProperties().put("textures", new Property("textures", texture));
                stack.set(DataComponentTypes.PROFILE, new ProfileComponent(profile));
            }
            if (attributes != null && attributes.length > 0) {
                NbtList al = new NbtList();
                for (AttributeData a : attributes) {
                    NbtCompound an = new NbtCompound();
                    an.putString("AttributeName", a.attributeName);
                    an.putDouble("Amount", a.amount);
                    an.putInt("Operation", a.operation);
                    an.putString("Slot", a.slot);
                    an.putString("Name", UUID.randomUUID().toString());
                    an.putIntArray("UUID", new int[]{
                        (int)(Math.random() * Integer.MAX_VALUE), (int)(Math.random() * Integer.MAX_VALUE),
                        (int)(Math.random() * Integer.MAX_VALUE), (int)(Math.random() * Integer.MAX_VALUE)
                    });
                    al.add(an);
                }
                nbt.put("AttributeModifiers", al);
            }
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            return stack;
        }

        public int getPrice() { return price; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { enabled = v; }
        public AutoBuyItemSettings getSettings() { return settings; }
    }

    public static class FunTimePotionItem implements AutoBuyableItem {
        private final String displayName;
        private final Item material;
        private final int price;
        private final List<StatusEffectInstance> effects;
        private final Integer customColor;
        private final AutoBuyItemSettings settings;
        private boolean enabled = true;

        public FunTimePotionItem(String name, Item mat, int price, List<StatusEffectInstance> eff, Integer color) {
            this.displayName = name; this.material = mat; this.price = price;
            this.effects = eff; this.customColor = color;
            this.settings = new AutoBuyItemSettings(price, mat, name);
        }

        public String getDisplayName() { return displayName; }
        public String getSearchName() { return stripStar(displayName); }

        public ItemStack createItemStack() {
            ItemStack stack = new ItemStack(material);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName).formatted(Formatting.BOLD, Formatting.LIGHT_PURPLE));
            if (effects != null && !effects.isEmpty()) {
                stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.empty(), Optional.ofNullable(customColor), effects, Optional.empty()));
            }
            NbtCompound nbt = new NbtCompound();
            nbt.putBoolean("FunTimeItem", true);
            nbt.putBoolean("FunTimePotion", true);
            nbt.putInt("HideFlags", 127);
            if (effects != null && !effects.isEmpty()) {
                NbtList el = new NbtList();
                for (StatusEffectInstance e : effects) {
                    NbtCompound en = new NbtCompound();
                    en.putString("effectId", e.getEffectType().getIdAsString());
                    en.putInt("amplifier", e.getAmplifier());
                    en.putInt("duration", e.getDuration());
                    el.add(en);
                }
                nbt.put("effects", el);
            }
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            return stack;
        }

        public int getPrice() { return price; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { enabled = v; }
        public AutoBuyItemSettings getSettings() { return settings; }
    }

    public static class FunTimeSpecialItem implements AutoBuyableItem {
        private final String displayName;
        private final Item material;
        private final int price;
        private final String funItemType;
        private final List<Text> loreTexts;
        private final AutoBuyItemSettings settings;
        private boolean enabled = true;

        public FunTimeSpecialItem(String name, Item mat, int price, String type, Text... lore) {
            this.displayName = name; this.material = mat; this.price = price;
            this.funItemType = type;
            this.loreTexts = (lore != null && lore.length > 0) ? Arrays.asList(lore) : null;
            this.settings = new AutoBuyItemSettings(price, mat, name);
        }

        public String getDisplayName() { return displayName; }
        public String getSearchName() { return stripStar(displayName); }

        public ItemStack createItemStack() {
            ItemStack stack = new ItemStack(material);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName).formatted(Formatting.BOLD, Formatting.YELLOW));
            if (loreTexts != null && !loreTexts.isEmpty())
                stack.set(DataComponentTypes.LORE, new LoreComponent(loreTexts));
            NbtCompound nbt = new NbtCompound();
            nbt.putBoolean("FunTimeItem", true);
            nbt.putBoolean("FunTimeSpecial", true);
            nbt.putString("funItemType", funItemType);
            nbt.putInt("HideFlags", 127);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            return stack;
        }

        public int getPrice() { return price; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { enabled = v; }
        public AutoBuyItemSettings getSettings() { return settings; }
        public String getFunItemType() { return funItemType; }
    }

    private static void addEnchantments(ItemStack stack, EnchantmentData[] enchantments) {
        var client = MinecraftClient.getInstance();
        if (client.world == null) return;
        var lookup = client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var builder = new ItemEnchantmentsComponent.Builder(
            stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
        for (EnchantmentData d : enchantments) {
            if (d.level > 0) {
                lookup.getOptional(d.enchantment).ifPresent(e -> builder.add(e, d.level));
            }
        }
        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
    }
}
