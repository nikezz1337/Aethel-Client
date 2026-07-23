package dev.aethel.module.list.misc;

import dev.aethel.config.FriendManager;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.TextSetting;

@ModuleInformation(
    moduleName = "NameProtect",
    moduleCategory = ModuleCategory.MISC,
    moduleDesc = "Скрывает никнеймы"
)
public class NameProtect extends Module {

    public static NameProtect INSTANCE;

    public final TextSetting replacementName =
            new TextSetting("Текст замены", "Protected", 32);
    public final BooleanSetting friends =
            new BooleanSetting("Скрывать друзей", true);
    public final BooleanSetting grief =
            new BooleanSetting("Скрывать информацию", false);

    public NameProtect() {
        INSTANCE = this;
    }

    public String patchIncomingText(String text) {
        return patch(text);
    }

    public String patch(String text) {
        if (text == null || !isEnabled() || mc == null || mc.player == null) return text;

        String out = text;
        String replacement = getReplacementName();
        String username = mc.getSession().getUsername();

        if (username != null && !username.isEmpty()) {
            out = replaceIgnoreCase(out, username, replacement);
        }

        if (friends.getValue()) {
            for (String friend : FriendManager.getFriends()) {
                out = replaceIgnoreCase(out, friend, replacement);
            }
        }

        if (grief.getValue()) {
            out = out.replaceAll("Анархия-\\d+", replacement);
            out = out.replaceAll("ГРИФ #\\d+", replacement);
        }

        return out;
    }

    public String getReplacementName() {
        String value = replacementName.getValue();
        return (value == null || value.isBlank()) ? "Protected" : value;
    }

    public boolean shouldHideGrief() {
        return grief.getValue();
    }

    private String replaceIgnoreCase(String text, String target, String replacement) {
        if (text == null || target == null || target.isEmpty()) return text;
        int firstIndex = indexOfIgnoreCase(text, target, 0);
        if (firstIndex < 0) return text;

        StringBuilder out = new StringBuilder(text.length() + replacement.length());
        int from = 0;
        int index = firstIndex;
        while (index >= 0) {
            out.append(text, from, index).append(replacement);
            from = index + target.length();
            index = indexOfIgnoreCase(text, target, from);
        }
        out.append(text, from, text.length());
        return out.toString();
    }

    private int indexOfIgnoreCase(String text, String target, int from) {
        int max = text.length() - target.length();
        for (int i = Math.max(0, from); i <= max; i++) {
            if (text.regionMatches(true, i, target, 0, target.length())) return i;
        }
        return -1;
    }
}
