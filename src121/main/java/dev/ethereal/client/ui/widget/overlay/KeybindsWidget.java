package dev.ethereal.client.ui.widget.overlay;

import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleManager;
import dev.ethereal.api.system.backend.KeyStorage;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.client.ui.widget.ListWidget;

import java.util.ArrayList;
import java.util.List;

public class KeybindsWidget extends ListWidget {
    public KeybindsWidget() { super(3f, 120f); }

    @Override public String getName() { return "Keybinds"; }
    @Override protected String getIcon() { return "g"; }

    @Override
    protected List<Row> collectRows() {
        List<Row> rows = new ArrayList<>();
        for (Module m : ModuleManager.getInstance().getModules()) {
            if (!m.isEnabled() || !m.hasBind()) continue;
            rows.add(new Row(m.getName(), m.getName(), KeyStorage.getBind(m.getBind()), UIColors.primary()));
        }
        return rows;
    }
}
