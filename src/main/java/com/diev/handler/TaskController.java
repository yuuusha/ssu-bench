package com.diev.handler;

import com.diev.entity.Role;
import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.entity.User;
import com.diev.service.TaskService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@Validated
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public Task createTask(
            @RequestParam UUID customerId,
            @RequestParam @NotBlank String title,
            @RequestParam @NotBlank String description,
            @RequestParam @NotNull @Positive Integer reward
    ) {

        return taskService.createTask(customerId, title, description, reward);
    }

    @PutMapping("/{id}")
    public Task updateTask(
            @PathVariable UUID id,
            @RequestParam @NotBlank String title,
            @RequestParam @NotBlank String description,
            @RequestParam @NotNull @Positive Integer reward,
            @RequestParam @NotNull TaskStatus status
    ) {
        return taskService.updateTask(id, title, description, reward, status.name());
    }

    @PostMapping("/{id}/publish")
    public Task publishTask(@PathVariable UUID id,
                            @RequestParam UUID customerId) {
        return taskService.updateTaskStatus(id, customerId, TaskStatus.PUBLISHED.name());
    }

    @GetMapping("/{id}")
    public Task getTask(@PathVariable UUID id) {

        return taskService.getTask(id);
    }

    @GetMapping
    public List<Task> getTasks(
            @RequestParam(defaultValue = "20") @Positive int limit,
            @RequestParam(defaultValue = "0") @PositiveOrZero int offset
    ) {

        return taskService.getTasks(limit, offset);
    }

    @PostMapping("/{id}/cancel")
    public void cancelTask(
            @PathVariable UUID id,
            @RequestParam UUID customerId
    ) {

        taskService.cancelTask(id, customerId);
    }
}