package com.synergy.invoicedemo;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthenticationServiceTest {

    @Test
    void shouldCreateJwtForAdminUser() {
        JwtService jwtService = new JwtService("invoice-demo-secret-key-change-me", 3600000L);

        String token = jwtService.generateToken(
            User.withUsername("admin")
                .password("admin")
                .roles("ADMIN")
                .build()
        );

        assertNotNull(token);
        assertEquals("admin", jwtService.extractUsername(token));
    }
}
