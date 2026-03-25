package com.diev.service;

import com.diev.entity.*;
import com.diev.exception.ConflictException;
import com.diev.exception.ForbiddenException;
import com.diev.exception.NotFoundException;
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
    private CurrentUserAccessService accessService;

    private BidService bidService;

    @BeforeEach
    void setUp() {
        bidService = new BidService(bidRepository, taskRepository, accessService);
    }

    @Test
    void createBidCreatesPendingBidForPublishedTask() {
        UUID taskId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, customerId, null);
        Bid created = new Bid(bidId, taskId, executorId, BidStatus.PENDING.name());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(bidRepository.findById(any(UUID.class))).thenReturn(Optional.of(created));

        Bid bid = bidService.createBid(taskId, executorId);

        verify(bidRepository).create(any(UUID.class), eq(taskId), eq(executorId), eq(BidStatus.PENDING.name()));
        assertEquals(BidStatus.PENDING.name(), bid.getStatus());
        assertEquals(taskId, bid.getTaskId());
        assertEquals(executorId, bid.getExecutorId());
    }

    @Test
    void createBidThrowsWhenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> bidService.createBid(taskId, executorId));

        assertEquals("Task not found.", ex.getMessage());
    }

    @Test
    void createBidThrowsWhenTaskIsNotPublished() {
        UUID taskId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.CREATED, UUID.randomUUID(), null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> bidService.createBid(taskId, executorId));

        assertEquals("Task is not open for bids.", ex.getMessage());
        verify(bidRepository, never()).create(any(), any(), any(), any());
    }

    @Test
    void completeBidMarksTaskAsDoneForAssignedExecutor() {
        UUID taskId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.IN_PROGRESS, UUID.randomUUID(), executorId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        bidService.markCompleted(taskId, executorId);
        verify(taskRepository).updateStatus(taskId, TaskStatus.DONE);
    }

    @Test
    void completeBidThrowsWhenExecutorIsNotAssigned() {
        UUID taskId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.IN_PROGRESS, UUID.randomUUID(), null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> bidService.markCompleted(taskId, executorId));

        assertEquals("Executor not assigned.", ex.getMessage());
        verify(taskRepository, never()).updateStatus(any(), any());
    }

    @Test
    void markCompletedThrowsWhenTaskIsNotInProgress() {
        UUID taskId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();
        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, UUID.randomUUID(), executorId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> bidService.markCompleted(taskId, executorId));

        assertEquals("Task is not in progress.", ex.getMessage());
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

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        bidService.selectBid(bidId, customerId);

        verify(bidRepository).updateStatus(bidId, BidStatus.ACCEPTED);
        verify(bidRepository).rejectOtherBids(taskId, bidId);
        verify(taskRepository).assignExecutor(taskId, executorId);
    }

    @Test
    void getBidsReturnsPagedList() {
        UUID taskId = UUID.randomUUID();
        List<Bid> bids = List.of(
                new Bid(UUID.randomUUID(), taskId, UUID.randomUUID(), BidStatus.PENDING.name()),
                new Bid(UUID.randomUUID(), taskId, UUID.randomUUID(), BidStatus.ACCEPTED.name())
        );

        when(bidRepository.findByTask(taskId, 20, 0)).thenReturn(bids);

        List<Bid> result = bidService.getBids(taskId, 20, 0);

        assertEquals(bids, result);
    }

    @Test
    void createBidThrowsWhenExecutorAlreadyHasBidForTask() {
        UUID taskId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, customerId, null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(bidRepository.countByTaskIdAndExecutorId(taskId, executorId)).thenReturn(1);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> bidService.createBid(taskId, executorId));

        assertEquals("Executor already has a bid for this task.", ex.getMessage());
        verify(bidRepository, never()).create(any(), any(), any(), any());
    }

    @Test
    void selectBidThrowsWhenCalledByNotOwner() {
        UUID taskId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, ownerId, null);
        Bid bid = new Bid(bidId, taskId, executorId, BidStatus.PENDING.name());

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doThrow(new ForbiddenException("ONLY_OWNER_CAN_SELECT_BID", "Only task owner can select bid."))
                .when(accessService)
                .requireOwnerOrAdmin(eq(customerId), eq(ownerId), anyString(), anyString());

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> bidService.selectBid(bidId, customerId));

        assertEquals("Only task owner can select bid.", ex.getMessage());
        verify(bidRepository, never()).updateStatus(any(), any());
        verify(bidRepository, never()).rejectOtherBids(any(), any());
        verify(taskRepository, never()).assignExecutor(any(), any());
    }

    @Test
    void selectBidThrowsWhenBidNotFound() {
        UUID bidId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> bidService.selectBid(bidId, customerId));

        assertEquals("Bid not found.", ex.getMessage());
        verify(taskRepository, never()).findById(any());
        verify(bidRepository, never()).updateStatus(any(), any());
        verify(bidRepository, never()).rejectOtherBids(any(), any());
        verify(taskRepository, never()).assignExecutor(any(), any());
    }

    @Test
    void selectBidThrowsWhenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();

        Bid bid = new Bid(bidId, taskId, executorId, BidStatus.PENDING.name());

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> bidService.selectBid(bidId, customerId));

        assertEquals("Task not found.", ex.getMessage());
        verify(bidRepository, never()).updateStatus(any(), any());
        verify(bidRepository, never()).rejectOtherBids(any(), any());
        verify(taskRepository, never()).assignExecutor(any(), any());
    }

    @Test
    void selectBidThrowsWhenTaskIsNotPublished() {
        UUID taskId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.CREATED, customerId, null);
        Bid bid = new Bid(bidId, taskId, executorId, BidStatus.PENDING.name());

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> bidService.selectBid(bidId, customerId));

        assertEquals("Task is not open for selecting bids.", ex.getMessage());
        verify(bidRepository, never()).updateStatus(any(), any());
        verify(bidRepository, never()).rejectOtherBids(any(), any());
        verify(taskRepository, never()).assignExecutor(any(), any());
    }

    @Test
    void selectBidThrowsWhenExecutorAlreadySelected() {
        UUID taskId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID executorId = UUID.randomUUID();

        Task task = new Task(taskId, "Title", "Desc", 100, TaskStatus.PUBLISHED, customerId, UUID.randomUUID());
        Bid bid = new Bid(bidId, taskId, executorId, BidStatus.PENDING.name());

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> bidService.selectBid(bidId, customerId));

        assertEquals("Executor already selected.", ex.getMessage());
        verify(bidRepository, never()).updateStatus(any(), any());
        verify(bidRepository, never()).rejectOtherBids(any(), any());
        verify(taskRepository, never()).assignExecutor(any(), any());
    }
}