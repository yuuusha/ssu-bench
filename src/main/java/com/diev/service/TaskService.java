package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.entity.User;
import com.diev.exception.BadRequestException;
import com.diev.exception.ConflictException;
import com.diev.exception.ForbiddenException;
import com.diev.exception.NotFoundException;
import com.diev.repo.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final CurrentUserAccessService accessService;

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
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

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Task updateTask(UUID id, String title, String description, Integer reward, String status, UUID currentUserId) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        accessService.requireOwnerOrAdmin(
                currentUserId,
                existing.getCustomerId(),
                "ONLY_OWNER_CAN_UPDATE_TASK",
                "Only task owner can update it."
        );

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

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Task publishTask(UUID id, UUID currentUserId) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        accessService.requireOwnerOrAdmin(
                currentUserId,
                existing.getCustomerId(),
                "ONLY_OWNER_CAN_PUBLISH_TASK",
                "Only task owner can publish it."
        );

        if (existing.getStatus() != TaskStatus.CREATED) {
            throw new ConflictException("TASK_NOT_PUBLISHABLE", "Task can be published only from CREATED status.");
        }

        taskRepository.updateStatus(id, TaskStatus.PUBLISHED);

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

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public void cancelTask(UUID taskId, UUID customerId) {
        Task existing = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        accessService.requireOwnerOrAdmin(
                customerId,
                existing.getCustomerId(),
                "ONLY_OWNER_CAN_CANCEL",
                "Only the task owner can cancel it."
        );

        if (existing.getStatus() == TaskStatus.DONE || existing.getStatus() == TaskStatus.CONFIRMED) {
            throw new ConflictException("TASK_ALREADY_DONE", "Completed task cannot be cancelled.");
        }

        taskRepository.cancel(taskId);
    }
}