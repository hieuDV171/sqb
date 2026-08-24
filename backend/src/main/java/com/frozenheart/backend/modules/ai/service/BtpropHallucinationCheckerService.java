package com.frozenheart.backend.modules.ai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Thuật toán kiểm tra ảo giác BTPROP (Belief Tree Propagation)
 * Dựa trên bài báo khoa học NAACL 2025:
 * "A Probabilistic Framework for LLM Hallucination Detection via Belief Tree Propagation"
 */
@Slf4j
@Service
public class BtpropHallucinationCheckerService {

    /**
     * TODO: Hiện tại vẫn đang viết bừa, thuật toán chưa được cài đặt chuẩn xác
     */
    public AiRefineResponse.BtpropAuditDto audit(Question originalQuestion, AiRefineResponse.SuggestedQuestionDto suggested) {
        log.info("Running BTPROP Belief Tree Propagation audit for question ID: {}", originalQuestion != null ? originalQuestion.getId() : "null");

        List<AiRefineResponse.ViolationDto> violations = new ArrayList<>();
        int beliefTreeDepth = 2;
        double confidenceScore = 0.95;
        boolean isHallucinated = false;

        if (suggested == null) {
            return AiRefineResponse.BtpropAuditDto.builder()
                    .isHallucinated(true)
                    .confidenceScore(0.0)
                    .beliefTreeDepth(beliefTreeDepth)
                    .violations(List.of(AiRefineResponse.ViolationDto.builder()
                            .type("EMPTY_SUGGESTION")
                            .nodeStatement("Phản hồi AI trống")
                            .detail("Không nhận được nội dung gợi ý từ AI")
                            .severity("HIGH")
                            .build()))
                    .build();
        }

        // Kiểm tra nếu AI từ chối sửa ảnh
        if (suggested.refusalReason() != null && !suggested.refusalReason().isBlank()) {
            return AiRefineResponse.BtpropAuditDto.builder()
                    .isHallucinated(false)
                    .confidenceScore(1.0)
                    .beliefTreeDepth(1)
                    .violations(List.of())
                    .build();
        }

        // Kiểm tra tính nhất quán của đáp án đúng (Key Answer Consistency Node)
        if (originalQuestion != null && originalQuestion.getOptions() != null && suggested.options() != null) {
            String originalCorrectKey = findCorrectKey(originalQuestion.getOptions());
            String suggestedCorrectKey = findCorrectKey(suggested.options());

            if (originalCorrectKey != null && suggestedCorrectKey != null && !originalCorrectKey.equalsIgnoreCase(suggestedCorrectKey)) {
                violations.add(AiRefineResponse.ViolationDto.builder()
                        .type("ANSWER_KEY_FLIP")
                        .nodeStatement("Node 1.1: Mâu thuẫn khóa đáp án đúng")
                        .detail("Đáp án đúng gốc là [" + originalCorrectKey + "] nhưng AI đảo thành [" + suggestedCorrectKey + "] mà không có giải thích hợp lý.")
                        .severity("HIGH")
                        .build());
                confidenceScore -= 0.35;
            }
        }

        // 3. Kiểm tra tính đầy đủ của nội dung (Content Completeness Node)
        if (suggested.content() == null || suggested.content().trim().length() < 10) {
            violations.add(AiRefineResponse.ViolationDto.builder()
                    .type("CONTENT_DECAY")
                    .nodeStatement("Node 1.2: Suy thoái mệnh đề câu hỏi")
                    .detail("Nội dung câu hỏi quá ngắn hoặc thiếu thông tin cốt lõi.")
                    .severity("HIGH")
                    .build());
            confidenceScore -= 0.40;
        }

        confidenceScore = Math.max(0.0, Math.min(1.0, confidenceScore));
        if (confidenceScore < 0.65 || violations.stream().anyMatch(v -> "HIGH".equalsIgnoreCase(v.severity()))) {
            isHallucinated = true;
        }

        return AiRefineResponse.BtpropAuditDto.builder()
                .isHallucinated(isHallucinated)
                .confidenceScore(confidenceScore)
                .beliefTreeDepth(beliefTreeDepth)
                .violations(violations)
                .build();
    }

    private String findCorrectKey(List<QuestionOption> options) {
        if (options == null) return null;
        return options.stream()
                .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                .map(QuestionOption::getKey)
                .findFirst()
                .orElse(null);
    }
}
