package com.diev.service;

import com.diev.entity.*;
import com.diev.repo.BidRepository;
import com.diev.repo.TaskRepository;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private Jdbi jdbi;

    @Mock
    private Handle handle;

    private BidService bidService;

    @BeforeEach
    void setUp() {
        bidService = new BidService(bidRepository, taskRepository, jdbi);
    }

    private void mockTransaction() {
        doAnswer(invocation -> {
            HandleConsumer<?> consumer = invocation.getArgument(0);
            consumer.useHandle(handle);
            return null;
        }).when(jdbi).useTransaction(any());
    }

    @Test
    void createBidCreatesPendingBidForPublishedTask() {
        UUID taskId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, UUID.randomUUID(), null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(bidRepository.findById(any(UUID.class))).thenReturn(
                Optional.of(new Bid(bidId, taskId, executorId, BidStatus.PENDING.name()))
        );

        Bid bid = bidService.createBid(taskId, executorId);

        verify(bidRepository).create(any(UUID.class), eq(taskId), eq(executorId), eq(BidStatus.PENDING.name()));
        assertEquals(BidStatus.PENDING.name(), bid.getStatus());
    }

    @Test
    void createBidThrowsWhenTaskNotFound() {
        UUID taskId = UUID.randomUUID();

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bidService.createBid(taskId, UUID.randomUUID()));

        assertEquals("Task not found", ex.getMessage());
    }

    @Test
    void createBidThrowsWhenTaskIsNotPublished() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.CREATED, UUID.randomUUID(), null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bidService.createBid(taskId, UUID.randomUUID()));

        assertEquals("Task is not open for bids", ex.getMessage());
        verify(bidRepository, never()).create(any(), any(), any(), any());
    }

    @Test
    void markCompletedMarksTaskAsDoneForAssignedExecutor() {
        UUID taskId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.IN_PROGRESS, UUID.randomUUID(), executorId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        bidService.markCompleted(taskId, executorId);

        verify(taskRepository).updateStatus(taskId, TaskStatus.DONE);
    }

    @Test
    void markCompletedThrowsWhenExecutorIsNotAssigned() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.IN_PROGRESS, UUID.randomUUID(), UUID.randomUUID());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bidService.markCompleted(taskId, UUID.randomUUID()));

        assertEquals("Only assigned executor can complete task", ex.getMessage());
        verify(taskRepository, never()).updateStatus(any(), any());
    }

    @Test
    void markCompletedThrowsWhenTaskIsNotInProgress() {
        UUID taskId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, UUID.randomUUID(), executorId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bidService.markCompleted(taskId, executorId));

        assertEquals("Task not in progress", ex.getMessage());
        verify(taskRepository, never()).updateStatus(any(), any());
    }

    @Test
    void selectBidAssignsExecutorAndUpdatesStatuses() {
        UUID taskId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, customerId, null);
        Bid bid = new Bid(bidId, taskId, executorId, BidStatus.PENDING.name());

        Update update = mock(Update.class, RETURNS_SELF);

        mockTransaction();

        when(handle.attach(TaskRepository.class)).thenReturn(taskRepository);
        when(handle.attach(BidRepository.class)).thenReturn(bidRepository);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        when(handle.createUpdate(anyString())).thenReturn(update);

        bidService.selectBid(taskId, bidId, customerId);

        verify(handle, atLeastOnce()).createUpdate(anyString());
    }
}