package dev.aethel.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.aethel.Aethel;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class FriendManager {

    private static final List<String> friends = new ArrayList<>();
    private static final File FILE = new File("Aethel", "friend.json");

    public static boolean add(String name) {
        if (friends.contains(name)) return false;
        friends.add(name);
        saveToFile();
        return true;
    }

    public static boolean remove(String name) {
        boolean removed = friends.remove(name);
        if (removed) saveToFile();
        return removed;
    }

    public static boolean isFriend(String name) {
        return friends.contains(name);
    }

    public static void clear() {
        friends.clear();
        saveToFile();
    }

    public static List<String> getFriends() {
        return friends;
    }

    public static void saveToFile() {
        try (FileWriter writer = new FileWriter(FILE)) {
            JsonArray array = new JsonArray();
            for (String friend : friends) {
                array.add(new JsonPrimitive(friend));
            }
            new GsonBuilder().setPrettyPrinting().create().toJson(array, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadFromFile() {
        if (!FILE.exists()) return;
        try (FileReader reader = new FileReader(FILE)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            friends.clear();
            for (var element : array) {
                friends.add(element.getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
