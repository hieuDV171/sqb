package com.frozenheart.backend.modules.session.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.entity.session.DuplicateWarning;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.modules.embedding.service.EmbeddingClientService;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.service.DuplicateDetectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateDetectionServiceImpl implements DuplicateDetectionService {

    private final QuestionRepository questionRepository;
    private final EmbeddingClientService embeddingClientService;

    @Async
    @Transactional
    @Override
    public void asyncCheckDuplicates(Long sessionId) {
        
        log.info("[DuplicateCheck] Bắt đầu kiểm tra trùng lặp 3 tầng cho Session ID: {}", sessionId);
        List<Question> questions = questionRepository.findBySessionId(sessionId);
        if (questions.isEmpty())
            return;

        // Trích xuất văn bản đầy đủ (Nội dung đề bài + Các đáp án A, B, C, D) để embedding ngữ nghĩa chuẩn 100%
        List<String> fullTexts = questions.stream()
                .map(this::buildFullTextForEmbedding)
                .toList();

        List<float[]> embeddings = embeddingClientService.getBatchTextEmbeddings(fullTexts);

        int questionsSize = questions.size();
        int embeddingSize = embeddings.size();
        for (int i = 0; i < questionsSize; i++) {
            Question q = questions.get(i);

            // Kiểm tra an toàn chống văng IndexOutOfBoundsException phòng trường hợp network timeout / lỗi service AI
            if (i < embeddingSize) {
                q.setTextEmbedding(embeddings.get(i));
            }

            List<DuplicateWarning> warnings = new ArrayList<>();

            // TODO: 3 tầng check trùng
            // - Tầng 1: Rule-Based (Exact Text Match trong DB)
            // - Tầng 2: Trigram (Postgres pg_trgm > 0.85) & pHash ảnh (Hamming <= 5)
            // - Tầng 3: Vector Cosine Similarity (Cosine > 0.90)
            
            q.setDuplicateWarnings(warnings);

        }

        // Cập nhật lại danh sách câu hỏi kèm theo vector và duplicateWarnings
        // Chỉ cập nhật duplicateWarnings, không liên quan index
        questionRepository.saveAll(questions);
        log.info("[DuplicateCheck] Hoàn thành kiểm tra trùng lặp cho Session ID: {}", sessionId);

    }

    /**
     * Ghép nội dung đề bài + các đáp án A, B, C, D thành 1 chuỗi ngữ nghĩa hoàn chỉnh
     */
    private String buildFullTextForEmbedding(Question q) {
        StringBuilder sb = new StringBuilder();
        if (q.getContent() != null) {
            sb.append(q.getContent().trim());
        }
        if (q.getOptions() != null && !q.getOptions().isEmpty()) {
            for (var opt : q.getOptions()) {
                if (opt == null) continue;
                sb.append("\n");
                if (opt.getKey() != null && !opt.getKey().isBlank()) {
                    sb.append(opt.getKey().trim()).append(". ");
                }
                if (opt.getText() != null && !opt.getText().isBlank()) {
                    sb.append(opt.getText().trim());
                }
            }
        }
        return sb.toString();
    }


}
