package antileak.base.client.modules.impl.misc;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import antileak.base.elysium;
import antileak.base.api.utils.replace.ReplaceUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.BooleanSetting;
import antileak.base.client.modules.settings.implement.TextSetting;
import antileak.base.mixin.ChatScreenAccessor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NameProtect extends Module {

    public static final NameProtect INSTANCE = new NameProtect();
    private final BooleanSetting friends = new BooleanSetting("Скрывать друзей", true);
    private final BooleanSetting grief = new BooleanSetting("Скрывать информацию", false);
    private final TextSetting nickname = new TextSetting("Никнейм", "elysiumdlc.fun", 32);

    private static final int PATCH_CACHE_LIMIT = 512;
    private final Map<String, String> patchCache = new LinkedHashMap<>(PATCH_CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > PATCH_CACHE_LIMIT;
        }
    };

    private NameProtect() {
        super("NameProtect", "Скрывает никнеймы", ModuleCategory.MISC);
        addSettings(friends, grief, nickname);
    }

    public String patch(String text) {
        if (text == null) {
            return null;
        }
        if (!shouldPatch()) {
            return text;
        }

        String cacheKey = getPatchCacheKey(text);
        String cached = patchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String out = text;
        String replacement = getReplacementName();
        out = replaceIgnoreCase(out, mc.getSession().getUsername(), replacement);
        if (friends.isState() && elysium.INSTANCE != null && elysium.INSTANCE.friendStorage != null) {
            for (String friend : elysium.INSTANCE.friendStorage.getFriends()) {
                out = replaceIgnoreCase(out, friend, replacement);
            }
        }
        out = patchGrief(out);
        patchCache.put(cacheKey, out);
        return out;
    }

    public String patchIncomingText(String text) {
        return patch(text);
    }

    public Text patchText(Text text) {
        if (text == null) {
            return null;
        }

        if (!shouldPatch()) {
            return text;
        }

        Text output = text;
        String replacement = getReplacementName();
        output = ReplaceUtils.replace(output, mc.getSession().getUsername(), replacement);
        if (friends.isState() && elysium.INSTANCE != null && elysium.INSTANCE.friendStorage != null) {
            for (String friend : elysium.INSTANCE.friendStorage.getFriends()) {
                output = ReplaceUtils.replace(output, friend, replacement);
            }
        }
        return output;
    }

    public String getReplacementName() {
        String value = nickname.get();
        return value == null || value.isBlank() ? "elysium" : value;
    }

    public boolean shouldHideGrief() {
        return grief.isState();
    }

    private String replaceIgnoreCase(String text, String target, String replacement) {
        if (text == null || target == null || target.isEmpty()) {
            return text;
        }
        int firstIndex = indexOfIgnoreCase(text, target, 0);
        if (firstIndex < 0) {
            return text;
        }

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
            if (text.regionMatches(true, i, target, 0, target.length())) {
                return i;
            }
        }
        return -1;
    }

    private String patchGrief(String text) {
        if (text == null || !grief.isState()) {
            return text;
        }

        String out = text.replaceAll("Анархия-\\d+", "elysiumclient.fun");
        out = out.replaceAll("ГРИФ #\\d+", "elysiumclient.fun");
        return out;
    }

    private String getPatchCacheKey(String text) {
        String username = mc != null && mc.getSession() != null ? mc.getSession().getUsername() : "";
        int friendsHash = 0;
        if (friends.isState() && elysium.INSTANCE != null && elysium.INSTANCE.friendStorage != null) {
            List<String> friendList = elysium.INSTANCE.friendStorage.getFriends();
            friendsHash = friendList.hashCode();
        }
        return username + '\u0001'
                + getReplacementName() + '\u0001'
                + friends.isState() + '\u0001'
                + grief.isState() + '\u0001'
                + friendsHash + '\u0001'
                + text;
    }

    private boolean shouldPatch() {
        return isEnable() && mc != null && mc.player != null && mc.world != null && !isFriendRemoveInputActive();
    }

    private boolean isFriendRemoveInputActive() {
        if (!(mc.currentScreen instanceof ChatScreen chatScreen)) {
            return false;
        }

        TextFieldWidget chatField = ((ChatScreenAccessor) chatScreen).elysium$getChatField();
        if (chatField == null) {
            return false;
        }

        String input = chatField.getText();
        if (input == null) {
            return false;
        }

        String normalized = input.trim().toLowerCase();
        String prefix = elysium.INSTANCE != null && elysium.INSTANCE.commandStorage != null
                ? elysium.INSTANCE.commandStorage.getPrefix().toLowerCase()
                : ".";
        return normalized.startsWith(prefix + "friend remove");
    }
}
