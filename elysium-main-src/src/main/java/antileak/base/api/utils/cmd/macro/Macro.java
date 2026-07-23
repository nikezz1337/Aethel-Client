package antileak.base.api.utils.cmd.macro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import antileak.base.client.modules.settings.implement.BindSetting;

@AllArgsConstructor @Getter
public class Macro {
    private String name, command;
    private BindSetting bind;
}