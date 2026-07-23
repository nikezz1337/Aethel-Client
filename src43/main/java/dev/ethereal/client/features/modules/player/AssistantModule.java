package dev.ethereal.client.features.modules.player;

import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.move.JumpEvent;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.system.backend.KeyStorage;
import dev.ethereal.api.system.backend.Pair;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.math.ProjectionUtil;
import dev.ethereal.api.utils.other.SlownessManager;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationComponent;
import dev.ethereal.client.features.modules.movement.InventoryMoveModule;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
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
import java.util.function.Supplier;

@ModuleRegister(name = "Assistant", category = Category.PLAYER)
public class AssistantModule extends Module {
    @Getter private static final AssistantModule instance = new AssistantModule();

    public enum Mode {
        FUNTIME, HOLYWORLD, LONYGRIEF
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

    private final Map<InventoryUtil.ItemUsage, Pair<BindSetting, Mode>> keyBindings = new HashMap<>();
    private final List<NamedKeyBind> namedKeyBindings = new ArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<Pair<Long, Vec3d>> consumables = new ArrayList<>();
    private final Map<Vec3d, String> consumableNames = new HashMap<>();
    boolean packet;

    private boolean taksa = false;

    public AssistantModule() {
        keyBindings.put(new InventoryUtil.ItemUsage(Items.ENDER_EYE, this), new Pair<>(new BindSetting("Дезориентация").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.NETHERITE_SCRAP, this), new Pair<>(new BindSetting("Трапка").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SUGAR, this), new Pair<>(new BindSetting("Явка").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.FIRE_CHARGE, this), new Pair<>(new BindSetting("Огненый смерч").value(-999), Mode.FUNTIME));
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

        addSettings(functions, mode);

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

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            packet = PlayerUtil.isHW() || PlayerUtil.isST();
            handleTickEvent();
        }));

        EventListener jumpEvent = JumpEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!isHotkeysEnabled.get() || mc.currentScreen != null) return;
            if (currentMode != Mode.FUNTIME) return;
            int bind = getWindChargeBind();
            if (!KeyStorage.isPressed(bind)) return;
            if (mc.player.getItemCooldownManager().isCoolingDown(Items.WIND_CHARGE.getDefaultStack())) return;
            taksa = true;
        }));

        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(this::renderEvent));
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(this::packetEvent));

        addEvents(tickEvent, jumpEvent, renderEvent, packetEvent);
    }

    private void renderEvent(Render2DEvent.Render2DEventData event) {
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

    private void packetEvent(PacketEvent.PacketEventData event) {
        if (!functions.isEnabled("Таймеры") || event.isSend()) return;
        if (currentMode == Mode.FUNTIME) FTTimers(event);
        else if (currentMode == Mode.HOLYWORLD) HWTimer(event);
    }

    private void FTTimers(PacketEvent.PacketEventData event) {
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

    private void HWTimer(PacketEvent.PacketEventData event) {
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

        boolean legit = !packet && InventoryMoveModule.getInstance().isLegit();

        if (currentMode == Mode.FUNTIME) {
            int windBind = getWindChargeBind();
            if (KeyStorage.isPressed(windBind) && mc.player.isOnGround()
                    && !mc.player.getItemCooldownManager().isCoolingDown(Items.WIND_CHARGE.getDefaultStack())) {
                mc.player.jump();
            }
        }

        if (taksa) {
            taksa = false;
            InventoryUtil.ItemUsage usage = getWindChargeUsage();
            if (usage != null) {
                RotationComponent.update(new Rotation(mc.player.getYaw(), 90f), 180f,
                        180f, 180f, 180f, 5, 100, false);
                usage.updateCustomRotation(new Rotation(mc.player.getYaw(), 90f));
                usage.setUseRotation(true);
                usage.packetMode();
            }
        }

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
            int invSlot = findNamedItem(false);
            int hbSlot = findNamedItem(true);

            if (mc.player.getOffHandStack() != null && isMatching(mc.player.getOffHandStack())) {
                InventoryUtil.useItem(Hand.OFF_HAND);
                forceUse = true;
                return;
            }

            if (mc.player.getMainHandStack() != null && isMatching(mc.player.getMainHandStack())) {
                InventoryUtil.useItem(Hand.MAIN_HAND);
                forceUse = true;
                return;
            }

            int oldSlot = mc.player.getInventory().selectedSlot;
            int bestSlot = InventoryUtil.findBestSlotInHotBar();

            if (hbSlot != -1) {
                InventoryUtil.swapToSlot(hbSlot);
                InventoryUtil.useItem(Hand.MAIN_HAND);
                InventoryUtil.swapToSlot(oldSlot);
                forceUse = true;
            } else if (invSlot != -1) {
                Runnable runnable = () -> {
                    InventoryUtil.swapSlots(invSlot, bestSlot);
                    InventoryUtil.swapToSlot(bestSlot);
                    InventoryUtil.useItem(Hand.MAIN_HAND);
                    InventoryUtil.swapToSlot(oldSlot);
                    InventoryUtil.swapSlots(invSlot, bestSlot);
                    forceUse = true;
                };
                if (SlownessManager.isEnabled()) {
                    SlownessManager.applySlowness(60L, runnable);
                } else {
                    runnable.run();
                }
            }
        }

        private void legitMode() {
            switch (usageState) {
                case IDLE -> {
                    targetSlot = findNamedItem(false);
                    if (targetSlot == -1) return;
                    usageState = InventoryUtil.ItemUsage.UsageState.MOVE_TO_OFFHAND;
                    stateDelay = 0;
                }
                case MOVE_TO_OFFHAND -> {
                    if (stateDelay-- > 0) return;
                    Runnable moveToOffhand = () -> {
                        InventoryUtil.swapToOffhand(targetSlot);
                        usageState = InventoryUtil.ItemUsage.UsageState.USE_ITEM;
                        stateDelay = 0;
                    };
                    if (SlownessManager.isEnabled()) {
                        SlownessManager.applySlowness(10L, moveToOffhand);
                    } else {
                        moveToOffhand.run();
                    }
                }
                case USE_ITEM -> {
                    if (stateDelay-- > 0) return;
                    InventoryUtil.useItem(Hand.OFF_HAND);
                    usageState = InventoryUtil.ItemUsage.UsageState.RESTORE_OFFHAND;
                    stateDelay = 0;
                }
                case RESTORE_OFFHAND -> {
                    if (stateDelay-- > 0) return;
                    Runnable restoreOffhand = () -> {
                        InventoryUtil.swapToOffhand(targetSlot);
                        usageState = InventoryUtil.ItemUsage.UsageState.RESTORE_SLOT;
                        stateDelay = 0;
                    };
                    if (SlownessManager.isEnabled()) {
                        SlownessManager.applySlowness(10L, restoreOffhand);
                    } else {
                        restoreOffhand.run();
                    }
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
