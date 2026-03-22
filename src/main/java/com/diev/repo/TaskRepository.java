package com.diev.repo;

import com.diev.entity.Task;
import com.diev.entity.TaskStatus;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(Task.class)
public interface TaskRepository {

    @SqlUpdate("""
        INSERT INTO tasks (id, title, description, reward, status, customer_id)
        VALUES (:id, :title, :description, :reward, :status, :customerId)
    """)
    void create(
            @Bind("id") UUID id,
            @Bind("title") String title,
            @Bind("description") String description,
            @Bind("reward") Integer reward,
            @Bind("status") String status,
            @Bind("customerId") UUID customerId
    );

    @SqlQuery("""
        SELECT *
        FROM tasks
        WHERE id = :id
    """)
    Optional<Task> findById(@Bind("id") UUID id);

    @SqlQuery("""
        SELECT *
        FROM tasks
        LIMIT :limit OFFSET :offset
    """)
    List<Task> findAll(
            @Bind("limit") int limit,
            @Bind("offset") int offset
    );

    @SqlUpdate("""
        UPDATE tasks
        SET status = :status
        WHERE id = :id
    """)
    void updateStatus(
            @Bind("id") UUID id,
            @Bind("status") TaskStatus status
    );

    @SqlUpdate("""
        UPDATE tasks
        SET title = :title,
            description = :description,
            reward = :reward,
            status = :status
        WHERE id = :id
    """)
    void update(
            @Bind("id") UUID id,
            @Bind("title") String title,
            @Bind("description") String description,
            @Bind("reward") Integer reward,
            @Bind("status") String status
    );

    @SqlUpdate("""
        UPDATE tasks
        SET selected_bid_id = :bidId,
            status = 'IN_PROGRESS'
        WHERE id = :taskId
    """)
    void assignBid(
            @Bind("taskId") UUID taskId,
            @Bind("bidId") UUID bidId
    );

    @SqlUpdate("""
        UPDATE tasks
        SET status = 'CANCELLED'
        WHERE id = :id
    """)
    void cancel(@Bind("id") UUID id);
}