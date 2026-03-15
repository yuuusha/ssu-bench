package com.diev.handler;

import com.diev.entity.Task;
import com.diev.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public Task createTask(
            @RequestParam UUID customerId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Integer reward
    ) {

        return taskService.createTask(customerId, title, description, reward);
    }

    @GetMapping("/{id}")
    public Task getTask(@PathVariable UUID id) {

        return taskService.getTask(id);
    }

    @GetMapping
    public List<Task> getTasks(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
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