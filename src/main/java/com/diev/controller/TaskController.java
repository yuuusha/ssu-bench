package com.diev.controller;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.security.CurrentUserId;
import com.diev.service.TaskService;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Validated
public class TaskController {

    private final TaskService taskService;

    @Resource(name = "controllerExecutor")
    private Executor controllerExecutor;

    @TimeLimiter(name = "http-controller")
    @PostMapping
    public CompletableFuture<Task> createTask(
            @CurrentUserId UUID customerId,
            @RequestParam @NotBlank String title,
            @RequestParam @NotBlank String description,
            @RequestParam @NotNull @Positive Integer reward
    ) {
        return CompletableFuture.supplyAsync(
                () -> taskService.createTask(customerId, title, description, reward),
                controllerExecutor
        );
    }

    @TimeLimiter(name = "http-controller")
    @PutMapping("/{id}")
    public CompletableFuture<Task> updateTask(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId,
            @RequestParam @NotBlank String title,
            @RequestParam @NotBlank String description,
            @RequestParam @NotNull @Positive Integer reward,
            @RequestParam @NotNull TaskStatus status
    ) {
        return CompletableFuture.supplyAsync(
                () -> taskService.updateTask(id, title, description, reward, status.name(), currentUserId),
                controllerExecutor
        );
    }

    @TimeLimiter(name = "http-controller")
    @PostMapping("/{id}/publish")
    public CompletableFuture<Task> publishTask(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId
    ) {
        return CompletableFuture.supplyAsync(
                () -> taskService.publishTask(id, currentUserId),
                controllerExecutor
        );
    }

    @TimeLimiter(name = "http-controller")
    @GetMapping("/{id}")
    public CompletableFuture<Task> getTask(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> taskService.getTask(id), controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @GetMapping
    public CompletableFuture<List<Task>> getTasks(
            @RequestParam(defaultValue = "20") @Positive int limit,
            @RequestParam(defaultValue = "0") @PositiveOrZero int offset
    ) {
        return CompletableFuture.supplyAsync(() -> taskService.getTasks(limit, offset), controllerExecutor);
    }

    @TimeLimiter(name = "http-controller")
    @PostMapping("/{id}/cancel")
    public CompletableFuture<Void> cancelTask(
            @PathVariable UUID id,
            @CurrentUserId UUID customerId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            taskService.cancelTask(id, customerId);
            return null;
        }, controllerExecutor);
    }
}