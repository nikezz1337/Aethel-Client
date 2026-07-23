package dev.ethereal.client.ui.widget.overlay;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.other.TextUtil;
import dev.ethereal.client.ui.widget.ListWidget;

import java.util.ArrayList;
import java.util.List;

public class CooldownsWidget extends ListWidget {
    public CooldownsWidget() { super(100f, 100f); }

    @Override public String getName() { return "Cooldowns"; }
    @Override protected String getIcon() { return "i"; }

    @Override
    protected List<Row> collectRows() {
        List<Row> rows = new ArrayList<>();
        if (mc.player == null) return rows;

        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        float delta = mc.getRenderTickCounter().getTickDelta(false);

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !manager.isCoolingDown(stack)) continue;

            Identifier groupId = manager.getGroup(stack);
            ItemCooldownManager.Entry entry = manager.entries.get(groupId);
            if (entry == null) continue;

            int remaining = Math.max(0, entry.endTick() - (manager.tick + (int) delta));
            if (remaining <= 0) continue;

            String name = stack.getItem().getName().getString();
            rows.add(new Row(name, name, TextUtil.getDurationText(remaining), UIColors.textColor()));
        }
        return rows;
    }
}
