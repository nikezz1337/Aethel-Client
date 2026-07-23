package dev.ethereal.inject.other;

import dev.ethereal.api.utils.auction.AuctionUtil;
import dev.ethereal.client.features.modules.other.TaksaBuy;
import dev.ethereal.api.utils.auction.ab.AutoPriceParser;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class MixinGenericContainerScreen extends HandledScreen<GenericContainerScreenHandler> {

    @Unique
    private ButtonWidget autobuy;
    @Unique
    private ButtonWidget parser;
    @Unique
    private SliderWidget price;

    public MixinGenericContainerScreen(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title, CallbackInfo ci) {
        int cx = (this.width - this.backgroundWidth) / 2;
        int cy = (this.height - this.backgroundHeight) / 2;

        autobuy = ButtonWidget.builder(
                Text.literal("AutoBuy: " + (TaksaBuy.getInstance().isEnabled() ? "§aON" : "§cOFF")),
                btn -> {
                    TaksaBuy instance = TaksaBuy.getInstance();
                    instance.toggle();
                    btn.setMessage(Text.literal("AutoBuy: " + (instance.isEnabled() ? "§aON" : "§cOFF")));
                }
        ).dimensions(cx + this.backgroundWidth / 2 - 107, cy - 25, 70, 20).build();
        autobuy.visible = false;
        addDrawableChild(autobuy);

        parser = ButtonWidget.builder(
                Text.literal("Парсер: OFF"),
                btn -> {
                    AutoPriceParser.setEnabled(!AutoPriceParser.isEnabled());
                    btn.setMessage(Text.literal("Парсер: " + (AutoPriceParser.isEnabled() ? "§aON" : "§cOFF")));
                }
        ).dimensions(cx + this.backgroundWidth / 2 - 33, cy - 25, 70, 20).build();
        parser.visible = false;
        addDrawableChild(parser);

        int initialDisc = AutoPriceParser.getDiscountPercent();
        price = new SliderWidget(
                cx + this.backgroundWidth / 2 + 41, cy - 25, 66, 20,
                Text.literal(initialDisc + "%"),
                initialDisc / 100.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal(AutoPriceParser.getDiscountPercent() + "%"));
            }
            @Override
            protected void applyValue() {
                AutoPriceParser.setDiscountPercent((int) Math.round(this.value * 100.0));
                updateMessage();
            }
        };
        price.visible = false;
        addDrawableChild(price);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        String titleStr = this.title != null ? this.title.getString() : "";
        boolean isSearchOrContainer = AuctionUtil.isSearchScreen(titleStr) || AuctionUtil.isContainerScreen(titleStr);
        boolean moduleActive = TaksaBuy.getInstance().isEnabled();

        if (autobuy != null) {
            autobuy.visible = isSearchOrContainer;
            if (autobuy.visible) {
                int cx = (this.width - this.backgroundWidth) / 2;
                int cy = (this.height - this.backgroundHeight) / 2;
                autobuy.setPosition(cx + this.backgroundWidth / 2 - 107, cy - 25);
                autobuy.setMessage(Text.literal("AutoBuy: " + (moduleActive ? "§aON" : "§cOFF")));
            }
        }

        if (parser != null) {
            parser.visible = isSearchOrContainer;
            if (isSearchOrContainer) {
                int cx = (this.width - this.backgroundWidth) / 2;
                int cy = (this.height - this.backgroundHeight) / 2;
                parser.setPosition(cx + this.backgroundWidth / 2 - 33, cy - 25);
                parser.setMessage(Text.literal("Парсер: " + (AutoPriceParser.isEnabled() ? "§aON" : "§cOFF")));
            }
        }

        if (price != null) {
            price.visible = isSearchOrContainer;
            if (isSearchOrContainer) {
                int cx = (this.width - this.backgroundWidth) / 2;
                int cy = (this.height - this.backgroundHeight) / 2;
                price.setPosition(cx + this.backgroundWidth / 2 + 41, cy - 25);
            }
        }
    }
}
