package com.synergy.invoicedemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class UserAccountService {

    @Bean
    public UserDetailsService userDetailsService(UserAccountRepository repository) {
        return username -> repository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CommandLineRunner seedAdmin(UserAccountRepository repository, PasswordEncoder encoder) {
        return args -> {
            if (repository.findByUsername("admin").isEmpty()) {
                repository.save(new UserAccount("admin", encoder.encode(System.getenv().getOrDefault("APP_ADMIN_PASSWORD", "admin")), List.of("ADMIN")));
            }
        };
    }
}
