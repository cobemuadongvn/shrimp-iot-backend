package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.NotificationResponse;
import com.example.shrimpiot.model.Alert;
import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.NotificationLog;
import com.example.shrimpiot.model.Pond;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.model.UserPondAccess;
import com.example.shrimpiot.repository.DeviceRepository;
import com.example.shrimpiot.repository.NotificationLogRepository;
import com.example.shrimpiot.repository.UserAccountRepository;
import com.example.shrimpiot.repository.UserPondAccessRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class NotificationService {
    private static final List<String> DISPATCH_STATUSES = List.of("CREATED", "SENT", "SKIPPED_DISABLED", "SKIPPED_NOT_CONFIGURED", "SKIPPED_NO_RECIPIENT");

    private final NotificationLogRepository repository;
    private final DeviceRepository deviceRepository;
    private final UserPondAccessRepository accessRepository;
    private final UserAccountRepository userRepository;
    private final WebSocketEventService webSocketEventService;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Value("${notification.app-enabled:true}") private boolean appEnabled;
    @Value("${notification.sms-enabled:false}") private boolean smsEnabled;
    @Value("${notification.email-enabled:false}") private boolean emailEnabled;
    @Value("${notification.sms-webhook-url:}") private String smsWebhookUrl;
    @Value("${notification.email-webhook-url:}") private String emailWebhookUrl;
    @Value("${notification.default-sms-recipient:}") private String defaultSmsRecipient;
    @Value("${notification.default-email-recipient:}") private String defaultEmailRecipient;
    @Value("${notification.anti-spam-enabled:true}") private boolean antiSpamEnabled;
    @Value("${notification.cooldown-minutes:30}") private long cooldownMinutes;

    public NotificationService(NotificationLogRepository repository,
                               DeviceRepository deviceRepository,
                               UserPondAccessRepository accessRepository,
                               UserAccountRepository userRepository,
                               WebSocketEventService webSocketEventService) {
        this.repository = repository;
        this.deviceRepository = deviceRepository;
        this.accessRepository = accessRepository;
        this.userRepository = userRepository;
        this.webSocketEventService = webSocketEventService;
    }

    /** Backward-compatible method used by older code paths and manual notification tests. */
    public void notifyAppAndMockSmsEmail(String deviceId, String message) {
        notifyAlert(deviceId, message);
    }

    /** Backward-compatible method used by manual notification tests. Anti-spam is not applied to manual tests. */
    public void notifyAlert(String deviceId, String message) {
        notifyInternal(deviceId, "MANUAL:" + System.currentTimeMillis(), null, null, message, false);
    }

    /**
     * Sends alert notifications with anti-spam based on the alert lifecycle.
     * eventKey contains alertId; therefore a resolved alert that becomes bad again can notify immediately.
     */
    public void notifyAlert(Alert alert) {
        if (alert == null) return;
        String eventKey = buildAlertEventKey(alert);
        if (shouldSuppressByCooldown(eventKey)) {
            logSuppressedCooldown(alert, eventKey);
            return;
        }
        notifyInternal(
                alert.getDeviceId(),
                eventKey,
                alert.getAlertType() == null ? null : alert.getAlertType().name(),
                alert.getSeverity() == null ? null : alert.getSeverity().name(),
                alert.getMessage(),
                true
        );
    }

    /** Log that a sensor reading still violates an already-open alert but notification is under cooldown. */
    public void logSuppressedCooldown(Alert alert, String latestMessage) {
        if (alert == null) return;
        String eventKey = buildAlertEventKey(alert);
        LocalDateTime cooldownUntil = findLastDispatch(eventKey)
                .map(log -> log.getCreatedAt().plusMinutes(Math.max(1, cooldownMinutes)))
                .orElse(LocalDateTime.now().plusMinutes(Math.max(1, cooldownMinutes)));
        createLog(
                alert.getDeviceId(),
                eventKey,
                alert.getAlertType() == null ? null : alert.getAlertType().name(),
                alert.getSeverity() == null ? null : alert.getSeverity().name(),
                "SYSTEM",
                "anti-spam",
                null,
                null,
                latestMessage == null || latestMessage.isBlank() ? alert.getMessage() : latestMessage,
                "SUPPRESSED_COOLDOWN",
                "Existing alert is still OPEN; repeated notification suppressed until " + cooldownUntil,
                true,
                "Thông số vẫn xấu nhưng chưa hết thời gian chờ chống spam",
                cooldownUntil
        );
    }

    public List<NotificationLog> getByDeviceId(String deviceId) {
        return repository.findByDeviceIdOrderByCreatedAtDesc(deviceId);
    }

    public List<NotificationResponse> getInAppNotifications(UserAccount user, String deviceId, boolean unreadOnly, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return repository.findInAppForUserAndDevice(deviceId, user.getId(), unreadOnly, PageRequest.of(0, safeLimit))
                .stream()
                .map(NotificationResponse::new)
                .toList();
    }

    public long countUnreadInApp(UserAccount user, String deviceId) {
        return repository.countUnreadInAppForUserAndDevice(deviceId, user.getId());
    }

    @Transactional
    public NotificationResponse markInAppAsRead(UserAccount user, Long notificationId) {
        NotificationLog log = repository.findVisibleInAppById(notificationId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("In-app notification not found or access denied: " + notificationId));
        if (!Boolean.TRUE.equals(log.getRead())) {
            log.setRead(true);
            log.setReadAt(LocalDateTime.now());
            log.setReadBy(user.getUsername());
            log = repository.save(log);
        }
        return new NotificationResponse(log);
    }

    @Transactional
    public int markAllInAppAsRead(UserAccount user, String deviceId) {
        return repository.markAllInAppReadForUserAndDevice(deviceId, user.getId(), LocalDateTime.now(), user.getUsername());
    }

    private boolean shouldSuppressByCooldown(String eventKey) {
        if (!antiSpamEnabled) return false;
        long minutes = Math.max(1, cooldownMinutes);
        Optional<NotificationLog> latest = findLastDispatch(eventKey);
        if (latest.isEmpty()) return false;
        LocalDateTime cooldownUntil = latest.get().getCreatedAt().plusMinutes(minutes);
        return LocalDateTime.now().isBefore(cooldownUntil);
    }

    private Optional<NotificationLog> findLastDispatch(String eventKey) {
        if (eventKey == null || eventKey.isBlank()) return Optional.empty();
        return repository.findTopByEventKeyAndSuppressedFalseAndStatusInOrderByCreatedAtDesc(eventKey, DISPATCH_STATUSES);
    }

    private String buildAlertEventKey(Alert alert) {
        String type = alert.getAlertType() == null ? "UNKNOWN" : alert.getAlertType().name();
        Long id = alert.getId();
        return "ALERT:" + (id == null ? "NEW" : id) + ":" + type;
    }

    private void notifyInternal(String deviceId,
                                String eventKey,
                                String alertType,
                                String severity,
                                String message,
                                boolean systemAlert) {
        if (appEnabled) {
            createInAppLogs(deviceId, eventKey, alertType, severity, message);
        }

        Set<String> phones = collectPhones(deviceId);
        if (phones.isEmpty() && defaultSmsRecipient != null && !defaultSmsRecipient.isBlank()) phones.add(defaultSmsRecipient);
        for (String phone : phones) {
            sendViaWebhook(deviceId, eventKey, alertType, severity, "SMS", phone, message, smsEnabled, smsWebhookUrl);
        }
        if (systemAlert && phones.isEmpty()) {
            createLog(deviceId, eventKey, alertType, severity, "SMS", "none", null, null, message, "SKIPPED_NO_RECIPIENT", "No phone recipient configured or assigned", false, null, null);
        }

        Set<String> emails = collectEmails(deviceId);
        if (emails.isEmpty() && defaultEmailRecipient != null && !defaultEmailRecipient.isBlank()) emails.add(defaultEmailRecipient);
        for (String email : emails) {
            sendViaWebhook(deviceId, eventKey, alertType, severity, "EMAIL", email, message, emailEnabled, emailWebhookUrl);
        }
        if (systemAlert && emails.isEmpty()) {
            createLog(deviceId, eventKey, alertType, severity, "EMAIL", "none", null, null, message, "SKIPPED_NO_RECIPIENT", "No email recipient configured or assigned", false, null, null);
        }
    }

    private void createInAppLogs(String deviceId, String eventKey, String alertType, String severity, String message) {
        Map<Long, UserAccount> recipients = collectInAppUsers(deviceId);
        if (recipients.isEmpty()) {
            NotificationLog saved = createLog(deviceId, eventKey, alertType, severity, "APP", "device-viewers", null, null, message, "CREATED", "In-app notification created", false, null, null);
            publishInApp(saved);
            return;
        }

        for (UserAccount user : recipients.values()) {
            NotificationLog saved = createLog(
                    deviceId,
                    eventKey,
                    alertType,
                    severity,
                    "APP",
                    user.getUsername(),
                    user.getId(),
                    user.getUsername(),
                    message,
                    "CREATED",
                    "In-app notification created for user " + user.getUsername(),
                    false,
                    null,
                    null
            );
            publishInApp(saved);
        }
    }

    private void publishInApp(NotificationLog log) {
        try {
            webSocketEventService.publishInAppNotification(new NotificationResponse(log));
        } catch (Exception ignored) {
            // REST polling still works even if websocket delivery fails.
        }
    }

    private Map<Long, UserAccount> collectInAppUsers(String deviceId) {
        Map<Long, UserAccount> recipients = new LinkedHashMap<>();

        // Admins receive all system notifications inside the application.
        for (UserAccount admin : userRepository.findByRoleAndActiveTrue(RoleName.ADMIN)) {
            if (admin.getId() != null) recipients.put(admin.getId(), admin);
        }

        Pond pond = getPond(deviceId);
        if (pond == null) return recipients;
        for (UserPondAccess access : accessRepository.findByPond(pond)) {
            UserAccount user = access.getUser();
            if (user != null && user.isActive() && user.getId() != null) {
                recipients.put(user.getId(), user);
            }
        }
        return recipients;
    }

    private Set<String> collectPhones(String deviceId) {
        Set<String> recipients = new LinkedHashSet<>();
        Pond pond = getPond(deviceId);
        if (pond == null) return recipients;
        for (UserPondAccess access : accessRepository.findByPond(pond)) {
            UserAccount user = access.getUser();
            if (user != null && user.isActive() && user.getPhone() != null && !user.getPhone().isBlank()) {
                recipients.add(user.getPhone().trim());
            }
        }
        return recipients;
    }

    private Set<String> collectEmails(String deviceId) {
        Set<String> recipients = new LinkedHashSet<>();
        Pond pond = getPond(deviceId);
        if (pond == null) return recipients;
        for (UserPondAccess access : accessRepository.findByPond(pond)) {
            UserAccount user = access.getUser();
            if (user != null && user.isActive() && user.getEmail() != null && !user.getEmail().isBlank()) {
                recipients.add(user.getEmail().trim());
            }
        }
        return recipients;
    }

    private Pond getPond(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId).map(Device::getPond).orElse(null);
    }

    private void sendViaWebhook(String deviceId,
                                String eventKey,
                                String alertType,
                                String severity,
                                String channel,
                                String recipient,
                                String message,
                                boolean enabled,
                                String webhookUrl) {
        if (!enabled) {
            createLog(deviceId, eventKey, alertType, severity, channel, recipient, null, null, message, "SKIPPED_DISABLED", channel + " notification disabled in application.yml", false, null, null);
            return;
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            createLog(deviceId, eventKey, alertType, severity, channel, recipient, null, null, message, "SKIPPED_NOT_CONFIGURED", channel + " webhook URL is not configured", false, null, null);
            return;
        }
        try {
            String json = "{\"channel\":\"" + escape(channel) + "\",\"recipient\":\"" + escape(recipient) + "\",\"message\":\"" + escape(message) + "\",\"deviceId\":\"" + escape(deviceId) + "\",\"eventKey\":\"" + escape(eventKey) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String status = response.statusCode() >= 200 && response.statusCode() < 300 ? "SENT" : "FAILED";
            createLog(deviceId, eventKey, alertType, severity, channel, recipient, null, null, message, status, "HTTP " + response.statusCode() + ": " + truncate(response.body()), false, null, null);
        } catch (Exception e) {
            createLog(deviceId, eventKey, alertType, severity, channel, recipient, null, null, message, "FAILED", e.getMessage(), false, null, null);
        }
    }

    private NotificationLog createLog(String deviceId,
                                      String eventKey,
                                      String alertType,
                                      String severity,
                                      String channel,
                                      String recipient,
                                      Long recipientUserId,
                                      String recipientUsername,
                                      String message,
                                      String status,
                                      String providerResponse,
                                      boolean suppressed,
                                      String suppressionReason,
                                      LocalDateTime cooldownUntil) {
        NotificationLog log = new NotificationLog();
        log.setDeviceId(deviceId);
        log.setEventKey(eventKey);
        log.setAlertType(alertType);
        log.setSeverity(severity);
        log.setChannel(channel);
        log.setRecipient(recipient);
        log.setRecipientUserId(recipientUserId);
        log.setRecipientUsername(recipientUsername);
        log.setMessage(message);
        log.setStatus(status);
        log.setProviderResponse(providerResponse);
        log.setSuppressed(suppressed);
        log.setSuppressionReason(suppressionReason);
        log.setCooldownUntil(cooldownUntil);
        log.setRead(false);
        return repository.save(log);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    private String truncate(String value) {
        if (value == null) return "";
        return value.length() > 300 ? value.substring(0, 300) : value;
    }
}
