package dev.ethereal.api.utils.auction;

import lombok.AllArgsConstructor;
import dev.ethereal.api.module.setting.ModeSetting;

@AllArgsConstructor
public enum ParseModeChoice implements ModeSetting.NamedChoice {
    FUN_TIME("Fun Time"),
    SPOOKY_TIME("Spooky Time"),
    HOLY_WORLD("Holy World"),
    REALLY_WORLD("Really World");

    private final String name;

    @Override
    public String getName() {
        return name;
    }
}
