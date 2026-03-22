package com.diev.service;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import com.diev.entity.User;
import com.diev.repo.PaymentRepository;
import com.diev.repo.TaskRepository;
import com.diev.repo.UserRepository;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final Jdbi jdbi;

    public PaymentService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void confirmTask(UUID taskId, UUID customerId) {

        jdbi.useTransaction(handle -> {

            TaskRepository taskRepo = handle.attach(TaskRepository.class);
            UserRepository userRepo = handle.attach(UserRepository.class);
            PaymentRepository paymentRepo = handle.attach(PaymentRepository.class);

            Task task = taskRepo.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            if (!task.getCustomerId().equals(customerId)) {
                throw new RuntimeException("Only customer can confirm task");
            }

            if (task.getStatus() != TaskStatus.DONE) {
                throw new RuntimeException("Task is not completed by executor");
            }

            UUID executorId = task.getExecutorId();

            if (executorId == null) {
                throw new RuntimeException("Executor not assigned");
            }

            int reward = task.getReward();

            // атомарное списание средств
            int updated = handle.createUpdate("""
                UPDATE users
                SET balance = balance - :amount
                WHERE id = :id
                AND balance >= :amount
        """)
                    .bind("id", task.getCustomerId())
                    .bind("amount", reward)
                    .execute();

            if (updated == 0) {
                throw new RuntimeException("Not enough balance");
            }

            // начисляем исполнителю
            handle.createUpdate("""
                UPDATE users
                SET balance = balance + :amount
                WHERE id = :id
        """)
                    .bind("id", executorId)
                    .bind("amount", reward)
                    .execute();

            // создаём запись платежа
            paymentRepo.create(
                    UUID.randomUUID(),
                    task.getCustomerId(),
                    executorId,
                    reward,
                    LocalDateTime.now()
            );

            // обновляем статус задачи
            handle.createUpdate("""
                UPDATE tasks
                SET status = :status
                WHERE id = :id
        """)
                    .bind("status", TaskStatus.CONFIRMED.name())
                    .bind("id", taskId)
                    .execute();
        });
    }
}