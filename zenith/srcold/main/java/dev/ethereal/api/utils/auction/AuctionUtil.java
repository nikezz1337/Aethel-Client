package dev.ethereal.api.utils.auction;

import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.player.PlayerUtil;
import lombok.experimental.UtilityClass;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

@UtilityClass
public class AuctionUtil implements QuickImports {

    public boolean buyMenu(String name) {
        return name.contains("Покупка предмета") || name.contains("☃") && name.contains("Подтверждение покупки");
    }

    public int getPrice(ItemStack stack) {
        for (Text text : stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC)) {
            String str = text.getString().replace("§r", "").replace("¤", "").trim();
            String textPrice = getStr();
            if (str.startsWith(textPrice)) return Integer.parseInt(str.replace(textPrice, "").replace(",", "").replace(" ", "").trim());
        }
        return -1;
    }

    private String getStr() {
        if (PlayerUtil.isFT()) {
            return "$ Ценa:";
        } else if (PlayerUtil.isST()) {
            return "$ Цена: $";
        } else if (PlayerUtil.isHW()) {
            return "▍ Цена за 1 ед.:";
        } else if (PlayerUtil.isRW()) {
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
