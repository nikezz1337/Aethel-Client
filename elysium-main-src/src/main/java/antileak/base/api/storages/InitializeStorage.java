package antileak.base.api.storages;


import antileak.base.elysium;
import antileak.base.api.QClient;
import antileak.base.api.events.EventInvoker;
import antileak.base.api.storages.implement.*;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.api.utils.tps.TPSCalc;
import antileak.base.mods.maseffects.MaseffectsParticleTypes;
import antileak.base.mods.particular.ParticularParticleTypes;
import antileak.base.client.modules.impl.render.TotemAngel;

public class InitializeStorage implements QClient {

    public void onInitialize() {
        EventInvoker.register(this);
        this.initStorages();
    }

    
    public void initStorages() {
        MaseffectsParticleTypes.register();
        ParticularParticleTypes.register();
        elysium.INSTANCE.moduleStorage = new ModuleStorage();
        elysium.INSTANCE.themeStorage = new ThemeStorage();
        elysium.INSTANCE.tpsCalc = new TPSCalc();
        EventInvoker.register(elysium.INSTANCE.tpsCalc);
        elysium.INSTANCE.localizationStorage = new LocalizationStorage();
        elysium.INSTANCE.freeLookStorage = new FreeLookStorage();
        elysium.INSTANCE.rotationStorage = new RotationStorage();
        elysium.INSTANCE.serverStorage = new ServerStorage();
        elysium.INSTANCE.serverStorage.ServerManager();
        elysium.INSTANCE.friendStorage = new FriendStorage();
        elysium.INSTANCE.macroStorage = new MacroStorage();
        elysium.INSTANCE.staffStorage = new StaffStorage();
        elysium.INSTANCE.waypointStorage = new WaypointStorage();
        elysium.INSTANCE.commandStorage = new CommandStorage();
        elysium.INSTANCE.configStorage = new ConfigStorage();
    }
}
