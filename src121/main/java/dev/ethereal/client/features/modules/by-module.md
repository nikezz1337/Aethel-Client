# Anti-detection паттерны

Контекст: это для написания модулей, которые НЕ палятся базовыми AC (NCP, Vulcan, Verus, Watchdog, Grim) и для понимания, что палится в собственной защите.

**Disclaimer:** ничего тут не "невидимка" — современные AC статистически детектят даже идеально написанный чит. Цель — снизить шанс мгновенного флага.

## Ротации (главный источник flagов)

### GCD (Gross Coordinate Distance)

Майнкрафт сэмплирует чувствительность мыши с шагом:
```
gcd = ((sensitivity * 0.6) + 0.2) ^ 3 * 1.2
```
Финальный yaw/pitch — всегда кратен `gcd * 0.15`.

Любая ротация, которая НЕ снапнута на GCD-сетку — мгновенный флаг.

```java
public static float gcd(float value, float sensitivity) {
    float f = (sensitivity * 0.6f) + 0.2f;
    float gcd = f * f * f * 1.2f * 0.15f;
    return Math.round(value / gcd) * gcd;
}
```

Применяй: `yaw = gcd(yaw, mc.gameSettings.mouseSensitivity)`.

### Smooth rotations

```java
public static float smooth(float current, float target, float speed) {
    float delta = MathHelper.wrapAngleTo180_float(target - current);
    return current + delta / speed; // speed = 8..16
}
```

Не делай speed=1 (instant snap) — палится. Минимум speed=4 + jitter.

### Silent rotations + body

В PRE MotionEvent:
```java
mc.thePlayer.rotationYaw = targetYaw;
mc.thePlayer.rotationPitch = targetPitch;
// renderYawOffset не трогаем — иначе тело развернётся
```
В POST вернуть `rotationYaw` обратно. Но `rotationYawHead` оставить таргетным один тик чтобы свинг шёл от правильной точки.

## KillAura — что палится

- Атака без свинга → flag instantly.
- Свинг с интервалом ровно 50ms каждый тик → flag (auto-clicker pattern).
- Атака под углом > 90° от look vector → reach/aim flag.
- Hit через стену когда `getMouseOver()` показывает блок → flag.

Решения:
- CPS distribution: gaussian вокруг 8-10 со стандартным отклонением 1.5.
- Always swing на хит, swing миксин.
- Raycast от eye в target hitbox — если результат не taget'a → не бить.

## Ротационные профили под конкретные AC/сервера (рабочие, 2026)

Выреверсено при порте 1.21.4-базы (FunTime/Releon 16.04, SpookyTime, ReallyWorld,
CakeWorld, HolyWorld) в 1.16.5. Это **per-tick** функции: на входе `current` (текущий
серверный yaw/pitch) и `target` (наведение на ближайшую видимую точку хитбокса), на
выходе следующий yaw/pitch. `attack` = «можно бить в этом тике» (наш `canAttack`).

Общие правила для ВСЕХ профилей:
- `delta = wrapDegrees(target - current)` по каждой оси, `total = hypot(yawΔ, pitchΔ)`.
- Per-axis straight-line cap: `cap = abs(axisΔ/total) * MAX` → клампим Δ в `[-cap, cap]`.
  Это держит траекторию «по прямой» к таргету (AC палят кривые дуги).
- **Финал всегда GCD-снапить** (см. секцию GCD). Снап делать ОДИН раз на applied-delta.
- Джиттер (sin/cos) добавлять **только когда `!attack`** (в окне атаки — чистая ротация).
- `lerp(t, a, b)` тут = `a + t*(b-a)`; «speed» = коэффициент lerp (0..1, больше = резче).
- Аим-поинт: ближайшая ВИДИМАЯ точка хитбокса (raycast), не центр — иначе reach/aim flag.

### FunTime / Releon 16.04 (FTAngle)
- **attack:** `cap = abs(axisΔ/total)*130°`; `next = lerp(0.85, current, current + clamp(Δ,±cap))`.
- **idle (таргет есть, бить нельзя):** shake `yaw += rand(18..28)*sin(ms/60)`,
  `pitch += rand(6..16)*cos(ms/60)`; лимит доворота = `0°` пока не прошло 535ms после хита,
  потом `45°`. Каждый **86-й** хит в окне 250ms → `pitch = -90°` (флик вниз) + swing на 240ms.
- Суть: жёсткий 130° follow + сильное сглаживание 0.85, тряска в простое, редкий флик-«тэлл».

### SpookyTime / Releon (SPAngle)
- `yawLimit = min(|yawΔ|, 74° + rand(0..1.03))`, `pitchLimit = min(|pitchΔ|, 32.33°)`.
- Per axis: `reached = |Δ| >= limit`; `maxStep = reached ? rand(65..100) : rand(7.7..12.1)`;
  `scale = min(total, maxStep)/total`; если `!reached` → `scale = ease(scale)`,
  `ease(t)=t*(0.5+0.5t)`; `next = current + Δ*scale` (pitch клампить -89..90).
- **Sway:** `yaw += sin((ms%12000)/1200 * 3 * 2π) * 1.15 * gaussian()`.
- На потере таргета: 50ms «hold» (заморозка) → 500ms eased-lerp назад к натуральному взгляду.

### ReallyWorld (RWAngle)
- `cap = abs(axisΔ/total)*180°`; speed≈1 → `next = lerp(rand(1..1.2), current, current+clampΔ)`.
- **idle jitter:** `yaw += -6*cos(ms/90)`, `pitch += 6*sin(ms/90)`.
- Каждый **50-й** хит в окне 200ms → `pitch = lerp(0.55, current, current-90)` (резкий флик вниз).

### CakeWorld (LGAngle)
- `cap = 180°`; `baseSpeed = attack ? 0.93 : 0.56`; close-range (<0.66 блока) → доп. замедление.
- **idle jitter агрессивный:** `yaw += rand(20..26)*sin(ms/25)`, `pitch += rand(8..23)*sin(ms/27)`.

### HolyWorld (HWAngle)
- `cap = 180°`; `speed = attack ? rand(0.86..0.96) : rand(0.1..0.4)`;
  `next = lerp(rand(speed..speed+0.2), current, current+clampΔ)`; джиттера нет.
- Суть: быстрый плавный доворот в атаке, очень медленный дрейф в простое.

### Matrix (MatrixAngle)
- `yawCap = attack ? 360° : 100°`, `pitchCap = 180°`; `speed = attack ? 1 : rand(0..0.5)`.
- **idle jitter с рандомной частотой:** `yaw += rand(0..6)*sin(ms/rand(15..145))`,
  `pitch += rand(1..3)*sin(ms/rand(15..145))` — рандомная частота ломает спектральный анализ.

### HvH-сервера (HAngle)
- `cap = 360°` обе оси, `speed≈1` (почти инстант — на HvH ротационных проверок нет/слабые).
- idle jitter `5*sin(ms/45)` по обеим осям. Для HvH главное — скорость/реоч, а не «легит».

### Sloth (SlothAngle) — лёгкий/легит
- Фикс. мелкий шаг: `yawStep = clamp(Δ, ±rand(7..12)°)`, `pitchStep = clamp(Δ, ±rand(4..7)°)`,
  + микро-джиттер `±0.3°`, GCD-снап. Медленно, но почти не отличимо от руки.

**Замечания по портингу/мэппингам:**
- 1.16.5 (Mojang-имена): yaw/pitch это поля `mc.player.rotationYaw/rotationPitch`,
  `MathHelper.lerp(t,a,b)`, `MathHelper.wrapDegrees`, `Vector3d`, `Hand.MAIN_HAND`,
  `swingArm(Hand)`, `MathHelper.TAU` нет → `(float)(Math.PI*2)`.
- Применять ротацию надо тихо (silent) + FreeLook на камеру, иначе видно рывки (см. ниже).
- Счётчик хитов и таймер последнего удара нужны для фликов %86 (FunTime) и %50 (ReallyWorld) —
  гейтить по rising-edge `canAttack`.

## Velocity

`S12PacketEntityVelocity` cancel:
- Просто cancel пакета → не помогает, motion применяется при следующем onLivingUpdate из других источников.
- Правильно: запоминаем cancel и в `onLivingUpdate` HEAD умножаем `motionX/Z` на `0` (или коэффициент).

## Movement

### Bhop/Speed

Базовый Vanilla bhop:
```java
@EventTarget
public void onMotion(MotionEvent e) {
    if (e.getStage() != MotionEvent.Stage.PRE) return;
    if (!mc.thePlayer.onGround) return;
    if (!isMoving()) return;
    mc.thePlayer.jump();
}
```
Палится Hypixel Watchdog (BHopA). Лучше: jump каждые 2-3 тика + Speed boost на ~0.0009 от ваниловой.

### Sprint OnlyForward

Sprint только когда `moveForward > 0` и `moveStrafing == 0`. Иначе омни-спринт палится.

### NoSlowDown (для еды/блокировки меча)

Отправлять `C0BPacketEntityAction(STOP_SNEAKING)` каждый тик при использовании предмета. Это палится `0.3.x` хотя бы.

## Scaffold

- Не ставь блок прямо вниз каждый тик (Tower) — лимитируй на 5-7 блоков/сек.
- Ротация на постановку — gcd-снапнутая.
- Если `useItem` тик — отправь swing и анимацию (`C0APacketAnimation`).
- "Rewinside" пакетный паттерн (cancel+resend C08) детектят почти везде.

## Auto-clicker

- CPS не константный.
- Не строго на тике (jitter в ms).
- Не одинаковый pattern каждый раз — добавляй случайные "double clicks" иногда.

## Что палится 100% (избегай)

- Setting yaw на сервер БЕЗ smooth rotation (instant snap > 30°).
- Onground spoof при `motionY < -0.5`.
- Reach > 6.0 на любом AC.
- AutoBlock одновременно с атакой (старый pattern).
- Step > 1.5 без `C03PacketPlayer` с промежуточными позициями.

## В сторону защиты собственного клиента

Если ты пишешь не модуль, а защиту — все эти паттерны нужны как сигнатуры:
- Логируй мгновенные yaw delta > 60° между двумя моушенами.
- Считай variance CPS на окне 100 кликов — если < 0.5 → bot.
- Проверяй gcd-snap входящих ротаций — если каждая ротация на чужой sensitivity → suspicious.
- Анализируй timing C0A/C02 — должны быть в одном тике, не в разных.
