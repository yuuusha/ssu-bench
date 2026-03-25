package com.diev.configuration;

import com.diev.middleware.RequestContextFilter;
import lombok.NonNull;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebFilterConfiguration {
    @Bean
    public FilterRegistrationBean<@NonNull RequestContextFilter> requestContextFilterRegistration() {
        FilterRegistrationBean<@NonNull RequestContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}