package com.frozenheart.backend.modules.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.frozenheart.backend.modules.search.document.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchIndexManager {

    private final ElasticsearchClient client;

    @FunctionalInterface
    private interface IndexBuilderConsumer {
        void accept(CreateIndexRequest.Builder builder) throws Exception;
    }

    /**
     * Tự động kiểm tra và khởi tạo toàn bộ 6 Index trên Elasticsearch nếu chưa tồn
     * tại
     */
    public void initAllIndices() {
        try {
            createIndexIfNotExists(UserSearchDoc.INDEX_NAME, this::buildUserIndex);
            createIndexIfNotExists(SubjectSearchDoc.INDEX_NAME, this::buildSubjectIndex);
            createIndexIfNotExists(PostSearchDoc.INDEX_NAME, this::buildPostIndex);
            createIndexIfNotExists(SessionSearchDoc.INDEX_NAME, this::buildSessionIndex);
            createIndexIfNotExists(QuestionSearchDoc.INDEX_NAME, this::buildQuestionIndex);
        } catch (Exception e) {
            log.warn("[ElasticsearchIndexManager] ⚠️ Chưa thể khởi tạo Index trên Elasticsearch: {}", e.getMessage());
        }
    }

    private void createIndexIfNotExists(String indexName, IndexBuilderConsumer builderConsumer) {
        try {
            boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();

            if (!exists) {
                CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder().index(indexName);
                builderConsumer.accept(builder);
                client.indices().create(builder.build());
                log.info("[ElasticsearchIndexManager] ✅ Đã tạo thành công Index: {}", indexName);
            }
        } catch (Exception e) {
            log.error("[ElasticsearchIndexManager] ❌ Lỗi khi kiểm tra/tạo index {}: {}", indexName, e.getMessage());
        }
    }

    // ==========================================
    // MAPPINGS CHI TIẾT TỪNG INDEX
    // ==========================================

    // 1. User Index
    private void buildUserIndex(CreateIndexRequest.Builder builder) {
        builder.mappings(m -> m
                .properties("id", p -> p.long_(l -> l))
                // Nhóm tìm kiếm Full-Text
                .properties("full_name", p -> p.text(t -> t))
                .properties("bio", p -> p.text(t -> t))
                // Nhóm lọc chính xác (Filter / Exact Match)
                .properties("student_lecturer_code", p -> p.keyword(k -> k))
                .properties("email", p -> p.keyword(k -> k))
                .properties("faculty", p -> p.keyword(k -> k))
                .properties("major", p -> p.keyword(k -> k))
                .properties("role", p -> p.keyword(k -> k))
                // Nhóm tính điểm độ liên quan (Boosting / Scoring)
                .properties("total_proposed_questions", p -> p.integer(i -> i))
                .properties("badges_count", p -> p.integer(i -> i))
                .properties("followers_count", p -> p.integer(i -> i))
                .properties("created_at", p -> p.date(d -> d))
                // Nhóm chỉ hiển thị (Display-only: Lưu trong _source, không đánh Inverted Index
                // để tiết kiệm RAM)
                .properties("avatar_url", p -> p.keyword(k -> k.index(false)))
                .properties("avatar_frame_url", p -> p.keyword(k -> k.index(false))));
    }

    // 2. Subject Index
    private void buildSubjectIndex(CreateIndexRequest.Builder builder) {
        builder.mappings(m -> m
                .properties("id", p -> p.long_(l -> l))
                .properties("subject_code", p -> p.keyword(k -> k))
                .properties("subject_name", p -> p.text(t -> t))
                .properties("total_questions", p -> p.integer(i -> i))
                .properties("total_sessions", p -> p.integer(i -> i)));
    }

    // 3. Post Index
    private void buildPostIndex(CreateIndexRequest.Builder builder) {
        builder.mappings(m -> m
                .properties("id", p -> p.long_(l -> l))
                .properties("content", p -> p.text(t -> t))
                .properties("author_id", p -> p.long_(l -> l))
                .properties("author_name", p -> p.text(t -> t))

                .properties("post_type", p -> p.keyword(k -> k))
                .properties("visibility", p -> p.keyword(k -> k))

                .properties("react_count", p -> p.integer(i -> i))
                .properties("comment_count", p -> p.integer(i -> i))
                .properties("created_at", p -> p.date(d -> d))
                // Display-only fields
                .properties("author_avatar_url", p -> p.keyword(k -> k.index(false)))
                .properties("author_frame_url", p -> p.keyword(k -> k.index(false)))
                .properties("media_urls", p -> p.keyword(k -> k.index(false))));
    }

    // 4. Session Index
    private void buildSessionIndex(CreateIndexRequest.Builder builder) {
        builder.mappings(m -> m
                .properties("id", p -> p.long_(l -> l))
                .properties("title", p -> p.text(t -> t))
                .properties("topic", p -> p.text(t -> t))
                .properties("content", p -> p.text(t -> t))
                .properties("subject_id", p -> p.long_(l -> l))
                .properties("subject_name", p -> p.text(t -> t))
                .properties("status", p -> p.keyword(k -> k))
                .properties("author_id", p -> p.long_(l -> l))
                .properties("question_count", p -> p.integer(i -> i))
                .properties("react_count", p -> p.integer(i -> i))
                .properties("comment_count", p -> p.integer(i -> i))
                .properties("created_at", p -> p.date(d -> d))
                // Tác giả ẩn danh (Display-only)
                .properties("author_name", p -> p.keyword(k -> k.index(false)))
                .properties("author_avatar_url", p -> p.keyword(k -> k.index(false)))
                .properties("author_frame_url", p -> p.keyword(k -> k.index(false))));
    }

    // 5. Question Index
    private void buildQuestionIndex(CreateIndexRequest.Builder builder) {
        builder.mappings(m -> m
                .properties("id", p -> p.long_(l -> l))
                .properties("question_code", p -> p.keyword(k -> k))
                .properties("subject_id", p -> p.long_(l -> l))
                .properties("subject_name", p -> p.text(t -> t))
                .properties("topic_id", p -> p.long_(l -> l))
                .properties("topic_name", p -> p.text(t -> t))
                .properties("status", p -> p.keyword(k -> k))
                .properties("difficulty", p -> p.keyword(k -> k))

                // Tầng công khai (Sinh viên & Peer Learning)
                .properties("original_content", p -> p.text(t -> t))
                .properties("original_options_text", p -> p.text(t -> t))
                .properties("original_explanation", p -> p.text(t -> t))

                // Tầng ngân hàng đề lõi (Chỉ Giảng viên)
                .properties("core_content", p -> p.text(t -> t))
                .properties("core_options_text", p -> p.text(t -> t))
                .properties("core_explanation", p -> p.text(t -> t))

                // 🌟 Tầng Dense Vector 384 chiều (AI Search)
                .properties("text_vector", p -> p.denseVector(d -> d
                        .dims(384)
                        .index(true)
                        .similarity(DenseVectorSimilarity.Cosine)
                    ))
                .properties("image_vectors", p -> p.denseVector(d -> d
                        .dims(384)
                        .index(true)
                        .similarity(DenseVectorSimilarity.Cosine)
                    ))

                // Chỉ số xếp hạng & tương tác
                .properties("avg_rating", p -> p.double_(db -> db))
                .properties("rating_count", p -> p.integer(i -> i))
                .properties("react_count", p -> p.integer(i -> i))
                .properties("comment_count", p -> p.integer(i -> i))
                .properties("author_id", p -> p.long_(l -> l))
                .properties("created_at", p -> p.date(d -> d))
                .properties("updated_at", p -> p.date(d -> d))

                // Display-only fields
                .properties("image_urls", p -> p.keyword(k -> k.index(false)))
                .properties("author_name", p -> p.keyword(k -> k.index(false)))
            );
    }
}
