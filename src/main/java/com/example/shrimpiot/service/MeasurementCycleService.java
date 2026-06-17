package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.MeasurementCycleRequest;
import com.example.shrimpiot.dto.MeasurementCycleResponse;
import com.example.shrimpiot.model.DeviceOperationConfig;
import com.example.shrimpiot.model.MeasurementCycle;
import com.example.shrimpiot.model.MeasurementCycleStatus;
import com.example.shrimpiot.model.RelayAction;
import com.example.shrimpiot.model.SensorReading;
import com.example.shrimpiot.repository.MeasurementCycleRepository;
import com.example.shrimpiot.repository.SensorReadingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MeasurementCycleService {
    private final MeasurementCycleRepository repository;
    private final DeviceOperationConfigService configService;
    private final CommandService commandService;
    private final SensorReadingRepository readingRepository;
    private final SalinityControlService salinityControlService;

    public MeasurementCycleService(
            MeasurementCycleRepository repository,
            DeviceOperationConfigService configService,
            CommandService commandService,
            SensorReadingRepository readingRepository,
            SalinityControlService salinityControlService
    ) {
        this.repository = repository;
        this.configService = configService;
        this.commandService = commandService;
        this.readingRepository = readingRepository;
        this.salinityControlService = salinityControlService;
    }

    public MeasurementCycleResponse start(MeasurementCycleRequest request, String startedBy) {
        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            throw new IllegalArgumentException("deviceId is required");
        }
        List<MeasurementCycleStatus> active = List.of(
                MeasurementCycleStatus.FILLING,
                MeasurementCycleStatus.STABILIZING,
                MeasurementCycleStatus.MEASURING,
                MeasurementCycleStatus.DRAINING
        );
        if (repository.existsByDeviceIdAndStatusIn(request.getDeviceId(), active)) {
            throw new IllegalArgumentException("A measurement cycle is already running for device: " + request.getDeviceId());
        }
        MeasurementCycle cycle = new MeasurementCycle();
        cycle.setDeviceId(request.getDeviceId());
        cycle.setSampleSource(request.getSampleSource() == null ? "Buồng đo trung tâm" : request.getSampleSource());
        cycle.setStartedBy(startedBy == null ? "APP" : startedBy);
        cycle.setStatus(MeasurementCycleStatus.FILLING);
        cycle.setMessage("Đang bơm nước vào buồng đo");
        MeasurementCycle saved = repository.save(cycle);
        runAsync(saved.getId());
        return new MeasurementCycleResponse(saved);
    }

    public MeasurementCycleResponse getCurrent(String deviceId) {
        return repository.findTopByDeviceIdOrderByStartedAtDesc(deviceId)
                .map(MeasurementCycleResponse::new)
                .orElseThrow(() -> new IllegalArgumentException("No measurement cycle found for device: " + deviceId));
    }

    public List<MeasurementCycleResponse> history(String deviceId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return repository.findByDeviceIdOrderByStartedAtDesc(deviceId, PageRequest.of(0, safeLimit)).stream()
                .map(MeasurementCycleResponse::new)
                .toList();
    }

    private void runAsync(Long cycleId) {
        Thread worker = new Thread(() -> executeCycle(cycleId), "measurement-cycle-" + cycleId);
        worker.setDaemon(true);
        worker.start();
    }

    private void executeCycle(Long cycleId) {
        try {
            MeasurementCycle cycle = repository.findById(cycleId).orElseThrow();
            DeviceOperationConfig config = configService.getOrCreate(cycle.getDeviceId());

            // Relay 1: bơm nước vào buồng đo. Chỉ chạy khi user bấm "Đo ngay" hoặc AI_AUTO tự đo lại.
            commandService.createAutoCommand(cycle.getDeviceId(), 1, RelayAction.ON, "MEASUREMENT_CYCLE:" + cycleId + " fill start");
            sleepSeconds(config.getFillDurationSeconds());
            commandService.createAutoCommand(cycle.getDeviceId(), 1, RelayAction.OFF, "MEASUREMENT_CYCLE:" + cycleId + " fill stop");

            update(cycleId, MeasurementCycleStatus.STABILIZING, "Đang chờ nước ổn định trước khi đo", null);
            sleepSeconds(config.getStabilizingSeconds());

            update(cycleId, MeasurementCycleStatus.MEASURING, "Đang chờ cảm biến gửi dữ liệu pH, nhiệt độ, DO và độ mặn", null);
            LocalDateTime readingWindowStart = LocalDateTime.now().minusSeconds(1);
            sleepSeconds(config.getMeasurementDurationSeconds());
            Optional<SensorReading> latestReading = latestReadingAfter(cycle.getDeviceId(), readingWindowStart, config.getReadingMaxAgeSeconds());

            update(cycleId, MeasurementCycleStatus.DRAINING, "Đang xả nước khỏi buồng đo", null);
            commandService.createAutoCommand(cycle.getDeviceId(), 2, RelayAction.ON, "MEASUREMENT_CYCLE:" + cycleId + " drain start");
            sleepSeconds(config.getMeasurementDrainDurationSeconds());
            commandService.createAutoCommand(cycle.getDeviceId(), 2, RelayAction.OFF, "MEASUREMENT_CYCLE:" + cycleId + " drain stop");

            if (latestReading.isEmpty()) {
                update(cycleId, MeasurementCycleStatus.ERROR,
                        "Không nhận được dữ liệu cảm biến mới trong chu kỳ đo. Kiểm tra Arduino/API /api/readings.", LocalDateTime.now());
                return;
            }

            SensorReading reading = latestReading.get();
            salinityControlService.startFromMeasurementIfEligible(reading, "AI_AUTO_AFTER_MEASURE_NOW:" + cycleId);
            update(cycleId, MeasurementCycleStatus.COMPLETED,
                    "Hoàn thành chu kỳ đo. Reading mới nhất ID=" + reading.getId()
                            + ", độ mặn=" + reading.getSalinity()
                            + ". Nếu đang AI_AUTO và độ mặn cao, backend đã bắt đầu chu trình xử lý tự động.",
                    LocalDateTime.now());
        } catch (Exception e) {
            try {
                MeasurementCycle cycle = repository.findById(cycleId).orElse(null);
                if (cycle != null) forceMeasurementPumpsOff(cycle.getDeviceId(), "Error in measurement cycle " + cycleId);
                update(cycleId, MeasurementCycleStatus.ERROR, "Chu kỳ đo lỗi: " + e.getMessage(), LocalDateTime.now());
            } catch (Exception ignored) {}
        }
    }

    private void update(Long cycleId, MeasurementCycleStatus status, String message, LocalDateTime completedAt) {
        MeasurementCycle cycle = repository.findById(cycleId).orElseThrow();
        cycle.setStatus(status);
        cycle.setMessage(message);
        if (completedAt != null) cycle.setCompletedAt(completedAt);
        repository.save(cycle);
    }

    private Optional<SensorReading> latestReadingAfter(String deviceId, LocalDateTime after, int maxAgeSeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliestAllowed = now.minusSeconds(maxAgeSeconds);
        LocalDateTime from = after.isAfter(earliestAllowed) ? after : earliestAllowed;
        return readingRepository.findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(deviceId, from, now).stream()
                .findFirst();
    }

    private void forceMeasurementPumpsOff(String deviceId, String reason) {
        try { commandService.createAutoCommand(deviceId, 1, RelayAction.OFF, reason + " - relay 1 OFF"); } catch (Exception ignored) {}
        try { commandService.createAutoCommand(deviceId, 2, RelayAction.OFF, reason + " - relay 2 OFF"); } catch (Exception ignored) {}
    }

    private void sleepSeconds(Integer seconds) throws InterruptedException {
        Thread.sleep(Math.max(1, seconds == null ? 1 : seconds) * 1000L);
    }
}
