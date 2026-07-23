package dev.aethel.util.auction.ab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class BuyableRegistry {
    private final List<BuyableItem> items = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File cfg = new File("aethel/autobuy_config.json");
    private boolean initialized = false;

    public void init() {
        if (initialized) return;
        initialized = true;

        items.clear();

        add("Шлем Infinity", Items.NETHERITE_HELMET, stack ->
                stack.getItem() == Items.NETHERITE_HELMET &&
                        ItemMatcher.matchesByTooltip(stack, "Подводник", "Взрывоустойчивость V", "Огнеупорность V",
                                "Защита от снарядов V", "Защита V", "Подводное дыхание III", "Прочность V", "Непробиваемый II"));

        add("Нагрудник Infinity", Items.NETHERITE_CHESTPLATE, stack ->
                stack.getItem() == Items.NETHERITE_CHESTPLATE &&
                        ItemMatcher.matchesByTooltip(stack, "Взрывоустойчивость V", "Огнеупорность V",
                                "Защита от снарядов V", "Защита V", "Прочность V", "Непробиваемый II"));

        add("Штаны Infinity", Items.NETHERITE_LEGGINGS, stack ->
                stack.getItem() == Items.NETHERITE_LEGGINGS &&
                        ItemMatcher.matchesByTooltip(stack, "Взрывоустойчивость V", "Огнеупорность V",
                                "Защита от снарядов V", "Защита V", "Прочность V", "Непробиваемый II"));

        add("Ботинки Infinity", Items.NETHERITE_BOOTS, stack ->
                stack.getItem() == Items.NETHERITE_BOOTS &&
                        ItemMatcher.matchesByTooltip(stack, "Взрывоустойчивость V", "Подводная ходьба III",
                                "Невесомость IV", "Огнеупорность V", "Защита от снарядов V", "Защита V",
                                "Скорость души III", "Прочность V", "Непробиваемый II"));

        add("Шлем Eternity", Items.NETHERITE_HELMET, stack ->
                stack.getItem() == Items.NETHERITE_HELMET &&
                        ItemMatcher.matchesByTooltip(stack, "Подводник", "Взрывоустойчивость V", "Огнеупорность V",
                                "Защита от снарядов V", "Защита V", "Подводное дыхание III", "Прочность V", "Непробиваемый I") &&
                        !ItemMatcher.matchesByTooltipAny(stack, "Непробиваемый II"));

        add("Нагрудник Eternity", Items.NETHERITE_CHESTPLATE, stack ->
                stack.getItem() == Items.NETHERITE_CHESTPLATE &&
                        ItemMatcher.matchesByTooltip(stack, "Взрывоустойчивость V", "Огнеупорность V",
                                "Защита от снарядов V", "Защита V", "Прочность V", "Непробиваемый I") &&
                        !ItemMatcher.matchesByTooltipAny(stack, "Непробиваемый II"));

        add("Штаны Eternity", Items.NETHERITE_LEGGINGS, stack ->
                stack.getItem() == Items.NETHERITE_LEGGINGS &&
                        ItemMatcher.matchesByTooltip(stack, "Взрывоустойчивость V", "Огнеупорность V",
                                "Защита от снарядов V", "Защита V", "Прочность V", "Непробиваемый I") &&
                        !ItemMatcher.matchesByTooltipAny(stack, "Непробиваемый II"));

        add("Ботинки Eternity", Items.NETHERITE_BOOTS, stack ->
                stack.getItem() == Items.NETHERITE_BOOTS &&
                        ItemMatcher.matchesByTooltip(stack, "Подводная ходьба III",
                                "Невесомость IV", "Огнеупорность V", "Защита от снарядов V", "Защита V",
                                "Скорость души III", "Прочность V", "Непробиваемый I") &&
                        !ItemMatcher.matchesByTooltipAny(stack, "Непробиваемый II"));

        add("Меч Eternity", Items.NETHERITE_SWORD, stack ->
                stack.getItem() == Items.NETHERITE_SWORD &&
                        ItemMatcher.matchesByTooltip(stack, "Бич членистоногих VII", "Заговор огня II", "Добыча V",
                                "Починка", "Острота VII", "Небесная кара VII", "Разящий клинок III", "Прочность V",
                                "Богач I", "Разрушитель II", "Критический II"));

        add("Кирка Eternity", Items.NETHERITE_PICKAXE, stack ->
                stack.getItem() == Items.NETHERITE_PICKAXE &&
                        ItemMatcher.matchesByTooltip(stack, "Эффективность X", "Удача V", "Починка", "Прочность V",
                                "Магнетизм I", "Неразрушимость I", "Автоплавка", "Опытный III", "Бур II"));

        add("Талисман Infinity", Items.TOTEM_OF_UNDYING, stack ->
                stack.getItem() == Items.TOTEM_OF_UNDYING &&
                        ItemMatcher.matchesByTooltip(stack, "Макс. здоровье II", "Урон II", "Броня II", "Скорость II"));

        add("Талисман Eternity", Items.TOTEM_OF_UNDYING, stack ->
                stack.getItem() == Items.TOTEM_OF_UNDYING &&
                        ItemMatcher.matchesByTooltip(stack, "Урон II", "Броня II", "Скорость II") &&
                        !ItemMatcher.matchesByTooltipAny(stack, "Макс. здоровье II"));

        add("Динамит Б", Items.TNT, stack ->
                stack.getItem() == Items.TNT &&
                        ItemMatcher.containsInLore(stack, "10 раз") &&
                        !ItemMatcher.containsInLore(stack, "разрушает"));

        add("С4 взрывчатка", Items.TNT, stack ->
                stack.getItem() == Items.TNT &&
                        ItemMatcher.containsInLore(stack, "разрушает") &&
                        !ItemMatcher.containsInLore(stack, "воде"));

        add("Стан", Items.NETHER_STAR, stack ->
                stack.getItem() == Items.NETHER_STAR &&
                        ItemMatcher.containsInLore(stack, "особенности:"));

        add("Броневая элитра", Items.ELYTRA, stack ->
                stack.getItem() == Items.ELYTRA &&
                        ItemMatcher.containsInLore(stack, "позволяет") &&
                        ItemMatcher.containsInLore(stack, "как обычная элитра"));

        add("Золотой спавнер", Items.SPAWNER, stack ->
                stack.getItem() == Items.SPAWNER &&
                        ItemMatcher.containsInLore(stack, "виртуально"));

        add("Чарка", Items.ENCHANTED_GOLDEN_APPLE, stack ->
                stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE);

        add("Гепл", Items.GOLDEN_APPLE, stack ->
                stack.getItem() == Items.GOLDEN_APPLE);

        add("Порох", Items.GUNPOWDER, stack ->
                stack.getItem() == Items.GUNPOWDER);

        add("Тотем", Items.TOTEM_OF_UNDYING, stack ->
                stack.getItem() == Items.TOTEM_OF_UNDYING);

        add("Особый компас", Items.COMPASS, stack ->
                stack.getItem() == Items.COMPASS &&
                        ItemMatcher.containsInLore(stack, "ведёт"));

        add("Мифическая сфера", Items.PLAYER_HEAD, stack ->
                stack.getItem() == Items.PLAYER_HEAD &&
                        ItemMatcher.matchesByTooltip(stack, "Урон 3", "Броня 2") &&
                        !ItemMatcher.matchesByTooltipAny(stack, "Скорость I", "Скорость II", "Макс. здоровье II") &&
                        !ItemMatcher.matchesDisplayName(stack, "Легендарная сфера"));

        add("Сфера Eternity", Items.PLAYER_HEAD, stack ->
                stack.getItem() == Items.PLAYER_HEAD &&
                        ItemMatcher.matchesByTooltip(stack, "Скорость II", "Урон II", "Броня II"));

        add("Сфера Armortality", Items.PLAYER_HEAD, stack ->
                stack.getItem() == Items.PLAYER_HEAD &&
                        ItemMatcher.matchesByTooltip(stack, "Макс. здоровье II", "Урон II", "Броня II"));

        add("Меч Stinger", Items.NETHERITE_SWORD, stack ->
                stack.getItem() == Items.NETHERITE_SWORD &&
                        ItemMatcher.matchesByTooltip(stack, "Бич членистоногих VII", "Заговор огня II", "Добыча V",
                                "Починка", "Острота VI", "Небесная кара VII", "Разящий клинок III", "Прочность IV",
                                "Критический II", "Богач I"));

        add("Кирка Stinger", Items.NETHERITE_PICKAXE, stack ->
                stack.getItem() == Items.NETHERITE_PICKAXE &&
                        ItemMatcher.matchesByTooltip(stack, "Эффективность VIII", "Удача IV", "Починка", "Прочность IV",
                                "Магнетизм I", "Неразрушимость I", "Автоплавка", "Опытный III", "Бур I"));

        add("Трезубец с самонаводкой", Items.TRIDENT, stack ->
                stack.getItem() == Items.TRIDENT &&
                        ItemMatcher.matchesByTooltipAny(stack, "Самонаводка I", "Самонаводка II", "Самонаводка III", "Самонаводка IV"));

        add("Зелье победителя", Items.POTION, stack ->
                stack.getItem() == Items.POTION &&
                        ItemMatcher.containsInLore(stack, "Эффекты:") && ItemMatcher.containsInLore(stack, "- Сила III (8:00") && ItemMatcher.containsInLore(stack, "- Скорость III (8:00"));

        add("Динамит Б2", Items.TNT, stack ->
                stack.getItem() == Items.TNT &&
                        ItemMatcher.containsInLore(stack, "практически"));

        add("Стиллер", Items.TNT, stack ->
                stack.getItem() == Items.TNT &&
                        ItemMatcher.containsInLore(stack, "50%"));

        add("Надёжный стиллер", Items.TNT, stack ->
                stack.getItem() == Items.TNT &&
                        ItemMatcher.containsInLore(stack, "спавнер") &&
                        ItemMatcher.containsInLore(stack, "75%"));

        add("Разрывная волна", Items.TNT, stack ->
                stack.getItem() == Items.TNT &&
                        ItemMatcher.containsInLore(stack, "взрывает") &&
                        ItemMatcher.containsInLore(stack, "разрушает") &&
                        ItemMatcher.containsInLore(stack, "воде"));

        add("Взрывная трапка", Items.PRISMARINE_SHARD, stack ->
                stack.getItem() == Items.PRISMARINE_SHARD &&
                        ItemMatcher.containsInLore(stack, "взрыве наносит"));

        add("Трапка", Items.POPPED_CHORUS_FRUIT, stack ->
                stack.getItem() == Items.POPPED_CHORUS_FRUIT &&
                        ItemMatcher.containsInLore(stack, "создаёт") &&
                        (ItemMatcher.containsInLore(stack, "коробку") || ItemMatcher.containsInLore(stack, "в месте")));

        add("Гравитация", Items.FEATHER, stack ->
                stack.getItem() == Items.FEATHER &&
                        ItemMatcher.containsInLore(stack, "когда предмет"));

        add("Справедливость", Items.POTION, stack ->
                stack.getItem() == Items.POTION &&
                        ItemMatcher.containsInLore(stack, "когда предмет"));

        add("Тнт-пушка", Items.DISPENSER, stack ->
                stack.getItem() == Items.DISPENSER &&
                        ItemMatcher.containsInLore(stack, "запускает"));

        add("Уникальный приват", Items.ANCIENT_DEBRIS, stack ->
                stack.getItem() == Items.ANCIENT_DEBRIS &&
                        ItemMatcher.containsInLore(stack, "имеет свойства"));

        add("Универсальный ключ", Items.TRIPWIRE_HOOK, stack ->
                stack.getItem() == Items.TRIPWIRE_HOOK &&
                        ItemMatcher.containsInLore(stack, "используйте данный ключ"));

        add("Яйцо дракона", Items.DRAGON_EGG, stack ->
                stack.getItem() == Items.DRAGON_EGG &&
                        ItemMatcher.containsInLore(stack, "стоимость яйца"));

        add("Золотая кирка джейка", Items.GOLDEN_PICKAXE, stack ->
                stack.getItem() == Items.GOLDEN_PICKAXE &&
                        ItemMatcher.containsInLore(stack, "сломав спавнер этой киркой"));

        add("Осколочное эндер яйцо", Items.ENDERMAN_SPAWN_EGG, stack ->
                stack.getItem() == Items.ENDERMAN_SPAWN_EGG &&
                        ItemMatcher.containsInLore(stack, "осколочные эндер яйца"));

        add("Скорость 3", Items.PLAYER_HEAD, stack ->
                stack.getItem() == Items.PLAYER_HEAD &&
                        ItemMatcher.matchesByTooltip(stack, "Скорость III"));

        add("Осколок сферы", Items.PLAYER_HEAD, stack ->
                stack.getItem() == Items.PLAYER_HEAD &&
                        ItemMatcher.containsInLore(stack, "/spheres"));

        add("Сфера Флеша", Items.PLAYER_HEAD, stack ->
                stack.getItem() == Items.PLAYER_HEAD &&
                        ItemMatcher.matchesByTooltip(stack, "Скорость III", "Броня I"));

        add("Сфера Цербера", Items.PLAYER_HEAD, stack ->
                stack.getItem() == Items.PLAYER_HEAD &&
                        ItemMatcher.matchesByTooltip(stack, "Урон V"));

        add("Рюкзак Iɴғɪɴɪᴛʏ", Items.LIME_SHULKER_BOX, stack ->
                stack.getItem() == Items.LIME_SHULKER_BOX &&
                        (ItemMatcher.containsInLore(stack, "36 слот") ||
                                ItemMatcher.containsInLore(stack, "слотов: 36") ||
                                (ItemMatcher.containsInLore(stack, "36") && ItemMatcher.containsInLore(stack, "рюкзак"))));

        add("Рюкзак IV", Items.MAGENTA_SHULKER_BOX, stack ->
                stack.getItem() == Items.MAGENTA_SHULKER_BOX &&
                        (ItemMatcher.containsInLore(stack, "27 слот") ||
                                ItemMatcher.containsInLore(stack, "слотов: 27") ||
                                (ItemMatcher.containsInLore(stack, "27") && ItemMatcher.containsInLore(stack, "рюкзак"))));

        add("Рюкзак III", Items.RED_SHULKER_BOX, stack ->
                stack.getItem() == Items.RED_SHULKER_BOX &&
                        (ItemMatcher.containsInLore(stack, "21 слот") ||
                                ItemMatcher.containsInLore(stack, "слотов: 21") ||
                                (ItemMatcher.containsInLore(stack, "21") && ItemMatcher.containsInLore(stack, "рюкзак"))));

        add("Рюкзак II", Items.LIGHT_BLUE_SHULKER_BOX, stack ->
                stack.getItem() == Items.LIGHT_BLUE_SHULKER_BOX &&
                        (ItemMatcher.containsInLore(stack, "15 слот") ||
                                ItemMatcher.containsInLore(stack, "слотов: 15") ||
                                (ItemMatcher.containsInLore(stack, "15") && ItemMatcher.containsInLore(stack, "рюкзак"))));

        add("Рюкзак I", Items.PINK_SHULKER_BOX, stack ->
                stack.getItem() == Items.PINK_SHULKER_BOX &&
                        (ItemMatcher.containsInLore(stack, "9 слот") ||
                                ItemMatcher.containsInLore(stack, "слотов: 9") ||
                                (ItemMatcher.containsInLore(stack, "9") && ItemMatcher.containsInLore(stack, "рюкзак"))));

        add("Талисман Сатиры", Items.TOTEM_OF_UNDYING, stack ->
                stack.getItem() == Items.TOTEM_OF_UNDYING &&
                        ItemMatcher.matchesByTooltip(stack, "Урон III", "Спешка II"));

        add("Опыт 100", Items.EXPERIENCE_BOTTLE, stack ->
                stack.getItem() == Items.EXPERIENCE_BOTTLE &&
                        ItemMatcher.containsInLore(stack, "30971") &&
                        ItemMatcher.containsInLore(stack, "(100 ур.)"));

        add("Опыт 50", Items.EXPERIENCE_BOTTLE, stack ->
                stack.getItem() == Items.EXPERIENCE_BOTTLE &&
                        ItemMatcher.containsInLore(stack, "5345") &&
                        ItemMatcher.containsInLore(stack, "(50 ур.)"));

        add("Шлем солнца", Items.GOLDEN_HELMET, stack ->
                stack.getItem() == Items.GOLDEN_HELMET &&
                        ItemMatcher.containsInLore(stack, "имеет свойства"));

        add("Нерушимые элитры", Items.ELYTRA, stack ->
                stack.getItem() == Items.ELYTRA &&
                        ItemMatcher.matchesDisplayName(stack, "Нерушимые элитры"));

        add("Взрывчатое вещество", Items.CLAY, stack ->
                stack.getItem() == Items.CLAY &&
                        ItemMatcher.containsInLore(stack, "особенности:"));

        add("Меч Фармер II", Items.NETHERITE_SWORD, stack ->
                stack.getItem() == Items.NETHERITE_SWORD &&
                        ItemMatcher.matchesByTooltipAny(stack, "Фармер II"));

        add("Меч Фармер III", Items.NETHERITE_SWORD, stack ->
                stack.getItem() == Items.NETHERITE_SWORD &&
                        ItemMatcher.matchesByTooltipAny(stack, "Фармер III"));

        add("Меч Фармер IV", Items.NETHERITE_SWORD, stack ->
                stack.getItem() == Items.NETHERITE_SWORD &&
                        ItemMatcher.matchesByTooltipAny(stack, "Фармер IV"));

        add("Меч Фармер V", Items.NETHERITE_SWORD, stack ->
                stack.getItem() == Items.NETHERITE_SWORD &&
                        ItemMatcher.matchesByTooltipAny(stack, "Фармер V"));

        add("Меч Фармер VI", Items.NETHERITE_SWORD, stack ->
                stack.getItem() == Items.NETHERITE_SWORD &&
                        ItemMatcher.matchesByTooltipAny(stack, "Фармер VI"));

        add("Меч Фармер VII", Items.NETHERITE_SWORD, stack ->
                stack.getItem() == Items.NETHERITE_SWORD &&
                        ItemMatcher.matchesByTooltipAny(stack, "Фармер VII"));

        add("Мечта шахтера", Items.NETHERITE_PICKAXE, stack ->
                stack.getItem() == Items.NETHERITE_PICKAXE &&
                        ItemMatcher.matchesByTooltipAny(stack, "Прочность X", "Прочность XI", "Прочность XII", "Прочность XIII", "Прочность XIV", "Прочность XV"));

        add("Меч Цербера", Items.NETHERITE_SWORD, stack ->
                stack.getItem() == Items.NETHERITE_SWORD &&
                        ItemMatcher.matchesByTooltipAny(stack, "Острота VIII"));

        add("Руна бессмертие", Items.ORANGE_DYE, stack ->
                stack.getItem() == Items.ORANGE_DYE &&
                        ItemMatcher.containsInLore(stack, "неуязвимость"));

        add("Руна Восстановление", Items.RED_DYE, stack ->
                stack.getItem() == Items.RED_DYE &&
                        ItemMatcher.containsInLore(stack, "восстановиться"));

        add("Динамит А", Items.TNT, stack ->
                stack.getItem() == Items.TNT &&
                        ItemMatcher.containsInLore(stack, "3 раз"));

        load();
    }

    private void add(String name, net.minecraft.item.Item displayItem, java.util.function.Predicate<ItemStack> checker) {
        items.add(new BuyableItem(name, displayItem, checker));
    }

    public BuyableItem findMatch(ItemStack stack) {
        for (BuyableItem item : items) {
            if (item.matches(stack)) {
                return item;
            }
        }
        return null;
    }

    public List<BuyableItem> getAll() {
        return new ArrayList<>(items);
    }

    public BuyableItem getByName(String name) {
        for (BuyableItem item : items) {
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }

    public void save() {
        try {
            cfg.getParentFile().mkdirs();

            Map<String, ItemConfig> configs = new HashMap<>();
            for (BuyableItem item : items) {
                ItemConfig config = new ItemConfig();
                config.enabled = item.isEnabled();
                config.maxPrice = item.getMaxPrice();
                configs.put(item.getName(), config);
            }

            try (FileWriter writer = new FileWriter(cfg)) {
                gson.toJson(configs, writer);
            }
        } catch (Exception ignored) {

        }
    }

    public void load() {
        if (!cfg.exists()) return;

        try (FileReader reader = new FileReader(cfg)) {
            Type type = new TypeToken<Map<String, ItemConfig>>(){}.getType();
            Map<String, ItemConfig> configs = gson.fromJson(reader, type);

            if (configs == null) return;

            for (BuyableItem item : items) {
                ItemConfig config = configs.get(item.getName());
                if (config != null) {
                    item.setEnabled(config.enabled);
                    item.setMaxPrice(config.maxPrice);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ItemConfig {
        boolean enabled;
        int maxPrice;
    }
}
