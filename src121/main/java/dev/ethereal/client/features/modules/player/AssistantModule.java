package dev.ethereal.client.features.modules.player;

import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.move.JumpEvent;
import dev.ethereal.api.event.events.player.other.MovementInputEvent;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.backend.KeyStorage;
import dev.ethereal.api.system.backend.Pair;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.math.ProjectionUtil;
import dev.ethereal.api.utils.player.DirectionalInput;
import dev.ethereal.api.utils.player.InventoryActionUtil;
import dev.ethereal.api.utils.player.InventoryFlowManager;
import dev.ethereal.api.utils.player.InventoryTask;
import dev.ethereal.api.utils.player.InventoryToolkit;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationComponent;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ModuleRegister(name = "Assistant", category = Category.PLAYER)
public class AssistantModule extends Module {
    @Getter private static final AssistantModule instance = new AssistantModule();

    public enum Mode {
        FUNTIME, HOLYWORLD, LONYGRIEF
    }

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
        FINISH,
        ST_HOTBAR_SWITCH,
        ST_HOTBAR_USE,
        ST_HOTBAR_RESTORE,
        ST_OPEN_INV,
        ST_MOVE_CURSOR,
        ST_PRESS_DIGIT,
        ST_CLOSE_INV,
        ST_USE,
        ST_REOPEN,
        ST_MOVE_BACK,
        ST_PRESS_BACK,
        ST_CLOSE_FINAL
    }

    private final Supplier<Boolean> isHotkeysEnabled = () -> getFunctions().isEnabled("Горячие клавиши");
    private final Supplier<Boolean> isHWKeys = () -> isHotkeysEnabled.get() && getMode().is("HolyWorld");
    private final Supplier<Boolean> isFTKeys = () -> isHotkeysEnabled.get() && getMode().is("FunTime");
    private final Supplier<Boolean> isLGKeys = () -> isHotkeysEnabled.get() && getMode().is("LonyGrief");

    private Mode currentMode = Mode.FUNTIME;

    @Getter private final MultiBooleanSetting functions = new MultiBooleanSetting("Опции").value(
            new BooleanSetting("Горячие клавиши").value(true),
            new BooleanSetting("Таймеры").value(true),
            new BooleanSetting("Писать о перезарядке").value(true)
    );

    @Getter private final ModeSetting mode = new ModeSetting("Сервер").value("FunTime")
            .values("FunTime", "HolyWorld", "LonyGrief").setVisible(isHotkeysEnabled)
            .onAction(() -> {
                currentMode = switch (getMode().getValue()) {
                    case "FunTime" -> Mode.FUNTIME;
                    case "LonyGrief" -> Mode.LONYGRIEF;
                    default -> Mode.HOLYWORLD;
                };
            });

    private final ModeSetting useMode = new ModeSetting("Режим")
            .value("Default").values("Default", "Legit").setVisible(isHotkeysEnabled);

    private final SliderSetting legitDelay = new SliderSetting("Задержка")
            .value(0.0f).range(0.0f, 200.0f).step(10.0f)
            .setVisible(() -> isHotkeysEnabled.get() && useMode.is("Legit"));

    private final Map<InventoryUtil.ItemUsage, Pair<BindSetting, Mode>> keyBindings = new HashMap<>();
    private final List<NamedKeyBind> namedKeyBindings = new ArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<Pair<Long, Vec3d>> consumables = new ArrayList<>();
    private final Map<Vec3d, String> consumableNames = new HashMap<>();
    private boolean taksa = false;
    private boolean funtimePitchUse = false;
    private FuntimePhase funtimePhase = FuntimePhase.READY;
    private Item funtimeItem = null;
    private Predicate<ItemStack> funtimeMatcher = stack -> false;
    private int funtimeSavedSlot = -1;
    private int funtimeItemSlot = -1;
    private int funtimeSwapSlot = -1;
    private int funtimeHotbarSlot = -1;
    private int funtimeTargetSlot = -1;
    private int funtimeHotbarDigit = -1;
    private long funtimeLastPhaseTime = 0L;
    private InventoryActionUtil.MovementSnapshot funtimeMovementSnapshot;

    public AssistantModule() {
        keyBindings.put(new InventoryUtil.ItemUsage(Items.ENDER_EYE, this), new Pair<>(new BindSetting("Дезориентация").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.NETHERITE_SCRAP, this), new Pair<>(new BindSetting("Трапка").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SUGAR, this), new Pair<>(new BindSetting("Явка").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.FIRE_CHARGE, this), new Pair<>(new BindSetting("Огненный смерч").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.DRIED_KELP, this), new Pair<>(new BindSetting("Пласт").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SNOWBALL, this), new Pair<>(new BindSetting("Снежок заморозка").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.PHANTOM_MEMBRANE, this), new Pair<>(new BindSetting("Божка").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.WIND_CHARGE, this), new Pair<>(new BindSetting("воздухан").value(-999), Mode.FUNTIME));

        namedKeyBindings.add(new NamedKeyBind(Items.SPLASH_POTION, "хлопушка", new BindSetting("Хлопушка").value(-999), Mode.FUNTIME));
        namedKeyBindings.add(new NamedKeyBind(Items.SPLASH_POTION, "святая", new BindSetting("Святая вода").value(-999), Mode.FUNTIME));
        namedKeyBindings.add(new NamedKeyBind(Items.SPLASH_POTION, "снотворное", new BindSetting("Снотворное").value(-999), Mode.FUNTIME));
        namedKeyBindings.add(new NamedKeyBind(Items.SPLASH_POTION, "гнева", new BindSetting("Зелье гнева").value(-999), Mode.FUNTIME));
        namedKeyBindings.add(new NamedKeyBind(Items.SPLASH_POTION, "паладина", new BindSetting("Зелье паладина").value(-999), Mode.FUNTIME));
        namedKeyBindings.add(new NamedKeyBind(Items.SPLASH_POTION, "ассасина", new BindSetting("Зелье ассасина").value(-999), Mode.FUNTIME));
        namedKeyBindings.add(new NamedKeyBind(Items.SPLASH_POTION, "радиации", new BindSetting("Зелье радиации").value(-999), Mode.FUNTIME));

        keyBindings.put(new InventoryUtil.ItemUsage(Items.PRISMARINE_SHARD, this), new Pair<>(new BindSetting("Взрывная трапка").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.POPPED_CHORUS_FRUIT, this), new Pair<>(new BindSetting("Трапка").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SNOWBALL, this), new Pair<>(new BindSetting("Снежок").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.NETHER_STAR, this), new Pair<>(new BindSetting("Станка").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.FIRE_CHARGE, this), new Pair<>(new BindSetting("Взрывная шипучка").value(-999), Mode.HOLYWORLD));

        keyBindings.put(new InventoryUtil.ItemUsage(Items.CLAY_BALL, this), new Pair<>(new BindSetting("Ливалка").value(-999), Mode.LONYGRIEF));

        addSettings(functions, mode, useMode, legitDelay);

        keyBindings.forEach((key, value) -> {
            if (value.right() == Mode.HOLYWORLD) value.left().setVisible(isHWKeys);
            if (value.right() == Mode.FUNTIME) value.left().setVisible(isFTKeys);
            if (value.right() == Mode.LONYGRIEF) value.left().setVisible(isLGKeys);
            addSettings(value.left());
        });

        namedKeyBindings.forEach(bind -> {
            if (bind.mode == Mode.HOLYWORLD) bind.setting.setVisible(isHWKeys);
            if (bind.mode == Mode.FUNTIME) bind.setting.setVisible(isFTKeys);
            if (bind.mode == Mode.LONYGRIEF) bind.setting.setVisible(isLGKeys);
            addSettings(bind.setting);
        });
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent event) {
        if (isDefaultInvPhase(funtimePhase)) {
            event.setDirectionalInput(DirectionalInput.NONE);
            event.setJump(false);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (funtimePhase != FuntimePhase.READY) {
            executeFuntimeUse();
        }
        handleTickEvent();
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (!isHotkeysEnabled.get() || currentMode != Mode.FUNTIME || mc.player == null) return;
        if (mc.currentScreen != null || event.action() != 1 || funtimePhase != FuntimePhase.READY) return;

        for (Map.Entry<InventoryUtil.ItemUsage, Pair<BindSetting, Mode>> entry : keyBindings.entrySet()) {
            if (entry.getValue().right() != Mode.FUNTIME) continue;
            if (!InventoryActionUtil.matchesBind(event, entry.getValue().left().getValue())) continue;
            Item item = entry.getKey().getItem();
            funtimePitchUse = false;
            startFuntimeUse(stack -> stack.isOf(item), item);
            return;
        }

        for (NamedKeyBind bind : namedKeyBindings) {
            if (bind.mode != Mode.FUNTIME) continue;
            if (!InventoryActionUtil.matchesBind(event, bind.setting.getValue())) continue;
            funtimePitchUse = false;
            startFuntimeUse(bind::isMatching, bind.item);
            return;
        }
    }

    @EventHandler
    public void onJump(JumpEvent event) {
        if (!isHotkeysEnabled.get() || mc.currentScreen != null) return;
        if (currentMode != Mode.FUNTIME) return;
        int bind = getWindChargeBind();
        if (!KeyStorage.isPressed(bind)) return;
        if (mc.player.getItemCooldownManager().isCoolingDown(Items.WIND_CHARGE.getDefaultStack())) return;
        taksa = true;
    }

    @EventHandler
    private void renderEvent(Render2DEvent event) {
        if (!functions.isEnabled("Таймеры")) return;
        MatrixStack matrixStack = event.matrixStack();
        consumables.removeIf(cons -> cons.left() - System.currentTimeMillis() <= 0);

        for (Pair<Long, Vec3d> cons : consumables) {
            Vec3d position = cons.right();
            Vector2f screenPos = ProjectionUtil.project(position);
            if (screenPos.x == Float.MAX_VALUE || screenPos.y == Float.MAX_VALUE) continue;

            double time = MathUtil.round((double) (cons.left() - System.currentTimeMillis()) / 1000, 1);
            String name = consumableNames.getOrDefault(position, "Таймер");
            String text = name + ": " + time + "s";
            float size = 7f, gap = 3f;
            float textWidth = Fonts.PS_BOLD.getWidth(text, size);
            float posX = screenPos.x - textWidth / 2f, posY = screenPos.y;

            RenderUtil.BLUR_RECT.draw(matrixStack, posX, posY, textWidth + gap * 2f, size + gap * 2f, 2f, UIColors.blur());
            Fonts.PS_BOLD.drawText(matrixStack, text, posX + gap, posY + gap, size, UIColors.textColor());
        }
    }

    @EventHandler
    private void packetEvent(PacketEvent event) {
        if (!functions.isEnabled("Таймеры") || event.isSend()) return;
        if (currentMode == Mode.FUNTIME) FTTimers(event);
        else if (currentMode == Mode.HOLYWORLD) HWTimer(event);
    }

    private void FTTimers(PacketEvent event) {
        if (!(event.packet() instanceof PlaySoundS2CPacket soundPacket)) return;
        String soundPath = soundPacket.getSound().getIdAsString();

        if (soundPath.equals("minecraft:block.piston.contract")) {
            Vec3d pos = Vec3d.ofCenter(new BlockPos((int) soundPacket.getX(), (int) soundPacket.getY(), (int) soundPacket.getZ()));
            consumables.add(new Pair<>(System.currentTimeMillis() + 15000, pos));
            consumableNames.put(pos, "Trap");
        } else if (soundPath.equals("minecraft:block.anvil.place")) {
            BlockPos soundPos = new BlockPos((int) soundPacket.getX(), (int) soundPacket.getY(), (int) soundPacket.getZ());
            long delay = 250;
            scheduler.schedule(() -> getCube(soundPos, 4, 4).stream()
                    .filter(pos -> getDist(soundPos, pos) > 2 && mc.world.getBlockState(pos).getBlock() == Blocks.COBBLESTONE)
                    .min(Comparator.comparing(pos -> getDist(soundPos, pos)))
                    .ifPresent(pos -> {
                        if (getCube(pos, 1, 1).stream().anyMatch(p -> mc.world.getBlockState(p).getBlock() == Blocks.ANVIL)) return;
                        long solidCount = getCube(pos, 1, 1).stream().filter(p -> {
                            BlockState s = mc.world.getBlockState(p);
                            return !s.isAir() && s.isSolidBlock(mc.world, p);
                        }).count();
                        if (solidCount == 18 || solidCount == 15 || solidCount == 5) {
                            int time = solidCount == 18 || solidCount == 15 ? 20000 : 15000;
                            Vec3d addPos = Vec3d.ofCenter(pos).add(0, solidCount == 5 ? -1.5 : 0, 0);
                            consumables.add(new Pair<>(System.currentTimeMillis() + time - delay, addPos));
                            consumableNames.put(addPos, solidCount == 18 || solidCount == 15 ? "Plast" : "Trap");
                        }
                    }), delay, TimeUnit.MILLISECONDS);
        }
    }

    private void HWTimer(PacketEvent event) {
        if (!(event.packet() instanceof PlaySoundS2CPacket soundPacket)) return;
        String soundPath = soundPacket.getSound().getIdAsString();
        float volume = soundPacket.getVolume(), pitch = soundPacket.getPitch();

        if (soundPath.equals("minecraft:entity.generic.explode") && volume == 1.0f && pitch == 1.0f) {
            Vec3d pos = new Vec3d(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ());
            consumables.add(new Pair<>(System.currentTimeMillis() + 11000, pos));
            consumableNames.put(pos, "Trap");
        } else if (soundPath.equals("minecraft:block.beacon.deactivate") && volume == 1.5f && pitch == 1.0f) {
            Vec3d pos = new Vec3d(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ());
            consumables.add(new Pair<>(System.currentTimeMillis() + 15000, pos));
            consumableNames.put(pos, "Stun");
        }
    }

    private void handleTickEvent() {
        if (!isHotkeysEnabled.get() || mc.currentScreen != null) return;
        if (funtimePhase != FuntimePhase.READY) return;

        boolean legit = useMode.is("Legit");

        if (currentMode == Mode.FUNTIME) {
            int windBind = getWindChargeBind();
            if (KeyStorage.isPressed(windBind) && mc.player.isOnGround()
                    && !mc.player.getItemCooldownManager().isCoolingDown(Items.WIND_CHARGE.getDefaultStack())) {
                mc.player.jump();
            }
        }

        if (taksa) {
            taksa = false;
            if (currentMode == Mode.FUNTIME) {
                RotationComponent.update(new Rotation(mc.player.getYaw(), 90f), 180f,
                        180f, 180f, 180f, 5, 100, false);
                funtimePitchUse = true;
                startFuntimeUse(stack -> stack.isOf(Items.WIND_CHARGE), Items.WIND_CHARGE);
            } else {
                InventoryUtil.ItemUsage usage = getWindChargeUsage();
                if (usage != null) {
                    RotationComponent.update(new Rotation(mc.player.getYaw(), 90f), 180f,
                            180f, 180f, 180f, 5, 100, false);
                    usage.updateCustomRotation(new Rotation(mc.player.getYaw(), 90f));
                    usage.setUseRotation(true);
                    usage.packetMode();
                }
            }
        }

        if (currentMode == Mode.FUNTIME) return;

        keyBindings.forEach((key, value) -> {
            if (key.getItem() == Items.WIND_CHARGE) return;
            if (value.right() == currentMode) {
                key.handleUse(value.left().getValue(), legit);
            }
        });

        for (NamedKeyBind bind : namedKeyBindings) {
            if (bind.mode == currentMode) {
                bind.handleUse(legit);
            }
        }
    }

    private void startFuntimeUse(Predicate<ItemStack> matcher, Item item) {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (funtimePhase != FuntimePhase.READY || !InventoryFlowManager.script.isFinished()) return;
        if (mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) return;

        funtimeItem = item;
        funtimeMatcher = matcher;
        funtimeSavedSlot = mc.player.getInventory().selectedSlot;

        int[] slots = findFuntimeSlots(matcher);
        int hotbarSlot = slots[0];
        int containerSlot = slots[1];

        if (hotbarSlot == -1 && containerSlot == -1) {
            funtimeItem = null;
            funtimeMatcher = stack -> false;
            funtimePitchUse = false;
            return;
        }

        if (useMode.is("Legit")) {
            if (hotbarSlot != -1) {
                funtimeTargetSlot = hotbarSlot;
                funtimeHotbarDigit = funtimeSavedSlot;
                funtimeLastPhaseTime = System.currentTimeMillis();
                funtimePhase = FuntimePhase.ST_HOTBAR_SWITCH;
            } else {
                funtimeTargetSlot = containerSlot;
                funtimeHotbarDigit = funtimeSavedSlot;
                funtimeLastPhaseTime = 0L;
                funtimePhase = FuntimePhase.ST_OPEN_INV;
            }
            return;
        }

        if (hotbarSlot != -1) {
            funtimeHotbarSlot = hotbarSlot;
            InventoryToolkit.switchTo(funtimeHotbarSlot);
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

        if (mc.currentScreen != null && !isFuntimeScreenPhase(funtimePhase)) {
            resetFuntimeUse();
            return;
        }

        switch (funtimePhase) {
            case HOTBAR_USE -> {
                useFuntimeMainHand();
                funtimePhase = FuntimePhase.HOTBAR_RESTORE;
            }
            case HOTBAR_RESTORE -> {
                InventoryToolkit.switchTo(funtimeSavedSlot);
                funtimePhase = FuntimePhase.FINISH;
            }
            case INV_STOP -> {
                funtimeMovementSnapshot = InventoryActionUtil.stopMovement();
                funtimePhase = FuntimePhase.INV_WAIT;
            }
            case INV_WAIT -> {
                funtimePhase = FuntimePhase.INV_SWAP_IN;
            }
            case INV_SWAP_IN -> {
                InventoryToolkit.clickSlot(funtimeItemSlot, funtimeSwapSlot, SlotActionType.SWAP);
                funtimePhase = FuntimePhase.INV_SWITCH;
            }
            case INV_SWITCH -> {
                InventoryToolkit.switchTo(funtimeSwapSlot);
                funtimePhase = FuntimePhase.INV_USE;
            }
            case INV_USE -> {
                useFuntimeMainHand();
                funtimePhase = FuntimePhase.INV_SWAP_OUT;
            }
            case INV_SWAP_OUT -> {
                InventoryToolkit.clickSlot(funtimeItemSlot, funtimeSwapSlot, SlotActionType.SWAP);
                funtimePhase = FuntimePhase.INV_CLOSE;
            }
            case INV_CLOSE -> {
                InventoryTask.closeScreen(true);
                if (funtimeMovementSnapshot != null) funtimeMovementSnapshot.restore();
                funtimePhase = FuntimePhase.FINISH;
            }
            case ST_HOTBAR_SWITCH -> {
                if (!funtimePhaseReady()) return;
                InventoryToolkit.switchTo(funtimeTargetSlot);
                funtimePhase = FuntimePhase.ST_HOTBAR_USE;
            }
            case ST_HOTBAR_USE -> {
                if (!funtimePhaseReady()) return;
                useFuntimeMainHand();
                funtimePhase = FuntimePhase.ST_HOTBAR_RESTORE;
            }
            case ST_HOTBAR_RESTORE -> {
                if (!funtimePhaseReady()) return;
                InventoryToolkit.switchTo(funtimeSavedSlot);
                funtimePhase = FuntimePhase.FINISH;
            }
            case ST_OPEN_INV -> {
                if (!funtimePhaseReady()) return;
                mc.setScreen(new InventoryScreen(mc.player));
                funtimePhase = FuntimePhase.ST_MOVE_CURSOR;
            }
            case ST_MOVE_CURSOR -> {
                if (!funtimePhaseReady()) return;
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
                InventoryActionUtil.moveCursorToSlot(screen, funtimeTargetSlot);
                funtimePhase = FuntimePhase.ST_PRESS_DIGIT;
            }
            case ST_PRESS_DIGIT -> {
                if (!funtimePhaseReady()) return;
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
                InventoryActionUtil.pressHotbarKey(screen, funtimeHotbarDigit);
                funtimePhase = FuntimePhase.ST_CLOSE_INV;
            }
            case ST_CLOSE_INV -> {
                if (!funtimePhaseReady()) return;
                InventoryActionUtil.closeCurrentScreenWithInventoryKey();
                funtimePhase = FuntimePhase.ST_USE;
            }
            case ST_USE -> {
                if (!funtimePhaseReady()) return;
                if (mc.currentScreen != null) return;
                useFuntimeMainHand();
                funtimePhase = FuntimePhase.ST_REOPEN;
            }
            case ST_REOPEN -> {
                if (!funtimePhaseReady()) return;
                mc.setScreen(new InventoryScreen(mc.player));
                funtimePhase = FuntimePhase.ST_MOVE_BACK;
            }
            case ST_MOVE_BACK -> {
                if (!funtimePhaseReady()) return;
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
                InventoryActionUtil.moveCursorToSlot(screen, funtimeTargetSlot);
                funtimePhase = FuntimePhase.ST_PRESS_BACK;
            }
            case ST_PRESS_BACK -> {
                if (!funtimePhaseReady()) return;
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
                InventoryActionUtil.pressHotbarKey(screen, funtimeHotbarDigit);
                funtimePhase = FuntimePhase.ST_CLOSE_FINAL;
            }
            case ST_CLOSE_FINAL -> {
                if (!funtimePhaseReady()) return;
                InventoryActionUtil.closeCurrentScreenWithInventoryKey();
                funtimePhase = FuntimePhase.FINISH;
            }
            case FINISH -> resetFuntimeUse();
            case READY -> {
            }
        }
    }

    private void useFuntimeMainHand() {
        if (funtimePitchUse) {
            RotationComponent.update(new Rotation(mc.player.getYaw(), 90f), 180f,
                    180f, 180f, 180f, 5, 100, false);
        }
        InventoryToolkit.interactItem(Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean funtimePhaseReady() {
        long now = System.currentTimeMillis();
        if (now - funtimeLastPhaseTime >= legitDelay.getValue().longValue()) {
            funtimeLastPhaseTime = now;
            return true;
        }
        return false;
    }

    private boolean isFuntimeScreenPhase(FuntimePhase phase) {
        return switch (phase) {
            case ST_MOVE_CURSOR, ST_PRESS_DIGIT, ST_CLOSE_INV, ST_MOVE_BACK, ST_PRESS_BACK, ST_CLOSE_FINAL -> true;
            default -> false;
        };
    }

    private boolean isDefaultInvPhase(FuntimePhase phase) {
        return switch (phase) {
            case INV_STOP, INV_WAIT, INV_SWAP_IN, INV_SWITCH, INV_USE, INV_SWAP_OUT, INV_CLOSE -> true;
            default -> false;
        };
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

    private void resetFuntimeUse() {
        if (funtimeMovementSnapshot != null) {
            funtimeMovementSnapshot.restore();
            funtimeMovementSnapshot = null;
        }
        if (mc.player != null && funtimeSavedSlot >= 0 && funtimeSavedSlot <= 8) {
            InventoryToolkit.switchTo(funtimeSavedSlot);
        }
        funtimePhase = FuntimePhase.READY;
        funtimeItem = null;
        funtimeMatcher = stack -> false;
        funtimeSavedSlot = -1;
        funtimeItemSlot = -1;
        funtimeSwapSlot = -1;
        funtimeHotbarSlot = -1;
        funtimeTargetSlot = -1;
        funtimeHotbarDigit = -1;
        funtimeLastPhaseTime = 0L;
        funtimePitchUse = false;
    }

    private InventoryUtil.ItemUsage getWindChargeUsage() {
        for (Map.Entry<InventoryUtil.ItemUsage, Pair<BindSetting, Mode>> entry : keyBindings.entrySet()) {
            if (entry.getKey().getItem() == Items.WIND_CHARGE && entry.getValue().right() == currentMode) {
                return entry.getKey();
            }
        }
        return null;
    }

    private int getWindChargeBind() {
        for (Map.Entry<InventoryUtil.ItemUsage, Pair<BindSetting, Mode>> entry : keyBindings.entrySet()) {
            if (entry.getKey().getItem() == Items.WIND_CHARGE && entry.getValue().right() == currentMode) {
                return entry.getValue().left().getValue();
            }
        }
        return -999;
    }

    private double getDist(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX(), dy = pos1.getY() - pos2.getY(), dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private List<BlockPos> getCube(BlockPos center, int xRadius, int yRadius) {
        List<BlockPos> sphere = new ArrayList<>();
        for (int x = -xRadius; x <= xRadius; x++) {
            for (int y = -yRadius; y <= yRadius; y++) {
                for (int z = -xRadius; z <= xRadius; z++) {
                    sphere.add(center.add(x, y, z));
                }
            }
        }
        return sphere;
    }

    private final class NamedKeyBind {
        private final Item item;
        private final String namePart;
        private final BindSetting setting;
        private final Mode mode;

        private boolean forceUse = false;
        private int targetSlot = -1;
        private int stateDelay = 0;
        private Runnable pendingAction = null;
        private InventoryUtil.ItemUsage.UsageState usageState = InventoryUtil.ItemUsage.UsageState.IDLE;

        private NamedKeyBind(Item item, String namePart, BindSetting setting, Mode mode) {
            this.item = item;
            this.namePart = namePart.toLowerCase(Locale.ROOT);
            this.setting = setting;
            this.mode = mode;
        }

        private void handleUse(boolean isLegit) {
            if (!isLegit) pendingAction = null;

            if (mc.currentScreen == null && pendingAction != null) pendingAction.run();

            int bind = setting.getValue();
            if (!KeyStorage.isPressed(bind) && mc.currentScreen == null) {
                forceUse = false;
                return;
            }

            if (forceUse || mc.currentScreen != null) return;

            if (isLegit) {
                pendingAction = this::legitMode;
            } else {
                packetMode();
            }
        }

        private void packetMode() {
            int hbSlot = findNamedItem(true);
            int invSlot = findNamedItem(false);

            if (isMatching(mc.player.getMainHandStack())) {
                InventoryUtil.useItem(Hand.MAIN_HAND);
                forceUse = true;
                return;
            }

            int oldSlot = mc.player.getInventory().selectedSlot;

            if (hbSlot != -1) {
                InventoryUtil.swapToSlot(hbSlot);
                InventoryUtil.useItem(Hand.MAIN_HAND);
                InventoryUtil.swapToSlot(oldSlot);
                forceUse = true;
            } else if (invSlot != -1) {
                int screenSlot = invSlot < 9 ? invSlot + 36 : invSlot;
                int swapTarget = oldSlot;
                InventoryToolkit.clickSlot(screenSlot, swapTarget, net.minecraft.screen.slot.SlotActionType.SWAP);
                InventoryToolkit.switchTo(swapTarget);
                InventoryToolkit.interactItem(Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);
                InventoryToolkit.clickSlot(screenSlot, swapTarget, net.minecraft.screen.slot.SlotActionType.SWAP);
                InventoryTask.closeScreen(true);
                forceUse = true;
            }
        }

        private void legitMode() {
            switch (usageState) {
                case IDLE -> {
                    targetSlot = findNamedItem(true);
                    if (targetSlot == -1) targetSlot = findNamedItem(false);
                    if (targetSlot == -1) return;
                    usageState = InventoryUtil.ItemUsage.UsageState.MOVE_TO_OFFHAND;
                    stateDelay = 0;
                }
                case MOVE_TO_OFFHAND -> {
                    if (stateDelay-- > 0) return;
                    if (targetSlot < 9) {
                        InventoryUtil.swapToSlot(targetSlot);
                    } else {
                        int swapTo = mc.player.getInventory().selectedSlot;
                        int screenSlot = targetSlot < 9 ? targetSlot + 36 : targetSlot;
                        InventoryToolkit.clickSlot(screenSlot, swapTo, net.minecraft.screen.slot.SlotActionType.SWAP);
                        InventoryToolkit.switchTo(swapTo);
                    }
                    usageState = InventoryUtil.ItemUsage.UsageState.USE_ITEM;
                    stateDelay = 0;
                }
                case USE_ITEM -> {
                    if (stateDelay-- > 0) return;
                    InventoryUtil.useItem(Hand.MAIN_HAND);
                    usageState = InventoryUtil.ItemUsage.UsageState.RESTORE_OFFHAND;
                    stateDelay = 0;
                }
                case RESTORE_OFFHAND -> {
                    if (stateDelay-- > 0) return;
                    if (targetSlot >= 9) {
                        int swapTo = mc.player.getInventory().selectedSlot;
                        int screenSlot = targetSlot;
                        InventoryToolkit.clickSlot(screenSlot, swapTo, net.minecraft.screen.slot.SlotActionType.SWAP);
                        InventoryTask.closeScreen(true);
                    }
                    usageState = InventoryUtil.ItemUsage.UsageState.RESTORE_SLOT;
                    stateDelay = 0;
                }
                case RESTORE_SLOT -> {
                    if (stateDelay-- > 0) return;
                    usageState = InventoryUtil.ItemUsage.UsageState.IDLE;
                    pendingAction = null;
                    forceUse = true;
                }
            }
        }

        private int findNamedItem(boolean inHotbar) {
            DefaultedList<ItemStack> main = mc.player.getInventory().main;
            int firstSlot = inHotbar ? 0 : 9;
            int lastSlot = inHotbar ? 9 : 36;
            for (int i = firstSlot; i < lastSlot; i++) {
                ItemStack stack = main.get(i);
                if (isMatching(stack)) return i;
            }
            return -1;
        }

        private boolean isMatching(ItemStack stack) {
            if (stack == null || stack.isEmpty() || stack.getItem() != item) return false;
            return stack.getName().getString().toLowerCase(Locale.ROOT).contains(namePart);
        }
    }
}