package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.*;
import com.example.shrimpiot.model.*;
import com.example.shrimpiot.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final AuthService authService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatIntentService intentService;
    private final ChatKnowledgeService knowledgeService;
    private final SensorReadingService sensorReadingService;
    private final AlertService alertService;
    private final RelayStateService relayStateService;
    private final DeviceRepository deviceRepository;
    private final DeviceRelayRepository relayRepository;

    public ChatService(
            AuthService authService,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            ChatIntentService intentService,
            ChatKnowledgeService knowledgeService,
            SensorReadingService sensorReadingService,
            AlertService alertService,
            RelayStateService relayStateService,
            DeviceRepository deviceRepository,
            DeviceRelayRepository relayRepository
    ) {
        this.authService = authService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.intentService = intentService;
        this.knowledgeService = knowledgeService;
        this.sensorReadingService = sensorReadingService;
        this.alertService = alertService;
        this.relayStateService = relayStateService;
        this.deviceRepository = deviceRepository;
        this.relayRepository = relayRepository;
    }

    public ChatResponse sendMessage(String authorization, ChatRequest request) {
        UserAccount user = authService.getCurrentUser(authorization);
        String intent = intentService.detectIntent(request.getMessage());
        String deviceId = normalizeDeviceId(request.getDeviceId(), intent);

        // Pha 2 đọc dữ liệu hệ thống nên kiểm tra quyền truy cập thiết bị.
        // Pha 1 hỏi kiến thức cơ bản không bắt buộc phải có deviceId.
        if (deviceId != null && !"BASIC_KNOWLEDGE".equals(intent)) {
            authService.validateAccessToDevice(authorization, deviceId);
        }

        ChatSession session = getOrCreateSession(user, request.getSessionId(), deviceId, request.getMessage());

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setRole(ChatMessageRole.USER);
        userMessage.setContent(request.getMessage());
        userMessage.setDeviceId(deviceId);
        ChatMessage savedUserMessage = messageRepository.save(userMessage);

        String answer = buildAnswer(intent, request.getMessage(), deviceId);

        ChatMessage botMessage = new ChatMessage();
        botMessage.setSession(session);
        botMessage.setRole(ChatMessageRole.ASSISTANT);
        botMessage.setContent(answer);
        botMessage.setDeviceId(deviceId);
        ChatMessage savedBotMessage = messageRepository.save(botMessage);

        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        return new ChatResponse(
                session.getId(),
                intent,
                new ChatMessageResponse(savedUserMessage),
                new ChatMessageResponse(savedBotMessage)
        );
    }

    public List<ChatSessionResponse> getSessions(String authorization) {
        UserAccount user = authService.getCurrentUser(authorization);
        List<ChatSession> sessions = user.getRole() == RoleName.ADMIN
                ? sessionRepository.findAllByOrderByUpdatedAtDesc()
                : sessionRepository.findByUserOrderByUpdatedAtDesc(user);
        return sessions.stream().map(ChatSessionResponse::new).toList();
    }

    public List<ChatMessageResponse> getMessages(String authorization, Long sessionId) {
        UserAccount user = authService.getCurrentUser(authorization);
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found: " + sessionId));
        if (user.getRole() != RoleName.ADMIN && !session.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Access denied to this chat session");
        }
        return messageRepository.findBySessionOrderByCreatedAtAsc(session)
                .stream()
                .map(ChatMessageResponse::new)
                .toList();
    }

    private ChatSession getOrCreateSession(UserAccount user, Long sessionId, String deviceId, String firstMessage) {
        if (sessionId != null) {
            ChatSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Chat session not found: " + sessionId));
            if (user.getRole() != RoleName.ADMIN && !session.getUser().getId().equals(user.getId())) {
                throw new SecurityException("Access denied to this chat session");
            }
            return session;
        }

        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setDeviceId(deviceId);
        session.setTitle(makeTitle(firstMessage));
        return sessionRepository.save(session);
    }

    private String makeTitle(String message) {
        if (message == null || message.isBlank()) return "Cuộc trò chuyện mới";
        String clean = message.trim();
        return clean.length() > 60 ? clean.substring(0, 60) + "..." : clean;
    }

    private String normalizeDeviceId(String deviceId, String intent) {
        if (deviceId != null && !deviceId.isBlank()) return deviceId.trim();
        if ("BASIC_KNOWLEDGE".equals(intent)) return null;
        return "device_01";
    }

    private String buildAnswer(String intent, String rawMessage, String deviceId) {
        return switch (intent) {
            case "LATEST_READING" -> answerLatestReading(deviceId);
            case "OPEN_ALERTS" -> answerOpenAlerts(deviceId);
            case "RELAY_STATUS" -> answerRelayStatus(deviceId);
            case "DEVICE_STATUS" -> answerDeviceStatus(deviceId);
            default -> knowledgeService.answerBasicQuestion(rawMessage);
        };
    }

    private String answerLatestReading(String deviceId) {
        try {
            SensorReadingResponse latest = sensorReadingService.getLatest(deviceId);
            return String.format(
                    "Dữ liệu mới nhất của %s:\n- Nhiệt độ: %.2f°C\n- pH: %.2f\n- EC: %.2f\n- Độ mặn: %.2f\n- Oxy hòa tan: %.2f mg/L\n- Trạng thái: %s\n- Nhận xét: %s",
                    latest.getDeviceId(),
                    safe(latest.getTemperature()),
                    safe(latest.getPh()),
                    safe(latest.getEcValue()),
                    safe(latest.getSalinity()),
                    safe(latest.getDoValue()),
                    latest.getStatus(),
                    latest.getMessage()
            );
        } catch (Exception e) {
            return "Chưa lấy được dữ liệu cảm biến mới nhất cho thiết bị " + deviceId + ". Lý do: " + e.getMessage();
        }
    }

    private String answerOpenAlerts(String deviceId) {
        List<AlertResponse> alerts = alertService.getOpenAlerts(deviceId);
        if (alerts.isEmpty()) {
            return "Hiện tại thiết bị " + deviceId + " chưa có cảnh báo chưa xử lý.";
        }

        StringBuilder sb = new StringBuilder("Các cảnh báo đang mở của ").append(deviceId).append(":\n");
        for (AlertResponse alert : alerts) {
            sb.append("- ")
                    .append(alert.getAlertType())
                    .append(" [").append(alert.getSeverity()).append("] ")
                    .append(alert.getMessage())
                    .append("\n");
        }
        sb.append("Gợi ý: Ưu tiên xử lý cảnh báo DANGER trước, sau đó xác nhận đã xử lý trên hệ thống.");
        return sb.toString();
    }

    private String answerRelayStatus(String deviceId) {
        List<DeviceRelay> relays = relayRepository.findByDeviceDeviceId(deviceId);
        if (relays.isEmpty()) {
            return "Chưa có cấu hình relay cho thiết bị " + deviceId + ".";
        }

        StringBuilder sb = new StringBuilder("Trạng thái relay hiện tại của ").append(deviceId).append(":\n");
        for (DeviceRelay relay : relays) {
            RelayState state = relayStateService.getOrCreateRelayState(deviceId, relay.getRelayNo(), relay.getName());
            sb.append("- Relay ").append(relay.getRelayNo())
                    .append(" - ").append(relay.getName())
                    .append(": ").append(state.getCurrentState())
                    .append("\n");
        }
        return sb.toString();
    }

    private String answerDeviceStatus(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        String pondName = device.getPond() == null ? "chưa gán ao" : device.getPond().getName();
        return "Thiết bị " + device.getDeviceId() + " - " + device.getName() + " hiện có trạng thái cấu hình: "
                + device.getStatus() + ", kết nối: " + device.getConnectionStatus()
                + ", ao: " + pondName
                + ", lần cuối thấy thiết bị: " + (device.getLastSeenAt() == null ? "chưa có" : device.getLastSeenAt()) + ".";
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }
}
