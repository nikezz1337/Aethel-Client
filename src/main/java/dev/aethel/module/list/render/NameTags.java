package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventHUD;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.ModeListSetting;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.util.render.providers.ColorProvider;

@ModuleInformation(
        moduleName = "NameTags",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Кастомные теги над игроками, предметами и сущностями"
)
public class NameTags extends Module {

    private static NameTags instance;

    public static NameTags getInstance() {
        return instance;
    }

    public final ModeListSetting targets = new ModeListSetting("Цели",
            new BooleanSetting("Себя", true),
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Животные", false),
            new BooleanSetting("Мобы", false),
            new BooleanSetting("Предметы", true)
    );

    public final ModeListSetting information = new ModeListSetting("Информация",
            new BooleanSetting("Предметы", true),
            new BooleanSetting("Зелья", false)
    );

    public final ModeListSetting options = new ModeListSetting("Настройки",
            new BooleanSetting("Индикация", true),
            new BooleanSetting("Зачарования", false),
            new BooleanSetting("Only hands", false)
    );

    public final BooleanSetting box3d = new BooleanSetting("3D Бокс", false);
    public final SliderSetting boxAlpha = new SliderSetting("Прозрачность бокса", 0.3f, 0.0f, 1.0f, 0.05f);

    private final NameTagsRender nameTagsRender = new NameTagsRender(this);

    public NameTags() {
        instance = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Subscribe
    private void onRender(EventHUD e) {
        if (mc.world == null || mc.player == null) return;
        nameTagsRender.onRender(e);
    }
}
