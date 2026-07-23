package antileak.base.client.modules.impl.movement;

import com.adl.nativeprotect.Native;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventMoveInput;
import antileak.base.api.events.implement.EventPacket;
import antileak.base.api.events.implement.EventUpdatePost;
import antileak.base.api.utils.network.NetworkUtils;
import antileak.base.client.modules.Module;
@Native
public class Speed extends Module {

    public static Speed INSTANCE = new Speed();

    private int ticks = 0;
    private int groundTicks = 0;

    public Speed() {
        super("Speed", "Новый bypass speed", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @EventLink
    public void onMoveInput(EventMoveInput event) {
        if (mc.player == null) {
            return;
        }

        if (mc.player.verticalCollision) {
            groundTicks++;
        } else {
            groundTicks = 0;
        }

        if (groundTicks >= 1) {
            mc.player.jump();
        }
    }

    @EventLink
    public void onPostMotion(EventUpdatePost event) {
        if (mc.player == null || mc.world == null) {
            resetState();
            return;
        }

        setTimerSpeed(1.7F);

        if (ticks > 3) {
            double boost = 0.03D;

            if (ticks % 2 == 0) {
                addVelocity(0.0D, 0.03D, 0.0D);
                boost = mc.player.isOnGround() ? 0.085D : 0.03D;
            }

            double yaw = Math.toRadians(getMoveYaw());
            double x = -Math.sin(yaw);
            double z = Math.cos(yaw);

            if (getMoveYaw() == -1.0F) {
                x = 0.0D;
                z = 0.0D;
            }

            addVelocity(x * boost, 0.0D, z * boost);
        }

        ticks++;

        if (ticks % 2 == 0 && mc.player.networkHandler != null) {
            setTimerSpeed(0.3F);
            NetworkUtils.sendSilentPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
    }

    @EventLink
    public void onPacket(EventPacket event) {
        if (mc.player == null || event.getType() != EventPacket.Type.RECEIVE) {
            return;
        }

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            if (ticks % 2 == 1) {
                ticks++;
            }

            setTimerSpeed(1.0F);
        }
    }

    private float getMoveYaw() {
        if (mc.player == null) {
            return -1.0F;
        }

        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;

        if (forward == 0.0F && strafe == 0.0F) {
            return -1.0F;
        }

        float yaw = mc.player.getYaw();

        if (forward != 0.0F) {
            if (strafe > 0.0F) {
                yaw += forward > 0.0F ? -45.0F : 45.0F;
            } else if (strafe < 0.0F) {
                yaw += forward > 0.0F ? 45.0F : -45.0F;
            }

            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
        }

        if (strafe > 0.0F) {
            yaw += 90.0F;
        } else if (strafe < 0.0F) {
            yaw -= 90.0F;
        }

        return yaw;
    }

    private void addVelocity(double x, double y, double z) {
        if (mc.player == null) {
            return;
        }

        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x + x, velocity.y + y, velocity.z + z);
    }

    private void setTimerSpeed(float speed) {
        if (mc.player != null) {
            mc.player.speed = speed;
        }
    }

    private void resetState() {
        ticks = 0;
        groundTicks = 0;
        setTimerSpeed(1.0F);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }
}
