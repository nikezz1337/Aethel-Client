package dev.ethereal.client.ui.widget.overlay;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.other.TextUtil;
import dev.ethereal.client.ui.widget.ListWidget;

import java.util.ArrayList;
import java.util.List;

public class PotionsWidget extends ListWidget {
    public PotionsWidget() { super(3f, 120f); }

    @Override public String getName() { return "Potions"; }
    @Override protected String getIcon() { return "j"; }

    @Override
    protected List<Row> collectRows() {
        List<Row> rows = new ArrayList<>();
        if (mc.player == null) return rows;

        for (StatusEffectInstance e : mc.player.getActiveStatusEffects().values()) {
            String name = Language.getInstance().get(e.getTranslationKey())
                    + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
            String id = e.getEffectType().getKey().get().getValue().getPath();
            Identifier tex = Identifier.of("minecraft", "textures/mob_effect/" + id + ".png");
            rows.add(new Row(e.getTranslationKey(), name, TextUtil.getDurationText(e.getDuration()), UIColors.textColor(), tex));
        }
        return rows;
    }
}
