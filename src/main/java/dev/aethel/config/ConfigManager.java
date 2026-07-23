package dev.aethel.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aethel.Aethel;
import dev.aethel.module.Module;
import dev.aethel.module.settings.Setting;
import dev.aethel.util.draggable.DragManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final File AETHEL_DIR = new File("Aethel");
    private static final File CONFIGS_DIR = new File(AETHEL_DIR, "configs");
    public static boolean loading = false;

    public static void init() {
        AETHEL_DIR.mkdirs();
        CONFIGS_DIR.mkdirs();
    }

    public static void save(String name) {
        JsonObject root = new JsonObject();
        for (Module module : Aethel.getInstance().getModuleStorage().getModules()) {
            JsonObject modObj = new JsonObject();
            modObj.addProperty("enabled", module.isEnabled());
            modObj.addProperty("key", module.getKey());
            JsonObject settingsObj = new JsonObject();
            for (Setting setting : module.getSettings()) {
                settingsObj.addProperty(setting.getName(), setting.getValueAsString());
            }
            modObj.add("settings", settingsObj);
            root.add(module.getName(), modObj);
        }
        JsonObject draggablesObj = new JsonObject();
        DragManager.draggableElements.forEach((nameKey, draggable) -> {
            JsonObject dObj = new JsonObject();
            dObj.addProperty("x", draggable.getX());
            dObj.addProperty("y", draggable.getY());
            draggablesObj.add(nameKey, dObj);
        });
        root.add("draggables", draggablesObj);
        root.add("staff", StaffManager.save());
        write(new File(CONFIGS_DIR, name + ".json"), root);
    }

    public static void load(String name) {
        File file = new File(CONFIGS_DIR, name + ".json");
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            loading = true;
            for (Module module : Aethel.getInstance().getModuleStorage().getModules()) {
                if (!root.has(module.getName())) continue;
                JsonObject modObj = root.getAsJsonObject(module.getName());
                if (modObj.has("enabled")) {
                    boolean enabled = modObj.get("enabled").getAsBoolean();
                    if (enabled != module.isEnabled()) module.toggle();
                }
                if (modObj.has("key")) module.setKey(modObj.get("key").getAsInt());
                if (modObj.has("settings")) {
                    JsonObject settingsObj = modObj.getAsJsonObject("settings");
                    for (Setting setting : module.getSettings()) {
                        if (settingsObj.has(setting.getName())) {
                            setting.setValueFromString(settingsObj.get(setting.getName()).getAsString());
                        }
                    }
                }
            }
            loading = false;
            if (root.has("draggables")) {
                JsonObject draggablesObj = root.getAsJsonObject("draggables");
                draggablesObj.entrySet().forEach(entry -> {
                    JsonObject dObj = entry.getValue().getAsJsonObject();
                    String nameKey = entry.getKey();
                    var draggable = DragManager.draggableElements.get(nameKey);
                    if (draggable != null) {
                        draggable.setX(dObj.get("x").getAsFloat());
                        draggable.setY(dObj.get("y").getAsFloat());
                    }
                });
            }
            if (root.has("staff")) StaffManager.load(root.getAsJsonArray("staff"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void delete(String name) {
        new File(CONFIGS_DIR, name + ".json").delete();
    }

    public static java.util.List<String> getConfigs() {
        java.util.List<String> list = new java.util.ArrayList<>();
        File[] files = CONFIGS_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                list.add(f.getName().replace(".json", ""));
            }
        }
        return list;
    }

    public static void autoSave() {
        save("autocfg");
    }

    private static void write(File file, JsonObject root) {
        try (FileWriter writer = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
