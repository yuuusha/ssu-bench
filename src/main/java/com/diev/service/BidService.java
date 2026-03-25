package com.diev.service;

import com.diev.entity.*;
import com.diev.exception.ConflictException;
import com.diev.exception.ErrorCode;
import com.diev.exception.NotFoundException;
import com.diev.repo.BidRepository;
import com.diev.repo.TaskRepository;
import lombok.RequiredArgsConstructor;
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
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));

        if (task.getCustomerId().equals(executorId)) {
            throw new ConflictException(ErrorCode.CUSTOMER_CANNOT_BID);
        }

        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new ConflictException(ErrorCode.TASK_NOT_OPEN_FOR_BIDS);
        }

        if (bidRepository.countByTaskIdAndExecutorId(taskId, executorId) > 0) {
            throw new ConflictException(ErrorCode.BID_ALREADY_EXISTS);
        }

        UUID bidId = UUID.randomUUID();

        bidRepository.create(
                bidId,
                taskId,
                executorId,
                BidStatus.PENDING.name()
        );

        return bidRepository.findById(bidId)
                .orElseThrow(() -> new ConflictException(ErrorCode.BID_NOT_CREATED));
    }

    public List<Bid> getBids(UUID taskId, int limit, int offset) {
        return bidRepository.findByTask(taskId, limit, offset);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Transactional
    public void selectBid(UUID bidId, UUID customerId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BID_NOT_FOUND));

        UUID taskId = bid.getTaskId();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));

        accessService.requireOwnerOrAdmin(
                customerId,
                task.getCustomerId(),
                ErrorCode.ONLY_OWNER_CAN_SELECT_BID
        );

        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new ConflictException(ErrorCode.TASK_NOT_OPEN_FOR_SELECTION);
        }

        if (task.getExecutorId() != null) {
            throw new ConflictException(ErrorCode.EXECUTOR_ALREADY_SELECTED);
        }

        bidRepository.updateStatus(bidId, BidStatus.ACCEPTED);
        bidRepository.rejectOtherBids(taskId, bidId);
        taskRepository.assignExecutor(taskId, bid.getExecutorId());
    }

    @PreAuthorize("hasAnyRole('EXECUTOR', 'ADMIN')")
    public void markCompleted(UUID taskId, UUID executorId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));

        if (task.getExecutorId() == null) {
            throw new ConflictException(ErrorCode.EXECUTOR_NOT_ASSIGNED);
        }

        accessService.requireOwnerOrAdmin(
                executorId,
                task.getExecutorId(),
                ErrorCode.ONLY_ASSIGNED_EXECUTOR_CAN_COMPLETE
        );

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new ConflictException(ErrorCode.TASK_NOT_IN_PROGRESS);
        }

        taskRepository.updateStatus(taskId, TaskStatus.DONE);
    }
}