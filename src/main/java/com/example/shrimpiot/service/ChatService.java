package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.AlertResponse;
import com.example.shrimpiot.dto.ChatMessageResponse;
import com.example.shrimpiot.dto.ChatRequest;
import com.example.shrimpiot.dto.ChatResponse;
import com.example.shrimpiot.dto.ChatSessionResponse;
import com.example.shrimpiot.dto.SensorReadingResponse;
import com.example.shrimpiot.model.ChatMessage;
import com.example.shrimpiot.model.ChatMessageRole;
import com.example.shrimpiot.model.ChatSession;
import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.DeviceRelay;
import com.example.shrimpiot.model.RelayState;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.repository.ChatMessageRepository;
import com.example.shrimpiot.repository.ChatSessionRepository;
import com.example.shrimpiot.repository.DeviceRelayRepository;
import com.example.shrimpiot.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final OpenAiChatAssistantService openAiChatAssistantService;

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
            DeviceRelayRepository relayRepository,
            OpenAiChatAssistantService openAiChatAssistantService
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
        this.openAiChatAssistantService = openAiChatAssistantService;
    }

    public ChatResponse sendMessage(String authorization, ChatRequest request) {
        UserAccount user = authService.getCurrentUser(authorization);
        String intent = intentService.detectIntent(request.getMessage());
        String deviceId = normalizeDeviceId(request.getDeviceId(), intent);

        // Phase 2 reads system data, so device access must be checked first.
        // Basic knowledge questions do not require a device id.
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
            case "LATEST_READING" -> answerLatestReading(rawMessage, intent, deviceId);
            case "OPEN_ALERTS" -> answerOpenAlerts(rawMessage, intent, deviceId);
            case "RELAY_STATUS" -> answerRelayStatus(rawMessage, intent, deviceId);
            case "DEVICE_STATUS" -> answerDeviceStatus(rawMessage, intent, deviceId);
            default -> answerBasicKnowledge(rawMessage, intent, deviceId);
        };
    }

    private String answerLatestReading(String rawMessage, String intent, String deviceId) {
        try {
            SensorReadingResponse latest = sensorReadingService.getLatest(deviceId);
            String fallback = String.format(
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
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("latestReading", sensorReadingContext(latest));
            return aiAnswerOrFallback(rawMessage, intent, deviceId, context, fallback);
        } catch (Exception e) {
            return "Chưa lấy được dữ liệu cảm biến mới nhất cho thiết bị " + deviceId + ". Lý do: " + e.getMessage();
        }
    }

    private String answerOpenAlerts(String rawMessage, String intent, String deviceId) {
        List<AlertResponse> alerts = alertService.getOpenAlerts(deviceId);
        if (alerts.isEmpty()) {
            String fallback = "Hiện tại thiết bị " + deviceId + " chưa có cảnh báo chưa xử lý.";
            return aiAnswerOrFallback(rawMessage, intent, deviceId, Map.of("openAlerts", List.of()), fallback);
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

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("openAlerts", alerts.stream().map(this::alertContext).toList());
        return aiAnswerOrFallback(rawMessage, intent, deviceId, context, sb.toString());
    }

    private String answerRelayStatus(String rawMessage, String intent, String deviceId) {
        List<DeviceRelay> relays = relayRepository.findByDeviceDeviceId(deviceId);
        if (relays.isEmpty()) {
            String fallback = "Chưa có cấu hình relay cho thiết bị " + deviceId + ".";
            return aiAnswerOrFallback(rawMessage, intent, deviceId, Map.of("relays", List.of()), fallback);
        }

        StringBuilder sb = new StringBuilder("Trạng thái relay hiện tại của ").append(deviceId).append(":\n");
        List<Map<String, Object>> relayContexts = relays.stream().map(relay -> {
            RelayState state = relayStateService.getOrCreateRelayState(deviceId, relay.getRelayNo(), relay.getName());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("relayNo", relay.getRelayNo());
            item.put("name", relay.getName());
            item.put("relayType", relay.getRelayType());
            item.put("currentState", state.getCurrentState());
            item.put("locked", relay.isLocked());
            item.put("lastUpdatedAt", state.getLastUpdatedAt());
            return item;
        }).toList();

        for (DeviceRelay relay : relays) {
            RelayState state = relayStateService.getOrCreateRelayState(deviceId, relay.getRelayNo(), relay.getName());
            sb.append("- Relay ").append(relay.getRelayNo())
                    .append(" - ").append(relay.getName())
                    .append(": ").append(state.getCurrentState())
                    .append("\n");
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("relays", relayContexts);
        return aiAnswerOrFallback(rawMessage, intent, deviceId, context, sb.toString());
    }

    private String answerDeviceStatus(String rawMessage, String intent, String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        String pondName = device.getPond() == null ? "chưa gán ao" : device.getPond().getName();
        String fallback = "Thiết bị " + device.getDeviceId() + " - " + device.getName() + " hiện có trạng thái cấu hình: "
                + device.getStatus() + ", kết nối: " + device.getConnectionStatus()
                + ", ao: " + pondName
                + ", lần cuối thấy thiết bị: " + (device.getLastSeenAt() == null ? "chưa có" : device.getLastSeenAt()) + ".";

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("device", deviceContext(device, pondName));
        return aiAnswerOrFallback(rawMessage, intent, deviceId, context, fallback);
    }

    private String answerBasicKnowledge(String rawMessage, String intent, String deviceId) {
        String fallback = knowledgeService.answerBasicQuestion(rawMessage);
        return aiAnswerOrFallback(rawMessage, intent, deviceId, Map.of("topic", "basic aquaculture knowledge"), fallback);
    }

    private String aiAnswerOrFallback(String rawMessage, String intent, String deviceId, Object context, String fallback) {
        return openAiChatAssistantService.answer(rawMessage, intent, deviceId, context, fallback)
                .orElse(fallback);
    }

    private Map<String, Object> sensorReadingContext(SensorReadingResponse latest) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("deviceId", latest.getDeviceId());
        context.put("temperatureCelsius", latest.getTemperature());
        context.put("ph", latest.getPh());
        context.put("ecValue", latest.getEcValue());
        context.put("salinityPpt", latest.getSalinity());
        context.put("dissolvedOxygenMgL", latest.getDoValue());
        context.put("status", latest.getStatus());
        context.put("ruleStatus", latest.getRuleStatus());
        context.put("anomalyStatus", latest.getAnomalyStatus());
        context.put("mlStatus", latest.getMlStatus());
        context.put("finalStatus", latest.getFinalStatus());
        context.put("message", latest.getMessage());
        context.put("aiMessage", latest.getAiMessage());
        context.put("recommendedAction", latest.getRecommendedAction());
        context.put("createdAt", latest.getCreatedAt());
        return context;
    }

    private Map<String, Object> alertContext(AlertResponse alert) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", alert.getId());
        context.put("alertType", alert.getAlertType());
        context.put("severity", alert.getSeverity());
        context.put("status", alert.getStatus());
        context.put("message", alert.getMessage());
        context.put("createdAt", alert.getCreatedAt());
        return context;
    }

    private Map<String, Object> deviceContext(Device device, String pondName) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("deviceId", device.getDeviceId());
        context.put("name", device.getName());
        context.put("status", device.getStatus());
        context.put("connectionStatus", device.getConnectionStatus());
        context.put("pondName", pondName);
        context.put("installationPosition", device.getInstallationPosition());
        context.put("lastSeenAt", device.getLastSeenAt());
        return context;
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }
}
