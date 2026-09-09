package com.frozenheart.backend.core.entity.media;

import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.questioneditlog.AiAnalysisResult;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.frozenheart.backend.core.constant.EmbeddingConstants;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "question_medias")
public class QuestionMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    // Kết quả thuật toán pHash là dãy 64 bit = 8 bytes tương đương với kiểu Long
    // Nên lưu dưới dạng long để có thể thực hiện tính toán nếu cần
    // Nếu lưu dưới dạng String thì khi tính toán sẽ mất thêm 1 bước chuyển đổi
    // String <-> Long
    private Long pHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private AiAnalysisResult aiAnalysisResult;

    // @Type(PgVectorType.class)
    @Column(columnDefinition = "vector(" + EmbeddingConstants.IMAGE_EMBEDDING_DIMS + ")")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private float[] mediaEmbedding;

    private String objectKey; // object key S3/MinIO

    @Column(length = 500)
    private String url;

    private Long fileSize; //

    private int width;
    private int height;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MediaTarget mediaTarget; // CONTENT (ảnh đề bài) hoặc OPTION (ảnh đáp án)

    @Enumerated(value = EnumType.STRING)
    @Column(length = 50)
    private ProcessingStatus processingStatus;

    private Instant uploadedAt;
    private Instant processedAt;

    private Instant deletedAt;

    // ---------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;
    // ---------------------

}
