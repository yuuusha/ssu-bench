package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
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

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
    }

    @Test
    void createTaskCreatesTaskWithCreatedStatus() {
        UUID customerId = UUID.randomUUID();
        String title = "Task title";
        String description = "Task description";
        Integer reward = 100;

        when(taskRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.of(new Task(id, title, description, reward, TaskStatus.CREATED, customerId, null));
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
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, customerId, null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        taskService.cancelTask(taskId, customerId);

        verify(taskRepository).cancel(taskId);
    }

    @Test
    void cancelTaskThrowsWhenNotOwner() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, UUID.randomUUID(), null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.cancelTask(taskId, UUID.randomUUID()));

        assertEquals("Only owner can cancel", ex.getMessage());
        verify(taskRepository, never()).cancel(any());
    }

    @Test
    void cancelTaskThrowsWhenAlreadyConfirmed() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.CONFIRMED, customerId, UUID.randomUUID());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.cancelTask(taskId, customerId));

        assertEquals("Completed task cannot be cancelled", ex.getMessage());
        verify(taskRepository, never()).cancel(any());
    }
}