package com.diev.repo;

import com.diev.entity.User;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(User.class)
public interface UserRepository {

    @SqlQuery("""
        SELECT *
        FROM users
        WHERE id = :id
    """)
    Optional<User> findById(@Bind("id") UUID id);

    @SqlQuery("""
        SELECT *
        FROM users
        WHERE email = :email
    """)
    Optional<User> findByEmail(@Bind("email") String email);

    @SqlQuery("""
        SELECT *
        FROM users
        LIMIT :limit OFFSET :offset
    """)
    List<User> findAll(
            @Bind("limit") int limit,
            @Bind("offset") int offset
    );

    @SqlUpdate("""
        INSERT INTO users (id, email, password, role, balance, blocked)
        VALUES (:id, :email, :password, :role, :balance, :blocked)
    """)
    void create(
            @Bind("id") UUID id,
            @Bind("email") String email,
            @Bind("password") String password,
            @Bind("role") String role,
            @Bind("balance") long balance,
            @Bind("blocked") boolean blocked
    );

    @SqlUpdate("""
        UPDATE users
        SET email = :email,
            password = :passwordHash,
            role = :role,
            balance = :balance
        WHERE id = :id
    """)
    void update(
            @Bind("id") UUID id,
            @Bind("email") String email,
            @Bind("passwordHash") String passwordHash,
            @Bind("role") String role,
            @Bind("balance") long balance
    );

    @SqlUpdate("""
        DELETE FROM users
        WHERE id = :id
    """)
    void delete(@Bind("id") UUID id);

    @SqlUpdate("""
        UPDATE users
        SET balance = :balance
        WHERE id = :id
    """)
    void updateBalance(
            @Bind("id") UUID id,
            @Bind("balance") long balance
    );

    @SqlUpdate("""
        UPDATE users
        SET balance = balance - :amount
        WHERE id = :id
          AND balance >= :amount
    """)
    int decreaseBalanceIfEnough(
            @Bind("id") UUID id,
            @Bind("amount") long amount
    );

    @SqlUpdate("""
        UPDATE users
        SET balance = balance + :amount
        WHERE id = :id
    """)
    void increaseBalance(
            @Bind("id") UUID id,
            @Bind("amount") long amount
    );

    @SqlUpdate("""
        UPDATE users
        SET blocked = true
        WHERE id = :id
    """)
    void block(@Bind("id") UUID id);

    @SqlUpdate("""
        UPDATE users
        SET blocked = false
        WHERE id = :id
    """)
    void unblock(@Bind("id") UUID id);
}