package dev.aethel.util.auction;

import dev.aethel.util.IMinecraft;
import dev.aethel.util.auction.nbt.NbtUtils;
import dev.aethel.util.other.ServerUtil;
import lombok.experimental.UtilityClass;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class AuctionUtil implements IMinecraft {
    private final Pattern FORMAT_PATTERN = Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");
    private final Pattern PRICE_PATTERN = Pattern.compile("(\\d{1,3}(?:[\\s,._\\u00A0\\u202F]\\d{3})+|\\d+)");
    private final Pattern LABELED_PRICE_PATTERN = Pattern.compile(
            "(?iu)(?:\\bprice\\b|\\bcost\\b|\\b\\u0446\\u0435\\u043d\\u0430\\b|\\b\\u0441\\u0442\\u043e\\u0438\\u043c\\u043e\\u0441\\u0442\\u044c\\b)\\s*(?:[:\\uFF1A]\\s*)?\\$?\\s*([0-9]{1,3}(?:[\\s,._\\u00A0\\u202F]\\d{3})*|\\d+)"
    );

    public boolean buyMenu(String name) {
        return name.contains("Покупка предмета") || name.contains("☃") && name.contains("Подтверждение покупки") || name.contains("☃") && name.contains("Подозрительная цена") || name.startsWith("0u1t2z4");
    }

    public int getPrice(ItemStack stack) {
        int bestPrice = getPrice(stack.get(DataComponentTypes.CUSTOM_DATA));

        for (Text text : stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC)) {
            String line = stripFormatting(text.getString().replace("Â§r", "").replace("Â¤", "").trim());
            String textPrice = stripFormatting(getStr());
            if (!textPrice.isEmpty() && line.startsWith(textPrice)) {
                int parsed = parseDigits(line.replace(textPrice, ""));
                if (parsed > bestPrice) bestPrice = parsed;
            }

            int parsed = parsePrice(line);
            if (parsed > bestPrice) bestPrice = parsed;
        }

        return bestPrice;
    }

    public int getPrice(NbtComponent component) {
        if (component == null) return -1;

        NbtCompound root = component.copyNbt();
        if (!root.contains("display")) return -1;

        NbtCompound display = root.getCompound("display");
        NbtList lore = display.getList("Lore", 8);

        int bestPrice = -1;
        for (int i = 0; i < lore.size(); i++) {
            int price = parsePrice(stripFormatting(lore.getString(i)));
            if (price > bestPrice) bestPrice = price;
        }

        return bestPrice;
    }

    public boolean compareEnchantments(ItemStack stack1, ItemStack stack2) {
        NbtComponent nbt1 = stack1.getComponents().get(DataComponentTypes.CUSTOM_DATA);
        NbtComponent nbt2 = stack2.getComponents().get(DataComponentTypes.CUSTOM_DATA);
        if (nbt1 == null || nbt2 == null) return nbt1 == nbt2;

        NbtCompound enchants1 = NbtUtils.copyNbtKeys(nbt1.copyNbt(), "Enchantments");
        NbtCompound enchants2 = NbtUtils.copyNbtKeys(nbt2.copyNbt(), "Enchantments");
        return NbtUtils.matchesNbtValues(enchants1, enchants2);
    }

    private int parsePrice(String line) {
        if (!looksLikePriceLine(line)) return -1;

        Matcher labeledMatcher = LABELED_PRICE_PATTERN.matcher(line);
        if (labeledMatcher.find()) {
            int labeledPrice = parseDigits(labeledMatcher.group(1));
            if (labeledPrice >= 0) return labeledPrice;
        }

        int max = -1;
        Matcher matcher = PRICE_PATTERN.matcher(line);
        while (matcher.find()) {
            int value = parseDigits(matcher.group(1));
            if (value > max) max = value;
        }
        return max;
    }

    private boolean looksLikePriceLine(String text) {
        return text.contains("price")
                || text.contains("cost")
                || text.contains("coin")
                || text.contains("$")
                || text.contains("₽")
                || text.contains("цен")
                || text.contains("стоим")
                || text.contains("монет")
                || text.contains("\\u0446\\u0435\\u043d")
                || text.contains("\\u0441\\u0442\\u043e\\u0438\\u043c");
    }

    private int parseDigits(String value) {
        if (value == null || value.isBlank()) return -1;
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return -1;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private String stripFormatting(String input) {
        if (input == null || input.isBlank()) return "";
        String normalized = input
                .replace("Ã‚Â§", "§")
                .replace("Ãƒâ€šÃ‚Â§", "§")
                .replace("\\u00A7", "\u00A7");
        return FORMAT_PATTERN.matcher(normalized).replaceAll("").toLowerCase(Locale.ROOT);
    }

    private String getStr() {
        if (ServerUtil.isFT()) {
            return "$ Ценa:";
        } else if (ServerUtil.isST()) {
            return "$ Цена: $";
        } else if (ServerUtil.isHW()) {
            return "▍ Цена:";
        } else if (ServerUtil.isRW()) {
            return "Цена:";
        }

        return "";
    }

    public boolean isSearchScreen(String title) {
        return title.contains("[☃] Аукционы") || title.contains("Маркет") || title.contains("ꈁꀀꈂꌲꈂꀁ") || title.contains("Аукционы") || title.contains("Поиск:") || title.contains("Аукцион") || title.contains("Аукционы ") || title.equals("☃ Аукцион") || title.startsWith("☃ П:") || title.startsWith("☃ Поиск:") || title.startsWith("0A2z");
    }

    public boolean isContainerScreen(String title) {
        return title.contains("☃") && title.contains("Хранилище") || title.contains("Товары на продаже");
    }
}
