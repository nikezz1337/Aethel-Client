package dev.ethereal.api.system.client;

import lombok.Getter;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.utils.task.TaskPriority;
import dev.ethereal.api.utils.task.TaskProcessor;

@Getter
public class TimerManager {
    @Getter private static final TimerManager instance = new TimerManager();

    private final TaskProcessor<Float> taskProcessor = new TaskProcessor<>();

    public float getTimerSpeed() {
        return taskProcessor.fetchActiveTaskValue() != null ? taskProcessor.fetchActiveTaskValue() : 1.0f;
    }

    public void addTimer(float timer, TaskPriority taskPriority, Module provider, int ticks) {
        taskProcessor.addTask(new TaskProcessor.Task<>(ticks, taskPriority.getPriority(), provider, timer));
    }
}
