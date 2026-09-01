package com.synergy.invoicedemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("Application is running", System.currentTimeMillis());
    }

    public record HealthResponse(
        String status,
        long timestamp
    ) {}
}
