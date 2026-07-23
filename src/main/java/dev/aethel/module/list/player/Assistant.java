package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.*;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.*;
import dev.aethel.util.InventoryToolkit;
import dev.aethel.util.InventoryUtil;
import dev.aethel.util.inventory.InventoryTask;
import dev.aethel.util.keyboard.KeyStorage;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@ModuleInformation(
    moduleName = "Assistant",
    moduleCategory = ModuleCategory.PLAYER,
    moduleDesc = "Автоматическое использование предметов по горячим клавишам"
)
public class Assistant extends Module {

    public static Assistant INSTANCE;

    public enum Mode {
        FUNTIME, HOLYWORLD, LONYGRIEF
    }

    // ====== ФАЗОВЫЙ АВТОМАТ (FUNTIME) ======
    private enum FuntimePhase {
        READY,
        HOTBAR_USE,
        HOTBAR_RESTORE,
        INV_STOP,
        INV_WAIT,
        INV_SWAP_IN,
        INV_SWITCH,
        INV_USE,
        INV_SWAP_OUT,
        INV_CLOSE,
        FINISH
    }

    // ==================== НАСТРОЙКИ ====================

    private final MultiBooleanSetting functions = new MultiBooleanSetting("Functions", "",
            new BooleanSetting("Hotkeys", true),
            new BooleanSetting("Timers", true),
            new BooleanSetting("Cooldown chat", true)
    );

    private final ModeSetting mode = new ModeSetting("Server", "FunTime", "FunTime", "HolyWorld", "LonyGrief");

    // ==================== FUNTIME БИНДЫ ====================

    private final BindSetting ftDisorient     = new BindSetting("Дезориентация", -999);
    private final BindSetting perka    = new BindSetting("Перка", -999);
    private final BindSetting ftTrap          = new BindSetting("Трапка", -999);
    private final BindSetting ftSpawn         = new BindSetting("Явка", -999);
    private final BindSetting ftFireTornado   = new BindSetting("Воздухан", -999);
    private final BindSetting ftPlast         = new BindSetting("Пласт", -999);
    private final BindSetting ftFreeze        = new BindSetting("Снежок заморозка", -999);
    private final BindSetting ftBolka         = new BindSetting("Божка", -999);

    private final BindSetting ftPopada        = new BindSetting("Хлопушка", -999);
    private final BindSetting ftHolyWater     = new BindSetting("Святая вода", -999);
    private final BindSetting ftSleeping      = new BindSetting("Снотворное", -999);
    private final BindSetting ftRage          = new BindSetting("Зелье гнева", -999);
    private final BindSetting ftPaladin       = new BindSetting("Зелье паладина", -999);
    private final BindSetting ftAssassin      = new BindSetting("Зелье ассасина", -999);
    private final BindSetting ftRadiation     = new BindSetting("Зелье радиации", -999);

    // ==================== HOLYWORLD БИНДЫ ====================

    private final BindSetting hwExplosiveTrap = new BindSetting("Взрывная трапка", -999);
    private final BindSetting hwTrap          = new BindSetting("Трапка обыч", -999);
    private final BindSetting hwSnowball      = new BindSetting("Снежок", -999);
    private final BindSetting hwStand         = new BindSetting("Станка", -999);
    private final BindSetting hwExplosiveShika = new BindSetting("Взрывная шипучка", -999);

    // ==================== LONYGRIEF БИНДЫ ====================

    private final BindSetting lgLivalka       = new BindSetting("Ливалка", -999);

    // ==================== СПИСКИ БИНДОВ ====================

    private final List<ItemUsageEntry> itemUsages = new ArrayList<>();
    private final List<NamedKeyBind> namedKeyBinds = new ArrayList<>();

    // ==================== FUNTIME STATE ====================

    private Mode currentMode = Mode.FUNTIME;
    private FuntimePhase funtimePhase = FuntimePhase.READY;
    private Item funtimeItem = null;
    private Predicate<ItemStack> funtimeMatcher = stack -> false;
    private int funtimeSavedSlot = -1;
    private int funtimeItemSlot = -1;
    private int funtimeSwapSlot = -1;
    private int funtimeHotbarSlot = -1;

    // ==================== ПРОЧЕЕ ====================

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<TimerEntry> timers = new ArrayList<>();

    // ==================== КОНСТРУКТОР ====================

    public Assistant() {
        INSTANCE = this;

        // -- FUNTIME --
        addItemUsage(Items.ENDER_EYE,         ftDisorient,   Mode.FUNTIME);
        addItemUsage(Items.NETHERITE_SCRAP,   ftTrap,        Mode.FUNTIME);
        addItemUsage(Items.SUGAR,             ftSpawn,       Mode.FUNTIME);
        addItemUsage(Items.WIND_CHARGE,       ftFireTornado, Mode.FUNTIME);
        addItemUsage(Items.DRIED_KELP,        ftPlast,       Mode.FUNTIME);
        addItemUsage(Items.SNOWBALL,          ftFreeze,      Mode.FUNTIME);
        addItemUsage(Items.PHANTOM_MEMBRANE,  ftBolka,       Mode.FUNTIME);

        addNamedKeyBind(Items.SPLASH_POTION, "хлопушка",  ftPopada,    Mode.FUNTIME);
        addNamedKeyBind(Items.SPLASH_POTION, "святая",    ftHolyWater, Mode.FUNTIME);
        addNamedKeyBind(Items.SPLASH_POTION, "снотворное", ftSleeping, Mode.FUNTIME);
        addNamedKeyBind(Items.SPLASH_POTION, "гнева",     ftRage,      Mode.FUNTIME);
        addNamedKeyBind(Items.SPLASH_POTION, "паладина",  ftPaladin,   Mode.FUNTIME);
        addNamedKeyBind(Items.SPLASH_POTION, "ассасина",  ftAssassin,  Mode.FUNTIME);
        addNamedKeyBind(Items.SPLASH_POTION, "радиации",  ftRadiation, Mode.FUNTIME);

        // -- HOLYWORLD --
        addItemUsage(Items.PRISMARINE_SHARD,       hwExplosiveTrap, Mode.HOLYWORLD);
        addItemUsage(Items.POPPED_CHORUS_FRUIT,    hwTrap,          Mode.HOLYWORLD);
        addItemUsage(Items.SNOWBALL,               hwSnowball,      Mode.HOLYWORLD);
        addItemUsage(Items.NETHER_STAR,            hwStand,         Mode.HOLYWORLD);
        addItemUsage(Items.FIRE_CHARGE,            hwExplosiveShika,Mode.HOLYWORLD);

        // -- LONYGRIEF --
        addItemUsage(Items.CLAY_BALL,              lgLivalka,       Mode.LONYGRIEF);

        // Видимость
        mode.setVisible(() -> functions.isSelected("Hotkeys"));

        setVisible(ftDisorient,   Mode.FUNTIME);
        setVisible(ftTrap,        Mode.FUNTIME);
        setVisible(ftSpawn,       Mode.FUNTIME);
        setVisible(ftFireTornado, Mode.FUNTIME);
        setVisible(ftPlast,       Mode.FUNTIME);
        setVisible(ftFreeze,      Mode.FUNTIME);
        setVisible(ftBolka,       Mode.FUNTIME);
        setVisible(ftPopada,      Mode.FUNTIME);
        setVisible(ftHolyWater,   Mode.FUNTIME);
        setVisible(ftSleeping,    Mode.FUNTIME);
        setVisible(ftRage,        Mode.FUNTIME);
        setVisible(ftPaladin,     Mode.FUNTIME);
        setVisible(ftAssassin,    Mode.FUNTIME);
        setVisible(ftRadiation,   Mode.FUNTIME);
        setVisible(perka,   Mode.FUNTIME);
        setVisible(perka,   Mode.HOLYWORLD);
        setVisible(perka,   Mode.LONYGRIEF);
        setVisible(hwExplosiveTrap,Mode.HOLYWORLD);
        setVisible(hwTrap,        Mode.HOLYWORLD);
        setVisible(hwSnowball,    Mode.HOLYWORLD);
        setVisible(hwStand,       Mode.HOLYWORLD);
        setVisible(hwExplosiveShika,Mode.HOLYWORLD);
        setVisible(lgLivalka,     Mode.LONYGRIEF);
    }

    // ==================== SETUP ====================

    private void addItemUsage(Item item, BindSetting setting, Mode bindMode) {
        itemUsages.add(new ItemUsageEntry(new InventoryUtil.ItemUsage(item, this), setting, bindMode));
    }

    private void addNamedKeyBind(Item item, String namePart, BindSetting setting, Mode bindMode) {
        namedKeyBinds.add(new NamedKeyBind(item, namePart.toLowerCase(Locale.ROOT), setting, bindMode));
    }

    private void setVisible(BindSetting s, Mode m) {
        s.setVisible(() -> functions.isSelected("Hotkeys") && currentMode == m);
    }

    // ==================== ENABLE / DISABLE ====================

    @Override
    public void onEnable() {
        super.onEnable();
        currentMode = resolveMode();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetFuntimeUse();
        timers.clear();
    }

    private Mode resolveMode() {
        return switch (mode.getValue()) {
            case "HolyWorld" -> Mode.HOLYWORLD;
            case "LonyGrief" -> Mode.LONYGRIEF;
            default -> Mode.FUNTIME;
        };
    }

    // ==================== MOVEMENT INPUT (блокировка движения) ====================

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (isDefaultInvPhase(funtimePhase)) {
            event.forward = 0;
            event.strafe = 0;
            event.jump = false;
            event.sneak = false;
        }
    }

    // ==================== KEY INPUT ====================

    @Subscribe
    public void onKey(EventKeyInput event) {
        if (mc.player == null || mc.world == null) return;
        if (!functions.isSelected("Hotkeys")) return;
        if (event.getAction() != 1) return;
        if (mc.currentScreen != null) return;

        currentMode = resolveMode();

        if (currentMode == Mode.FUNTIME) {
            handleFuntimeKey();
        } else {
            handleOtherKey();
        }
    }

    private void handleFuntimeKey() {
        if (funtimePhase != FuntimePhase.READY) return;

        // Обычные предметы
        for (ItemUsageEntry entry : itemUsages) {
            if (entry.mode != Mode.FUNTIME) continue;
            int bind = entry.setting.getValue();
            if (bind == -999) continue;
            if (!KeyStorage.isPressed(bind)) continue;
            if (mc.player.getItemCooldownManager().isCoolingDown(entry.itemUsage.getItem().getDefaultStack())) continue;

            funtimePitchUse = false;
            startFuntimeUse(stack -> stack.isOf(entry.itemUsage.getItem()), entry.itemUsage.getItem());
            return;
        }

        // Named поты
        for (NamedKeyBind named : namedKeyBinds) {
            if (named.mode != Mode.FUNTIME) continue;
            int bind = named.setting.getValue();
            if (bind == -999) continue;
            if (!KeyStorage.isPressed(bind)) continue;
            if (mc.player.getItemCooldownManager().isCoolingDown(named.item.getDefaultStack())) continue;

            funtimePitchUse = false;
            startFuntimeUse(named::isMatching, named.item);
            return;
        }
    }

    private void handleOtherKey() {
        // HOLYWORLD — через InventoryUtil.useLegit (как в новой версии)
        // LONYGRIEF — старый useItemDirect
        for (ItemUsageEntry entry : itemUsages) {
            if (entry.mode != currentMode) continue;
            int bind = entry.setting.getValue();
            if (bind == -999) continue;
            if (!KeyStorage.isPressed(bind)) continue;
            if (mc.player.getItemCooldownManager().isCoolingDown(entry.itemUsage.getItem().getDefaultStack())) continue;

            if (currentMode == Mode.HOLYWORLD) {
                dev.aethel.util.player.other.InventoryUtil.useLegit(entry.itemUsage.getItem());
            } else {
                useItemDirect(entry.itemUsage.getItem());
            }
            return;
        }
    }

    // ==================== TICK (выполнение фаз FUNTIME) ====================

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        currentMode = resolveMode();

        if (funtimePhase != FuntimePhase.READY) {
            executeFuntimeUse();
        }
    }

    // ==================== ПАКЕТЫ (ТАЙМЕРЫ) ====================

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!functions.isSelected("Timers")) return;
        if (event.getType() != EventPacket.Type.RECEIVE) return;
        if (!(event.getPacket() instanceof PlaySoundS2CPacket sound)) return;

        if (currentMode == Mode.FUNTIME) handleFTTimers(sound);
        else if (currentMode == Mode.HOLYWORLD) handleHWTimers(sound);
    }

    private void handleFTTimers(PlaySoundS2CPacket sound) {
        String id = sound.getSound().getIdAsString();
        if (id.equals("minecraft:block.piston.contract")) {
            Vec3d pos = Vec3d.ofCenter(new BlockPos((int) sound.getX(), (int) sound.getY(), (int) sound.getZ()));
            timers.add(new TimerEntry(System.currentTimeMillis() + 15000, pos, "Trap"));
        } else if (id.equals("minecraft:block.anvil.place")) {
            BlockPos soundPos = new BlockPos((int) sound.getX(), (int) sound.getY(), (int) sound.getZ());
            scheduler.schedule(() -> {
                if (mc.world == null) return;
                getCube(soundPos, 4, 4).stream()
                    .filter(p -> getDist(soundPos, p) > 2 && mc.world.getBlockState(p).getBlock() == Blocks.COBBLESTONE)
                    .min(Comparator.comparing(p -> getDist(soundPos, p)))
                    .ifPresent(pos -> {
                        if (getCube(pos, 1, 1).stream().anyMatch(p -> mc.world.getBlockState(p).getBlock() == Blocks.ANVIL)) return;
                        long solidCount = getCube(pos, 1, 1).stream().filter(p -> {
                            var s = mc.world.getBlockState(p);
                            return !s.isAir() && s.isSolidBlock(mc.world, p);
                        }).count();
                        if (solidCount == 18 || solidCount == 15 || solidCount == 5) {
                            int time = solidCount == 18 || solidCount == 15 ? 20000 : 15000;
                            Vec3d addPos = Vec3d.ofCenter(pos).add(0, solidCount == 5 ? -1.5 : 0, 0);
                            timers.add(new TimerEntry(System.currentTimeMillis() + time, addPos, solidCount == 18 || solidCount == 15 ? "Plast" : "Trap"));
                        }
                    });
            }, 250, TimeUnit.MILLISECONDS);
        }
    }

    private void handleHWTimers(PlaySoundS2CPacket sound) {
        String id = sound.getSound().getIdAsString();
        float vol = sound.getVolume(), pitch = sound.getPitch();
        if (id.equals("minecraft:entity.generic.explode") && vol == 1.0f && pitch == 1.0f) {
            timers.add(new TimerEntry(System.currentTimeMillis() + 11000, new Vec3d(sound.getX(), sound.getY(), sound.getZ()), "Trap"));
        } else if (id.equals("minecraft:block.beacon.deactivate") && vol == 1.5f && pitch == 1.0f) {
            timers.add(new TimerEntry(System.currentTimeMillis() + 15000, new Vec3d(sound.getX(), sound.getY(), sound.getZ()), "Stun"));
        }
    }

    // ==================== РЕНДЕР ТАЙМЕРОВ ====================

    @Subscribe
    public void onRender(EventHUD event) {
        if (!functions.isSelected("Timers")) return;
        timers.removeIf(t -> t.endTime - System.currentTimeMillis() <= 0);

        DrawContext ctx = event.getDrawContext();
        long now = System.currentTimeMillis();
        int screenW = ctx.getScaledWindowWidth();
        int screenH = ctx.getScaledWindowHeight();

        for (TimerEntry t : timers) {
            double left = Math.max(0, (double) (t.endTime - now) / 1000.0);
            String text = t.name + ": " + String.format("%.1f", left) + "s";
            int w = mc.textRenderer.getWidth(text);
            int x = screenW / 2 - w / 2;
            int y = screenH / 2;
            ctx.fill(x - 3, y - 2, x + w + 3, y + 9 + 2, 0x88000000);
            ctx.drawText(mc.textRenderer, text, x, y, 0xFFFFFFFF, true);
        }
    }

    // ==================== FUNTIME PHASES ====================

    private boolean funtimePitchUse = false;

    private void startFuntimeUse(Predicate<ItemStack> matcher, Item item) {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (funtimePhase != FuntimePhase.READY) return;
        if (mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) return;

        funtimeItem = item;
        funtimeMatcher = matcher;
        funtimeSavedSlot = mc.player.getInventory().selectedSlot;

        int[] slots = findFuntimeSlots(matcher);
        int hotbarSlot = slots[0];
        int containerSlot = slots[1];

        if (hotbarSlot == -1 && containerSlot == -1) {
            resetFuntimeUse();
            return;
        }

        // Default (packet) mode — быстрый фазовый автомат как в src121
        if (hotbarSlot != -1) {
            funtimeHotbarSlot = hotbarSlot;
            InventoryToolkit.switchToLocal(funtimeHotbarSlot);
            funtimePhase = FuntimePhase.HOTBAR_USE;
        } else {
            funtimeItemSlot = containerSlot;
            funtimeSwapSlot = funtimeSavedSlot;
            funtimePhase = FuntimePhase.INV_STOP;
        }
    }

    private void executeFuntimeUse() {
        if (mc.player == null || mc.interactionManager == null) {
            resetFuntimeUse();
            return;
        }

        if (mc.currentScreen != null) {
            // Если экран открыт, а мы не в screen phase — сбрасываем
            resetFuntimeUse();
            return;
        }

        switch (funtimePhase) {
            case HOTBAR_USE -> {
                useFuntimeMainHand();
                funtimePhase = FuntimePhase.HOTBAR_RESTORE;
            }
            case HOTBAR_RESTORE -> {
                InventoryToolkit.switchToLocal(funtimeSavedSlot);
                funtimePhase = FuntimePhase.FINISH;
            }
            case INV_STOP -> {
                // Движение заблокируется в onMoveInput
                funtimePhase = FuntimePhase.INV_WAIT;
            }
            case INV_WAIT -> {
                funtimePhase = FuntimePhase.INV_SWAP_IN;
            }
            case INV_SWAP_IN -> {
                // Свап предмета из инвентаря в выбранный слот
                InventoryTask.clickSlot(funtimeItemSlot, funtimeSwapSlot, SlotActionType.SWAP);
                funtimePhase = FuntimePhase.INV_SWITCH;
            }
            case INV_SWITCH -> {
                InventoryToolkit.switchToLocal(funtimeSwapSlot);
                funtimePhase = FuntimePhase.INV_USE;
            }
            case INV_USE -> {
                useFuntimeMainHand();
                funtimePhase = FuntimePhase.INV_SWAP_OUT;
            }
            case INV_SWAP_OUT -> {
                // Свап обратно
                InventoryTask.clickSlot(funtimeItemSlot, funtimeSwapSlot, SlotActionType.SWAP);
                funtimePhase = FuntimePhase.INV_CLOSE;
            }
            case INV_CLOSE -> {
                InventoryTask.closeScreen(true);
                funtimePhase = FuntimePhase.FINISH;
            }
            case FINISH -> resetFuntimeUse();
        }
    }

    private void useFuntimeMainHand() {
        InventoryToolkit.interactItem(Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private int[] findFuntimeSlots(Predicate<ItemStack> matcher) {
        int hotbar = -1;
        int container = -1;
        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        for (int i = 0; i < main.size(); i++) {
            ItemStack stack = main.get(i);
            if (stack.isEmpty() || !matcher.test(stack)) continue;
            if (i < 9) {
                if (hotbar == -1) hotbar = i;
                if (container == -1) container = i + 36;
            } else if (container == -1) {
                container = i;
            }
            if (hotbar != -1 && container != -1) break;
        }
        return new int[]{hotbar, container};
    }

    private boolean isDefaultInvPhase(FuntimePhase phase) {
        return switch (phase) {
            case INV_STOP, INV_WAIT, INV_SWAP_IN, INV_SWITCH, INV_USE, INV_SWAP_OUT, INV_CLOSE -> true;
            default -> false;
        };
    }

    private void resetFuntimeUse() {
        if (mc.player != null && funtimeSavedSlot >= 0 && funtimeSavedSlot <= 8) {
            InventoryToolkit.switchToLocal(funtimeSavedSlot);
        }
        funtimePhase = FuntimePhase.READY;
        funtimeItem = null;
        funtimeMatcher = stack -> false;
        funtimeSavedSlot = -1;
        funtimeItemSlot = -1;
        funtimeSwapSlot = -1;
        funtimeHotbarSlot = -1;
        funtimePitchUse = false;
    }

    // ==================== DIRECT USE ДЛЯ HOLYWORLD/LONYGRIEF ====================
    // Аналог packetMode() из src121, но через InventoryToolkit

    private void useItemDirect(Item item) {
        if (mc.player == null || mc.interactionManager == null) return;

        // Предмет уже в руке
        if (mc.player.getMainHandStack().isOf(item)) {
            InventoryToolkit.interactItem(Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            return;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;

        // Ищем в хотбаре
        int hbSlot = findInHotbar(item);
        if (hbSlot != -1) {
            InventoryToolkit.switchToLocal(hbSlot);
            InventoryToolkit.interactItem(Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            InventoryToolkit.switchToLocal(oldSlot);
            return;
        }

        // Ищем в инвентаре
        int invSlot = findInInventory(item);
        if (invSlot != -1) {
            int bestSlot = InventoryUtil.findBestSlotInHotBar();
            if (bestSlot == -1) bestSlot = oldSlot;

            int screenSlot = invSlot < 9 ? invSlot + 36 : invSlot;

            // Свап из инвентаря в хотбар
            InventoryTask.clickSlot(screenSlot, bestSlot, SlotActionType.SWAP);
            InventoryToolkit.switchToLocal(bestSlot);
            InventoryToolkit.interactItem(Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            // Свап обратно
            InventoryTask.clickSlot(screenSlot, bestSlot, SlotActionType.SWAP);
        }
    }

    private int findInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private int findInInventory(Item item) {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    // ==================== УТИЛИТЫ ====================

    private double getDist(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dy = a.getY() - b.getY(), dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private List<BlockPos> getCube(BlockPos c, int xR, int yR) {
        List<BlockPos> r = new ArrayList<>();
        for (int x = -xR; x <= xR; x++)
            for (int y = -yR; y <= yR; y++)
                for (int z = -xR; z <= xR; z++)
                    r.add(c.add(x, y, z));
        return r;
    }

    // ==================== INNER CLASSES ====================

    public List<ItemUsageEntry> getItemUsages() { return itemUsages; }
    public List<NamedKeyBind> getNamedKeyBinds() { return namedKeyBinds; }

    public record ItemUsageEntry(InventoryUtil.ItemUsage itemUsage, BindSetting setting, Mode mode) {}

    public static class NamedKeyBind {
        public final Item item;
        public final String namePart;
        public final BindSetting setting;
        public final Mode mode;

        public NamedKeyBind(Item item, String namePart, BindSetting setting, Mode mode) {
            this.item = item;
            this.namePart = namePart;
            this.setting = setting;
            this.mode = mode;
        }

        private boolean isMatching(ItemStack stack) {
            if (stack == null || stack.isEmpty() || stack.getItem() != item) return false;
            return stack.getName().getString().toLowerCase(Locale.ROOT).contains(namePart);
        }
    }

    private record TimerEntry(long endTime, Vec3d position, String name) {}
}
