package dev.ethereal.api.utils.player.script;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

@SuppressWarnings("UnusedReturnValue")
public class Script {
    private final List<ScriptStep> scriptSteps = new CopyOnWriteArrayList<>();
    private final List<ScriptTickStep> scriptTickSteps = new CopyOnWriteArrayList<>();
    private long lastStepTime;
    private int currentStepIndex;
    private int currentTickStepIndex;
    private boolean interrupt;
    private LoopStrategy loopStrategy = new FiniteLoopStrategy(1);

    public Script() {
        cleanup();
    }

    public Script addStep(int delay, ScriptAction action) {
        return addStep(delay, action, () -> true, 0);
    }

    public Script addStep(int delay, ScriptAction action, BooleanSupplier condition) {
        return addStep(delay, action, condition, 0);
    }

    public Script addStep(int delay, ScriptAction action, int priority) {
        return addStep(delay, action, () -> true, priority);
    }

    public Script addStep(int delay, ScriptAction action, BooleanSupplier condition, int priority) {
        scriptSteps.add(new ScriptStep(delay, action, condition, priority));
        scriptSteps.sort(null);
        return this;
    }

    public Script addTickStep(int ticks, ScriptAction action) {
        return addTickStep(ticks, action, () -> true, 0);
    }

    public Script addTickStep(int ticks, ScriptAction action, BooleanSupplier condition) {
        return addTickStep(ticks, action, condition, 0);
    }

    public Script addTickStep(int ticks, ScriptAction action, int priority) {
        return addTickStep(ticks, action, () -> true, priority);
    }

    public Script addTickStep(int ticks, ScriptAction action, BooleanSupplier condition, int priority) {
        scriptTickSteps.add(new ScriptTickStep(ticks, action, condition, priority));
        scriptTickSteps.sort(null);
        return this;
    }

    public void resetTime() {
        lastStepTime = System.currentTimeMillis();
    }

    public void resetStepIndex() {
        currentStepIndex = 0;
        currentTickStepIndex = 0;
    }

    public Script cleanupIfFinished() {
        if (isFinished()) cleanup();
        return this;
    }

    public Script cleanup() {
        scriptSteps.clear();
        scriptTickSteps.clear();
        resetTime();
        resetStepIndex();
        interrupt = false;
        loopStrategy = new FiniteLoopStrategy(1);
        return this;
    }

    public void update() {
        if ((scriptSteps.isEmpty() && scriptTickSteps.isEmpty()) || interrupt) return;

        if (currentStepIndex < scriptSteps.size()) {
            ScriptStep currentStep = scriptSteps.get(currentStepIndex);
            if (currentStep.condition().getAsBoolean()
                    && System.currentTimeMillis() - lastStepTime >= currentStep.delay()) {
                currentStep.action().perform();
                ++currentStepIndex;
                resetTime();
                if (loopStrategy.shouldLoop(currentStepIndex, scriptSteps.size())) {
                    resetStepIndex();
                    loopStrategy.onLoop();
                }
            }
        }

        if (currentTickStepIndex < scriptTickSteps.size()) {
            ScriptTickStep currentTickStep = scriptTickSteps.get(currentTickStepIndex);
            if (currentTickStep.condition().getAsBoolean() && currentTickStep.ticks() <= 0) {
                currentTickStep.action().perform();
                ++currentTickStepIndex;
                resetTime();
                if (loopStrategy.shouldLoop(currentTickStepIndex, scriptTickSteps.size())) {
                    resetStepIndex();
                    loopStrategy.onLoop();
                }
            }
            currentTickStep.decrementTicks();
        }

        currentStepIndex = Math.min(currentStepIndex, scriptSteps.size());
        currentTickStepIndex = Math.min(currentTickStepIndex, scriptTickSteps.size());
    }

    public Script setLoopStrategy(LoopStrategy loopStrategy) {
        this.loopStrategy = loopStrategy;
        return this;
    }

    public boolean isFinished() {
        return currentStepIndex >= scriptSteps.size()
                && currentTickStepIndex >= scriptTickSteps.size()
                && !interrupt
                && loopStrategy.isFinished();
    }

    public void setInterrupt(boolean interrupt) {
        this.interrupt = interrupt;
    }

    public static class FiniteLoopStrategy implements LoopStrategy {
        private final int loopCount;
        private int currentLoop;

        public FiniteLoopStrategy(int loopCount) {
            this.loopCount = loopCount - 1;
        }

        @Override
        public boolean shouldLoop(int currentStepIndex, int totalSteps) {
            return currentStepIndex >= totalSteps && currentLoop < loopCount;
        }

        @Override
        public void onLoop() {
            ++currentLoop;
        }

        @Override
        public boolean isFinished() {
            return currentLoop >= loopCount;
        }
    }

    public interface LoopStrategy {
        boolean shouldLoop(int currentStepIndex, int totalSteps);

        void onLoop();

        boolean isFinished();
    }

    public record ScriptStep(int delay, ScriptAction action, BooleanSupplier condition, int priority)
            implements Comparable<ScriptStep> {
        @Override
        public int compareTo(ScriptStep otherStep) {
            return Integer.compare(otherStep.priority(), this.priority());
        }
    }

    public static final class ScriptTickStep implements Comparable<ScriptTickStep> {
        private int ticks;
        private final ScriptAction action;
        private final BooleanSupplier condition;
        private final int priority;

        public ScriptTickStep(int ticks, ScriptAction action, BooleanSupplier condition, int priority) {
            this.ticks = ticks;
            this.action = action;
            this.condition = condition;
            this.priority = priority;
        }

        public int ticks() {
            return ticks;
        }

        public ScriptAction action() {
            return action;
        }

        public BooleanSupplier condition() {
            return condition;
        }

        public int priority() {
            return priority;
        }

        @Override
        public int compareTo(ScriptTickStep otherStep) {
            return Integer.compare(otherStep.priority(), this.priority());
        }

        public void decrementTicks() {
            ticks--;
        }
    }

    public static class InfiniteLoopStrategy implements LoopStrategy {
        @Override
        public boolean shouldLoop(int currentStepIndex, int totalSteps) {
            return currentStepIndex >= totalSteps;
        }

        @Override
        public void onLoop() {
        }

        @Override
        public boolean isFinished() {
            return false;
        }
    }
}
