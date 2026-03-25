package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.exception.*;
import com.diev.repo.PaymentRepository;
import com.diev.repo.TaskRepository;
import com.diev.repo.UserRepository;
import lombok.RequiredArgsConstructor;
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
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));

        accessService.requireOwnerOrAdmin(
                customerId,
                task.getCustomerId(),
                ErrorCode.ONLY_CUSTOMER_CAN_CONFIRM
        );

        if (task.getStatus() != TaskStatus.DONE) {
            throw new ConflictException(ErrorCode.TASK_NOT_DONE);
        }

        UUID executorId = task.getExecutorId();

        if (executorId == null) {
            throw new ConflictException(ErrorCode.EXECUTOR_NOT_ASSIGNED);
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