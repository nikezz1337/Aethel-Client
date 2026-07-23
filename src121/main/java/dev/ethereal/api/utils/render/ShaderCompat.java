package dev.ethereal.api.utils.render;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

public final class ShaderCompat {
    private ShaderCompat() {
    }

    public static boolean isIrisShaderPackActive() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return false;
        }

        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstance = irisApiClass.getMethod("getInstance");
            Object irisApi = getInstance.invoke(null);
            Method isShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
            Object result = isShaderPackInUse.invoke(irisApi);
            return result instanceof Boolean active && active;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
