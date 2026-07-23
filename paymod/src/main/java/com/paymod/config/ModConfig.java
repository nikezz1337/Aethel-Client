package com.paymod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

public class ModConfig {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("paymod.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static double keepAmount = 10_000_000;
    private static String targetPlayer = "NURIKCURYMDIKKKK";

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (Reader reader = new FileReader(CONFIG_FILE)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json.has("keepAmount")) {
                    keepAmount = json.get("keepAmount").getAsDouble();
                }
                if (json.has("targetPlayer")) {
                    targetPlayer = json.get("targetPlayer").getAsString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("keepAmount", keepAmount);
        json.addProperty("targetPlayer", targetPlayer);
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(json, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double getKeepAmount() {
        return keepAmount;
    }

    public static String getTargetPlayer() {
        return targetPlayer;
    }

    public static void setKeepAmount(double amount) {
        keepAmount = amount;
    }

    public static void setTargetPlayer(String player) {
        targetPlayer = player;
    }
}
