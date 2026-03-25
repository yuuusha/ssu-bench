package com.diev.service;

import com.diev.entity.*;
import com.diev.exception.ConflictException;
import com.diev.exception.ForbiddenException;
import com.diev.exception.NotFoundException;
import com.diev.repo.BidRepository;
import com.diev.repo.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final TaskRepository taskRepository;
    private final CurrentUserAccessService accessService;

    @PreAuthorize("hasAnyRole('EXECUTOR', 'ADMIN')")
    public Bid createBid(UUID taskId, UUID executorId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        if (task.getCustomerId().equals(executorId)) {
            throw new ConflictException("CUSTOMER_CANNOT_BID", "Task owner cannot create a bid for own task.");
        }

        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new ConflictException("TASK_NOT_OPEN_FOR_BIDS", "Task is not open for bids.");
        }

        if (bidRepository.countByTaskIdAndExecutorId(taskId, executorId) > 0) {
            throw new ConflictException("BID_ALREADY_EXISTS", "Executor already has a bid for this task.");
        }

        UUID bidId = UUID.randomUUID();

        bidRepository.create(
                bidId,
                taskId,
                executorId,
                BidStatus.PENDING.name()
        );

        return bidRepository.findById(bidId)
                .orElseThrow(() -> new ConflictException("BID_NOT_CREATED", "Bid not created."));
    }

    public List<Bid> getBids(UUID taskId, int limit, int offset) {
        return bidRepository.findByTask(taskId, limit, offset);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Transactional
    public void selectBid(UUID bidId, UUID customerId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new NotFoundException("BID_NOT_FOUND", "Bid not found."));

        UUID taskId = bid.getTaskId();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        accessService.requireOwnerOrAdmin(
                customerId,
                task.getCustomerId(),
                "ONLY_OWNER_CAN_SELECT_BID",
                "Only task owner can select bid."
        );

        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new ConflictException("TASK_NOT_OPEN_FOR_SELECTION", "Task is not open for selecting bids.");
        }

        if (task.getExecutorId() != null) {
            throw new ConflictException("EXECUTOR_ALREADY_SELECTED", "Executor already selected.");
        }

        bidRepository.updateStatus(bidId, BidStatus.ACCEPTED);
        bidRepository.rejectOtherBids(taskId, bidId);
        taskRepository.assignExecutor(taskId, bid.getExecutorId());
    }

    @PreAuthorize("hasAnyRole('EXECUTOR', 'ADMIN')")
    public void markCompleted(UUID taskId, UUID executorId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        if (task.getExecutorId() == null) {
            throw new ConflictException("EXECUTOR_NOT_ASSIGNED", "Executor not assigned.");
        }

        accessService.requireOwnerOrAdmin(
                executorId,
                task.getExecutorId(),
                "ONLY_ASSIGNED_EXECUTOR_CAN_COMPLETE",
                "Only assigned executor can complete the task."
        );

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new ConflictException("TASK_NOT_IN_PROGRESS", "Task is not in progress.");
        }

        taskRepository.updateStatus(taskId, TaskStatus.DONE);
    }
}