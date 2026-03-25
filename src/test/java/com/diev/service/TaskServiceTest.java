package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.exception.ConflictException;
import com.diev.exception.ErrorCode;
import com.diev.exception.ForbiddenException;
import com.diev.repo.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CurrentUserAccessService accessService;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, accessService);
    }

    @Test
    void createTaskCreatesTaskWithCreatedStatus() {
        UUID customerId = UUID.randomUUID();
        String title = "Task title";
        String description = "Task description";
        Integer reward = 100;

        when(taskRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID generatedId = invocation.getArgument(0);
                    return Optional.of(new Task(generatedId, title, description, reward, TaskStatus.CREATED, customerId, null));
                });

        Task task = taskService.createTask(customerId, title, description, reward);

        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskRepository).create(any(UUID.class), eq(title), eq(description), eq(reward), statusCaptor.capture(), eq(customerId));

        assertEquals(TaskStatus.CREATED, task.getStatus());
        assertEquals(title, task.getTitle());
        assertEquals(reward, task.getReward());
        assertEquals(TaskStatus.CREATED.name(), statusCaptor.getValue());
    }

    @Test
    void updateTaskUpdatesOnlyWhenOwnerOrAdminAndEditable() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        String title = "Updated title";
        String description = "Updated description";
        Integer reward = 150;
        TaskStatus status = TaskStatus.CREATED;

        Task existing = new Task(taskId, "Old title", "Old description", 100, TaskStatus.CREATED, customerId, null);
        Task updated = new Task(taskId, title, description, reward, TaskStatus.CREATED, customerId, null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing), Optional.of(updated));
        doNothing().when(accessService).requireOwnerOrAdmin(currentUserId, customerId, ErrorCode.ONLY_OWNER_CAN_UPDATE_TASK);

        Task result = taskService.updateTask(taskId, title, description, reward, status.name(), currentUserId);

        verify(taskRepository).update(eq(taskId), eq(title), eq(description), eq(reward), eq(status.name()));
        assertEquals(title, result.getTitle());
        assertEquals(description, result.getDescription());
        assertEquals(reward, result.getReward());
    }

    @Test
    void publishTaskPublishesCreatedTask() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.CREATED, customerId, null);
        Task published = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, customerId, null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task), Optional.of(published));
        doNothing().when(accessService).requireOwnerOrAdmin(currentUserId, customerId, ErrorCode.ONLY_OWNER_CAN_PUBLISH_TASK);

        Task result = taskService.publishTask(taskId, currentUserId);

        verify(taskRepository).updateStatus(taskId, TaskStatus.PUBLISHED);
        assertEquals(TaskStatus.PUBLISHED, result.getStatus());
    }

    @Test
    void publishTaskThrowsWhenTaskAlreadyPublished() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, customerId, null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(accessService).requireOwnerOrAdmin(currentUserId, customerId, ErrorCode.ONLY_OWNER_CAN_PUBLISH_TASK);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> taskService.publishTask(taskId, currentUserId));

        assertEquals("Task can be published only from CREATED status.", ex.getMessage());
        verify(taskRepository, never()).updateStatus(any(), any());
    }

    @Test
    void getTaskReturnsTask() {
        UUID id = UUID.randomUUID();
        Task task = new Task(id, "Title", "Desc", 50, TaskStatus.PUBLISHED, UUID.randomUUID(), null);

        when(taskRepository.findById(id)).thenReturn(Optional.of(task));

        Task result = taskService.getTask(id);

        assertSame(task, result);
    }

    @Test
    void getTasksReturnsPagedList() {
        List<Task> tasks = List.of(
                new Task(UUID.randomUUID(), "A", "DA", 10, TaskStatus.CREATED, UUID.randomUUID(), null),
                new Task(UUID.randomUUID(), "B", "DB", 20, TaskStatus.PUBLISHED, UUID.randomUUID(), null)
        );

        when(taskRepository.findAll(20, 0)).thenReturn(tasks);

        List<Task> result = taskService.getTasks(20, 0);

        assertEquals(tasks, result);
    }

    @Test
    void cancelTaskCancelsWhenOwnerAndTaskNotConfirmed() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, customerId, null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(accessService).requireOwnerOrAdmin(currentUserId, customerId, ErrorCode.ONLY_OWNER_CAN_CANCEL);

        taskService.cancelTask(taskId, currentUserId);

        verify(taskRepository).cancel(taskId);
    }

    @Test
    void cancelTaskThrowsWhenNotOwner() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, customerId, null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doThrow(new ForbiddenException(ErrorCode.ONLY_OWNER_CAN_CANCEL))
                .when(accessService)
                .requireOwnerOrAdmin(eq(currentUserId), eq(customerId), eq(ErrorCode.ONLY_OWNER_CAN_CANCEL));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> taskService.cancelTask(taskId, currentUserId));

        assertEquals("Only the task owner can cancel it.", ex.getMessage());
        verify(taskRepository, never()).cancel(any());
    }

    @Test
    void cancelTaskThrowsWhenAlreadyConfirmed() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.CONFIRMED, customerId, UUID.randomUUID());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(accessService).requireOwnerOrAdmin(currentUserId, customerId, ErrorCode.ONLY_OWNER_CAN_CANCEL);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> taskService.cancelTask(taskId, currentUserId));

        assertEquals("Completed task cannot be cancelled.", ex.getMessage());
        verify(taskRepository, never()).cancel(any());
    }
}