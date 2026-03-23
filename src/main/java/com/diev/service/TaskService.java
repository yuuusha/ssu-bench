package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.entity.User;
import com.diev.exception.BadRequestException;
import com.diev.exception.ConflictException;
import com.diev.exception.ForbiddenException;
import com.diev.exception.NotFoundException;
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

        if (reward == null || reward <= 0) {
            throw new BadRequestException("INVALID_REWARD", "Reward must be greater than zero.");
        }

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
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));
    }

    public Task updateTask(UUID id, String title, String description, Integer reward, String status) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        if (reward == null || reward <= 0) {
            throw new BadRequestException("INVALID_REWARD", "Reward must be greater than zero.");
        }

        if (existing.getStatus() != TaskStatus.CREATED) {
            throw new ConflictException("TASK_NOT_EDITABLE", "Task cannot be updated in its current state.");
        }

        taskRepository.update(
                id,
                title,
                description,
                reward,
                status
        );

        return taskRepository.findById(id)
                .orElseThrow(() -> new ConflictException("TASK_NOT_EDITABLE", "Task cannot be updated in its current state."));
    }

    public Task updateTaskStatus(UUID id, UUID customerId, String status) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        if (existing.getCustomerId() != customerId) {
            throw new ForbiddenException("ONLY_OWNER_CAN_PUBLISH_TASK", "Only task owner can publish it.");
        }

        taskRepository.update(
                id,
                existing.getTitle(),
                existing.getDescription(),
                existing.getReward(),
                status
        );

        return taskRepository.findById(id)
                .orElseThrow(() -> new ConflictException("TASK_NOT_EDITABLE", "Task cannot be updated in its current state."));
    }

    public Task getTask(UUID id) {

        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));
    }

    public List<Task> getTasks(int limit, int offset) {

        return taskRepository.findAll(limit, offset);
    }

    public void cancelTask(UUID taskId, UUID customerId) {

        Task task = getTask(taskId);

        if (!task.getCustomerId().equals(customerId)) {
            throw new ForbiddenException("ONLY_OWNER_CAN_CANCEL", "Only the task owner can cancel it.");
        }

        if (task.getStatus() == TaskStatus.CONFIRMED) {
            throw new ConflictException("TASK_ALREADY_CONFIRMED", "Completed task cannot be cancelled.");
        }

        taskRepository.cancel(taskId);
    }
}