package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return live();
    }

    @GetMapping("/health/live")
    public ResponseEntity<ApiResponse<Map<String, String>>> live() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Backend is running",
                Map.of(
                        "service", "shrimp-iot-backend",
                        "status", "UP",
                        "check", "liveness"
                )
        ));
    }

    @GetMapping("/health/ready")
    public ResponseEntity<ApiResponse<Map<String, String>>> ready() {
        try {
            Integer databaseResult = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (databaseResult != null && databaseResult == 1) {
                return ResponseEntity.ok(ApiResponse.ok(
                        "Backend is ready",
                        Map.of(
                                "service", "shrimp-iot-backend",
                                "status", "UP",
                                "database", "UP",
                                "check", "readiness"
                        )
                ));
            }
        } catch (RuntimeException ignored) {
            // Do not expose database connection details in a public health endpoint.
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(
                        false,
                        "Backend is not ready",
                        Map.of(
                                "service", "shrimp-iot-backend",
                                "status", "DOWN",
                                "database", "DOWN",
                                "check", "readiness"
                        )
                ));
    }
}
