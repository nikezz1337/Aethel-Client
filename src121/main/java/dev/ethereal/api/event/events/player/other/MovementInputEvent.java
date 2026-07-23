package dev.ethereal.api.event.events.player.other;

import net.minecraft.util.PlayerInput;
import dev.ethereal.api.event.CancellableEvent;
import dev.ethereal.api.utils.player.DirectionalInput;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;

@AllArgsConstructor @Getter @Setter
public class MovementInputEvent extends CancellableEvent {
    PlayerInput playerInput;
    private boolean jump;
    private boolean sneak;
    private DirectionalInput directionalInput;
    public float movementForward, movementSideways;

    public MovementInputEvent(PlayerInput playerInput, boolean jump, boolean sneak, DirectionalInput directionalInput) {
        this.playerInput = playerInput;
        this.jump = jump;
        this.sneak = sneak;
        this.directionalInput = directionalInput;
        syncMovementFromInput();
    }

    public void setJumping(boolean jump) {
        this.jump = jump;
        playerInput = new PlayerInput(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), jump, playerInput.sneak(), playerInput.sprint());
        syncMovementFromInput();
    }

    public void setJump(boolean jump) {
        setJumping(jump);
    }

    public void setSneak(boolean sneak) {
        this.sneak = sneak;
    }

    public void setDirectional(boolean forward, boolean backward, boolean left, boolean right) {
        playerInput = new PlayerInput(forward, backward, left, right, playerInput.jump(), playerInput.sneak(), playerInput.sprint());
        directionalInput = new DirectionalInput(forward, backward, left, right);
        syncMovementFromInput();
    }

    public void setDirectionalInput(DirectionalInput input) {
        setDirectional(input.isForwards(), input.isBackwards(), input.isLeft(), input.isRight());
    }

    public int forward() {
        return playerInput.forward() ? 1 : playerInput.backward() ? -1 : 0;
    }

    public float sideways() {
        return playerInput.left() ? 1 : playerInput.right() ? -1 : 0;
    }

    private void syncMovementFromInput() {
        movementForward = playerInput.forward() ? 1F : playerInput.backward() ? -1F : 0F;
        movementSideways = playerInput.left() ? 1F : playerInput.right() ? -1F : 0F;
    }

    private void syncInputFromMovement() {
        boolean forward = movementForward > 0.0F;
        boolean backward = movementForward < 0.0F;
        boolean left = movementSideways > 0.0F;
        boolean right = movementSideways < 0.0F;
        playerInput = new PlayerInput(forward, backward, left, right, playerInput.jump(), playerInput.sneak(), playerInput.sprint());
        directionalInput = new DirectionalInput(forward, backward, left, right);
    }

    public void setMovementForward(float movementForward) {
        this.movementForward = movementForward;
        syncInputFromMovement();
    }

    public void setMovementSideways(float movementSideways) {
        this.movementSideways = movementSideways;
        syncInputFromMovement();
    }

    public void setYaw(float yaw, float yaw2) {
        float forward = getMovementForward();
        float sideways = getMovementSideways();
        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw2, forward, sideways)));
        if (forward == 0 && sideways == 0) return;
        float closestForward = 0, closestSideways = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedSideways = -1F; predictedSideways <= 1F; predictedSideways += 1F) {
                if (predictedSideways == 0 && predictedForward == 0) continue;

                double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, predictedForward, predictedSideways)));
                double difference = Math.abs(angle - predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestSideways = predictedSideways;
                }
            }
        }

        setMovementForward(closestForward);
        setMovementSideways(closestSideways);
    }

    private double direction(float yaw, double movementForward, double movementSideways) {
        if (movementForward < 0F) yaw += 180F;
        float forward = 1F;
        if (movementForward < 0F) forward = -0.5F;
        else if (movementForward > 0F) forward = 0.5F;
        if (movementSideways > 0F) yaw -= 90F * forward;
        if (movementSideways < 0F) yaw += 90F * forward;
        return Math.toRadians(yaw);
    }
}
