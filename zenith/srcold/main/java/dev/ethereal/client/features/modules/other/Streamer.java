package dev.ethereal.client.features.modules.other;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import dev.ethereal.api.auth.ProfileRepository;
import dev.ethereal.api.auth.UUIDUtils;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.system.configs.FriendManager;
import dev.ethereal.api.utils.player.PlayerUtil;
import lombok.Getter;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@ModuleRegister(name = "Streamer", category = Category.OTHER)
public class Streamer extends Module {
    @Getter private static final Streamer instance = new Streamer();

    @Getter private MultiBooleanSetting hide = new MultiBooleanSetting("Hides").value(
            new BooleanSetting("Name").value(true),
            new BooleanSetting("Hide friends").value(true).setVisible(() -> getHide().isEnabled("Name"))
    );

    public Streamer() {
        addSettings(hide);
    }

    private final ConcurrentHashMap<String, Integer> friendCounter = new ConcurrentHashMap<>();
    private final AtomicInteger globalCounter = new AtomicInteger(1);

    public String getProtectedName() {
        return this.isEnabled() && hide.isEnabled("Name") ? "Taksa" : mc.getSession().getUsername();
    }

    public String getProtectedFriendName(String name) {
        return this.isEnabled() && hide.isEnabled("Name") && hide.isEnabled("Hide friends") && FriendManager.getInstance().contains(name) ? generateProtectedFriendName(name) : name;
    }

    public String generateProtectedFriendName(String originalName) {
        int id = friendCounter.computeIfAbsent(originalName.toLowerCase(), key -> globalCounter.getAndIncrement());
        return "Друн " + id;
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null || !PlayerUtil.isFT()) return;
        }));

        addEvents(updateEvent);
    }
}
