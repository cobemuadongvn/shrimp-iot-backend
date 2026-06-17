package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.ControlScenarioRequest;
import com.example.shrimpiot.dto.ControlScenarioResponse;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.ControlScenarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/control-scenarios")
public class ControlScenarioController {
    private final ControlScenarioService service;
    private final AuthService authService;

    public ControlScenarioController(ControlScenarioService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ControlScenarioResponse>> createScenario(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ControlScenarioRequest request
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        ControlScenarioResponse response = service.createScenario(request);
        return ResponseEntity.ok(ApiResponse.ok("Control scenario created", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ControlScenarioResponse>> updateScenario(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody ControlScenarioRequest request
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        ControlScenarioResponse response = service.updateScenario(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Control scenario updated", response));
    }

    @GetMapping("/pond/{pondId}")
    public ResponseEntity<ApiResponse<List<ControlScenarioResponse>>> getScenariosByPond(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pondId
    ) {
        authService.validateAccessToPond(authorization, pondId);
        List<ControlScenarioResponse> response = service.getScenariosByPond(pondId);
        return ResponseEntity.ok(ApiResponse.ok("Control scenarios retrieved", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ControlScenarioResponse>> getScenario(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        ControlScenarioResponse response = service.getScenario(id);
        authService.validateAccessToPond(authorization, response.getPondId());
        return ResponseEntity.ok(ApiResponse.ok("Control scenario retrieved", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteScenario(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        service.deleteScenario(id);
        return ResponseEntity.ok(ApiResponse.ok("Control scenario deleted", null));
    }
}
