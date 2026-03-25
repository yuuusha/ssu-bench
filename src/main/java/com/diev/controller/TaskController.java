package com.diev.controller;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.security.CurrentUserId;
import com.diev.service.TaskService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Validated
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public Task createTask(
            @CurrentUserId UUID customerId,
            @RequestParam @NotBlank String title,
            @RequestParam @NotBlank String description,
            @RequestParam @NotNull @Positive Integer reward
    ) {
        return taskService.createTask(customerId, title, description, reward);
    }

    @PutMapping("/{id}")
    public Task updateTask(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId,
            @RequestParam @NotBlank String title,
            @RequestParam @NotBlank String description,
            @RequestParam @NotNull @Positive Integer reward,
            @RequestParam @NotNull TaskStatus status
    ) {
        return taskService.updateTask(id, title, description, reward, status.name(), currentUserId);
    }

    @PostMapping("/{id}/publish")
    public Task publishTask(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId
    ) {
        return taskService.publishTask(id, currentUserId);
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
            @CurrentUserId UUID customerId
    ) {

        taskService.cancelTask(id, customerId);
    }
}