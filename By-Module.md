# Anti-Detection — что работает на SpookyTime / Releon

## SpookyTime — Rotation bypass

### Что проверяет AC
1. **Constant rotation speed** — если yaw/pitch двигается с фиксированным step каждый тик → flag
2. **Spectral analysis (FFT)** — sin/cos с фиксированными периодами → легко детектится
3. **Instant snap** — поворот >60° за один тик без плавности → flag
4. **Predictable attack jitter** — одинаковый jitter при каждом ударе → flag
5. **No GCD snap** — yaw/pitch не кратен сетке чувствительности → flag

### Что сработало

#### 1. Рандомизация step (самое важное)
```java
// НЕЛЬЗЯ:
float maxStep = 30f; // константа → flag

// РАБОТАЕТ:
float maxStep = 22f + rand.nextFloat() * 12f; // 22-34, каждый тик разный
```
AC считает variance step между тиками. Если variance = 0 → бот.

#### 2. Smooth factor (плавность)
```java
// НЕЛЬЗЯ: полная дельта сразу
nextYaw = curYaw + deltaYaw;

// РАБОТАЕТ: чуть замедляем
float smooth = 0.82f + rand.nextFloat() * 0.13f; // 0.82-0.95
nextYaw = curYaw + deltaYaw * smooth;
```
Не 0.3-0.5 как раньше — это слишком медленно и тоже флагается (человек быстрее наводится). 0.82-0.95 — близко к instant но с микро-плавностью.

#### 3. Рандомные периоды sway (ломает FFT)
```java
// НЕЛЬЗЯ: фиксированные периоды
float sway = sin(t / 80.0) * 1.5 + cos(t / 50.0) * 3.0; // FFT находит пик

// РАБОТАЕТ: рандомные периоды каждый тик
float p1 = 70f + rand.nextFloat() * 30f;  // 70-100
float p2 = 40f + rand.nextFloat() * 20f;  // 40-60
float p3 = 110f + rand.nextFloat() * 30f; // 110-140
float p4 = 50f + rand.nextFloat() * 20f;  // 50-70
float sway = sin(t / p1) * (1.0 + rand.nextFloat() * 0.8) + cos(t / p2) * (2.0 + rand.nextFloat() * 2.0);
```
Если period меняется каждый тик — FFT не может зафиксировать паттерн.

#### 4. Randomized attack jitter
```java
// НЕЛЬЗЯ: фиксированный
float jitter = sin(t / 40.0) * 1.5 * cos(t / 50.0) * 2.0;

// РАБОТАЕТ: всё рандомное
float jitter = sin(t / (35f + rand.nextFloat() * 15f))
             * (1.0 + rand.nextFloat() * 1.0)
             * cos(t / (45f + rand.nextFloat() * 15f))
             * (1.5 + rand.nextFloat() * 1.0);
```

#### 5. Step limit 30° — работает, но randomize обязательно
```java
float maxStep = 22f + rand.nextFloat() * 12f; // 22-34°
```
Порог ~30° — человеческий максимум за тик. Но если ровно 30 каждый раз — flag.

#### 6. GCD snap — автоматически в URotations
`URotations.update()` уже вызывает `GCDUtil.getFixRotate()` — отдельно снапить не надо.

### Rotation state machine (SpookyTime)
```
Цель в обнаружении (!inAttackRange)
  → smooth track + sway

Цель в атаке, до удара (inAttackRange, !attack, !hasSavedRotation)
  → smooth track + sway (это "за 2 тика до удара")

Удар (attack=true)
  → track + randomized jitter + sway
  → SAVE yaw/pitch
  → attackTicks=3, holdTicks=9

После удара (postAttack, attackTicks>0)
  → track + randomized jitter + sway
  → UPDATE saved

Hold (hasSavedRotation && holdTicks>0)
  → HOLD saved + sway (не двигается к цели)

Hold expired (holdTicks=0)
  → hasSavedRotation=false
  → smooth track снова
```

### Aim point
- Рандом от подбородка (75% eye height) до макушки (100%)
- Перерандом при каждом ударе
- НЕ использовать UBoxPoints — он даёт одну точку, AC может детектить

---

## Общие паттерны (работают на все AC)

### Что всегда флагается
- Instant snap >30° без smooth
- Константный CPS (< 0.5 variance на 100 кликов)
- Reach > 6.0
- AutoBlock + атака одновременно
- Attacking без swing animation
- Swing с интервалом ровно 50ms (auto-clicker)
- Атака под углом >90° от look vector

### Что помогает
- GCD snap (обязательно)
- Randomized swing interval (gaussian ~8-10 CPS)
- Raycast от eye до target hitbox (не бить через стены)
- CPS distribution с std ~1.5
- Randomized double clicks иногда
