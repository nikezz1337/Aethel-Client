package dev.aethel.module.list.misc;

import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.util.sound.ClientSoundPlayer;

@ModuleInformation(
    moduleName = "ClientSounds",
    moduleCategory = ModuleCategory.MISC,
    moduleDesc = "Добавляет звуки клиента"
)
public class ClientSounds extends Module {

    public static ClientSounds INSTANCE;

    public final ModeSetting stateSounds = new ModeSetting("Режим", "Нет",
            "Нет", "Первый", "Второй", "Третий", "Четвертый", "Пятый", "Шестой");
    public final SliderSetting volume = new SliderSetting("Громкость", 50.0, 1.0, 100.0, 0.5);

    public ClientSounds() {
        INSTANCE = this;
    }

    public void playToggleSound(boolean enabled) {
        String soundName = stateSounds.getValue();
        if ("Нет".equals(soundName)) return;

        String fileName = switch (soundName) {
            case "Первый" -> "first.wav";
            case "Второй" -> "second.wav";
            case "Третий" -> "third.wav";
            case "Четвертый" -> "fourth.wav";
            case "Пятый" -> "fifth.wav";
            case "Шестой" -> "sixth.wav";
            default -> null;
        };

        if (fileName == null) return;

        float pitch = enabled ? 1.0f : 0.95f;
        ClientSoundPlayer.playSound(fileName, volume.getValue() / 100.0, pitch);
    }

    public void playOpenGui() {
        ClientSoundPlayer.playSound("opengui.wav", 0.6, 1.0f);
    }

    public void playCloseGui() {
        ClientSoundPlayer.playSound("closegui.wav", 0.6, 1.0f);
    }
}
