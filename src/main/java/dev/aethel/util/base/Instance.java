package dev.aethel.util.base;

import dev.aethel.Aethel;
import dev.aethel.module.Module;

public class Instance {
    public static <T extends Module> T get(Class<T> clazz) {
        return Aethel.getInstance().getModuleStorage().get(clazz);
    }
}
