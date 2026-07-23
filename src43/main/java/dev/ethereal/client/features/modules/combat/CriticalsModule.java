package dev.ethereal.client.features.modules.combat;

import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.player.world.AttackEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import lombok.Getter;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@ModuleRegister(name = "Criticals", category = Category.COMBAT)
public class CriticalsModule extends Module {
    @Getter private static final CriticalsModule instance = new CriticalsModule();

    private final ModeSetting mode = new ModeSetting("Режим").value("HolyWorld Old").values("HolyWorld Old", "Packet", "Web");

    public CriticalsModule() {
        addSettings(mode);
    }

    @Override
    public void onEvent() {
        EventListener attackListener = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;
            if (mc.player.isGliding() || mc.player.hasVehicle()) return;

            switch (mode.getValue()) {
                case "HolyWorld Old" -> doHolyWorldOld(event);
                case "Packet" -> doPacket();
                case "Web" -> doWeb();
            }
        }));

        addEvents(attackListener);
    }

    private void doHolyWorldOld(AttackEvent.AttackEventData event) {
        if (mc.player.isTouchingWater()) return;
        if (!mc.player.isOnGround() && mc.player.fallDistance == 0) {
            mc.player.fallDistance = 0.001f;
            AuraModule aura = AuraModule.getInstance();
            if (aura.isEnabled() && aura.target == event.entity()) return;
            sendPos(mc.player.getX(), mc.player.getY() - 1e-6, mc.player.getZ(), false);
        }
    }

    private void doPacket() {
        if (mc.player.isTouchingWater()) return;
        if (mc.player.isOnGround()) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();

            sendPos(x, y + 0.0625, z, false);
            sendPos(x, y, z, false);
            sendPos(x, y + 1.1E-5, z, false);
            sendPos(x, y, z, false);
        }
    }

    private void doWeb() {
        if (!mc.player.isOnGround() && mc.player.getVelocity().y < 0) {
            boolean inWeb = mc.world.getBlockState(mc.player.getBlockPos()).isOf(Blocks.COBWEB);

            if (inWeb) {
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();

                mc.player.fallDistance = 0.08f;
                sendPos(x, y + 0.035, z, false);
                sendPos(x, y, z, false);
                sendPos(x, y + 0.011, z, false);
                sendPos(x, y, z, false);
            }
        }
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false));
    }
}
