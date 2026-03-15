package com.diev.repo;

import com.diev.entity.Bid;
import com.diev.entity.BidStatus;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(Bid.class)
public interface BidRepository {

    @SqlUpdate("""
        INSERT INTO bids (id, task_id, executor_id, status, created_at)
        VALUES (:id, :taskId, :executorId, :status, NOW())
    """)
    void create(
            @Bind("id") UUID id,
            @Bind("taskId") UUID taskId,
            @Bind("executorId") UUID executorId,
            @Bind("status") String status
    );

    @SqlQuery("""
        SELECT *
        FROM bids
        WHERE id = :id
    """)
    Optional<Bid> findById(@Bind("id") UUID id);

    @SqlQuery("""
        SELECT *
        FROM bids
        WHERE task_id = :taskId
        ORDER BY created_at
    """)
    List<Bid> findByTask(@Bind("taskId") UUID taskId);

    @SqlUpdate("""
        UPDATE bids
        SET status = :status
        WHERE id = :id
    """)
    void updateStatus(
            @Bind("id") UUID id,
            @Bind("status") BidStatus status
    );

    @SqlQuery("""
        SELECT *
        FROM bids
        WHERE task_id = :taskId
        AND status = 'ACCEPTED'
        LIMIT 1
    """)
    Optional<Bid> findSelectedBid(@Bind("taskId") UUID taskId);

    @SqlUpdate("""
    UPDATE bids
    SET status = 'REJECTED'
    WHERE task_id = :taskId
    AND id <> :acceptedBidId
""")
    void rejectOtherBids(
            @Bind("taskId") UUID taskId,
            @Bind("acceptedBidId") UUID acceptedBidId
    );
}