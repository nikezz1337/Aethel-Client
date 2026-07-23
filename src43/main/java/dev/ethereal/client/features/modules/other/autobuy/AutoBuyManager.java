package dev.ethereal.client.features.modules.other.autobuy;

import dev.ethereal.api.system.backend.ClientInfo;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

public class AutoBuyManager {
    private static AutoBuyManager instance;
    private boolean enabled = false;
    private final File configFile;

    private AutoBuyManager() {
        configFile = new File(ClientInfo.CONFIG_PATH_OTHER, "autobuy_prices.json");
        loadPrices();
    }

    public static AutoBuyManager getInstance() {
        if (instance == null) instance = new AutoBuyManager();
        return instance;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<AutoBuyableItem> getAllItems() {
        return FunTimeProvider.getItems();
    }

    public void toggleItem(AutoBuyableItem item) {
        item.setEnabled(!item.isEnabled());
    }

    public void savePrice(String displayName, int price) {
        try {
            JSONObject json;
            if (configFile.exists()) {
                json = new JSONObject(new JSONTokener(new FileReader(configFile)));
            } else {
                json = new JSONObject();
            }
            json.put(displayName, price);
            configFile.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(configFile)) {
                w.write(json.toString(2));
            }
        } catch (Exception ignored) {}
    }

    public void loadPrices() {
        try {
            if (!configFile.exists()) return;
            JSONObject json = new JSONObject(new JSONTokener(new FileReader(configFile)));
            for (AutoBuyableItem item : FunTimeProvider.getItems()) {
                if (item == null || item.getDisplayName() == null) continue;
                if (json.has(item.getDisplayName())) {
                    int price = json.getInt(item.getDisplayName());
                    item.getSettings().setBuyBelow(price);
                }
            }
        } catch (Exception ignored) {}
    }
}
