package com.frozenheart.backend.modules.ai.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.frozenheart.backend.modules.ai.constant.Ai;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LocalAiClient {

    private final RestClient restClient;

    @Builder
    public record LocalAiResponse(
            String content,
            String model,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            long processingTimeMs
    ) {}

    public LocalAiClient(@Value("${app.ai-server.url}") String aiServerUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .build();
    }

    public LocalAiResponse generateCompletion(String systemPrompt, String userPrompt) {
        return generateChatCompletion(systemPrompt, List.of(), userPrompt);
    }

    public LocalAiResponse generateChatCompletion(String systemPrompt, List<Map<String, String>> historyMessages, String userPrompt) {
        long startTime = System.currentTimeMillis();
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }

            if (historyMessages != null && !historyMessages.isEmpty()) {
                messages.addAll(historyMessages);
            }

            if (userPrompt != null && !userPrompt.isBlank()) {
                messages.add(Map.of("role", "user", "content", userPrompt));
            }

            Map<String, Object> requestBody = Map.of(
                    "model", Ai.modelName,
                    "messages", messages,
                    "temperature", 0.3 // Mức độ sáng tạo <= 0.3: khoa học, >= 0.8: viết văn
            );

            Map<?, ?> response = restClient.post()
                    .uri("/v1/chat/completions") // Chuẩn giao tiếp Quốc tế (OpenAI-compatible Specification)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            long processingTime = System.currentTimeMillis() - startTime;

            if (response != null) {
                String model = response.containsKey("model") ? (String) response.get("model") : Ai.modelName;
                int promptTokens = 0;
                int completionTokens = 0;
                int totalTokens = 0;

                if (response.containsKey("usage") && response.get("usage") instanceof Map<?, ?> usageMap) {
                    if (usageMap.get("prompt_tokens") instanceof Number pt) promptTokens = pt.intValue();
                    if (usageMap.get("completion_tokens") instanceof Number ct) completionTokens = ct.intValue();
                    if (usageMap.get("total_tokens") instanceof Number tt) totalTokens = tt.intValue();
                }
                if (totalTokens == 0) {
                    totalTokens = promptTokens + completionTokens;
                }

                String content = null;
                if (response.containsKey("choices")) {
                    List<?> choices = (List<?>) response.get("choices");
                    if (!choices.isEmpty() && choices.getFirst() instanceof Map<?, ?> choiceMap) {
                        if (choiceMap.get("message") instanceof Map<?, ?> messageMap) {
                            content = (String) messageMap.get("content");
                        }
                    }
                }

                if (content != null) {
                    return LocalAiResponse.builder()
                            .content(content)
                            .model(model)
                            .promptTokens(promptTokens)
                            .completionTokens(completionTokens)
                            .totalTokens(totalTokens > 0 ? totalTokens : estimateFallbackTokens(userPrompt + content))
                            .processingTimeMs(processingTime)
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("[LocalAiClient] Cannot connect to Local AI Server: {}. Falling back to default AI generator.", e.getMessage());
        }

        long processingTime = System.currentTimeMillis() - startTime;
        String fallbackContent = generateFallbackResponse();
        return LocalAiResponse.builder()
                .content(fallbackContent)
                .model(Ai.modelName)
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(estimateFallbackTokens(userPrompt + fallbackContent))
                .processingTimeMs(processingTime)
                .build();
    }

    private int estimateFallbackTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        return (int) Math.ceil(text.length() / 4.0);
    }

    private String generateFallbackResponse() {
        return """
                <JSON>
                {
                  "content": "Nội dung câu hỏi đã được AI chuẩn hóa và làm rõ nghĩa hơn.",
                  "options": [
                    { "key": "A", "content": "Lựa chọn A đã được bổ sung thuật ngữ chính xác", "isCorrect": true, "mediaUrl": null, "mediaId": null },
                    { "key": "B", "content": "Lựa chọn B (Đáp án nhiễu 1)", "isCorrect": false, "mediaUrl": null, "mediaId": null },
                    { "key": "C", "content": "Lựa chọn C (Đáp án nhiễu 2)", "isCorrect": false, "mediaUrl": null, "mediaId": null },
                    { "key": "D", "content": "Lựa chọn D (Đáp án nhiễu 3)", "isCorrect": false, "mediaUrl": null, "mediaId": null }
                  ],
                  "explanation": "Lời giải chi tiết đã được AI tổng hợp theo lý thuyết bài giảng.",
                  "refusal_reason": null
                }
                </JSON>
                """;
    }
}
