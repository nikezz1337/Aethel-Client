package dev.ethereal.client.features.modules.player;

import dev.ethereal.api.event.events.player.world.AttackEvent;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.system.backend.Pair;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.math.ProjectionUtil;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.api.utils.rotation.manager.Rotation;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@ModuleRegister(name = "Assistant", category = Category.PLAYER)
public class AssistantModule extends Module {
    @Getter private static final AssistantModule instance = new AssistantModule();

    public enum Mode {
        FUNTIME,
        HOLYWORLD,
        LONYGRIEF;
    }

    private final Supplier<Boolean> isHotkeysEnabled = () -> getFunctions().isEnabled("Горячие клавиши");
    private final Supplier<Boolean> isHWKeys = () -> isHotkeysEnabled.get() && getMode().is("HolyWorld");
    private final Supplier<Boolean> isFTKeys = () -> isHotkeysEnabled.get() && getMode().is("FunTime");
    private final Supplier<Boolean> isLGKeys = () -> isHotkeysEnabled.get() && getMode().is("LonyGrief");

    private Mode currentMode = Mode.FUNTIME;

    @Getter private final MultiBooleanSetting functions = new MultiBooleanSetting("Опции").value(
            new BooleanSetting("Горячие клавиши").value(true),
            new BooleanSetting("Таймеры").value(false)
    );

    @Getter private final ModeSetting mode = new ModeSetting("Тип").value("FunTime")
            .values("FunTime", "HolyWorld", "LonyGrief").setVisible(isHotkeysEnabled)
            .onAction(() -> {
                currentMode = switch (getMode().getValue()) {
                    case "FunTime" -> Mode.FUNTIME;
                    case "LonyGrief" -> Mode.LONYGRIEF;
                    default -> Mode.HOLYWORLD;
                };
            });

    private final BooleanSetting legit = new BooleanSetting("Legit").value(true).setVisible(isHotkeysEnabled);
    private final Map<InventoryUtil.ItemUsage, Pair<BindSetting, Mode>> keyBindings = new HashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<Pair<Long, Vec3d>> consumables = new ArrayList<>();
    private final Map<Vec3d, String> consumableNames = new HashMap<>();

    public AssistantModule() {
        keyBindings.put(new InventoryUtil.ItemUsage(Items.ENDER_EYE, this), new Pair<>(new BindSetting("Дезориентация").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.NETHERITE_SCRAP, this), new Pair<>(new BindSetting("Трапка").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SUGAR, this), new Pair<>(new BindSetting("Явная Пыль").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.FIRE_CHARGE, this), new Pair<>(new BindSetting("Огненый Смерч").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.DRIED_KELP, this), new Pair<>(new BindSetting("Пласт").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SNOWBALL, this), new Pair<>(new BindSetting("Снежок заморозки").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.PHANTOM_MEMBRANE, this), new Pair<>(new BindSetting("Божья Аура").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.WIND_CHARGE, this), new Pair<>(new BindSetting("Заряд ветра").value(-999), Mode.FUNTIME));

        keyBindings.put(new InventoryUtil.ItemUsage(Items.PRISMARINE_SHARD, this), new Pair<>(new BindSetting("Взрывная трапка").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.POPPED_CHORUS_FRUIT, this), new Pair<>(new BindSetting("Трапка обыч").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SNOWBALL, this), new Pair<>(new BindSetting("снежок").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.NETHER_STAR, this), new Pair<>(new BindSetting("Стан").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.FIRE_CHARGE, this), new Pair<>(new BindSetting("Взрывная штучка").value(-999), Mode.HOLYWORLD));

        keyBindings.put(new InventoryUtil.ItemUsage(Items.CLAY_BALL, this), new Pair<>(new BindSetting("Ливалка").value(-999), Mode.LONYGRIEF));

        addSettings(functions, mode, legit);

        keyBindings.forEach((key, value) -> {
            if (value.right() == Mode.HOLYWORLD) {
                value.left().setVisible(isHWKeys);
            }

            if (value.right() == Mode.FUNTIME) {
                value.left().setVisible(isFTKeys);
            }

            if (value.right() == Mode.LONYGRIEF) {
                value.left().setVisible(isLGKeys);
            }
            addSettings(value.left());
        });
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            handleTickEvent();
        }));

        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(this::renderEvent));

        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(this::packetEvent));

        addEvents(tickEvent, renderEvent, packetEvent);
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

        keyBindings.forEach((key, value) -> {
            if (value.right() == currentMode) {
                key.handleUse(value.left().getValue(), legit.getValue());
            }
        });
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
}
