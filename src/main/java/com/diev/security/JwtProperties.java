package com.diev.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Setter
@Getter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret = "ssubench-dev-secret-change-me";
    private String issuer = "ssu-bench";
    private Duration expiration = Duration.ofHours(24);

}