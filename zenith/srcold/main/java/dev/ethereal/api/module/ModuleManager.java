package dev.ethereal.api.module;

import lombok.Getter;
import dev.ethereal.client.features.modules.combat.*;
import dev.ethereal.client.features.modules.combat.elytratarget.ElytraTargetModule;
import dev.ethereal.client.features.modules.movement.*;
import dev.ethereal.client.features.modules.movement.fly.FlightModule;
import dev.ethereal.client.features.modules.movement.nitrofirework.NitroFireworkModule;
import dev.ethereal.client.features.modules.movement.noslow.NoSlowModule;
import dev.ethereal.client.features.modules.movement.speed.SpeedModule;
import dev.ethereal.client.features.modules.movement.spider.SpiderModule;
import dev.ethereal.client.features.modules.other.*;
import dev.ethereal.client.features.modules.other.AutoLooterModule;
import dev.ethereal.client.features.modules.player.*;
import dev.ethereal.client.features.modules.render.*;
import dev.ethereal.client.features.modules.render.nametags.NameTagsModule;
import dev.ethereal.client.features.modules.render.particles.ParticlesModule;
import dev.ethereal.client.features.modules.render.targetesp.TargetEspModule;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleManager {
    @Getter private final static ModuleManager instance = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();

    public void load() {
        register(
                SprintModule.getInstance(),
                Taksa.getInstance(),
                TPBack.getInstance(),
                Streamer.getInstance(),
                WaterSpeedModule.getInstance(),
                AirStuck.getInstance(),
                ClickGUIModule.getInstance(),
                SoundsRemove.getInstance(),
                NoDesync.getInstance(),
                AmbienceModule.getInstance(),
                NoJumpDelayModule.getInstance(),
                AuraModule.getInstance(),
                AutoRespawnModule.getInstance(),
                InterfaceModule.getInstance(),
                AutoLeaveModule.getInstance(),
                NameTagsModule.getInstance(),
                AutoToolModule.getInstance(),
                TPAcceptModule.getInstance(),
                VelocityModule.getInstance(),
                AutoFishModule.getInstance(),
                NoPushModule.getInstance(),
                NoSlowModule.getInstance(),
                AutoTotemModule.getInstance(),
                InventoryMoveModule.getInstance(),
                ItemSwapModule.getInstance(),
                RemovalsModule.getInstance(),
                ClickPearlModule.getInstance(),
                ElytraSwapModule.getInstance(),
                ElytraMotionModule.getInstance(),
                ChestStealerModule.getInstance(),
                NitroFireworkModule.getInstance(),
                SwingAnimationModule.getInstance(),
                AutoDuelModule.getInstance(),
                AutoLooterModule.getInstance(),
                ViewModelModule.getInstance(),
                SpeedModule.getInstance(),
                AutoBuffModule.getInstance(),
                JoinerModule.getInstance(),
                //WardenHelperModule.getInstance(),
                CameraClipModule.getInstance(),
                PointersModule.getInstance(),
                MouseTweaksModule.getInstance(),
                AssistantModule.getInstance(),
                ElytraTargetModule.getInstance(),
                AutoExplosionModule.getInstance(),
                NoWebModule.getInstance(),
                PredictionsModule.getInstance(),
                NoFriendHurtModule.getInstance(),
                NoEntityTraceModule.getInstance(),
                ParticlesModule.getInstance(),
                SeeInvisiblesModule.getInstance(),
                HealthResolverModule.getInstance(),
                JumpCircleModule.getInstance(),
                ToggleSoundsModule.getInstance(),
                NightVisionModule.getInstance(),
                NoServerPackModule.getInstance(),
                AuctionHelperModule.getInstance(),
                TargetEspModule.getInstance(),
                LonyESPModule.getInstance(),
                SpiderModule.getInstance(),
                FlightModule.getInstance(),
                VardenESP.getInstance()
        );

        modules.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    public void register(Module... modules) {
        this.modules.addAll(List.of(modules));
    }
}