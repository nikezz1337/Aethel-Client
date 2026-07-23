package dev.aethel.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

public class StaffManager {

    private static final List<String> staff = new ArrayList<>();

    public static boolean add(String name) {
        if (staff.contains(name)) return false;
        staff.add(name);
        return true;
    }

    public static boolean remove(String name) {
        return staff.remove(name);
    }

    public static boolean isStaff(String name) {
        return staff.contains(name);
    }

    public static List<String> getStaff() {
        return staff;
    }

    public static void clear() {
        staff.clear();
    }

    public static JsonArray save() {
        JsonArray array = new JsonArray();
        for (String s : staff) {
            array.add(new JsonPrimitive(s));
        }
        return array;
    }

    public static void load(JsonArray array) {
        staff.clear();
        for (var element : array) {
            staff.add(element.getAsString());
        }
    }
}
