package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.entity.User;
import com.diev.exception.ConflictException;
import com.diev.exception.ForbiddenException;
import com.diev.exception.InsufficientBalanceException;
import com.diev.exception.NotFoundException;
import com.diev.repo.PaymentRepository;
import com.diev.repo.TaskRepository;
import com.diev.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserAccessService accessService;

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Transactional
    public void confirmTask(UUID taskId, UUID customerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));

        accessService.requireOwnerOrAdmin(
                customerId,
                task.getCustomerId(),
                "ONLY_CUSTOMER_CAN_CONFIRM",
                "Only customer can confirm the task."
        );

        if (task.getStatus() != TaskStatus.DONE) {
            throw new ConflictException("TASK_NOT_DONE", "Task is not completed by executor.");
        }

        UUID executorId = task.getExecutorId();

        if (executorId == null) {
            throw new ConflictException("EXECUTOR_NOT_ASSIGNED", "Executor not assigned.");
        }

        int reward = task.getReward();
        int updated = userRepository.decreaseBalanceIfEnough(task.getCustomerId(), reward);

        if (updated == 0) {
            throw new InsufficientBalanceException();
        }

        userRepository.increaseBalance(executorId, reward);
        paymentRepository.create(
                UUID.randomUUID(),
                task.getCustomerId(),
                executorId,
                reward,
                LocalDateTime.now()
        );
        taskRepository.updateStatus(taskId, TaskStatus.CONFIRMED);
    }
}