package dev.aethel.module.list.misc;

import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.SliderSetting;

@ModuleInformation(
    moduleName = "ItemScroller",
    moduleCategory = ModuleCategory.MISC,
    moduleDesc = "Быстрое перемещение предметов"
)
public class ItemScroller extends Module {
    public static final ItemScroller INSTANCE = new ItemScroller();
    private final SliderSetting delay = new SliderSetting("Задержка", 50, 0, 200, 10);
    private long lastQuickMoveAt;

    public ItemScroller() {}

    public boolean canQuickMove() {
        long now = System.currentTimeMillis();
        if (now - lastQuickMoveAt < (long) delay.getFloatValue()) return false;
        lastQuickMoveAt = now;
        return true;
    }

    public void resetTimer() {
        lastQuickMoveAt = 0;
    }
}
