package ru.zenith.implement.features.modules.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.zenith.core.Main;
import ru.zenith.implement.features.modules.combat.Aura;
import ru.zenith.implement.features.modules.combat.killaura.attack.AttackHandler;
import ru.zenith.implement.features.modules.combat.killaura.rotation.Angle;
import ru.zenith.implement.features.modules.combat.killaura.rotation.AngleUtil;

import java.security.SecureRandom;

/**
 * Легитная ротация - более плавная и человекоподобная версия Matrix
 * Имитирует реальное движение мыши с микро-задержками и плавными переходами
 */
public class LegitSmoothMode extends AngleSmoothMode {
    private final SecureRandom random = new SecureRandom();
    private float lastYawOffset = 0;
    private float lastPitchOffset = 0;
    private long lastUpdateTime = 0;
    
    public LegitSmoothMode() {
        super("Legit");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        AttackHandler attackHandler = Main.getInstance().getAttackPerpetrator().getAttackHandler();
        Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        Aura aura = Aura.getInstance();
        
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);
        long currentTime = System.currentTimeMillis();
        
        // Плавное обновление оффсетов с интерполяцией (имитация человеческой руки)
        if (currentTime - lastUpdateTime > 50) {
            lastYawOffset = MathHelper.lerp(0.3f, lastYawOffset, randomLerp(-0.8f, 0.8f));
            lastPitchOffset = MathHelper.lerp(0.3f, lastPitchOffset, randomLerp(-0.4f, 0.4f));
            lastUpdateTime = currentTime;
        }
        
        // Микро-дрожание как у реальной мыши (очень маленькое)
        float microJitterYaw = canAttack ? 0 : (float) (randomLerp(0.3f, 0.6f) * Math.sin(currentTime / 80D)) + lastYawOffset * 0.5f;
        float microJitterPitch = canAttack ? 0 : (float) (randomLerp(0.15f, 0.3f) * Math.cos(currentTime / 100D)) + lastPitchOffset * 0.3f;
        
        // Динамическая скорость - медленнее при большой дистанции, быстрее при атаке
        float baseSpeed;
        if (canAttack) {
            baseSpeed = randomLerp(0.7f, 0.9f);
        } else if (rotationDifference > 30) {
            // Большой угол - начинаем медленно, ускоряемся
            baseSpeed = randomLerp(0.2f, 0.35f);
        } else if (rotationDifference > 10) {
            baseSpeed = randomLerp(0.35f, 0.5f);
        } else {
            // Маленький угол - плавное доведение
            baseSpeed = randomLerp(0.4f, 0.6f);
        }
        
        // Добавляем случайные "паузы" как у человека (иногда замедляемся)
        if (random.nextInt(100) < 8 && !canAttack) {
            baseSpeed *= randomLerp(0.3f, 0.6f);
        }
        
        // Рассчитываем максимальные углы поворота
        float lineYaw = Math.abs(yawDelta / rotationDifference) * 180;
        float linePitch = Math.abs(pitchDelta / rotationDifference) * 180;
        
        // Ограничиваем максимальную скорость поворота (антиснап)
        float maxYawSpeed = canAttack ? 35f : 25f;
        float maxPitchSpeed = canAttack ? 20f : 15f;
        
        lineYaw = Math.min(lineYaw, maxYawSpeed);
        linePitch = Math.min(linePitch, maxPitchSpeed);
        
        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
        
        // Применяем интерполяцию с рандомизацией
        float speedVariation = randomLerp(baseSpeed - 0.05f, baseSpeed + 0.05f);
        
        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(speedVariation, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + microJitterYaw);
        moveAngle.setPitch(MathHelper.lerp(speedVariation, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + microJitterPitch);
        
        return moveAngle;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        // Меньший рандом для более точного наведения
        return new Vec3d(0.08, 0.08, 0.08);
    }
}
