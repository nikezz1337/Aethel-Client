package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.other.StopWatch;

import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "AutoLooter", category = Category.OTHER, description = "Автоматически лутает сундуки и вазы, депозитит в клан-хранилище")
public class AutoLooterModule extends Module {
    @Getter private static final AutoLooterModule instance = new AutoLooterModule();

    private enum State {
        GET_INVIS, WAIT_HOME, LOOTING, USE_DARENA, WAIT_DARENA, CLAN_HOME, WAIT_CLAN_STORAGE, DEPOSIT
    }

    private State currentState = State.GET_INVIS;
    private BlockPos invisChest = null;
    private int noLootCounter = 0;

    private final StopWatch stateTimer = new StopWatch();
    private final StopWatch actionTimer = new StopWatch();

    @Override
    public void onEnable() {
        currentState = State.GET_INVIS;
        invisChest = null;
        noLootCounter = 0;
        stateTimer.reset();
        actionTimer.reset();
    }

    @Override
    public void onDisable() {
        invisChest = null;
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            switch (currentState) {
                case GET_INVIS        -> handleGetInvis();
                case WAIT_HOME        -> { if (stateTimer.isReached(8000)) { sendChat("/home"); setState(State.LOOTING, 0); } }
                case LOOTING          -> handleLooting();
                case USE_DARENA       -> { sendChat("/darena"); setState(State.WAIT_DARENA, 1000); }
                case WAIT_DARENA      -> { if (stateTimer.isReached(1000)) { clickPufferfish(); setState(State.CLAN_HOME, 3000); } }
                case CLAN_HOME        -> { if (stateTimer.isReached(3000)) { sendChat("/clan home storage"); setState(State.WAIT_CLAN_STORAGE, 8000); } }
                case WAIT_CLAN_STORAGE -> { if (stateTimer.isReached(8000)) setState(State.DEPOSIT, 1500); }
                case DEPOSIT          -> handleDeposit();
            }
        }));

        addEvents(tickEvent);
    }

    private void handleGetInvis() {
        if (invisChest == null) {
            invisChest = findInvisChest();
            if (invisChest == null) { setEnabled(false); return; }
        }

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            boolean grabbed = false;
            for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
                ItemStack stack = screen.getScreenHandler().slots.get(i).getStack();
                if (!stack.isEmpty() && isInvisPotion(stack)) {
                    mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                    grabbed = true;
                    break;
                }
            }
            mc.player.closeHandledScreen();
            if (grabbed) setState(State.WAIT_HOME, 8000);
            else if (isInventoryFull()) setEnabled(false);
        } else {
            interactBlock(invisChest);
        }
    }

    private BlockPos findInvisChest() {
        BlockPos origin = mc.player.getBlockPos();
        int radius = 32;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -8; y <= 8; y++) {
                    BlockPos pos = origin.add(x, y, z);
                    BlockEntity be = mc.world.getBlockEntity(pos);
                    if (!(be instanceof ChestBlockEntity)) continue;
                    BlockEntity signBe = mc.world.getBlockEntity(pos.up());
                    if (signBe instanceof SignBlockEntity sign) {
                        String text = sign.getFrontText().getMessage(0, false).getString().toLowerCase();
                        if (text.contains("inv") || text.contains("инвиз")) return pos;
                    }
                }
            }
        }
        return null;
    }

    private void handleLooting() {
        if (!actionTimer.isReached(200)) return;

        boolean looted = false;

        for (BlockPos pos : findNearbyChests(25)) {
            if (pos.equals(invisChest)) continue;
            if (lootContainer(pos)) { looted = true; break; }
        }

        if (!looted) {
            for (BlockPos pos : findVases()) {
                if (breakVase(pos)) { looted = true; break; }
            }
        }

        if (looted) {
            noLootCounter = 0;
            actionTimer.reset();
            setState(State.USE_DARENA, 0);
        } else {
            noLootCounter++;
            if (noLootCounter > 5) setEnabled(false);
        }
    }

    private boolean lootContainer(BlockPos pos) {
        if (mc.player.squaredDistanceTo(pos.toCenterPos()) > 625) return false;
        interactBlock(pos);
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return false;
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
            if (!screen.getScreenHandler().slots.get(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
        }
        mc.player.closeHandledScreen();
        return true;
    }

    private List<BlockPos> findNearbyChests(int radius) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos origin = mc.player.getBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -8; y <= 8; y++) {
                    BlockPos pos = origin.add(x, y, z);
                    if (mc.world.getBlockEntity(pos) instanceof ChestBlockEntity) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }

    private List<BlockPos> findVases() {
        List<BlockPos> list = new ArrayList<>();
        BlockPos origin = mc.player.getBlockPos();
        for (int x = -10; x <= 10; x++)
            for (int z = -10; z <= 10; z++)
                for (int y = -5; y <= 5; y++) {
                    BlockPos p = origin.add(x, y, z);
                    if (mc.world.getBlockState(p).getBlock() == Blocks.FLOWER_POT) list.add(p);
                }
        return list;
    }

    private boolean breakVase(BlockPos pos) {
        if (mc.player.squaredDistanceTo(pos.toCenterPos()) > 100) return false;
        mc.interactionManager.attackBlock(pos, Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private void clickPufferfish() {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
            ItemStack stack = screen.getScreenHandler().slots.get(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == Items.PUFFERFISH) {
                int syncId = screen.getScreenHandler().syncId;
                mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.PICKUP, mc.player);
                break;
            }
        }
        mc.player.closeHandledScreen();
    }

    private void handleDeposit() {
        if (!stateTimer.isReached(1500)) return;

        boolean deposited = false;
        for (BlockPos pos : findNearbyChests(10)) {
            if (pos.equals(invisChest)) continue;
            if (depositTo(pos)) { deposited = true; break; }
        }

        if (deposited || stateTimer.isReached(10000)) {
            setState(State.GET_INVIS, 0);
        }
    }

    private boolean depositTo(BlockPos pos) {
        if (mc.player.squaredDistanceTo(pos.toCenterPos()) > 100) return false;
        interactBlock(pos);
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return false;
        int syncId = screen.getScreenHandler().syncId;
        int chestSize = screen.getScreenHandler().slots.size() - 36;
        for (int inv = 9; inv < 36; inv++) {
            ItemStack stack = mc.player.getInventory().getStack(inv);
            if (stack.isEmpty() || isExcluded(stack)) continue;
            mc.interactionManager.clickSlot(syncId, chestSize + inv, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
        mc.player.closeHandledScreen();
        return true;
    }

    private void interactBlock(BlockPos pos) {
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false));
    }

    private void setState(State state, long delayMs) {
        currentState = state;
        stateTimer.setLastMS(-delayMs);
    }

    private void sendChat(String msg) {
        if (mc.player != null) mc.player.networkHandler.sendChatMessage(msg);
    }

    private boolean isInvisPotion(ItemStack stack) {
        if (stack.getItem() != Items.POTION && stack.getItem() != Items.SPLASH_POTION) return false;
        String name = stack.getName().getString().toLowerCase();
        return name.contains("invis") || name.contains("невидим");
    }

    private boolean isExcluded(ItemStack stack) {
        return stack.getItem() == Items.POTION || isInvisPotion(stack);
    }

    private boolean isInventoryFull() {
        for (int i = 9; i < 36; i++)
            if (mc.player.getInventory().getStack(i).isEmpty()) return false;
        return true;
    }
}
