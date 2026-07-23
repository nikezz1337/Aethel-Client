package dev.ethereal.client.features.modules.other.autobuy;

import java.util.List;

public class HolyTimeProvider {

    public static List<AutoBuyableItem> getItems() {
        return FunTimeProvider.getItems();
    }

    public static void reload() {
        FunTimeProvider.reload();
    }
}
