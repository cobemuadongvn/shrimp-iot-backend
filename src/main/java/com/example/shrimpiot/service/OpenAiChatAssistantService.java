package com.example.shrimpiot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OpenAiChatAssistantService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiChatAssistantService.class);

    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final int timeoutMs;
    private final int maxOutputTokens;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiChatAssistantService(
            @Value("${openai.chat.enabled:false}") boolean enabled,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.chat.model:gpt-5.4-mini}") String model,
            @Value("${openai.chat.endpoint:https://api.openai.com/v1/responses}") String endpoint,
            @Value("${openai.chat.timeout-ms:8000}") int timeoutMs,
            @Value("${openai.chat.max-output-tokens:700}") int maxOutputTokens,
            ObjectMapper objectMapper
    ) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
        this.timeoutMs = timeoutMs;
        this.maxOutputTokens = maxOutputTokens;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public Optional<String> answer(String userMessage, String intent, String deviceId, Object context, String fallbackAnswer) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("max_output_tokens", maxOutputTokens);
            payload.put("instructions", systemPrompt());
            payload.put("input", userPrompt(userMessage, intent, deviceId, context, fallbackAnswer));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenAI chat assistant returned non-2xx status: {}", response.statusCode());
                return Optional.empty();
            }

            String text = extractText(response.body());
            if (text == null || text.isBlank()) {
                log.warn("OpenAI chat assistant returned an empty response");
                return Optional.empty();
            }
            return Optional.of(limitLength(text.trim(), 2000));
        } catch (Exception ex) {
            log.warn("OpenAI chat assistant failed; falling back to local answer: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String systemPrompt() {
        return """
                Bạn là AI Assistant cho hệ thống IoT giám sát ao nuôi thủy hải sản.
                Trả lời bằng tiếng Việt, ngắn gọn, thực tế và dễ hiểu cho chủ ao hoặc kỹ thuật viên.
                Chỉ dựa trên dữ liệu hệ thống đã cung cấp; nếu thiếu dữ liệu thì nói rõ là chưa đủ dữ liệu.
                Không tự bịa chỉ số, không khẳng định đã điều khiển relay, không yêu cầu thao tác nguy hiểm.
                Với tình huống nguy cơ cao, ưu tiên khuyến nghị kiểm tra thực tế và xử lý thủ công an toàn.
                """;
    }

    private String userPrompt(String userMessage, String intent, String deviceId, Object context, String fallbackAnswer) throws Exception {
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("question", userMessage);
        prompt.put("intent", intent);
        prompt.put("deviceId", deviceId);
        prompt.put("systemContext", context);
        prompt.put("localFallbackAnswer", fallbackAnswer);
        prompt.put("answerRules", List.of(
                "Giải thích dựa trên dữ liệu ao nuôi trong systemContext.",
                "Nếu có chỉ số vượt ngưỡng, nêu chỉ số nào, mức độ, và việc nên làm tiếp theo.",
                "Không đưa ra lệnh bật/tắt thiết bị; chỉ gợi ý người vận hành kiểm tra hoặc dùng chức năng có sẵn trong app.",
                "Nếu localFallbackAnswer đã đủ, có thể viết lại tự nhiên hơn nhưng không thay đổi sự thật."
        ));
        return objectMapper.writeValueAsString(prompt);
    }

    private String extractText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode outputText = root.get("output_text");
        if (outputText != null && outputText.isTextual()) {
            return outputText.asText();
        }

        List<String> parts = new ArrayList<>();
        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.get("content");
                if (content == null || !content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    JsonNode text = contentItem.get("text");
                    if (text != null && text.isTextual()) {
                        parts.add(text.asText());
                    }
                }
            }
        }
        return parts.isEmpty() ? "" : String.join("\n", parts);
    }

    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
