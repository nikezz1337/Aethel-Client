package ru.zenith.implement.features.modules.render;

import dev.redstones.mediaplayerinfo.IMediaSession;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.setting.implement.BindSetting;
import ru.zenith.api.feature.module.setting.implement.ColorSetting;
import ru.zenith.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.keyboard.KeyEvent;
import ru.zenith.implement.features.draggables.MediaPlayer;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hud extends Module {
    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Elements", "Customize the interface elements")
                .value("Watermark", "Hot Keys", "Potions", "Staff List", "Target Hud", "Armor", "Cool Downs", "Inventory", "Player Info", "Boss Bars", "Notifications", "Score Board", "Media Player", "HotBar");

    public MultiSelectSetting notificationSettings = new MultiSelectSetting("Notifications", "Choose when the notification will appear")
            .value("Module Switch", "Staff Join", "Item Pick Up", "Auto Armor", "Break Shield").visible(()-> interfaceSettings.isSelected("Notifications"));

    public ColorSetting colorSetting = new ColorSetting("Client Color", "Select your client's color")
            .setColor(0xFF6C9AFD).presets(0xFF6C9AFD, 0xFF8C7FFF, 0xFFFFA576, 0xFFFF7B7B);

    BindSetting preSetting = new BindSetting("Previous Audio", "Turn on previous audio")
            .visible(()-> interfaceSettings.isSelected("Media Player"));

    BindSetting playSetting = new BindSetting("Stop/Play Audio",   "Stop/Play current audio")
            .visible(()-> interfaceSettings.isSelected("Media Player"));

    BindSetting nextSetting = new BindSetting("Next Audio","Turn on next audio")
            .visible(()-> interfaceSettings.isSelected("Media Player"));

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        setup(colorSetting, interfaceSettings, notificationSettings, preSetting, playSetting, nextSetting);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        IMediaSession session = MediaPlayer.getInstance().session;
        if (interfaceSettings.isSelected("Media Player") && session != null) {
            if (e.isKeyDown(preSetting.getKey(), true)) session.previous();
            if (e.isKeyDown(playSetting.getKey(), true)) session.playPause();
            if (e.isKeyDown(nextSetting.getKey(), true)) session.next();
        }
    }
}
