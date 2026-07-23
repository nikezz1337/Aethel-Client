package dev.aethel.module.list.misc;

import com.google.common.eventbus.Subscribe;
import dev.aethel.config.FriendManager;
import dev.aethel.event.list.EventPacket;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ModuleInformation(
    moduleName = "AutoAccept",
    moduleCategory = ModuleCategory.MISC,
    moduleDesc = "Авто-принимает входящие телепорты"
)
public class AutoAccept extends Module {

    private final BooleanSetting onlyFriend = new BooleanSetting("Только друзья", false);
    private String lastInviter;

    @Subscribe
    public void onEvent(EventPacket event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getType() != EventPacket.Type.RECEIVE) return;

        if (!(event.getPacket() instanceof GameMessageS2CPacket packet)) return;

        String raw = packet.content().getString().toLowerCase(Locale.ROOT);

        if (raw.contains("телепортироваться") || raw.contains("has requested teleport") || raw.contains("просит к вам телепортироваться")) {
            if (onlyFriend.getValue()) {
                boolean isFriend = false;
                for (String friend : FriendManager.getFriends()) {
                    if (raw.contains(friend.toLowerCase(Locale.ROOT))) {
                        isFriend = true;
                        break;
                    }
                }
                if (!isFriend) return;
            }
            mc.player.networkHandler.sendChatCommand("tpaccept");
            return;
        }

        if (raw.contains("приглашает вас в клан") || raw.contains("invites you to join the clan")) {
            lastInviter = solveName(packet.content().getString());
            return;
        }

        if (raw.contains("вступить") || raw.contains("отклонить") || raw.contains("accept") || raw.contains("decline")) {
            if (lastInviter == null) {
                lastInviter = solveName(packet.content().getString());
            }

            if (onlyFriend.getValue()) {
                if (lastInviter == null) return;
                boolean isFriend = false;
                for (String friend : FriendManager.getFriends()) {
                    if (lastInviter.equalsIgnoreCase(friend)) {
                        isFriend = true;
                        break;
                    }
                }
                if (!isFriend) {
                    lastInviter = null;
                    return;
                }
            }

            clickClanAcceptButton(packet.content());
            lastInviter = null;
        }
    }

    private void clickClanAcceptButton(Text text) {
        for (Text component : getTextComponents(text)) {
            Style style = component.getStyle();
            ClickEvent clickEvent = style.getClickEvent();
            String content = component.getString();

            if (clickEvent != null && (content.contains("Вступить") || content.contains("Принять") || content.contains("Accept"))) {
                String command = null;

                if (clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                    command = clickEvent.getValue();
                } else if (clickEvent.getAction() == ClickEvent.Action.SUGGEST_COMMAND) {
                    command = clickEvent.getValue();
                }

                if (command != null) {
                    if (command.startsWith("/")) command = command.substring(1);
                    mc.player.networkHandler.sendChatCommand(command);
                    return;
                }
            }
        }
    }

    private List<Text> getTextComponents(Text text) {
        List<Text> components = new ArrayList<>();
        components.add(text);
        for (Text sibling : text.getSiblings()) {
            components.addAll(getTextComponents(sibling));
        }
        return components;
    }

    private String solveName(String raw) {
        if (mc.getNetworkHandler() == null) return null;
        for (var entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().getName();
            if (raw.contains(name)) return name;
        }
        return null;
    }
}
