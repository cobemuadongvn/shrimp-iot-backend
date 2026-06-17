package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.MetricStatsResponse;
import com.example.shrimpiot.dto.ReportSummaryResponse;
import com.example.shrimpiot.model.Alert;
import com.example.shrimpiot.model.AlertStatus;
import com.example.shrimpiot.model.DeviceCommand;
import com.example.shrimpiot.model.SensorReading;
import com.example.shrimpiot.repository.AlertRepository;
import com.example.shrimpiot.repository.DeviceCommandRepository;
import com.example.shrimpiot.repository.SensorReadingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final SensorReadingRepository readingRepository;
    private final AlertRepository alertRepository;
    private final DeviceCommandRepository commandRepository;

    @Value("${report.max-export-rows:5000}")
    private int maxExportRows;

    public ReportService(SensorReadingRepository readingRepository,
                         AlertRepository alertRepository,
                         DeviceCommandRepository commandRepository) {
        this.readingRepository = readingRepository;
        this.alertRepository = alertRepository;
        this.commandRepository = commandRepository;
    }

    public ReportSummaryResponse buildSummary(String deviceId, LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        List<SensorReading> readings = readingRepository.findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtAsc(deviceId, from, to);
        List<Alert> alerts = alertRepository.findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(deviceId, from, to);
        List<DeviceCommand> commands = commandRepository.findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(deviceId, from, to);

        SensorReading latest = readings.stream().max(Comparator.comparing(SensorReading::getCreatedAt)).orElse(null);

        List<MetricStatsResponse> metrics = List.of(
                stats("temperature", readings, SensorReading::getTemperature, latest == null ? null : latest.getTemperature()),
                stats("ph", readings, SensorReading::getPh, latest == null ? null : latest.getPh()),
                stats("ecValue", readings, SensorReading::getEcValue, latest == null ? null : latest.getEcValue()),
                stats("salinity", readings, SensorReading::getSalinity, latest == null ? null : latest.getSalinity()),
                stats("doValue", readings, SensorReading::getDoValue, latest == null ? null : latest.getDoValue())
        );

        Map<String, Long> alertsByType = alerts.stream()
                .collect(Collectors.groupingBy(a -> a.getAlertType().name(), Collectors.counting()));

        Map<String, Long> commandsByRelay = commands.stream()
                .collect(Collectors.groupingBy(c -> "relay_" + c.getRelayNo(), Collectors.counting()));

        long openAlertCount = alerts.stream().filter(a -> a.getStatus() == AlertStatus.OPEN).count();
        long resolvedAlertCount = alerts.stream().filter(a -> a.getStatus() == AlertStatus.RESOLVED).count();
        long manualCommandCount = commands.stream().filter(c -> "MANUAL".equalsIgnoreCase(c.getSource())).count();
        long autoCommandCount = commands.stream().filter(c -> "AUTO".equalsIgnoreCase(c.getSource())).count();

        return new ReportSummaryResponse(
                deviceId, from, to,
                readings.size(), alerts.size(), openAlertCount, resolvedAlertCount,
                commands.size(), manualCommandCount, autoCommandCount,
                metrics, alertsByType, commandsByRelay
        );
    }

    public String buildSensorCsv(String deviceId, LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        List<SensorReading> readings = limit(readingRepository.findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtAsc(deviceId, from, to));
        StringBuilder sb = new StringBuilder("id,deviceId,createdAt,temperature,ph,ecValue,salinity,doValue,status,message\n");
        for (SensorReading r : readings) {
            sb.append(r.getId()).append(',')
                    .append(csv(r.getDeviceId())).append(',')
                    .append(r.getCreatedAt()).append(',')
                    .append(value(r.getTemperature())).append(',')
                    .append(value(r.getPh())).append(',')
                    .append(value(r.getEcValue())).append(',')
                    .append(value(r.getSalinity())).append(',')
                    .append(value(r.getDoValue())).append(',')
                    .append(r.getStatus()).append(',')
                    .append(csv(r.getMessage())).append('\n');
        }
        return sb.toString();
    }

    public String buildAlertCsv(String deviceId, LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        List<Alert> alerts = limit(alertRepository.findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(deviceId, from, to));
        StringBuilder sb = new StringBuilder("id,deviceId,createdAt,alertType,severity,status,message,resolvedAt,resolvedBy\n");
        for (Alert a : alerts) {
            sb.append(a.getId()).append(',')
                    .append(csv(a.getDeviceId())).append(',')
                    .append(a.getCreatedAt()).append(',')
                    .append(a.getAlertType()).append(',')
                    .append(a.getSeverity()).append(',')
                    .append(a.getStatus()).append(',')
                    .append(csv(a.getMessage())).append(',')
                    .append(a.getResolvedAt() == null ? "" : a.getResolvedAt()).append(',')
                    .append(csv(a.getResolvedBy())).append('\n');
        }
        return sb.toString();
    }

    public String buildCommandCsv(String deviceId, LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        List<DeviceCommand> commands = limit(commandRepository.findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(deviceId, from, to));
        StringBuilder sb = new StringBuilder("id,deviceId,createdAt,relayNo,action,status,source,requestedBy,message,sentAt,ackAt\n");
        for (DeviceCommand c : commands) {
            sb.append(c.getId()).append(',')
                    .append(csv(c.getDeviceId())).append(',')
                    .append(c.getCreatedAt()).append(',')
                    .append(c.getRelayNo()).append(',')
                    .append(c.getAction()).append(',')
                    .append(c.getStatus()).append(',')
                    .append(csv(c.getSource())).append(',')
                    .append(csv(c.getRequestedBy())).append(',')
                    .append(csv(c.getMessage())).append(',')
                    .append(c.getSentAt() == null ? "" : c.getSentAt()).append(',')
                    .append(c.getAckAt() == null ? "" : c.getAckAt()).append('\n');
        }
        return sb.toString();
    }

    private MetricStatsResponse stats(String name, List<SensorReading> readings, Function<SensorReading, Double> extractor, Double latest) {
        List<Double> values = readings.stream().map(extractor).filter(Objects::nonNull).toList();
        if (values.isEmpty()) return new MetricStatsResponse(name, null, null, null, latest);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return new MetricStatsResponse(name, min, max, avg, latest);
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) throw new IllegalArgumentException("from and to are required");
        if (to.isBefore(from)) throw new IllegalArgumentException("to must be after from");
    }

    private <T> List<T> limit(List<T> data) {
        int safeLimit = Math.max(1, maxExportRows);
        if (data.size() <= safeLimit) return data;
        return data.subList(0, safeLimit);
    }

    private String value(Double value) { return value == null ? "" : value.toString(); }

    private String csv(String value) {
        if (value == null) return "";
        return '"' + value.replace("\"", "\"\"").replace("\n", " ") + '"';
    }
}
