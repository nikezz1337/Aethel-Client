package antileak.base.client.modules.impl.misc;

import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.FloatSetting;
import antileak.base.client.modules.settings.implement.ModeSetting;

public class ClientSounds extends Module {

    public static ClientSounds INSTANCE = new ClientSounds();

    public final ModeSetting stateSounds = new ModeSetting("Режим", "Нет",
            "Первый", "Второй", "Третий", "Четвертый", "Пятый", "Шестой");
    public final FloatSetting volume = new FloatSetting("Громкость", 50.0f, 1.0f, 100.0f, 0.5f);

    public ClientSounds() {
        super("ClientSounds", "Добавляет звуки клиента", ModuleCategory.MISC);
        addSettings(stateSounds, volume);
    }
}
