package dev.aethel.module.settings;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MultiBooleanSetting extends Setting {
    private final Map<String, BooleanSetting> values = new LinkedHashMap<>();

    public MultiBooleanSetting(String name, String desc, BooleanSetting... settings) {
        super(name);
        for (BooleanSetting s : settings) {
            values.put(s.getName(), s);
        }
    }

    public boolean getValue(String key) {
        BooleanSetting setting = values.get(key);
        return setting != null && setting.getValue();
    }

    public BooleanSetting getSetting(String key) {
        return values.get(key);
    }

    public void setValue(String key, boolean value) {
        BooleanSetting setting = values.get(key);
        if (setting != null) setting.setValue(value);
    }

    public List<String> getSelected() {
        return values.entrySet().stream()
                .filter(e -> e.getValue().getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public boolean isSelected(String key) {
        return getValue(key);
    }

    public Collection<BooleanSetting> getSettings() {
        return values.values();
    }

    public Map<String, BooleanSetting> getMap() {
        return values;
    }

    public int getValues() {
        return (int) values.values().stream().filter(BooleanSetting::getValue).count();
    }

    @Override
    public String getValueAsString() {
        return String.join(",", getSelected());
    }

    @Override
    public void setValueFromString(String value) {
        for (BooleanSetting s : values.values()) {
            s.setValue(false);
        }
        if (value == null || value.isEmpty()) return;
        String cleaned = value.trim();
        if (cleaned.startsWith("[")) cleaned = cleaned.substring(1);
        if (cleaned.endsWith("]")) cleaned = cleaned.substring(0, cleaned.length() - 1);
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) return;
        String[] parts = cleaned.split(",");
        for (String key : parts) {
            key = key.trim();
            BooleanSetting s = values.get(key);
            if (s != null) s.setValue(true);
        }
    }

    @Override
    public MultiBooleanSetting setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
