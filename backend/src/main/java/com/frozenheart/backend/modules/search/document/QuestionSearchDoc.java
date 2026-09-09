package com.frozenheart.backend.modules.search.document;

import java.time.Instant;
import java.util.List;

import com.frozenheart.backend.core.entity.session.QuestionDifficulty;
import com.frozenheart.backend.core.entity.session.QuestionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSearchDoc {

    public static final String INDEX_NAME = "sqb_questions";

    private Long id;
    private String questionCode; // Mã câu hỏi (subjectId_authorCode_questionId)

    private Long subjectId;
    private String subjectName;
    private Long topicId;
    private String topicName;

    private QuestionStatus status; // APPROVED, REJECTED, PENDING
    private QuestionDifficulty difficulty; // UNCLASSIFIED, MEDIUM, HARD

    // =======================================================
    // NỘI DUNG CÂU HỎI
    // =======================================================
    private String content;
    private List<QuestionOptionDoc> options;
    private String explanation;

    // =======================================================
    // 3. TẦNG VECTOR ĐA PHƯƠNG TIỆN (Dense Vectors cho AI Search)
    // =======================================================
    // Vector ngữ nghĩa của đề bài (384 dims sinh từ multilingual-e5-small)
    private float[] textVector;

    // Danh sách vector hình ảnh minh họa đính kèm (Mỗi ảnh 384 dims sinh từ
    // facebook/dinov2-small)
    private List<float[]> imageVectors;
    private List<String> imageUrls; // Danh sách URL ảnh đính kèm để preview kết quả

    // =======================================================
    // 4. METADATA & CHỈ SỐ TƯƠNG TÁC (Tín hiệu xếp hạng độ liên quan)
    // =======================================================
    private Double avgRating;
    private Integer ratingCount;
    private Integer reactCount;
    private Integer commentCount;

    private Long authorId;
    private String authorName;

    private Instant createdAt;
    private Instant updatedAt;

}
