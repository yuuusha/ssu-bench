package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.entity.User;
import com.diev.repo.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task createTask(
            UUID customerId,
            String title,
            String description,
            Integer reward
    ) {

        UUID id = UUID.randomUUID();

        taskRepository.create(
                id,
                title,
                description,
                reward,
                TaskStatus.CREATED.name(),
                customerId
        );

        return taskRepository.findById(id)
                .orElseThrow();
    }

    public Task updateTask(UUID id, String title, String description, Integer reward, String status) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (existing.getStatus() != TaskStatus.CREATED) {
            throw new RuntimeException("Task cannot be updated");
        }

        taskRepository.update(
                id,
                title,
                description,
                reward,
                status
        );

        return taskRepository.findById(id)
                .orElseThrow();
    }

    public Task updateTaskStatus(UUID id, String status) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        taskRepository.update(
                id,
                existing.getTitle(),
                existing.getDescription(),
                existing.getReward(),
                status
        );

        return taskRepository.findById(id)
                .orElseThrow();
    }

    public Task getTask(UUID id) {

        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    public List<Task> getTasks(int limit, int offset) {

        return taskRepository.findAll(limit, offset);
    }

    public void cancelTask(UUID taskId, UUID customerId) {

        Task task = getTask(taskId);

        if (!task.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Only owner can cancel");
        }

        if (task.getStatus() == TaskStatus.CONFIRMED) {
            throw new RuntimeException("Completed task cannot be cancelled");
        }

        taskRepository.cancel(taskId);
    }
}