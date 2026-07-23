package dev.ethereal.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationMode;

import java.util.LinkedList;
import java.util.Random;

public class SpookyTimeRotation extends RotationMode {
    private static final Random RANDOM = new Random();
    private static final float GCD_STEP = 0.15f; // Minecraft GCD шаг

    // Полностью рандомные параметры скорости - только yaw ускорен
    private float baseYawSpeed = 28f;
    private float basePitchSpeed = 9f; 
    private float fastYawSpeed = 45f;
    private float fastPitchSpeed = 14f; 
    
    private float yawThreshold = 30f;
    private float pitchThreshold = 18f;

    private float microJitterYaw = 1.5f;
    private float microJitterPitch = 2.0f;

    private float chaosMultiplier = 1.0f;
    private long lastChaosUpdate = 0;
    private int chaosMode = 0;

    private float targetYaw = 0f;
    private float targetPitch = 0f;
    private float currentInterpYaw = 0f;
    private float currentInterpPitch = 0f;
    private float interpSpeed = 0.65f; // Скорость интерполяции - умеренно увеличена

    private float currentFallDistance = 3.0f;
    private long lastAttackTime = 0;
    private long attackCooldown = 350; // Умеренно уменьшен кулдаун

    // История для сглаживания
    private final LinkedList<Float> yawHistory = new LinkedList<>();
    private final LinkedList<Float> pitchHistory = new LinkedList<>();
    private final int historySize = 4;

    private long lastMoveTime = 0;
    private int movePhase = 0;

    private float wanderYaw = 0f;
    private float wanderPitch = 0f;
    private long lastWanderUpdate = 0;

    private float horizontalJitter = 0f;
    private long lastHorizontalJitterUpdate = 0;

    private boolean shouldSnapPitch = false;
    private long lastPitchSnap = 0;
    private float pitchSnapTarget = 0f;

    private int pitchPattern = 0;
    private long lastPitchPatternChange = 0;
    private float pitchPatternProgress = 0f;
    private float pitchDownOffset = 0f;
    
    private int tickCounter = 0;
    private float lastYawDelta = 0f;
    private float lastPitchDelta = 0f;

    public SpookyTimeRotation() {
        super("Spooky Time");
        regenerateParameters();
    }
    
    private void regenerateParameters() {
        // Умеренно увеличенные скорости - только yaw быстрее
        baseYawSpeed = 28f + randomRange(-6f, 6f);
        basePitchSpeed = 9f + randomRange(-2.5f, 2.5f); 
        fastYawSpeed = 45f + randomRange(-8f, 8f);
        fastPitchSpeed = 14f + randomRange(-4f, 4f); 

        yawThreshold = 30f + randomRange(-8f, 8f);
        pitchThreshold = 18f + randomRange(-6f, 6f);
        
        // Умеренный джиттер
        microJitterYaw = 1.5f + randomRange(-0.3f, 0.4f);
        microJitterPitch = 2.0f + randomRange(-0.4f, 0.5f);
        
        // Рандомная скорость интерполяции - умеренно увеличена
        interpSpeed = 0.65f + randomRange(-0.1f, 0.15f);
        
        // Рандомный фолдистанс от 2.7 до 3.4
        // currentFallDistance = 2.7f + randomRange(0f, 0.7f);
        
        // Рандомный кулдаун атаки от 300 до 450 мс - умеренно уменьшен
        attackCooldown = 300 + RANDOM.nextInt(151);
    }
    
    public float getFallDistance() {
        return 3.0f;
    }
    
    public boolean canAttack() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime >= attackCooldown) {
            lastAttackTime = currentTime;
            // Обновляем кулдаун для следующей атаки - умеренно быстрее
            attackCooldown = 320 + RANDOM.nextInt(131);
            return true;
        }
        return false;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        long currentTime = System.currentTimeMillis();
        tickCounter++;
        
        // Обновляем хаос режим для ломания паттерна
        updateChaosMode(currentTime);
        
        // Периодическое обновление параметров - более частое
        if (tickCounter % (150 + RANDOM.nextInt(150)) == 0) {
            regenerateParameters();
        }

        // Изменяем вектор цели - целимся выше (в голову/грудь)
        Rotation adjustedTarget = adjustTargetVector(targetRotation, entity);
        
        Rotation delta = RotationUtil.calculateDelta(currentRotation, adjustedTarget);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float totalDelta = (float) Math.hypot(yawDelta, pitchDelta);

        // Обновляем фазу движения
        updateMovePhase(totalDelta);
        
        // Блуждание прицела
        updateWander(currentTime);
        
        // Улучшенный горизонтальный джиттер
        updateHorizontalJitter(currentTime);
        
        // Резкие повороты питча иногда
        updatePitchSnap(currentTime, pitchDelta);
        
        // Уникальный паттерн питча
        updatePitchPattern(currentTime, pitchDelta);

        // Рассчитываем скорости с полной рандомизацией
        float yawSpeed = calculateYawSpeed(yawDelta, totalDelta);
        float pitchSpeed = calculatePitchSpeed(pitchDelta, totalDelta);
        
        // Применяем хаос модификатор
        yawSpeed *= chaosMultiplier;
        pitchSpeed *= chaosMultiplier;

        // Применяем easing
        float easedYawDelta = applyHumanEasing(yawDelta, yawSpeed);
        float easedPitchDelta = applyHumanEasing(pitchDelta, pitchSpeed);
        
        // Применяем интерполяцию для плавности
        targetYaw = easedYawDelta;
        targetPitch = easedPitchDelta;
        
        // Плавная интерполяция с рандомной скоростью - умеренно увеличена
        float currentInterpSpeed = interpSpeed + randomRange(-0.08f, 0.12f);
        currentInterpYaw = MathHelper.lerp(currentInterpSpeed, currentInterpYaw, targetYaw);
        currentInterpPitch = MathHelper.lerp(currentInterpSpeed, currentInterpPitch, targetPitch);
        
        easedYawDelta = currentInterpYaw;
        easedPitchDelta = currentInterpPitch;
        
        // Резкий поворот питча если нужно (пропускаем интерполяцию)\

        // Добавляем улучшенный микро-джиттер
        float jitterYaw = calculateMicroJitter(microJitterYaw, currentTime, true);
        float jitterPitch = calculateMicroJitter(microJitterPitch, currentTime, false);

        // Иногда пропускаем сглаживание для ломания паттерна, но чаще сглаживаем yaw
        if (RANDOM.nextFloat() > 0.15f) {
            easedYawDelta = smoothWithHistory(easedYawDelta, yawHistory);
        }
        if (RANDOM.nextFloat() > 0.3f) {
            easedPitchDelta = smoothWithHistory(easedPitchDelta, pitchHistory);
        }

        // Добавляем случайные отклонения и горизонтальный джиттер
        easedYawDelta += wanderYaw + horizontalJitter;
        easedPitchDelta += wanderPitch;
        
        // Применяем GCD с рандомом
        if (RANDOM.nextFloat() > 0.2f) {
            easedYawDelta = applyGCD(easedYawDelta);
            easedPitchDelta = applyGCD(easedPitchDelta);
        }
        
        // Иногда добавляем микро-паузу (нулевое движение)
        if (RANDOM.nextFloat() < 0.08f) {
            easedYawDelta *= 0.1f;
            easedPitchDelta *= 0.1f;
        }

        // Формируем итоговый угол
        Rotation result = new Rotation(
                currentRotation.getYaw() + easedYawDelta + jitterYaw,
                MathHelper.clamp(currentRotation.getPitch() + easedPitchDelta + jitterPitch, -90.0f, 90.0f)
        );

        lastYawDelta = easedYawDelta;
        lastPitchDelta = easedPitchDelta;
        lastMoveTime = currentTime;

        return result;
    }
    
    // Изменяем вектор цели чтобы целиться выше
    private Rotation adjustTargetVector(Rotation target, Entity entity) {
        if (entity == null) return target;
        
        // Целимся в голову (выше центра)
        // Уменьшаем pitch (более отрицательный = выше)
        float pitchAdjustment = -11f + randomRange(-4f, 4f);
        
        // Добавляем смещение вниз в начале паттерна
        if (pitchPattern == 0) {
            float downProgress = 1f - pitchPatternProgress;
            pitchAdjustment += pitchDownOffset * downProgress;
        }
        
        return new Rotation(
            target.getYaw(),
            MathHelper.clamp(target.getPitch() + pitchAdjustment, -90.0f, 90.0f)
        );
    }
    
    private void updateHorizontalJitter(long currentTime) {
        // Усиленный горизонтальный джиттер - быстрее
        if (currentTime - lastHorizontalJitterUpdate > 100 + RANDOM.nextInt(200)) {
            lastHorizontalJitterUpdate = currentTime;
            
            // Увеличенная амплитуда
            float targetJitter = randomRange(-0.8f, 0.8f);
            horizontalJitter = MathHelper.lerp(0.2f, horizontalJitter, targetJitter);
        } else {
            // Плавное затухание
            horizontalJitter *= 0.93f;
        }
    }
    
    private void updateChaosMode(long currentTime) {
        // Меняем режим хаоса для ломания паттерна
        if (currentTime - lastChaosUpdate > 1500 + RANDOM.nextInt(2500)) {
            lastChaosUpdate = currentTime;
            chaosMode = RANDOM.nextInt(4);
            
            chaosMultiplier = switch (chaosMode) {
                case 0 -> 1.0f + randomRange(-0.15f, 0.15f); // нормальный
                case 1 -> 0.7f + randomRange(-0.1f, 0.1f); // медленный
                case 2 -> 1.25f + randomRange(-0.15f, 0.2f); // быстрый
                case 3 -> 0.85f + randomRange(-0.3f, 0.4f); // хаос
                default -> 1.0f;
            };
        }
    }
    
    private void updatePitchSnap(long currentTime, float pitchDelta) {
        // Иногда делаем резкий поворот питча для обхода античита
        if (currentTime - lastPitchSnap > 1500 + RANDOM.nextInt(3500)) {
            if (RANDOM.nextFloat() < 0.2f && Math.abs(pitchDelta) > 4f) {
                lastPitchSnap = currentTime;
                shouldSnapPitch = true;
                // Резкий поворот на рандомный процент от дельты
                float snapPercent = 0.3f + randomRange(0f, 0.5f);
                pitchSnapTarget = pitchDelta * snapPercent;
            }
        }
    }
    
    private void updatePitchPattern(long currentTime, float pitchDelta) {
        // Меняем паттерн питча для уникальности
        if (currentTime - lastPitchPatternChange > 800 + RANDOM.nextInt(1500)) {
            lastPitchPatternChange = currentTime;
            pitchPatternProgress = 0f;
            
            // Выбираем рандомный паттерн
            float rand = RANDOM.nextFloat();
            if (rand < 0.3f) {
                pitchPattern = 0; // Медленно вниз
                pitchDownOffset = randomRange(3f, 8f);
            } else if (rand < 0.5f) {
                pitchPattern = 1; // Резко вверх
            } else {
                pitchPattern = 2; // Нормально
            }
        }
        
        // Обновляем прогресс паттерна
        pitchPatternProgress += 0.05f + randomRange(0f, 0.03f);
        if (pitchPatternProgress > 1f) {
            pitchPatternProgress = 1f;
        }
    }
    
    private float getPitchPatternMultiplier() {
        return switch (pitchPattern) {
            case 0 -> { // Медленно вниз - сначала очень медленно, потом ускоряется
                float t = pitchPatternProgress;
                yield 0.3f + (t * 0.5f);
            }
            case 1 -> { // Резко вверх - сначала быстро, потом замедляется
                float t = 1f - pitchPatternProgress;
                yield 1.8f - (t * 0.6f);
            }
            case 2 -> 1.0f; // Нормально
            default -> 1.0f;
        };
    }
    
    private float applyGCD(float delta) {
        // Применяем GCD с рандомом для легитимности
        if (Math.abs(delta) < 0.01f) return 0f;
        
        float sign = Math.signum(delta);
        float absDelta = Math.abs(delta);
        
        // Рандомный GCD шаг для ломания паттерна
        float gcdStep = GCD_STEP * (0.8f + randomRange(0f, 0.4f));
        
        // Округляем до ближайшего GCD шага
        float steps = Math.round(absDelta / gcdStep);
        float gcdDelta = steps * gcdStep;
        
        // Добавляем больше рандома для обхода детекта
        gcdDelta += randomRange(-gcdStep * 0.2f, gcdStep * 0.2f);
        
        // Иногда игнорируем GCD полностью
        if (RANDOM.nextFloat() < 0.15f) {
            return delta;
        }
        
        return sign * gcdDelta;
    }
    
    private void updateWander(long currentTime) {
        // Усиленное блуждание - быстрее
        if (currentTime - lastWanderUpdate > 200 + RANDOM.nextInt(300)) {
            lastWanderUpdate = currentTime;
            
            // Увеличенные значения
            float targetWanderYaw = randomRange(-0.5f, 0.5f);
            float targetWanderPitch = randomRange(-0.8f, 0.8f);
            
            // Плавный переход
            wanderYaw = MathHelper.lerp(0.15f, wanderYaw, targetWanderYaw);
            wanderPitch = MathHelper.lerp(0.2f, wanderPitch, targetWanderPitch);
        }
    }

    private float calculateYawSpeed(float yawDelta, float totalDelta) {
        float absYaw = Math.abs(yawDelta);
        // Меньше рандома для плавности
        float tickRandomness = 1f + randomRange(-0.05f, 0.05f);

        if (absYaw > yawThreshold) {
            // Увеличенная скорость на больших углах
            float speedMult = MathHelper.lerp(Math.min(absYaw / 90.0f, 1.0f), 0.7f, 1.1f);
            float baseSpeed = fastYawSpeed * speedMult * getPhaseMultiplier() * tickRandomness;
            return Math.max(18, baseSpeed + randomRange(-2f, 2f));
        } else {
            // Плавнее на малых углах
            float precision = 1.0f - (absYaw / yawThreshold);
            float speedMult = MathHelper.lerp(precision, 0.9f, 0.4f);
            float baseSpeed = baseYawSpeed * speedMult * getPhaseMultiplier() * tickRandomness;
            return baseSpeed + randomRange(-1.5f, 1.5f);
        }
    }

    private float calculatePitchSpeed(float pitchDelta, float totalDelta) {
        float absPitch = Math.abs(pitchDelta);
        // Рандомная скорость каждый тик
        float tickRandomness = 1f + randomRange(-0.15f, 0.15f);

        // Применяем уникальный паттерн питча
        float patternMultiplier = getPitchPatternMultiplier();

        if (absPitch > pitchThreshold) {
            float speedMult = MathHelper.lerp(Math.min(absPitch / 45.0f, 1.0f), 0.35f, 0.85f);
            float baseSpeed = fastPitchSpeed * speedMult * getPhaseMultiplier() * tickRandomness * patternMultiplier;
            return baseSpeed + randomRange(-2f, 2f);
        } else {
            float precision = 1.0f - (absPitch / pitchThreshold);
            float speedMult = MathHelper.lerp(precision, 0.65f, 0.2f);
            float baseSpeed = basePitchSpeed * speedMult * getPhaseMultiplier() * tickRandomness * patternMultiplier;
            return baseSpeed + randomRange(-1.2f, 1.2f);
        }
    }

    private float applyHumanEasing(float delta, float maxSpeed) {
        if (Math.abs(delta) < 0.01f) return 0;

        float sign = Math.signum(delta);
        float absDelta = Math.abs(delta);

        // Меньше рандома для плавности
        float speedVariation = maxSpeed * (0.8f + randomRange(0, 0.35f));
        float clampedDelta = Math.min(absDelta, speedVariation);

        float t = clampedDelta / Math.max(absDelta, 1.0f);
        
        // Более плавный easing
        float easePower = 1.8f + randomRange(0, 0.6f);
        float eased = 1.0f - (float)Math.pow(1.0f - t, easePower);

        // Меньше рандома в финальном факторе
        float randomFactor = 1.0f + randomRange(-0.12f, 0.12f);
        
        // Реже используем линейное движение
        if (RANDOM.nextFloat() < 0.12f) {
            eased = t;
        }

        return sign * clampedDelta * eased * randomFactor;
    }

    private float calculateMicroJitter(float amplitude, long time, boolean isYaw) {
        // Полностью рандомные частоты каждый раз
        double baseFreq1 = isYaw ? (120.0 + RANDOM.nextDouble() * 100.0) : (160.0 + RANDOM.nextDouble() * 100.0);
        double baseFreq2 = isYaw ? (60.0 + RANDOM.nextDouble() * 50.0) : (90.0 + RANDOM.nextDouble() * 50.0);
        double baseFreq3 = isYaw ? (220.0 + RANDOM.nextDouble() * 100.0) : (280.0 + RANDOM.nextDouble() * 100.0);
        
        // Рандомные фазовые сдвиги
        double phaseShift1 = RANDOM.nextDouble() * Math.PI * 2;
        double phaseShift2 = RANDOM.nextDouble() * Math.PI * 2;
        double phaseShift3 = RANDOM.nextDouble() * Math.PI * 2;
        
        double phase1 = (time / baseFreq1) + phaseShift1;
        double phase2 = (time / baseFreq2) + phaseShift2;
        double phase3 = (time / baseFreq3) + phaseShift3;

        // Рандомные веса волн - увеличены
        float weight1 = 0.5f + randomRange(0f, 0.5f);
        float weight2 = 0.4f + randomRange(0f, 0.4f);
        float weight3 = 0.3f + randomRange(0f, 0.4f);

        float wave1 = (float) Math.sin(phase1) * weight1;
        float wave2 = (float) Math.sin(phase2) * weight2;
        float wave3 = (float) Math.cos(phase3) * weight3;

        float jitter = (wave1 + wave2 + wave3) * amplitude;
        
        // Больше случайного шума
        jitter += randomRange(-amplitude * 0.7f, amplitude * 0.7f);
        
        // Чаще и сильнее рывки
        if (RANDOM.nextFloat() < 0.04f) {
            jitter += randomRange(-amplitude * 2.0f, amplitude * 2.0f);
        }
        
        // Иногда инвертируем джиттер
        if (RANDOM.nextFloat() < 0.15f) {
            jitter *= -1.0f;
        }

        return jitter;
    }

    private float smoothWithHistory(float value, LinkedList<Float> history) {
        history.addLast(value);
        while (history.size() > historySize) {
            history.removeFirst();
        }

        float sum = 0;
        float weightSum = 0;
        int i = 0;
        for (float v : history) {
            float weight = (float) Math.pow(1.6f, i);
            sum += v * weight;
            weightSum += weight;
            i++;
        }

        return sum / weightSum;
    }

    private void updateMovePhase(float totalDelta) {
        if (totalDelta > 60.0f) {
            movePhase = 0; // Быстрое наведение
        } else if (totalDelta < 8.0f) {
            movePhase = 2; // Удержание на цели
        } else {
            movePhase = 1; // Точное наведение
        }
    }

    private float getPhaseMultiplier() {
        return switch (movePhase) {
            case 0 -> 1.15f + randomRange(-0.1f, 0.1f);
            case 1 -> 0.8f + randomRange(-0.1f, 0.1f);
            case 2 -> 0.4f + randomRange(-0.08f, 0.08f);
            default -> 1.0f;
        };
    }
    

    private float randomRange(float min, float max) {
        return MathHelper.lerp(RANDOM.nextFloat(), min, max);
    }
}
