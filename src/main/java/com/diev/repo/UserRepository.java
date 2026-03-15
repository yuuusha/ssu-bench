package com.diev.repo;

import com.diev.entity.User;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

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

    @SqlUpdate("""
        INSERT INTO users (id, email, password, role, balance, blocked, created_at)
        VALUES (:id, :email, :password, :role, :balance, :blocked, NOW())
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
        SET balance = :balance
        WHERE id = :id
    """)
    void updateBalance(
            @Bind("id") UUID id,
            @Bind("balance") long balance
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