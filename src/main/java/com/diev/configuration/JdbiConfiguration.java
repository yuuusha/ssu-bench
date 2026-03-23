package com.diev.configuration;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.repo.BidRepository;
import com.diev.repo.PaymentRepository;
import com.diev.repo.TaskRepository;
import com.diev.repo.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.jackson2.Jackson2Config;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Configuration
public class JdbiConfiguration {

//    @Bean
//    public Jdbi jdbi(DataSource dataSource) {
//        Jdbi jdbi = Jdbi.create(dataSource)
//                .installPlugin(new SqlObjectPlugin());
//
////        // Маппер для Role (enum)
////        jdbi.registerRowMapper(Role.class, (rs, ctx) -> Role.valueOf(rs.getString("role")));
////
////        // Маппер для User (record)
////        jdbi.registerRowMapper(User.class, new RowMapper<User>() {
////            @Override
////            public User map(ResultSet rs, StatementContext ctx) throws SQLException {
////                return new User(
////                        (UUID) rs.getObject("id"),
////                        rs.getString("email"),
////                        rs.getString("password"),
////                        Role.valueOf(rs.getString("role")),
////                        rs.getInt("balance"),
////                        rs.getBoolean("blocked")
////                );
////            }
////        });
//
//        jdbi.installPlugin(new SqlObjectPlugin());
//        jdbi.installPlugin(new Jackson2Plugin());
//        //jdbi.installPlugin(new Jsr310Plugin());
//
//        return jdbi;
//    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public Jdbi jdbi(DataSource dataSource, ObjectMapper objectMapper) {
        TransactionAwareDataSourceProxy proxy = new TransactionAwareDataSourceProxy(dataSource);
        Jdbi jdbi = Jdbi.create(proxy);
        jdbi.installPlugin(new PostgresPlugin());
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new Jackson2Plugin());
        jdbi.installPlugin(new JdbiPlugin() {
            @Override
            public void customizeJdbi(Jdbi jdbi) {
                jdbi.getConfig(Jackson2Config.class)
                        .setMapper(objectMapper.copy());
            }
        });
        return jdbi;
    }


    @Bean
    public UserRepository userRepository(Jdbi jdbi) {
        return jdbi.onDemand(UserRepository.class);
    }

    @Bean
    public TaskRepository taskRepository(Jdbi jdbi) {
        return jdbi.onDemand(TaskRepository.class);
    }

    @Bean
    public BidRepository bidRepository(Jdbi jdbi) {
        return jdbi.onDemand(BidRepository.class);
    }

    @Bean
    public PaymentRepository paymentRepository(Jdbi jdbi) {
        return jdbi.onDemand(PaymentRepository.class);
    }
}
