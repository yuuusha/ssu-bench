package com.diev.service;

import com.diev.entity.*;
import com.diev.exception.ConflictException;
import com.diev.exception.ForbiddenException;
import com.diev.exception.NotFoundException;
import com.diev.repo.BidRepository;
import com.diev.repo.TaskRepository;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BidService {

    private final BidRepository bidRepository;
    private final TaskRepository taskRepository;
    private final Jdbi jdbi;

    public BidService(
            BidRepository bidRepository,
            TaskRepository taskRepository,
            Jdbi jdbi
    ) {
        this.bidRepository = bidRepository;
        this.taskRepository = taskRepository;
        this.jdbi = jdbi;
    }

    public Bid createBid(UUID taskId, UUID executorId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new ConflictException("TASK_NOT_OPEN_FOR_BIDS", "Task is not open for bids.");
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

    public void selectBid(UUID taskId, UUID bidId, UUID customerId) {

        jdbi.useTransaction(handle -> {

            TaskRepository taskRepo = handle.attach(TaskRepository.class);
            BidRepository bidRepo = handle.attach(BidRepository.class);

            Task task = taskRepo.findById(taskId)
                    .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

            if (!task.getCustomerId().equals(customerId)) {
                throw new ForbiddenException("ONLY_OWNER_CAN_SELECT_BID", "Only task owner can select bid.");
            }

            if (task.getStatus() != TaskStatus.PUBLISHED) {
                throw new ConflictException("TASK_NOT_OPEN_FOR_SELECTION", "Task is not open for selecting bids.");
            }

            if (task.getExecutorId() != null) {
                throw new ConflictException("EXECUTOR_ALREADY_SELECTED", "Executor already selected.");
            }

            Bid bid = bidRepo.findById(bidId)
                    .orElseThrow(() -> new NotFoundException("BID_NOT_FOUND", "Bid not found."));

            if (!bid.getTaskId().equals(taskId)) {
                throw new ConflictException("BID_DOES_NOT_BELONG_TO_TASK", "Bid does not belong to this task.");
            }

            // принимаем выбранный bid
            bidRepo.updateStatus(bidId, BidStatus.ACCEPTED);

            // отклоняем остальные bids
            bidRepo.rejectOtherBids(taskId, bidId);

            // назначаем исполнителя задаче
            handle.createUpdate("""
                UPDATE tasks
                SET executor_id = :executorId,
                    status = :status
                WHERE id = :taskId
        """)
                    .bind("executorId", bid.getExecutorId())
                    .bind("status", TaskStatus.IN_PROGRESS.name())
                    .bind("taskId", taskId)
                    .execute();
        });
    }

    public void markCompleted(UUID taskId, UUID executorId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        if (!executorId.equals(task.getExecutorId())) {
            throw new ForbiddenException("ONLY_ASSIGNED_EXECUTOR_CAN_COMPLETE", "Only assigned executor can complete the task.");
        }

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new ConflictException("TASK_NOT_IN_PROGRESS", "Task is not in progress.");
        }

        taskRepository.updateStatus(taskId, TaskStatus.DONE);
    }
}