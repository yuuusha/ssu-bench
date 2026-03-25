package com.diev.repo;

import com.diev.entity.Payment;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.time.LocalDateTime;
import java.util.UUID;

@RegisterBeanMapper(Payment.class)
public interface PaymentRepository {

    @SqlUpdate("""
        INSERT INTO payments (id, from_user, to_user, amount, created_at)
        VALUES (:id, :fromUserId, :toUserId, :amount, :createdAt)
    """)
    void create(
            @Bind("id") UUID id,
            @Bind("fromUserId") UUID fromUserId,
            @Bind("toUserId") UUID toUserId,
            @Bind("amount") Integer amount,
            @Bind("createdAt") LocalDateTime createdAt
    );

}