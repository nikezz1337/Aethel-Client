package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.client.features.modules.combat.Aura;

public class RotationPlan {
    public enum Task { AIM, RESET, IDLE }

    public boolean moveCorrection() {
        return Aura.getInstance().isMoveCorrection();
    }

    public boolean freeMoveCorrection() {
        return Aura.getInstance().isMoveCorrection() && Aura.getInstance().isSilentCorrection();
    }

    public boolean clientLook() { return false; }
    public dev.ethereal.api.module.Module provider() { return null; }
}
