package dev.ethereal.api.system.backend;

import lombok.Getter;
import dev.ethereal.api.module.setting.Setting;
import dev.ethereal.api.system.interfaces.QuickImports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class Configurable implements QuickImports {
    private final List<Setting<?>> settings = new ArrayList<>();

    public void addSettings(Setting<?>... settings) {
        this.settings.addAll(Arrays.asList(settings));
    }

    public void addSettings(List<Setting<?>> settings) {
        this.settings.addAll(settings);
    }
}
