package com.frozenheart.backend.core.util;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromptSanitizerUtil {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore\\s+previous\\s+instructions?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("bỏ\\s+qua\\s+(tất\\s+cả\\s+)?(quy\\s+tắc|chỉ\\s+thị|yêu\\s+cầu)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system\\s+prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act\\s+as", Pattern.CASE_INSENSITIVE),
            Pattern.compile("jailbreak", Pattern.CASE_INSENSITIVE),
            Pattern.compile("dan\\s+mode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("override\\s+(system|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("tiết\\s+lộ\\s+(system|quy\\s+tắc|prompt)", Pattern.CASE_INSENSITIVE)
    );

    public static void validatePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return;
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                log.warn("[PromptSanitizerUtil] Potential prompt injection detected: '{}'", prompt);
                throw new AppException(ResponseCode.CONTENT_VIOLATES_POLICY, "Yêu cầu chứa từ khóa không được phép hoặc có dấu hiệu vi phạm chính sách an toàn");
            }
        }
    }

    public static String cleanText(String text) {
        if (text == null) return "";
        // Loại bỏ các thẻ XML/HTML nguy hiểm mà attacker cố tình chèn vào để đóng tag sớm
        return text.replace("</user_instruction>", "")
                .replace("<user_instruction>", "")
                .replace("</user_query>", "")
                .replace("<user_query>", "")
                .replace("</original_question>", "")
                .replace("<original_question>", "")
                .replace("</JSON>", "")
                .replace("<JSON>", "")
                .trim();
    }
}
