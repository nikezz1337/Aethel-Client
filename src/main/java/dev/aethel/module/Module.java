package dev.aethel.module;

import net.minecraft.client.MinecraftClient;
import dev.aethel.Aethel;
import dev.aethel.config.ConfigManager;
import dev.aethel.module.list.misc.ClientSounds;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.module.settings.Setting;
import dev.aethel.ui.hud.NotificationRenderer;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.QuickLogger;
import dev.aethel.util.base.Instance;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Module implements IMinecraft, QuickLogger {
    private final String name, desc;
    private final ModuleCategory category;
    private int key;
    private boolean enabled;
    private final Animation animation = new Animation(Easing.BACK_OUT, 450);

    public static final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<Setting> settings = new ArrayList<>();

    public Module() {
        ModuleInformation information = getClass().getAnnotation(ModuleInformation.class);

        this.name = information.moduleName();
        this.desc = information.moduleDesc();
        this.category = information.moduleCategory();
        this.key = information.moduleKeybind();
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Animation getAnimation() {
        return animation;
    }

    public MinecraftClient getMc() {
        return mc;
    }

    public List<Setting> getSettings() {
        return Arrays.stream(this.getClass().getDeclaredFields()).map(field -> {
            try {
                field.setAccessible(true);
                return field.get(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }).filter(field -> field instanceof Setting).map(field -> (Setting) field).collect(Collectors.toList());
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
            if (!ConfigManager.loading) NotificationRenderer.post(name, enabled);
        }
    }


    public void onEnable() {
        Aethel.getInstance().getEventBus().register(this);
        if (!ConfigManager.loading && shouldPlayToggleSound() && ClientSounds.INSTANCE != null && ClientSounds.INSTANCE.isEnabled()) {
            ClientSounds.INSTANCE.playToggleSound(true);
        }
    }

    public void onDisable() {
        Aethel.getInstance().getEventBus().unregister(this);
        if (!ConfigManager.loading && shouldPlayToggleSound() && ClientSounds.INSTANCE != null && ClientSounds.INSTANCE.isEnabled()) {
            ClientSounds.INSTANCE.playToggleSound(false);
        }
    }

    protected boolean shouldPlayToggleSound() {
        return this.getClass() != ClientSounds.class;
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }
}
