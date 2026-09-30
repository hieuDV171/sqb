package com.frozenheart.backend.modules.ai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;
import com.frozenheart.backend.modules.ai.dto.BtpropSidecarRequest;
import com.frozenheart.backend.modules.ai.dto.BtpropSidecarResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Thuật toán kiểm tra ảo giác BTPROP (Belief Tree Propagation)
 * Kết nối tới Python AI Sidecar (FastAPI) để dựng cây niềm tin và suy luận xác suất Markov.
 * Dựa trên bài báo khoa học NAACL 2025:
 * "A Probabilistic Framework for LLM Hallucination Detection via Belief Tree Propagation"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BtpropHallucinationCheckerService {

    private final AiSidecarClientService aiSidecarClientService;

    public AiRefineResponse.BtpropAuditDto audit(Question originalQuestion, AiRefineResponse.SuggestedQuestionDto suggested) {
        log.info("Running BTPROP Belief Tree Propagation audit for question ID: {}", 
                originalQuestion != null ? originalQuestion.getId() : "null");

        if (suggested == null) {
            return AiRefineResponse.BtpropAuditDto.builder()
                    .isHallucinated(true)
                    .confidenceScore(0.0)
                    .beliefTreeDepth(0)
                    .violations(List.of(AiRefineResponse.ViolationDto.builder()
                            .type("EMPTY_SUGGESTION")
                            .nodeStatement("Phản hồi AI trống")
                            .detail("Không nhận được nội dung gợi ý từ AI")
                            .severity("HIGH")
                            .build()))
                    .build();
        }

        // Nếu AI từ chối sinh câu hỏi (ví dụ người dùng yêu cầu sửa ảnh)
        if (suggested.refusalReason() != null && !suggested.refusalReason().isBlank()) {
            return AiRefineResponse.BtpropAuditDto.builder()
                    .isHallucinated(false)
                    .confidenceScore(1.0)
                    .beliefTreeDepth(0)
                    .violations(List.of())
                    .build();
        }

        // 1. Trích xuất ngữ cảnh Môn học & Chủ đề để truyền vào Dynamic Prompt
        String subject = "Khoa học máy tính";
        String topic = null;
        if (originalQuestion != null) {
            if (originalQuestion.getSession() != null && originalQuestion.getSession().getSubject() != null) {
                subject = originalQuestion.getSession().getSubject().getName();
            }
            if (originalQuestion.getTopic() != null) {
                topic = originalQuestion.getTopic().getName();
            }
        }

        // 2. Chuyển đổi danh sách options sang DTO của AI Sidecar (Hỗ trợ nhiều lựa chọn & nhiều đáp án đúng)
        List<BtpropSidecarRequest.OptionDto> sidecarOptions = new ArrayList<>();
        if (suggested.options() != null) {
            for (QuestionOption opt : suggested.options()) {
                sidecarOptions.add(BtpropSidecarRequest.OptionDto.builder()
                        .key(opt.getKey() != null ? opt.getKey() : "")
                        .text(opt.getText() != null ? opt.getText() : "")
                        .isCorrect(Boolean.TRUE.equals(opt.getIsCorrect()))
                        .mediaUrl(opt.getMediaUrl())
                        .mediaId(opt.getMediaId())
                        .build());
            }
        }

        BtpropSidecarRequest request = BtpropSidecarRequest.builder()
                .subject(subject)
                .topic(topic)
                .content(suggested.content() != null ? suggested.content() : "")
                .options(sidecarOptions)
                .explanation(suggested.explanation() != null ? suggested.explanation() : "")
                .fastMode(false)
                .build();

        // 3. Gọi sang AI Sidecar để thẩm định qua Cây Niềm Tin
        BtpropSidecarResponse response = aiSidecarClientService.auditQuestion(request);

        // Fallback an toàn (Graceful Degradation): Nếu AI Sidecar gặp lỗi hoặc timeout, không chặn đứng luồng làm việc
        if (response == null || "error".equalsIgnoreCase(response.status()) || "unverified".equalsIgnoreCase(response.status())) {
            log.warn("[BTProp] AI Sidecar phản hồi không sẵn sàng. Fallback sang cảnh báo UNVERIFIED.");
            return fallbackAudit(response);
        }

        List<AiRefineResponse.ViolationDto> violations = new ArrayList<>();
        if (response.violations() != null) {
            for (BtpropSidecarResponse.ViolationDto v : response.violations()) {
                violations.add(AiRefineResponse.ViolationDto.builder()
                        .type(v.type())
                        .nodeStatement(v.nodeStatement())
                        .detail(v.detail())
                        .severity(v.severity())
                        .build());
            }
        }

        return AiRefineResponse.BtpropAuditDto.builder()
                .isHallucinated(response.isHallucinated())
                .confidenceScore(response.confidenceScore())
                .beliefTreeDepth(response.beliefTreeDepth())
                .violations(violations)
                .build();
    }

    private AiRefineResponse.BtpropAuditDto fallbackAudit(BtpropSidecarResponse response) {
        String detail = (response != null && response.issues() != null && !response.issues().isEmpty())
                ? String.join("; ", response.issues())
                : "Không thể kết nối đến AI Sidecar BTProp hoặc Sidecar chưa cấu hình API Key. Cần giảng viên tự rà soát.";

        return AiRefineResponse.BtpropAuditDto.builder()
                .isHallucinated(false)
                .confidenceScore(0.5)
                .beliefTreeDepth(0)
                .violations(List.of(AiRefineResponse.ViolationDto.builder()
                        .type("UNVERIFIED")
                        .nodeStatement("Dịch vụ kiểm chứng chưa hoàn tất")
                        .detail(detail)
                        .severity("LOW")
                        .build()))
                .build();
    }
}
