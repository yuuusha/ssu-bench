package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.repo.PaymentRepository;
import com.diev.repo.TaskRepository;
import com.diev.repo.UserRepository;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private Jdbi jdbi;

    @Mock
    private Handle handle;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(jdbi);
    }

    private void mockTransaction() {
        doAnswer(invocation -> {
            HandleConsumer<?> consumer = invocation.getArgument(0);
            consumer.useHandle(handle);
            return null;
        }).when(jdbi).useTransaction(any());
    }

    @Test
    void confirmTaskTransfersBalanceAndCreatesPayment() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.DONE, customerId, executorId);

        mockTransaction();

        when(handle.attach(TaskRepository.class)).thenReturn(taskRepository);
        when(handle.attach(UserRepository.class)).thenReturn(userRepository);
        when(handle.attach(PaymentRepository.class)).thenReturn(paymentRepository);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(handle.createUpdate(anyString())).thenReturn(mock(Update.class, RETURNS_SELF));
        when(handle.createUpdate(anyString()).execute()).thenReturn(1);

        paymentService.confirmTask(taskId, customerId);

        verify(paymentRepository).create(any(UUID.class), eq(customerId), eq(executorId), eq(100), any(LocalDateTime.class));
        verify(taskRepository).findById(taskId);
    }

    @Test
    void confirmTaskThrowsWhenCalledByNotOwner() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.DONE, UUID.randomUUID(), UUID.randomUUID());

        mockTransaction();

        when(handle.attach(TaskRepository.class)).thenReturn(taskRepository);
        when(handle.attach(UserRepository.class)).thenReturn(userRepository);
        when(handle.attach(PaymentRepository.class)).thenReturn(paymentRepository);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.confirmTask(taskId, customerId));

        assertEquals("Only customer can confirm task", ex.getMessage());
        verify(paymentRepository, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void confirmTaskThrowsWhenTaskIsNotDone() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.IN_PROGRESS, customerId, UUID.randomUUID());

        mockTransaction();

        when(handle.attach(TaskRepository.class)).thenReturn(taskRepository);
        when(handle.attach(UserRepository.class)).thenReturn(userRepository);
        when(handle.attach(PaymentRepository.class)).thenReturn(paymentRepository);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.confirmTask(taskId, customerId));

        assertEquals("Task is not completed by executor", ex.getMessage());
        verify(paymentRepository, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void confirmTaskThrowsWhenExecutorIsNotAssigned() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.DONE, customerId, null);

        mockTransaction();

        when(handle.attach(TaskRepository.class)).thenReturn(taskRepository);
        when(handle.attach(UserRepository.class)).thenReturn(userRepository);
        when(handle.attach(PaymentRepository.class)).thenReturn(paymentRepository);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.confirmTask(taskId, customerId));

        assertEquals("Executor not assigned", ex.getMessage());
        verify(paymentRepository, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void confirmTaskThrowsWhenNotEnoughBalance() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.DONE, customerId, executorId);

        Update update = mock(Update.class, RETURNS_SELF);

        mockTransaction();

        when(handle.attach(TaskRepository.class)).thenReturn(taskRepository);
        when(handle.attach(UserRepository.class)).thenReturn(userRepository);
        when(handle.attach(PaymentRepository.class)).thenReturn(paymentRepository);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(handle.createUpdate(anyString())).thenReturn(update);
        when(update.execute()).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.confirmTask(taskId, customerId));

        assertEquals("Not enough balance", ex.getMessage());
        verify(paymentRepository, never()).create(any(), any(), any(), any(), any());
    }
}