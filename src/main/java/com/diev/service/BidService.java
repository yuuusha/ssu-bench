package com.diev.service;

import com.diev.entity.*;
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
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new RuntimeException("Task is not open for bids");
        }

        UUID bidId = UUID.randomUUID();

        bidRepository.create(
                bidId,
                taskId,
                executorId,
                BidStatus.PENDING.name()
        );

        return bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not created"));
    }

    public List<Bid> getBids(UUID taskId) {
        return bidRepository.findByTask(taskId);
    }

    public void selectBid(UUID taskId, UUID bidId, UUID customerId) {

        jdbi.useTransaction(handle -> {

            TaskRepository taskRepo = handle.attach(TaskRepository.class);
            BidRepository bidRepo = handle.attach(BidRepository.class);

            Task task = taskRepo.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            if (!task.getCustomerId().equals(customerId)) {
                throw new RuntimeException("Only task owner can select bid");
            }

            if (task.getStatus() != TaskStatus.PUBLISHED) {
                throw new RuntimeException("Task is not open for selecting bids");
            }

            if (task.getExecutorId() != null) {
                throw new RuntimeException("Executor already selected");
            }

            Bid bid = bidRepo.findById(bidId)
                    .orElseThrow(() -> new RuntimeException("Bid not found"));

            if (!bid.getTaskId().equals(taskId)) {
                throw new RuntimeException("Bid does not belong to this task");
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
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!executorId.equals(task.getExecutorId())) {
            throw new RuntimeException("Only assigned executor can complete task");
        }

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new RuntimeException("Task not in progress");
        }

        taskRepository.updateStatus(taskId, TaskStatus.DONE);
    }
}