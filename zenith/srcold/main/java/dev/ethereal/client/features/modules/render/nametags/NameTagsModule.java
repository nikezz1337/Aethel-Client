package dev.ethereal.client.features.modules.render.nametags;

import lombok.Getter;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ColorSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.utils.combat.TargetManager;

import java.awt.*;
import java.util.function.Supplier;

@ModuleRegister(name = "Name Tags", category = Category.RENDER)
public class NameTagsModule extends Module {
    @Getter private static final NameTagsModule instance = new NameTagsModule();

    public final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Себя").value(true),
            new BooleanSetting("Игроки").value(true),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Предметы").value(true)
    );
    public final MultiBooleanSetting information = new MultiBooleanSetting("Информация").value(
            new BooleanSetting("Предметы").value(true),
            new BooleanSetting("Зелья").value(false)
    );

    private final Supplier<Boolean> itemsIsEnabled = () -> information.isEnabled("Предметы");

    public final MultiBooleanSetting options = new MultiBooleanSetting("Настройки").value(
            new BooleanSetting("Идикация").value(true).setVisible(itemsIsEnabled),
            new BooleanSetting("Зачарования").value(false).setVisible(itemsIsEnabled),
            new BooleanSetting("Only hands").value(false).setVisible(itemsIsEnabled)
    );

    public final BooleanSetting box3d = new BooleanSetting("3D Box").value(false);
    public final SliderSetting boxAlpha = new SliderSetting("Box alpha").value(0.3f).range(0.0f, 1f).step(0.05f).setVisible(box3d::getValue);

    public final TargetManager.EntityFilter entityFilter = new TargetManager.EntityFilter(targets.getList());

    private final NameTagsRender nameTagsRender = new NameTagsRender(this);

    public NameTagsModule() {
        addSettings(targets, information, options, box3d, boxAlpha);
    }

    @Override
    public void onEvent() {
        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(1, event -> {
            entityFilter.targetSettings = targets.getList();
            entityFilter.needFriends = true;

            nameTagsRender.onRender(event);
        }));

        addEvents(render2DEvent);
    }
}
