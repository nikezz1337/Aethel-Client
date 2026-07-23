package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.client.features.modules.combat.AuraModule;

public class RotationPlan {
    public enum Task { AIM, RESET, IDLE }
    
    public boolean moveCorrection() {
        return AuraModule.getInstance().isEnabled() && AuraModule.getInstance().moveCorrection.getValue();
    }
    
    public boolean freeMoveCorrection() {
        return AuraModule.getInstance().isEnabled() && AuraModule.getInstance().moveCorrection.getValue() && AuraModule.getInstance().correctionMode.is("Свободная");
    }
    
    public boolean clientLook() { return false; }
    public dev.ethereal.api.module.Module provider() { return null; }
}
