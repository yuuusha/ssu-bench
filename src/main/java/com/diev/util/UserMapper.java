//package com.diev.util;
//
//import com.diev.entity.Role;
//import com.diev.entity.User;
//import org.jdbi.v3.core.mapper.RowMapper;
//import org.jdbi.v3.core.statement.StatementContext;
//
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.UUID;
//
//public class UserMapper implements RowMapper<User> {
//    @Override
//    public User map(ResultSet rs, StatementContext ctx) throws SQLException {
//        return new User(
//                (UUID) rs.getObject("id"),
//                rs.getString("email"),
//                rs.getString("password"),
//                Role.valueOf(rs.getString("role")),
//                rs.getInt("balance"),
//                rs.getBoolean("blocked")
//        );
//    }
//}