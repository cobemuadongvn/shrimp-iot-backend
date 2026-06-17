package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Backend is running",
                Map.of("service", "shrimp-iot-backend", "status", "UP")
        ));
    }
}
