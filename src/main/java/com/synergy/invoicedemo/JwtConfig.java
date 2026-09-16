package com.synergy.invoicedemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtService jwtService(@Value("${app.jwt.secret:local-invoice-demo-secret-32-bytes-minimum!!}") String secret,
                                @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {
        return new JwtService(secret, expirationMs);
    }
}
