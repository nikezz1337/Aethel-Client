package dev.aethel.module.settings;

import java.util.function.Supplier;

public class TextSetting extends Setting {
    private String value;
    private final int maxLength;

    public TextSetting(String name, String defaultValue, int maxLength) {
        super(name);
        this.value = defaultValue;
        this.maxLength = maxLength;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value != null && value.length() <= maxLength ? value : this.value;
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public String getValueAsString() {
        return value;
    }

    @Override
    public void setValueFromString(String value) {
        setValue(value);
    }

    @Override
    public TextSetting setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
