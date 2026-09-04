package com.frozenheart.backend.modules.search.service;

import java.util.List;

public interface SearchSyncService {

    // ==========================================
    // 1. ĐỒNG BỘ TOÀN BỘ (FULL RE-INDEX QUA BULK API)
    // ==========================================
    void syncAllUsers();
    void syncAllSubjects();
    void syncAllPosts();
    void syncAllSessions();
    void syncAllQuestions();
    void syncAll(); // Đồng bộ toàn bộ dữ liệu từ Postgres sang Elasticsearch

    // ==========================================
    // 2. ĐỒNG BỘ TỨC THỜI TỪNG BẢN GHI (REAL-TIME SYNC)
    // ==========================================
    void syncUser(Long userId);
    void syncSubject(Long subjectId);
    void syncPost(Long postId);
    void syncSession(Long sessionId);
    void syncQuestion(Long questionId);

    void syncPostsBatch(List<Long> postIds);
    void syncQuestionsBatch(List<Long> questionIds);
    void syncSessionsBatch(List<Long> sessionIds);
    void syncUsersBatch(List<Long> userIds);
    void syncSubjectsBatch(List<Long> subjectIds);

    // ==========================================
    // 3. XÓA BẢN GHI KHỎI ELASTICSEARCH
    // ==========================================
    void deleteDocument(String indexName, Long id);
    void deleteDocumentsBatch(String indexName, List<Long> ids);
}
