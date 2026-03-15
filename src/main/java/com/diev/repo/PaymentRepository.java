package com.diev.repo;

import com.diev.entity.Payment;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.util.List;
import java.util.UUID;

@RegisterBeanMapper(Payment.class)
public interface PaymentRepository {

    @SqlUpdate("""
        INSERT INTO payments (id, from_user_id, to_user_id, amount, task_id, created_at)
        VALUES (:id, :fromUserId, :toUserId, :amount, :taskId, NOW())
    """)
    void create(
            @Bind("id") UUID id,
            @Bind("fromUserId") UUID fromUserId,
            @Bind("toUserId") UUID toUserId,
            @Bind("amount") Integer amount,
            @Bind("taskId") UUID taskId
    );

    @SqlQuery("""
        SELECT *
        FROM payments
        WHERE from_user_id = :userId
        OR to_user_id = :userId
        ORDER BY created_at DESC
    """)
    List<Payment> findByUser(@Bind("userId") UUID userId);
}