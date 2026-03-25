package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.exception.*;
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
            throw new BadRequestException(ErrorCode.INVALID_REWARD);
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
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Task updateTask(UUID id, String title, String description, Integer reward, String status, UUID currentUserId) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));

        accessService.requireOwnerOrAdmin(
                currentUserId,
                existing.getCustomerId(),
                ErrorCode.ONLY_OWNER_CAN_UPDATE_TASK
        );

        if (reward == null || reward <= 0) {
            throw new BadRequestException(ErrorCode.INVALID_REWARD);
        }

        if (existing.getStatus() != TaskStatus.CREATED) {
            throw new ConflictException(ErrorCode.TASK_NOT_EDITABLE);
        }

        taskRepository.update(
                id,
                title,
                description,
                reward,
                status
        );

        return taskRepository.findById(id)
                .orElseThrow(() -> new ConflictException(ErrorCode.TASK_NOT_EDITABLE));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Task publishTask(UUID id, UUID currentUserId) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));

        accessService.requireOwnerOrAdmin(
                currentUserId,
                existing.getCustomerId(),
                ErrorCode.ONLY_OWNER_CAN_PUBLISH_TASK
        );

        if (existing.getStatus() != TaskStatus.CREATED) {
            throw new ConflictException(ErrorCode.TASK_NOT_PUBLISHABLE);
        }

        taskRepository.updateStatus(id, TaskStatus.PUBLISHED);

        return taskRepository.findById(id)
                .orElseThrow(() -> new ConflictException(ErrorCode.TASK_NOT_EDITABLE));
    }

    public Task getTask(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));
    }

    public List<Task> getTasks(int limit, int offset) {
        return taskRepository.findAll(limit, offset);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public void cancelTask(UUID taskId, UUID customerId) {
        Task existing = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));

        accessService.requireOwnerOrAdmin(
                customerId,
                existing.getCustomerId(),
                ErrorCode.ONLY_OWNER_CAN_CANCEL
        );

        if (existing.getStatus() == TaskStatus.DONE || existing.getStatus() == TaskStatus.CONFIRMED) {
            throw new ConflictException(ErrorCode.TASK_ALREADY_DONE);
        }

        taskRepository.cancel(taskId);
    }
}