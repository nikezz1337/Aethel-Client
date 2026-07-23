package fun.wonderful.api.storages.implement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import fun.wonderful.api.QClient;
import fun.wonderful.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import fun.wonderful.client.modules.Module;
import fun.wonderful.client.modules.impl.combat.*;
import fun.wonderful.client.modules.impl.misc.*;
import fun.wonderful.client.modules.impl.movement.*;
import fun.wonderful.client.modules.impl.player.*;
import fun.wonderful.client.modules.impl.render.*;
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
