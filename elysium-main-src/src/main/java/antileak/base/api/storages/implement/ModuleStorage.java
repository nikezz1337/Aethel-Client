package antileak.base.api.storages.implement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import antileak.base.api.QClient;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.impl.combat.*;
import antileak.base.client.modules.impl.misc.*;
import antileak.base.client.modules.impl.movement.*;
import antileak.base.client.modules.impl.player.*;
import antileak.base.client.modules.impl.render.*;
import java.util.Arrays;

@Getter
@Setter
public class ModuleStorage implements QClient {

    public ModuleStorage() {
        this.initModules();
    }

    private void initModules() {
        ModuleClass.INSTANCE.initialize();
    }
}
