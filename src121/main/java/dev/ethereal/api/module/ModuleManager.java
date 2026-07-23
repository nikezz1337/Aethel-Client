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
                AntiBotModule.getInstance(),
                CriticalsModule.getInstance(),
                ItemsTracker.getInstance(),
                OpenWallsModule.getInstance(),
                Aura.getInstance(),
                AutoEat.getInstance(),
                AutoInvisible.getInstance(),
                ClanUpgraderModule.getInstance(),
                Taksa.getInstance(),
                TaksaBuy.getInstance(),
                Streamer.getInstance(),
                WaterSpeedModule.getInstance(),
                AirStuck.getInstance(),
                ClickGUIModule.getInstance(),
                SoundsRemove.getInstance(),
                NoDesync.getInstance(),
                AmbienceModule.getInstance(),
                NoJumpDelayModule.getInstance(),
                AimBotModule.getInstance(),
                AutoRespawnModule.getInstance(),
                HealthResolverModule.getInstance(),
                InterfaceModule.getInstance(),
                SkyShaderModule.getInstance(),
                BlockOverlayModule.getInstance(),
                HandOverlayModule.getInstance(),
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
                ViewModelModule.getInstance(),
                SpeedModule.getInstance(),
                AutoBuffModule.getInstance(),
                JoinerModule.getInstance(),
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
                FullbrightModule.getInstance(),
                JumpCircleModule.getInstance(),
                ToggleSoundsModule.getInstance(),
                NoServerPackModule.getInstance(),
                ShulkerPreviewModule.getInstance(),
                AuctionHelperModule.getInstance(),
                TargetEspModule.getInstance(),
                SpiderModule.getInstance(),
                ItemESP.getInstance(),
                FlightModule.getInstance(),
                VardenESP.getInstance(),
                StructureTimerModule.getInstance(),
                ItemRadiusModule.getInstance(),
                FreecamModule.getInstance()
        );

        modules.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    public void register(Module... modules) {
        this.modules.addAll(List.of(modules));
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> clazz) {
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }
}
