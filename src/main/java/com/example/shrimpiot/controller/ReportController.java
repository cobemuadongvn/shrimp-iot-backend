package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.ReportSummaryResponse;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final AuthService authService;
    private final ReportService reportService;

    public ReportController(AuthService authService, ReportService reportService) {
        this.authService = authService;
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Report summary", reportService.buildSummary(deviceId, from, to)));
    }

    @GetMapping(value = "/sensors.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportSensorsCsv(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return csv("sensor-readings-" + deviceId + ".csv", reportService.buildSensorCsv(deviceId, from, to));
    }

    @GetMapping(value = "/alerts.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportAlertsCsv(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return csv("alerts-" + deviceId + ".csv", reportService.buildAlertCsv(deviceId, from, to));
    }

    @GetMapping(value = "/commands.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportCommandsCsv(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return csv("commands-" + deviceId + ".csv", reportService.buildCommandCsv(deviceId, from, to));
    }

    private ResponseEntity<byte[]> csv(String filename, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }
}
