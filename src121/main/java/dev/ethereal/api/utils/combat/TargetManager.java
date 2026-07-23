package dev.ethereal.api.utils.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Getter;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.system.configs.FriendManager;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.combat.AntiBotUtil;
import dev.ethereal.client.features.modules.other.HealthResolverModule;

public class TargetManager implements QuickImports {
    @Getter private LivingEntity currentTarget;
    private Stream<LivingEntity> potentialTargets;

    public void lockTarget(LivingEntity target) {
        if (currentTarget == null) {
            currentTarget = target;
        }
    }

    public void releaseTarget() {
        currentTarget = null;
    }

    public void validateTarget(Predicate<LivingEntity> predicate) {
        findFirstMatch(predicate).ifPresent(this::lockTarget);

        if (currentTarget != null && !predicate.test(currentTarget)) {
            releaseTarget();
        }
    }

    public void searchTargets(Iterable<Entity> entities, float maxDistance) {
        if (isTargetOutOfRange(maxDistance)) {
            releaseTarget();
        }

        potentialTargets = createStreamFromEntities(entities, maxDistance);
    }

    private boolean isTargetOutOfRange(float maxDistance) {
        return currentTarget != null && RotationUtil.getSpot(currentTarget).distanceTo(mc.player.getEyePos()) > maxDistance;
    }

    private Stream<LivingEntity> createStreamFromEntities(Iterable<Entity> entities, float maxDistance) {
        Vec3d eyePos = mc.player.getEyePos();
        return StreamSupport.stream(entities.spliterator(), false)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(it -> new TargetCandidate(it, eyePos.distanceTo(RotationUtil.getSpot(it))))
                .filter(it -> it.distance() <= maxDistance)
                .sorted(java.util.Comparator.comparingDouble(TargetCandidate::distance))
                .map(TargetCandidate::entity);
    }

    private record TargetCandidate(LivingEntity entity, double distance) {}

    private java.util.Optional<LivingEntity> findFirstMatch(java.util.function.Predicate<LivingEntity> predicate) {
        return potentialTargets != null ? potentialTargets.filter(predicate).findFirst() : java.util.Optional.empty();
    }

    public static class EntityFilter implements QuickImports {
        public List<String> targetSettings;
        public boolean needFriends = false;

        public EntityFilter(List<String> targetSettings) {
            this.targetSettings = targetSettings;
        }

        public boolean isValid(LivingEntity entity) {
            if (isLocalPlayer(entity)) return false;
            if (isInvalidHealth(entity)) return false;
            if (isBotPlayer(entity)) return false;

            return isValidEntityType(entity);
        }

        private boolean isLocalPlayer(LivingEntity entity) {
            return entity == mc.player;
        }

        private boolean isInvalidHealth(LivingEntity entity) {
            if (!entity.isAlive()) return true;

            if (entity instanceof PlayerEntity) {
                return HealthResolverModule.getInstance().getHealthFromScoreboard(entity)[0] <= 0;
            }

            return entity.getHealth() <= 0;
        }

        private boolean isBotPlayer(LivingEntity entity) {
            if (entity == mc.player.getControllingVehicle()) return true;
            if (entity instanceof PlayerEntity player && AntiBotUtil.isBot(player)) return true;
            return false;
        }

        private boolean isValidEntityType(LivingEntity entity) {
            if (entity instanceof PlayerEntity player) {
                if (FriendManager.getInstance().contains(player.getName().getString()) && !needFriends) {
                    return false;
                } else {
                    return targetSettings.contains("Игроки");
                }
            } else if (entity instanceof AnimalEntity) {
                return targetSettings.contains("Животные");
            } else if (entity instanceof MobEntity) {
                return targetSettings.contains("Мобы");
            } else return !(entity instanceof ArmorStandEntity);
        }
    }
}
