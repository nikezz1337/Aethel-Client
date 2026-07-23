package dev.aethel.module.list.player;

import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.list.combat.Aura;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.MultiBooleanSetting;
import net.minecraft.block.*;

@ModuleInformation(
        moduleName = "NoInteract",
        moduleCategory = ModuleCategory.PLAYER,
        moduleDesc = "Блокирует взаимодействие с блоками"
)
public class NoInteract extends Module {

    public final MultiBooleanSetting blocks = new MultiBooleanSetting("Блоки", "",
            new BooleanSetting("Верстаки", true),
            new BooleanSetting("Стол зачарований", true),
            new BooleanSetting("Кровати", true),
            new BooleanSetting("Сундуки", true),
            new BooleanSetting("Эндер-сундуки", true),
            new BooleanSetting("Печки", true),
            new BooleanSetting("Бочки", true),
            new BooleanSetting("Шалкеры", true),
            new BooleanSetting("Дропперы", true),
            new BooleanSetting("Диспенсеры", true),
            new BooleanSetting("Воронки", true),
            new BooleanSetting("Наковальни", true),
            new BooleanSetting("Котлы", true),
            new BooleanSetting("Таблички", false),
            new BooleanSetting("Звонки", false),
            new BooleanSetting("Компостеры", false),
            new BooleanSetting("Пивоварни", true),
            new BooleanSetting("Джукбоксы", false),
            new BooleanSetting("Камманы", false),
            new BooleanSetting("Биконы", true),
            new BooleanSetting("Столы крафта", true),
            new BooleanSetting("Столы алхимика", true),
            new BooleanSetting("Ткацкий станок", true),
            new BooleanSetting("Стол картографа", true),
            new BooleanSetting("Стол оружейника", true),
            new BooleanSetting("Камнерез", true)
    );

    public final BooleanSetting onlyAura =
            new BooleanSetting("Только при Aura", false);

    public NoInteract() {
    }

    public boolean shouldPreventInteract(Block block) {
        if (onlyAura.getValue()) {
            return Aura.getInstance() != null && Aura.getInstance().isEnabled();
        }

        if (block instanceof CraftingTableBlock && blocks.isSelected("Верстаки")) return true;
        if (block instanceof EnchantingTableBlock && blocks.isSelected("Стол зачарований")) return true;
        if (block instanceof BedBlock && blocks.isSelected("Кровати")) return true;
        if (block instanceof ChestBlock && blocks.isSelected("Сундуки")) return true;
        if (block instanceof EnderChestBlock && blocks.isSelected("Эндер-сундуки")) return true;
        if (block instanceof AbstractFurnaceBlock && blocks.isSelected("Печки")) return true;
        if (block instanceof BarrelBlock && blocks.isSelected("Бочки")) return true;
        if (block instanceof ShulkerBoxBlock && blocks.isSelected("Шалкеры")) return true;
        if (block instanceof DropperBlock && blocks.isSelected("Дропперы")) return true;
        if (block instanceof DispenserBlock && blocks.isSelected("Диспенсеры")) return true;
        if (block instanceof HopperBlock && blocks.isSelected("Воронки")) return true;
        if (block instanceof AnvilBlock && blocks.isSelected("Наковальни")) return true;
        if (block instanceof AbstractCauldronBlock && blocks.isSelected("Котлы")) return true;
        if (block instanceof AbstractSignBlock && blocks.isSelected("Таблички")) return true;
        if (block instanceof BellBlock && blocks.isSelected("Звонки")) return true;
        if (block instanceof ComposterBlock && blocks.isSelected("Компостеры")) return true;
        if (block instanceof BrewingStandBlock && blocks.isSelected("Пивоварни")) return true;
        if (block instanceof JukeboxBlock && blocks.isSelected("Джукбоксы")) return true;
        if (block instanceof CommandBlock && blocks.isSelected("Камманы")) return true;
        if (block instanceof BeaconBlock && blocks.isSelected("Биконы")) return true;
        if (block instanceof CartographyTableBlock && blocks.isSelected("Стол картографа")) return true;
        if (block instanceof LoomBlock && blocks.isSelected("Ткацкий станок")) return true;
        if (block instanceof SmithingTableBlock && blocks.isSelected("Стол оружейника")) return true;
        if (block instanceof StonecutterBlock && blocks.isSelected("Камнерез")) return true;
        if (block instanceof RespawnAnchorBlock && blocks.isSelected("Столы алхимика")) return true;
        if (block instanceof GrindstoneBlock && blocks.isSelected("Столы крафта")) return true;
        if (block instanceof LecternBlock && blocks.isSelected("Столы алхимика")) return true;

        return false;
    }
}
