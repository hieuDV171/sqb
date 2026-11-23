package com.frozenheart.backend.modules.ai.client;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LocalAiClient {

    private final RestClient restClient;
    private final String modelName;
    private final boolean configured;

    @Builder
    public record LocalAiResponse(
            String content,
            String model,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            long processingTimeMs
    ) {}

    public LocalAiClient(@Value("${app.ai-server.url}") String aiServerUrl,
            @Value("${app.ai-server.model}") String modelName,
            @Value("${app.ai-server.api-key}") String apiKey,
            @Value("${ai.sidecar.url}") String sidecarUrl) {
        String baseUrl = aiServerUrl.strip().replaceAll("/+$", "");
        this.modelName = modelName.strip();
        this.configured = !baseUrl.isBlank() && !this.modelName.isBlank()
                && !baseUrl.equals(sidecarUrl.strip().replaceAll("/+$", ""));

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeaders(headers -> {
                    if (!apiKey.isBlank()) headers.setBearerAuth(apiKey.strip());
                })
                .build();
    }

    public LocalAiResponse generateCompletion(String systemPrompt, String userPrompt) {
        long startTime = System.currentTimeMillis();
        try {
            return generateRequiredCompletion(systemPrompt, userPrompt);
        } catch (AppException e) {
            // Game 6 vẫn có thể dùng dữ liệu mẫu khi máy chủ sinh nội dung không sẵn sàng.
            String fallbackContent = generateFallbackResponse();
            return LocalAiResponse.builder()
                    .content(fallbackContent)
                    .model(modelName)
                    .totalTokens(estimateFallbackTokens(userPrompt + fallbackContent))
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    public LocalAiResponse generateRequiredCompletion(String systemPrompt, String userPrompt) {
        if (!configured) {
            throw unavailable("Dịch vụ tạo đề xuất chưa được cấu hình. Bạn có thể biên tập câu hỏi trực tiếp.");
        }

        long startTime = System.currentTimeMillis();
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }

            if (userPrompt != null && !userPrompt.isBlank()) {
                messages.add(Map.of("role", "user", "content", userPrompt));
            }

            Map<String, Object> requestBody = Map.of(
                    "model", modelName,
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
                String model = response.get("model") instanceof String name ? name : modelName;
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
                if (response.get("choices") instanceof List<?> choices) {
                    if (!choices.isEmpty() && choices.getFirst() instanceof Map<?, ?> choiceMap) {
                        if (choiceMap.get("message") instanceof Map<?, ?> messageMap
                                && messageMap.get("content") instanceof String message) {
                            content = message;
                        }
                    }
                }

                if (content != null && !content.isBlank()) {
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
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("[LocalAiClient] Máy chủ sinh nội dung trả về HTTP {}", status);
            throw unavailable(switch (status) {
                case 401, 403 -> "Dịch vụ tạo đề xuất chưa được cấp quyền. Hãy liên hệ quản trị viên.";
                case 404 -> "Dịch vụ tạo đề xuất chưa có endpoint hoặc mô hình phù hợp. Hãy liên hệ quản trị viên.";
                case 429 -> "Dịch vụ tạo đề xuất đã chạm giới hạn sử dụng. Hãy thử lại sau.";
                default -> "Dịch vụ tạo đề xuất đang gặp lỗi. Hãy thử lại sau.";
            });
        } catch (ResourceAccessException e) {
            log.warn("[LocalAiClient] Không liên lạc được với máy chủ sinh nội dung: {}",
                    e.getMostSpecificCause().getClass().getSimpleName());
            throw unavailable(e.getMostSpecificCause() instanceof SocketTimeoutException
                    ? "Dịch vụ tạo đề xuất phản hồi quá lâu. Bạn có thể thử lại hoặc biên tập trực tiếp."
                    : "Không kết nối được với dịch vụ tạo đề xuất. Hãy thử lại sau.");
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("[LocalAiClient] Không đọc được phản hồi từ máy chủ sinh nội dung: {}", e.getClass().getSimpleName());
            throw unavailable("Dịch vụ tạo đề xuất đang gặp lỗi. Hãy thử lại sau.");
        }

        throw unavailable("Dịch vụ AI trả về phản hồi trống. Hãy thử lại.");
    }

    private AppException unavailable(String message) {
        return new AppException(ResponseCode.AI_SERVICE_UNAVAILABLE, message);
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
