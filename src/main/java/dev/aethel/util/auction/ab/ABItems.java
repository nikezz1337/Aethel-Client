package dev.aethel.util.auction.ab;

import dev.aethel.util.IMinecraft;
import dev.aethel.util.auction.nbt.NbtUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.io.File;

@UtilityClass
public class ABItems implements IMinecraft {
    private final File ITEMS_DIR = new File("aethel/other/autobuy-items");

    public ItemStack krushHelmet() { return load("krushHelmet", new ItemStack(Items.NETHERITE_HELMET)); }
    public ItemStack krushChestplate() { return load("krushChestplate", new ItemStack(Items.NETHERITE_CHESTPLATE)); }
    public ItemStack krushLeggings() { return load("krushLeggings", new ItemStack(Items.NETHERITE_LEGGINGS)); }
    public ItemStack krushBoots() { return load("krushBoots", new ItemStack(Items.NETHERITE_BOOTS)); }
    public ItemStack krushSword() { return load("krushSword", new ItemStack(Items.NETHERITE_SWORD)); }
    public ItemStack krushPickaxe() { return load("krushPickaxe", new ItemStack(Items.BOW)); }
    public ItemStack krushTrident() { return load("krushTrident", new ItemStack(Items.TRIDENT)); }
    public ItemStack krushCrossbow() { return load("krushCrossbow", new ItemStack(Items.CROSSBOW)); }

    public ItemStack serka() { return load("serka", new ItemStack(Items.SPLASH_POTION)); }
    public ItemStack agentka() { return load("agentka", new ItemStack(Items.SPLASH_POTION)); }
    public ItemStack killerka() { return load("killerka", new ItemStack(Items.SPLASH_POTION)); }
    public ItemStack otrizhka() { return load("otrizhka", new ItemStack(Items.SPLASH_POTION)); }
    public ItemStack medika() { return load("medika", new ItemStack(Items.SPLASH_POTION)); }
    public ItemStack pobedilka() { return load("pobedilka", new ItemStack(Items.SPLASH_POTION)); }

    public ItemStack proklyatayaStrela() { return load("proklyatayaStrela", new ItemStack(Items.ARROW)); }
    public ItemStack adskayaStrela() { return load("adskayaStrela", new ItemStack(Items.ARROW)); }
    public ItemStack paranoiaStrela() { return load("paranoiaStrela", new ItemStack(Items.ARROW)); }
    public ItemStack snezhnayaStrela() { return load("snezhnayaStrela", new ItemStack(Items.ARROW)); }

    public ItemStack otmichkaArmor() { return load("otmichkaArmor", new ItemStack(Items.TRIPWIRE_HOOK)); }
    public ItemStack otmichkaResources() { return load("otmichkaResources", new ItemStack(Items.TRIPWIRE_HOOK)); }
    public ItemStack otmichkaSpheres() { return load("otmichkaSpheres", new ItemStack(Items.TRIPWIRE_HOOK)); }
    public ItemStack otmichkaTools() { return load("otmichkaTools", new ItemStack(Items.TRIPWIRE_HOOK)); }
    public ItemStack otmichkaWeapons() { return load("otmichkaWeapons", new ItemStack(Items.TRIPWIRE_HOOK)); }

    public ItemStack tierBlack() { return load("tierBlack", new ItemStack(Items.TNT)); }
    public ItemStack tierWhite() { return load("tierWhite", new ItemStack(Items.TNT)); }

    public ItemStack desor() { return load("desor", new ItemStack(Items.ENDER_EYE)); }
    public ItemStack plast() { return load("plast", new ItemStack(Items.DRIED_KELP)); }
    public ItemStack bozhka() { return load("bozhka", new ItemStack(Items.PHANTOM_MEMBRANE)); }
    public ItemStack snezhok() { return load("snezhok", new ItemStack(Items.SNOWBALL)); }
    public ItemStack trapka() { return load("trapka", new ItemStack(Items.NETHERITE_SCRAP)); }
    public ItemStack yavka() { return load("yavka", new ItemStack(Items.SUGAR)); }

    public ItemStack dedalaTier3() { return load("dedalaTier3", new ItemStack(Items.TOTEM_OF_UNDYING)); }
    public ItemStack exidnaTier3() { return load("exidnaTier3", new ItemStack(Items.TOTEM_OF_UNDYING)); }
    public ItemStack garmoniiTier3() { return load("garmoniiTier3", new ItemStack(Items.TOTEM_OF_UNDYING)); }
    public ItemStack graniTier3() { return load("graniTier3", new ItemStack(Items.TOTEM_OF_UNDYING)); }
    public ItemStack haronTier3() { return load("haronTier3", new ItemStack(Items.TOTEM_OF_UNDYING)); }
    public ItemStack phoenixTier3() { return load("phoenixTier3", new ItemStack(Items.TOTEM_OF_UNDYING)); }
    public ItemStack tritonTier3() { return load("tritonTier3", new ItemStack(Items.TOTEM_OF_UNDYING)); }
    public ItemStack krush() { return load("krush", new ItemStack(Items.TOTEM_OF_UNDYING)); }
    public ItemStack karatel() { return load("karatel", new ItemStack(Items.TOTEM_OF_UNDYING)); }

    public ItemStack andromedaTier3() { return load("andromedaTier3", new ItemStack(Items.PLAYER_HEAD)); }
    public ItemStack apollonaTier3() { return load("apollonaTier3", new ItemStack(Items.PLAYER_HEAD)); }
    public ItemStack astreaTier3() { return load("astreaTier3", new ItemStack(Items.PLAYER_HEAD)); }
    public ItemStack himeraTier3() { return load("himeraTier3", new ItemStack(Items.PLAYER_HEAD)); }
    public ItemStack osirisaTier3() { return load("osirisaTier3", new ItemStack(Items.PLAYER_HEAD)); }
    public ItemStack pandoraTier3() { return load("pandoraTier3", new ItemStack(Items.PLAYER_HEAD)); }
    public ItemStack titanTier3() { return load("titanTier3", new ItemStack(Items.PLAYER_HEAD)); }

    private ItemStack load(String name, ItemStack fallback) {
        if (mc.getNetworkHandler() == null) return fallback;
        ItemStack stack = NbtUtils.loadItemStack(name, ITEMS_DIR, mc.getNetworkHandler().getRegistryManager());
        return stack.isEmpty() ? fallback : stack;
    }
}
