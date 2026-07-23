package dev.ethereal.api.utils.rotation.rotations;

import dev.ethereal.api.utils.math.MathUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationMode;

import java.util.Random;

public class HvHRotation extends RotationMode {
    private static final Random RANDOM = new Random();

    private int snapCounter = 0;

    public HvHRotation() {
        super("HvH");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        
        float totalDelta = (float) Math.hypot(yawDelta, pitchDelta);
        
        snapCounter++;

        if (totalDelta > 25.0f) {
            float yawSpeed = Math.abs(yawDelta) * (0.95f + MathUtil.random(0, 0.1f));
            float pitchSpeed = Math.abs(pitchDelta) * (0.95f + MathUtil.random(0, 0.1f));
            
            float newYaw = currentRotation.getYaw() + Math.signum(yawDelta) * Math.min(Math.abs(yawDelta), yawSpeed);
            float newPitch = currentRotation.getPitch() + Math.signum(pitchDelta) * Math.min(Math.abs(pitchDelta), pitchSpeed);
 
            return new Rotation(newYaw, MathHelper.clamp(newPitch, -90.0f, 90.0f));
        }

        if (totalDelta > 5.0f) {
            float speedMult = 0.75f + MathUtil.random(0, 0.2f);
            
            float newYaw = currentRotation.getYaw() + yawDelta * speedMult;
            float newPitch = currentRotation.getPitch() + pitchDelta * speedMult;
            
            if (snapCounter % 3 == 0) {
                newYaw += MathUtil.random(-0.5f, 0.5f);
                newPitch += MathUtil.random(-0.3f, 0.3f);
            }
  
            return new Rotation(newYaw, MathHelper.clamp(newPitch, -90.0f, 90.0f));
        }
        
        float microYaw = yawDelta * (0.4f + MathUtil.random(0, 0.15f));
        float microPitch = pitchDelta * (0.4f + MathUtil.random(0, 0.15f));
        
        if (snapCounter % 2 == 0) {
            microYaw += MathUtil.random(-0.2f, 0.2f);
            microPitch += MathUtil.random(-0.1f, 0.1f);
        }
        
        float newYaw = currentRotation.getYaw() + microYaw;
        float newPitch = currentRotation.getPitch() + microPitch;

        return new Rotation(newYaw, MathHelper.clamp(newPitch, -90.0f, 90.0f));
    }

}
