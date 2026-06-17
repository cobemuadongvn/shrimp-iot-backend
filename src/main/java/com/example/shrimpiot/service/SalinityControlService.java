package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.SalinityCorrectionRequest;
import com.example.shrimpiot.dto.SalinityCorrectionResponse;
import com.example.shrimpiot.model.DeviceOperationConfig;
import com.example.shrimpiot.model.OperationMode;
import com.example.shrimpiot.model.RelayAction;
import com.example.shrimpiot.model.SalinityCorrectionCycle;
import com.example.shrimpiot.model.SalinityCorrectionStatus;
import com.example.shrimpiot.model.SensorReading;
import com.example.shrimpiot.repository.SalinityCorrectionCycleRepository;
import com.example.shrimpiot.repository.SensorReadingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SalinityControlService {
    private final SalinityCorrectionCycleRepository repository;
    private final SensorReadingRepository readingRepository;
    private final DeviceOperationConfigService configService;
    private final CommandService commandService;

    public SalinityControlService(
            SalinityCorrectionCycleRepository repository,
            SensorReadingRepository readingRepository,
            DeviceOperationConfigService configService,
            CommandService commandService
    ) {
        this.repository = repository;
        this.readingRepository = readingRepository;
        this.configService = configService;
        this.commandService = commandService;
    }

    /**
     * Giữ lại để tương thích với code cũ. Bản mới KHÔNG tự xử lý độ mặn chỉ vì Arduino gửi reading.
     * AI_AUTO chỉ bắt đầu sau chu kỳ "Đo ngay" hoặc sau lần tự đo lại trong correction cycle.
     */
    public void handleReading(SensorReading reading) {
        // Intentionally no-op.
    }

    /**
     * Được gọi sau khi người dùng bấm "Đo ngay" và measurement cycle đã lấy được reading hợp lệ.
     * MANUAL: chỉ cảnh báo, không chạy bơm 3/4.
     * AI_AUTO: nếu độ mặn cao và đủ điều kiện an toàn, tự chạy chu trình xử lý + tự đo lại theo phương án B.
     */
    public Optional<SalinityCorrectionResponse> startFromMeasurementIfEligible(SensorReading reading, String triggeredBy) {
        if (reading == null || reading.getDeviceId() == null || reading.getSalinity() == null) return Optional.empty();

        DeviceOperationConfig config = configService.getOrCreate(reading.getDeviceId());
        if (config.getOperationMode() != OperationMode.AI_AUTO || !Boolean.TRUE.equals(config.getSalinityAutoEnabled())) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(config.getSafetyLockEnabled())) {
            return Optional.empty();
        }
        if (!isFreshReading(reading, config)) {
            return Optional.empty();
        }
        if (reading.getSalinity() <= config.getSalinityHighThreshold()) {
            return Optional.empty();
        }
        if (!canStartCycle(reading.getDeviceId(), config)) {
            return Optional.empty();
        }

        SalinityCorrectionCycle cycle = createCycle(reading.getDeviceId(), reading.getSalinity(), config,
                triggeredBy == null ? "SYSTEM_AI_AUTO_MEASURE_NOW" : triggeredBy);
        runAsync(cycle.getId());
        return Optional.of(new SalinityCorrectionResponse(cycle));
    }

    public SalinityCorrectionResponse startManualOrAdmin(SalinityCorrectionRequest request, String triggeredBy) {
        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            throw new IllegalArgumentException("deviceId is required");
        }
        DeviceOperationConfig config = configService.getOrCreate(request.getDeviceId());
        if (Boolean.TRUE.equals(config.getSafetyLockEnabled())) {
            throw new IllegalArgumentException("Safety lock is enabled for device: " + request.getDeviceId());
        }
        if (!canStartCycle(request.getDeviceId(), config)) {
            throw new IllegalArgumentException("A salinity correction cycle is already running or cooldown is active for device: " + request.getDeviceId());
        }

        Double currentSalinity = request.getCurrentSalinity();
        if (currentSalinity == null) {
            currentSalinity = readingRepository.findTopByDeviceIdOrderByCreatedAtDesc(request.getDeviceId())
                    .map(SensorReading::getSalinity)
                    .orElse(null);
        }
        if (currentSalinity == null) {
            throw new IllegalArgumentException("currentSalinity is required when no latest reading exists");
        }

        SalinityCorrectionCycle cycle = createCycle(request.getDeviceId(), currentSalinity, config, triggeredBy == null ? "APP" : triggeredBy);
        runAsync(cycle.getId());
        return new SalinityCorrectionResponse(cycle);
    }

    public SalinityCorrectionResponse getCurrent(String deviceId) {
        return repository.findTopByDeviceIdOrderByStartedAtDesc(deviceId)
                .map(SalinityCorrectionResponse::new)
                .orElseThrow(() -> new IllegalArgumentException("No salinity correction cycle found for device: " + deviceId));
    }

    public List<SalinityCorrectionResponse> history(String deviceId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return repository.findByDeviceIdOrderByStartedAtDesc(deviceId, PageRequest.of(0, safeLimit)).stream()
                .map(SalinityCorrectionResponse::new)
                .toList();
    }

    private SalinityCorrectionCycle createCycle(String deviceId, Double salinity, DeviceOperationConfig config, String triggeredBy) {
        SalinityCorrectionCycle cycle = new SalinityCorrectionCycle();
        cycle.setDeviceId(deviceId);
        cycle.setStartSalinity(salinity);
        cycle.setLatestSalinity(salinity);
        cycle.setTargetSalinity(config.getSalinityStopThreshold());
        cycle.setMaxRetryCount(config.getMaxRetryCount());
        cycle.setRetryCount(0);
        cycle.setStatus(SalinityCorrectionStatus.SALINITY_HIGH_DETECTED);
        cycle.setMessage("Phát hiện độ mặn cao. AI_AUTO chuẩn bị xả bớt nước ao, bơm nước ngọt và tự đo lại.");
        cycle.setTriggeredBy(triggeredBy);
        return repository.save(cycle);
    }

    private boolean canStartCycle(String deviceId, DeviceOperationConfig config) {
        if (hasActiveCycle(deviceId)) return false;
        LocalDateTime cooldownStart = LocalDateTime.now().minusMinutes(config.getCooldownMinutes());
        return !repository.existsByDeviceIdAndStartedAtAfter(deviceId, cooldownStart);
    }

    private boolean hasActiveCycle(String deviceId) {
        return repository.existsByDeviceIdAndStatusIn(deviceId, List.of(
                SalinityCorrectionStatus.SALINITY_HIGH_DETECTED,
                SalinityCorrectionStatus.DRAINING_SALTY_WATER,
                SalinityCorrectionStatus.ADDING_FRESH_WATER,
                SalinityCorrectionStatus.WAITING_MIXING,
                SalinityCorrectionStatus.RECHECKING
        ));
    }

    private void runAsync(Long cycleId) {
        Thread worker = new Thread(() -> executeCycle(cycleId), "salinity-correction-" + cycleId);
        worker.setDaemon(true);
        worker.start();
    }

    private void executeCycle(Long cycleId) {
        try {
            while (true) {
                SalinityCorrectionCycle cycle = repository.findById(cycleId).orElseThrow();
                DeviceOperationConfig config = configService.getOrCreate(cycle.getDeviceId());

                int retry = cycle.getRetryCount() == null ? 0 : cycle.getRetryCount();
                if (Boolean.TRUE.equals(config.getSafetyLockEnabled())) {
                    forceAllPumpsOff(cycle.getDeviceId(), "Safety lock enabled during salinity correction cycle " + cycleId);
                    update(cycleId, SalinityCorrectionStatus.NEED_MANUAL_CHECK,
                            "Khóa an toàn đang bật. Dừng AI_AUTO và yêu cầu kiểm tra thủ công.",
                            LocalDateTime.now(), null, retry);
                    return;
                }
                if (retry >= config.getMaxRetryCount()) {
                    forceAllPumpsOff(cycle.getDeviceId(), "Max retry reached in salinity correction cycle " + cycleId);
                    update(cycleId, SalinityCorrectionStatus.NEED_MANUAL_CHECK,
                            "Độ mặn vẫn chưa về ngưỡng dừng sau số lần xử lý tối đa. Cần người vận hành kiểm tra thủ công.",
                            LocalDateTime.now(), null, retry);
                    return;
                }

                update(cycleId, SalinityCorrectionStatus.DRAINING_SALTY_WATER,
                        "AI_AUTO: bật bơm 3 để xả bớt nước ao có độ mặn cao", null, null, retry);
                commandService.createAutoCommand(cycle.getDeviceId(), 3, RelayAction.ON, "SALINITY_CORRECTION:" + cycleId + " drain pond water start");
                sleepSeconds(config.getSalinityDrainDurationSeconds());
                commandService.createAutoCommand(cycle.getDeviceId(), 3, RelayAction.OFF, "SALINITY_CORRECTION:" + cycleId + " drain pond water stop");

                update(cycleId, SalinityCorrectionStatus.ADDING_FRESH_WATER,
                        "AI_AUTO: bật bơm 4 để cấp nước ngọt pha loãng độ mặn", null, null, retry);
                commandService.createAutoCommand(cycle.getDeviceId(), 4, RelayAction.ON, "SALINITY_CORRECTION:" + cycleId + " add freshwater start");
                sleepSeconds(config.getFreshwaterDurationSeconds());
                commandService.createAutoCommand(cycle.getDeviceId(), 4, RelayAction.OFF, "SALINITY_CORRECTION:" + cycleId + " add freshwater stop");

                update(cycleId, SalinityCorrectionStatus.WAITING_MIXING,
                        "AI_AUTO: chờ nước ao hòa trộn trước khi tự đo lại", null, null, retry);
                sleepSeconds(config.getMixingWaitSeconds());

                if (!Boolean.TRUE.equals(config.getAutoRemeasureEnabled())) {
                    update(cycleId, SalinityCorrectionStatus.NEED_MANUAL_CHECK,
                            "Đã xử lý bằng bơm 3/4 nhưng autoRemeasureEnabled=false. Cần người dùng bấm Đo ngay để kiểm tra lại.",
                            LocalDateTime.now(), null, retry + 1);
                    return;
                }

                update(cycleId, SalinityCorrectionStatus.RECHECKING,
                        "AI_AUTO: tự chạy bơm 1/2 để đo lại độ mặn sau xử lý", null, null, retry);
                Optional<SensorReading> latestReading = performReMeasurement(cycle.getDeviceId(), config, cycleId);
                if (latestReading.isEmpty() || latestReading.get().getSalinity() == null) {
                    update(cycleId, SalinityCorrectionStatus.NEED_MANUAL_CHECK,
                            "Không nhận được dữ liệu độ mặn mới sau khi tự đo lại. Dừng AI_AUTO và yêu cầu kiểm tra thủ công.",
                            LocalDateTime.now(), null, retry + 1);
                    return;
                }

                Double latestSalinity = latestReading.get().getSalinity();
                if (latestSalinity <= config.getSalinityStopThreshold()) {
                    update(cycleId, SalinityCorrectionStatus.COMPLETED,
                            "Độ mặn đã về ngưỡng dừng an toàn. Hoàn thành chu trình AI_AUTO.",
                            LocalDateTime.now(), latestSalinity, retry + 1);
                    return;
                }

                update(cycleId, SalinityCorrectionStatus.SALINITY_HIGH_DETECTED,
                        "Độ mặn sau đo lại vẫn trên ngưỡng dừng. AI_AUTO sẽ lặp lại nếu chưa vượt số lần tối đa.",
                        null, latestSalinity, retry + 1);
            }
        } catch (Exception e) {
            try {
                SalinityCorrectionCycle cycle = repository.findById(cycleId).orElse(null);
                if (cycle != null) forceAllPumpsOff(cycle.getDeviceId(), "Error in salinity correction cycle " + cycleId);
                update(cycleId, SalinityCorrectionStatus.ERROR,
                        "Chu trình xử lý độ mặn lỗi: " + e.getMessage(), LocalDateTime.now(), null, null);
            } catch (Exception ignored) {}
        }
    }

    private void update(Long cycleId, SalinityCorrectionStatus status, String message, LocalDateTime completedAt, Double latestSalinity, Integer retryCount) {
        SalinityCorrectionCycle cycle = repository.findById(cycleId).orElseThrow();
        cycle.setStatus(status);
        cycle.setMessage(message);
        if (completedAt != null) cycle.setCompletedAt(completedAt);
        if (latestSalinity != null) cycle.setLatestSalinity(latestSalinity);
        if (retryCount != null) cycle.setRetryCount(retryCount);
        repository.save(cycle);
    }

    private Optional<SensorReading> performReMeasurement(String deviceId, DeviceOperationConfig config, Long cycleId) throws InterruptedException {
        commandService.createAutoCommand(deviceId, 1, RelayAction.ON, "SALINITY_CORRECTION:" + cycleId + " auto re-measure fill start");
        sleepSeconds(config.getFillDurationSeconds());
        commandService.createAutoCommand(deviceId, 1, RelayAction.OFF, "SALINITY_CORRECTION:" + cycleId + " auto re-measure fill stop");

        sleepSeconds(config.getStabilizingSeconds());
        LocalDateTime readingWindowStart = LocalDateTime.now().minusSeconds(1);
        sleepSeconds(config.getMeasurementDurationSeconds());
        Optional<SensorReading> reading = latestReadingAfter(deviceId, readingWindowStart, config.getReadingMaxAgeSeconds());

        commandService.createAutoCommand(deviceId, 2, RelayAction.ON, "SALINITY_CORRECTION:" + cycleId + " auto re-measure drain start");
        sleepSeconds(config.getMeasurementDrainDurationSeconds());
        commandService.createAutoCommand(deviceId, 2, RelayAction.OFF, "SALINITY_CORRECTION:" + cycleId + " auto re-measure drain stop");
        return reading;
    }

    private Optional<SensorReading> latestReadingAfter(String deviceId, LocalDateTime after, int maxAgeSeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliestAllowed = now.minusSeconds(maxAgeSeconds);
        LocalDateTime from = after.isAfter(earliestAllowed) ? after : earliestAllowed;
        return readingRepository.findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(deviceId, from, now).stream()
                .filter(r -> r.getSalinity() != null)
                .findFirst();
    }

    private boolean isFreshReading(SensorReading reading, DeviceOperationConfig config) {
        if (reading.getCreatedAt() == null) return false;
        return reading.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(config.getReadingMaxAgeSeconds()));
    }

    private void forceAllPumpsOff(String deviceId, String reason) {
        try { commandService.createAutoCommand(deviceId, 1, RelayAction.OFF, reason + " - relay 1 OFF"); } catch (Exception ignored) {}
        try { commandService.createAutoCommand(deviceId, 2, RelayAction.OFF, reason + " - relay 2 OFF"); } catch (Exception ignored) {}
        try { commandService.createAutoCommand(deviceId, 3, RelayAction.OFF, reason + " - relay 3 OFF"); } catch (Exception ignored) {}
        try { commandService.createAutoCommand(deviceId, 4, RelayAction.OFF, reason + " - relay 4 OFF"); } catch (Exception ignored) {}
    }

    private void sleepSeconds(Integer seconds) throws InterruptedException {
        Thread.sleep(Math.max(1, seconds == null ? 1 : seconds) * 1000L);
    }
}
