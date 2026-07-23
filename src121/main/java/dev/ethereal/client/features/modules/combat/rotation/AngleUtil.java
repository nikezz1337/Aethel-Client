package dev.ethereal.client.features.modules.combat.rotation;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.system.interfaces.QuickImports;

import static java.lang.Math.hypot;
import static java.lang.Math.toDegrees;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class AngleUtil implements QuickImports {
    public static Angle fromVec2f(Vec2f vector2f) {
        return new Angle(vector2f.y, vector2f.x);
    }

    public static Angle fromVec3d(Vec3d vector) {
        return new Angle((float) wrapDegrees(toDegrees(Math.atan2(vector.z, vector.x)) - 90), (float) wrapDegrees(toDegrees(-Math.atan2(vector.y, hypot(vector.x, vector.z)))));
    }



    public static Angle calculateDelta(Angle start, Angle end) {
        float deltaYaw = MathHelper.wrapDegrees(end.getYaw() - start.getYaw());
        float deltaPitch = MathHelper.wrapDegrees(end.getPitch() - start.getPitch());
        return new Angle(deltaYaw, deltaPitch);
    }

    public static Angle calculateAngle(Vec3d to) {
        return fromVec3d(to.subtract(mc.player.getEyePos()));
    }

    public static Angle pitch(float pitch) {
        return new Angle(mc.player.getYaw(), pitch);
    }

    public static Angle cameraAngle() {
        return new Angle(mc.player.getYaw(), mc.player.getPitch());
    }
}
