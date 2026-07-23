package dev.aethel.config;

import java.util.*;

public class MacroManager {

    public static class Macro {
        public final String name;
        public final int key;
        public final String command;

        public Macro(String name, int key, String command) {
            this.name = name;
            this.key = key;
            this.command = command;
        }
    }

    private static final Map<String, Macro> macros = new LinkedHashMap<>();

    public static void add(String name, int key, String command) {
        macros.put(name, new Macro(name, key, command));
    }

    public static Macro remove(String name) {
        return macros.remove(name);
    }

    public static Macro get(String name) {
        return macros.get(name);
    }

    public static List<Macro> getAll() {
        return List.copyOf(macros.values());
    }

    public static void clear() {
        macros.clear();
    }

    public static boolean exists(String name) {
        return macros.containsKey(name);
    }
}
