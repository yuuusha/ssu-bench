package com.diev.configuration;

import com.diev.repo.BidRepository;
import com.diev.repo.PaymentRepository;
import com.diev.repo.TaskRepository;
import com.diev.repo.UserRepository;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class JdbiConfiguration {

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        return Jdbi.create(dataSource)
                .installPlugin(new SqlObjectPlugin());
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
