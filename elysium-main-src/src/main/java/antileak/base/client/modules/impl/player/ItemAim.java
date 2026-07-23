package antileak.base.client.modules.impl.player;

import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.api.storages.implement.RotationStorage;
import antileak.base.api.utils.rotate.Rotation;
import antileak.base.api.utils.rotate.RotationUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.BooleanSetting;
import antileak.base.client.modules.settings.implement.ListSetting;

public class ItemAim extends Module {

    public static ItemAim INSTANCE = new ItemAim();
    public ListSetting element = new ListSetting("Лутать",
            new BooleanSetting("Шары", true),
            new BooleanSetting("Элитры", true));

    public ItemAim() {
        super("ItemAim","Автоматически наводиться на предмет", ModuleCategory.PLAYER);
        addSettings(element);
    }

    
    @EventLink
    public void onEvent(final EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        ItemEntity targetItem = findTargetItem();
        if (targetItem == null) return;

        Vec2f rotations = getItemRotations(targetItem);
        RotationStorage.update(new Rotation(rotations.x, rotations.y), 360f, 360f, 360f, 360f, 0, 1, false);
    }

    private ItemEntity findTargetItem() {
        ItemEntity bestItem = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;
            if (!isWantedItem(itemEntity)) continue;

            double distance = mc.player.squaredDistanceTo(itemEntity);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestItem = itemEntity;
            }
        }

        return bestItem;
    }

    private boolean isWantedItem(ItemEntity itemEntity) {
        return (element.is("Шары") && itemEntity.getStack().isOf(Items.PLAYER_HEAD))
                || (element.is("Элитры") && itemEntity.getStack().isOf(Items.ELYTRA));
    }

    private Vec2f getItemRotations(ItemEntity itemEntity) {
        Vec3d targetPos = itemEntity.getBoundingBox().getCenter();
        return RotationUtils.getRotations(targetPos);
    }
}
