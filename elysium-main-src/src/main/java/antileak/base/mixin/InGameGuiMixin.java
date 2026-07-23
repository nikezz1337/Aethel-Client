package antileak.base.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import antileak.base.api.QClient;
import antileak.base.api.events.EventInvoker;
import antileak.base.api.events.implement.EventRender;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.api.utils.SidebarEntry;
import antileak.base.client.modules.impl.misc.NameProtect;

import java.util.Comparator;
import java.util.List;

@Mixin(InGameHud.class)
public class InGameGuiMixin implements QClient {

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void renderVignetteOverlay(DrawContext context, Entity entity, CallbackInfo ci) {
        if (ModuleClass.noVignette.isEnable()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (EventInvoker.hasListeners(EventRender.Default.class)) {
            new EventRender.Default(context, tickCounter.getTickDelta(true)).call();
        }
    }

    private static final int DOMAIN_COLOR = 0xED6521;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private PlayerEntity getCameraPlayer() {
        return null;
    }

    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void elysium$renderPatchedScoreboard(DrawContext drawContext, ScoreboardObjective objective, CallbackInfo ci) {
        if (!elysium$shouldPatchScoreboard()) {
            return;
        }

        try {
            Scoreboard scoreboard = objective.getScoreboard();
            NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED);

            List<SidebarEntry> lines = scoreboard.getScoreboardEntries(objective).stream()
                    .filter(entry -> !entry.hidden())
                    .sorted(Comparator.comparing(ScoreboardEntry::value).reversed()
                            .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER))
                    .limit(15L)
                    .map(entry -> {
                        try {
                            Team team = scoreboard.getScoreHolderTeam(entry.owner());
                            Text name = elysium$patchText(Team.decorateName(team, entry.name()));
                            Text score = entry.formatted(numberFormat);
                            int scoreWidth = this.client.textRenderer.getWidth(score);
                            return new SidebarEntry(name, score, scoreWidth);
                        } catch (Exception e) {
                            Text fallback = Text.literal("???");
                            return new SidebarEntry(fallback, fallback, 0);
                        }
                    })
                    .toList();

            Text title;
            try {
                title = elysium$patchText(objective.getDisplayName());
            } catch (Exception e) {
                title = Text.literal("???");
            }

            int titleWidth = this.client.textRenderer.getWidth(title);
            int maxWidth = titleWidth;
            int separatorWidth = this.client.textRenderer.getWidth(": ");

            for (SidebarEntry line : lines) {
                maxWidth = Math.max(maxWidth, this.client.textRenderer.getWidth(line.name) + (line.scoreWidth > 0 ? separatorWidth + line.scoreWidth : 0));
            }

            int lineCount = lines.size();
            int totalHeight = lineCount * 9;
            int bottom = drawContext.getScaledWindowHeight() / 2 + totalHeight / 3;
            int left = drawContext.getScaledWindowWidth() - maxWidth - 3;
            int right = drawContext.getScaledWindowWidth() - 1;
            int bodyColor = this.client.options.getTextBackgroundColor(0.3F);
            int headerColor = this.client.options.getTextBackgroundColor(0.4F);
            int top = bottom - lineCount * 9;

            drawContext.fill(left - 2, top - 10, right, top - 1, headerColor);
            drawContext.fill(left - 2, top - 1, right, bottom, bodyColor);
            drawContext.drawText(this.client.textRenderer, title, left + maxWidth / 2 - titleWidth / 2, top - 9, -1, false);

            for (int index = 0; index < lineCount; ++index) {
                SidebarEntry line = lines.get(index);
                int y = bottom - (lineCount - index) * 9;
                drawContext.drawText(this.client.textRenderer, line.name, left, y, -1, false);
                drawContext.drawText(this.client.textRenderer, line.score, right - line.scoreWidth, y, -1, false);
            }

            ci.cancel();
        } catch (Exception ignored) {
        }
    }

    private boolean elysium$shouldPatchScoreboard() {
        return ModuleClass.INSTANCE != null
                && ModuleClass.INSTANCE.nameProtect != null
                && ModuleClass.INSTANCE.nameProtect.isEnable();
    }

    private Text elysium$patchText(Text text) {
        NameProtect nameProtect = ModuleClass.INSTANCE.nameProtect;
        Text patched = nameProtect.patchText(text);
        String patchedString = patched.getString();

        if (nameProtect.shouldHideGrief()) {
            if (patchedString.contains("Анархия-")) {
                patchedString = patchedString.replaceAll("Анархия-\\d+", "elysiumdlc.fun");
            }
            if (patchedString.contains("ГРИФ #")) {
                patchedString = patchedString.replaceAll("ГРИФ #\\d+", "elysiumdlc.fun");
            }
        }

        patchedString = elysium$sanitizeForIdentifier(patchedString);

        if (patchedString.equals(patched.getString())) {
            return patched;
        }
        return Text.literal(patchedString).setStyle(patched.getStyle().withColor(TextColor.fromRgb(DOMAIN_COLOR)));
    }

    private String elysium$sanitizeForIdentifier(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 0x20 && c != 0x7F) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}