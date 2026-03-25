package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.exception.ConflictException;
import com.diev.exception.ForbiddenException;
import com.diev.exception.InsufficientBalanceException;
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
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CurrentUserAccessService accessService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(taskRepository, userRepository, paymentRepository, accessService);
    }

    @Test
    void confirmTaskTransfersBalanceAndCreatesPayment() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.DONE, customerId, executorId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.decreaseBalanceIfEnough(customerId, 100L)).thenReturn(1);

        paymentService.confirmTask(taskId, customerId);

        verify(userRepository).decreaseBalanceIfEnough(customerId, 100L);
        verify(userRepository).increaseBalance(executorId, 100L);
        verify(paymentRepository).create(any(UUID.class), eq(customerId), eq(executorId), eq(100), any(LocalDateTime.class));
        verify(taskRepository).updateStatus(taskId, TaskStatus.CONFIRMED);
    }

    @Test
    void confirmTaskThrowsWhenCalledByNotOwner() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.DONE, UUID.randomUUID(), UUID.randomUUID());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doThrow(new ForbiddenException("ONLY_CUSTOMER_CAN_CONFIRM", "Only customer can confirm the task."))
                .when(accessService)
                .requireOwnerOrAdmin(eq(customerId), any(), anyString(), anyString());

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> paymentService.confirmTask(taskId, customerId));

        assertEquals("Only customer can confirm the task.", ex.getMessage());
        verify(paymentRepository, never()).create(any(), any(), any(), any(), any());
        verify(userRepository, never()).decreaseBalanceIfEnough(any(), anyLong());
        verify(userRepository, never()).increaseBalance(any(), anyLong());
        verify(taskRepository, never()).updateStatus(any(), any());
    }

    @Test
    void confirmTaskThrowsWhenTaskIsNotDone() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.IN_PROGRESS, customerId, UUID.randomUUID());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(accessService).requireOwnerOrAdmin(eq(customerId), eq(customerId), anyString(), anyString());

        ConflictException ex = assertThrows(ConflictException.class,
                () -> paymentService.confirmTask(taskId, customerId));

        assertEquals("Task is not completed by executor.", ex.getMessage());
        verify(paymentRepository, never()).create(any(), any(), any(), any(), any());
        verify(userRepository, never()).decreaseBalanceIfEnough(any(), anyLong());
        verify(userRepository, never()).increaseBalance(any(), anyLong());
        verify(taskRepository, never()).updateStatus(any(), any());
    }

    @Test
    void confirmTaskThrowsWhenExecutorIsNotAssigned() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.DONE, customerId, null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> paymentService.confirmTask(taskId, customerId));

        assertEquals("Executor not assigned.", ex.getMessage());
        verify(paymentRepository, never()).create(any(), any(), any(), any(), any());
        verify(userRepository, never()).decreaseBalanceIfEnough(any(), anyLong());
        verify(userRepository, never()).increaseBalance(any(), anyLong());
        verify(taskRepository, never()).updateStatus(any(), any());
    }

    @Test
    void confirmTaskThrowsWhenNotEnoughBalance() {
        UUID taskId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.DONE, customerId, executorId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.decreaseBalanceIfEnough(customerId, 100L)).thenReturn(0);

        InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class,
                () -> paymentService.confirmTask(taskId, customerId));

        assertEquals("Not enough balance.", ex.getMessage());
        verify(userRepository).decreaseBalanceIfEnough(customerId, 100L);
        verify(userRepository, never()).increaseBalance(any(), anyLong());
        verify(paymentRepository, never()).create(any(), any(), any(), any(), any());
        verify(taskRepository, never()).updateStatus(any(), any());
    }
}