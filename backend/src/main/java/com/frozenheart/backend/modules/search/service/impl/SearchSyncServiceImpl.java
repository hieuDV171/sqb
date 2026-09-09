package com.frozenheart.backend.modules.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.post.PostVisibility;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.search.document.*;
import com.frozenheart.backend.modules.search.service.SearchSyncService;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchSyncServiceImpl implements SearchSyncService {

    private final ElasticsearchClient client;
    private final UserProfileRepository userProfileRepository;
    private final SubjectRepository subjectRepository;
    private final PostRepository postRepository;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;

    // =========================================================================
    // 1. FULL RE-INDEX (BULK API)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public void syncAll() {
        log.info("[SearchSyncService] 🚀 Bắt đầu Full Re-index toàn bộ dữ liệu lên Elasticsearch...");
        syncAllSubjects();
        syncAllUsers();
        syncAllPosts();
        syncAllSessions();
        syncAllQuestions();
        log.info("[SearchSyncService] 🎉 Hoàn tất Full Re-index toàn bộ hệ thống.");
    }

    @Override
    @Transactional(readOnly = true)
    public void syncAllUsers() {
        List<UserProfile> profiles = userProfileRepository.findAllVerifiedActiveUsersForSearch();
        if (profiles.isEmpty()) return;

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (UserProfile up : profiles) {
                UserSearchDoc doc = mapToUserDoc(up);
                br.operations(op -> op.index(idx -> idx
                        .index(UserSearchDoc.INDEX_NAME)
                        .id(doc.getId().toString())
                        .document(doc)
                ));
            }

            BulkResponse response = client.bulk(br.build());
            if (response.errors()) {
                log.error("[SearchSyncService] Có lỗi khi bulk index UserSearchDoc: {}", response);
            } else {
                log.info("[SearchSyncService] ✅ Đã đồng bộ {} users lên Elasticsearch.", profiles.size());
            }
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi syncAllUsers: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncAllSubjects() {
        List<Subject> subjects = subjectRepository.findAll();
        if (subjects.isEmpty()) return;

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Subject s : subjects) {
                SubjectSearchDoc doc = mapToSubjectDoc(s);
                br.operations(op -> op.index(idx -> idx
                        .index(SubjectSearchDoc.INDEX_NAME)
                        .id(doc.getId().toString())
                        .document(doc)
                ));
            }

            BulkResponse response = client.bulk(br.build());
            if (response.errors()) {
                log.error("[SearchSyncService] Có lỗi khi bulk index SubjectSearchDoc: {}", response);
            } else {
                log.info("[SearchSyncService] ✅ Đã đồng bộ {} subjects lên Elasticsearch.", subjects.size());
            }
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi syncAllSubjects: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncAllPosts() {
        List<Post> posts = postRepository.findAllPublicPostsForSearch();
        if (posts.isEmpty()) return;

        // Tránh N+1: Lấy trước toàn bộ UserProfile của tác giả
        Set<Long> posterIds = posts.stream()
                .map(p -> p.getPoster() != null ? p.getPoster().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(posterIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Post p : posts) {
                UserProfile authorProfile = p.getPoster() != null ? profileMap.get(p.getPoster().getId()) : null;
                PostSearchDoc doc = mapToPostDoc(p, authorProfile);
                br.operations(op -> op.index(idx -> idx
                        .index(PostSearchDoc.INDEX_NAME)
                        .id(doc.getId().toString())
                        .document(doc)
                ));
            }

            BulkResponse response = client.bulk(br.build());
            if (response.errors()) {
                log.error("[SearchSyncService] Có lỗi khi bulk index PostSearchDoc: {}", response);
            } else {
                log.info("[SearchSyncService] ✅ Đã đồng bộ {} posts lên Elasticsearch.", posts.size());
            }
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi syncAllPosts: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncAllSessions() {
        List<Session> sessions = sessionRepository.findAllResolvedSessionsForSearch();
        if (sessions.isEmpty()) return;

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Session s : sessions) {
                SessionSearchDoc doc = mapToSessionDoc(s);
                br.operations(op -> op.index(idx -> idx
                        .index(SessionSearchDoc.INDEX_NAME)
                        .id(doc.getId().toString())
                        .document(doc)
                ));
            }

            BulkResponse response = client.bulk(br.build());
            if (response.errors()) {
                log.error("[SearchSyncService] Có lỗi khi bulk index SessionSearchDoc: {}", response);
            } else {
                log.info("[SearchSyncService] ✅ Đã đồng bộ {} sessions lên Elasticsearch.", sessions.size());
            }
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi syncAllSessions: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncAllQuestions() {
        List<Question> questions = questionRepository.findAllReviewedQuestionsForSearch();
        if (questions.isEmpty()) return;

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Question q : questions) {
                QuestionSearchDoc doc = mapToQuestionDoc(q);
                br.operations(op -> op.index(idx -> idx
                        .index(QuestionSearchDoc.INDEX_NAME)
                        .id(doc.getId().toString())
                        .document(doc)
                ));
            }

            BulkResponse response = client.bulk(br.build());
            if (response.errors()) {
                log.error("[SearchSyncService] Có lỗi khi bulk index QuestionSearchDoc: {}", response);
            } else {
                log.info("[SearchSyncService] ✅ Đã đồng bộ {} questions lên Elasticsearch.", questions.size());
            }
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi syncAllQuestions: {}", e.getMessage());
        }
    }

    // =========================================================================
    // 2. REAL-TIME SYNC (SINGLE DOCUMENT)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public void syncUser(Long userId) {
        userProfileRepository.findByUserIdWithUser(userId).ifPresent(up -> {
            if (up.getUser().isVerified() && up.getUser().isActive()) {
                UserSearchDoc doc = mapToUserDoc(up);
                indexDocument(UserSearchDoc.INDEX_NAME, doc.getId().toString(), doc);
            } else {
                deleteDocument(UserSearchDoc.INDEX_NAME, userId);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public void syncSubject(Long subjectId) {
        subjectRepository.findById(subjectId).ifPresent(sub -> {
            SubjectSearchDoc doc = mapToSubjectDoc(sub);
            indexDocument(SubjectSearchDoc.INDEX_NAME, doc.getId().toString(), doc);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public void syncPost(Long postId) {
        postRepository.findByIdFetchPosterForSearch(postId).ifPresentOrElse(p -> {
            if (p.getDeletedAt() == null && p.getVisibility() != PostVisibility.ONLY_ME) {
                UserProfile authorProfile = p.getPoster() != null
                        ? userProfileRepository.findByUserId(p.getPoster().getId()).orElse(null)
                        : null;
                PostSearchDoc doc = mapToPostDoc(p, authorProfile);
                indexDocument(PostSearchDoc.INDEX_NAME, doc.getId().toString(), doc);
            } else {
                deleteDocument(PostSearchDoc.INDEX_NAME, postId);
            }
        }, () -> deleteDocument(PostSearchDoc.INDEX_NAME, postId));
    }

    @Override
    @Transactional(readOnly = true)
    public void syncSession(Long sessionId) {
        sessionRepository.findByIdFetchSubjectAndProposerForSearch(sessionId).ifPresentOrElse(s -> {
            if (s.getStatus() == SessionStatus.RESOLVED) {
                SessionSearchDoc doc = mapToSessionDoc(s);
                indexDocument(SessionSearchDoc.INDEX_NAME, doc.getId().toString(), doc);
            } else {
                deleteDocument(SessionSearchDoc.INDEX_NAME, sessionId);
            }
        }, () -> deleteDocument(SessionSearchDoc.INDEX_NAME, sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public void syncQuestion(Long questionId) {
        questionRepository.findByIdFetchDetailsForSearch(questionId).ifPresentOrElse(q -> {
            if (q.getStatus() == QuestionStatus.APPROVED || q.getStatus() == QuestionStatus.REJECTED) {
                QuestionSearchDoc doc = mapToQuestionDoc(q);
                indexDocument(QuestionSearchDoc.INDEX_NAME, doc.getId().toString(), doc);
            } else {
                deleteDocument(QuestionSearchDoc.INDEX_NAME, questionId);
            }
        }, () -> deleteDocument(QuestionSearchDoc.INDEX_NAME, questionId));
    }

    @Override
    @Transactional(readOnly = true)
    public void syncPostsBatch(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return;
        List<Post> posts = postRepository.findAllById(postIds);
        if (posts.isEmpty()) return;

        Set<Long> posterIds = posts.stream()
                .map(p -> p.getPoster() != null ? p.getPoster().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(posterIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Post p : posts) {
                if (p.getDeletedAt() == null && p.getVisibility() != PostVisibility.ONLY_ME) {
                    UserProfile authorProfile = p.getPoster() != null ? profileMap.get(p.getPoster().getId()) : null;
                    PostSearchDoc doc = mapToPostDoc(p, authorProfile);
                    br.operations(op -> op.index(idx -> idx
                            .index(PostSearchDoc.INDEX_NAME)
                            .id(doc.getId().toString())
                            .document(doc)
                    ));
                } else {
                    br.operations(op -> op.delete(del -> del
                            .index(PostSearchDoc.INDEX_NAME)
                            .id(p.getId().toString())
                    ));
                }
            }
            BulkResponse response = client.bulk(br.build());
            if (!response.errors()) {
                log.info("[SearchSyncService] ✅ Đã bulk sync/delete {} posts.", posts.size());
            }
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi syncPostsBatch: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncQuestionsBatch(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return;
        List<Question> questions = questionRepository.findByIdInFetchDetailsForSearch(questionIds);
        if (questions.isEmpty()) return;

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Question q : questions) {
                if (q.getStatus() == QuestionStatus.APPROVED || q.getStatus() == QuestionStatus.REJECTED) {
                    QuestionSearchDoc doc = mapToQuestionDoc(q);
                    br.operations(op -> op.index(idx -> idx
                            .index(QuestionSearchDoc.INDEX_NAME)
                            .id(doc.getId().toString())
                            .document(doc)
                    ));
                } else {
                    br.operations(op -> op.delete(del -> del
                            .index(QuestionSearchDoc.INDEX_NAME)
                            .id(q.getId().toString())
                    ));
                }
            }
            BulkResponse response = client.bulk(br.build());
            if (!response.errors()) {
                log.info("[SearchSyncService] ✅ Đã bulk sync/delete {} questions.", questions.size());
            }
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi syncQuestionsBatch: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncSessionsBatch(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return;
        List<Session> sessions = sessionRepository.findByIdInFetchSubjectAndProposerForSearch(sessionIds);
        if (sessions.isEmpty()) return;

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Session s : sessions) {
                if (s.getStatus() == SessionStatus.RESOLVED) {
                    SessionSearchDoc doc = mapToSessionDoc(s);
                    br.operations(op -> op.index(idx -> idx
                            .index(SessionSearchDoc.INDEX_NAME)
                            .id(doc.getId().toString())
                            .document(doc)
                    ));
                } else {
                    br.operations(op -> op.delete(del -> del
                            .index(SessionSearchDoc.INDEX_NAME)
                            .id(s.getId().toString())
                    ));
                }
            }
            BulkResponse response = client.bulk(br.build());
            if (!response.errors()) {
                log.info("[SearchSyncService] ✅ Đã bulk sync/delete {} sessions.", sessions.size());
            }
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi syncSessionsBatch: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncUsersBatch(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        List<UserProfile> profiles = userProfileRepository.findByIdInVerifiedActiveUsersForSearch(userIds);
        if (profiles.isEmpty()) return;

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (UserProfile up : profiles) {
                UserSearchDoc doc = mapToUserDoc(up);
                br.operations(op -> op.index(idx -> idx
                        .index(UserSearchDoc.INDEX_NAME)
                        .id(doc.getId().toString())
                        .document(doc)
                ));
            }
            BulkResponse response = client.bulk(br.build());
            if (!response.errors()) {
                log.info("[SearchSyncService] ✅ Đã bulk sync {} users.", profiles.size());
            }
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi syncUsersBatch: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncSubjectsBatch(List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) return;
        List<Subject> subjects = subjectRepository.findAllById(subjectIds);
        if (subjects.isEmpty()) return;

        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Subject s : subjects) {
                SubjectSearchDoc doc = mapToSubjectDoc(s);
                br.operations(op -> op.index(idx -> idx
                        .index(SubjectSearchDoc.INDEX_NAME)
                        .id(doc.getId().toString())
                        .document(doc)
                ));
            }
            BulkResponse response = client.bulk(br.build());
            if (!response.errors()) {
                log.info("[SearchSyncService] ✅ Đã bulk sync {} subjects.", subjects.size());
            }
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi syncSubjectsBatch: {}", e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String indexName, Long id) {
        try {
            client.delete(d -> d.index(indexName).id(id.toString()));
            log.info("[SearchSyncService] 🗑️ Đã xóa document ID {} khỏi index {}", id, indexName);
        } catch (Exception e) {
            log.warn("[SearchSyncService] Lỗi khi xóa document ID {} khỏi index {}: {}", id, indexName, e.getMessage());
        }
    }

    @Override
    public void deleteDocumentsBatch(String indexName, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Long id : ids) {
                br.operations(op -> op.delete(del -> del.index(indexName).id(id.toString())));
            }
            BulkResponse response = client.bulk(br.build());
            if (!response.errors()) {
                log.info("[SearchSyncService] 🗑️ Đã bulk delete {} documents khỏi index {}", ids.size(), indexName);
            }
        } catch (Exception e) {
            log.warn("[SearchSyncService] Lỗi khi bulk delete documents khỏi index {}: {}", indexName, e.getMessage());
        }
    }

    private <T> void indexDocument(String indexName, String id, T doc) {
        try {
            client.index(i -> i.index(indexName).id(id).document(doc));
            log.info("[SearchSyncService] 🔄 Đã cập nhật document ID {} vào index {}", id, indexName);
        } catch (Exception e) {
            log.error("[SearchSyncService] Lỗi khi index document ID {} vào index {}: {}", id, indexName, e.getMessage());
        }
    }

    // =========================================================================
    // 3. MAPPERS
    // =========================================================================

    private UserSearchDoc mapToUserDoc(UserProfile up) {
        return UserSearchDoc.builder()
                .id(up.getUserId())
                .fullName(up.getFullName())
                .studentLecturerCode(up.getStudentLecturerCode())
                .email(up.getUser() != null ? up.getUser().getEmail() : null)
                .bio(up.getBio())
                .avatarUrl(up.getAvatarUrl())
                .avatarFrameUrl(up.getAvatarFrameUrl())
                .faculty(up.getFaculty())
                .major(up.getMajor())
                .role(up.getUser() != null ? up.getUser().getRole() : null)
                .totalProposedQuestions(up.getTotalProposedQuestion())
                .badgesCount(up.getBadgesCount())
                .followersCount(up.getFollowersCount())
                .createdAt(up.getUser() != null ? up.getUser().getCreatedAt() : null)
                .build();
    }

    private SubjectSearchDoc mapToSubjectDoc(Subject s) {
        return SubjectSearchDoc.builder()
                .id(s.getId())
                .subjectCode(s.getCode())
                .subjectName(s.getName())
                .build();
    }

    private PostSearchDoc mapToPostDoc(Post p, UserProfile authorProfile) {
        List<String> mediaUrls = p.getMediaUrls() != null
                ? p.getMediaUrls().stream().map(MediaItem::url).filter(Objects::nonNull).toList()
                : Collections.emptyList();

        return PostSearchDoc.builder()
                .id(p.getId())
                .content(p.getContent())
                .authorId(p.getPoster() != null ? p.getPoster().getId() : null)
                .authorName(authorProfile != null ? authorProfile.getFullName() : "Người dùng")
                .authorAvatarUrl(authorProfile != null ? authorProfile.getAvatarUrl() : null)
                .authorFrameUrl(authorProfile != null ? authorProfile.getAvatarFrameUrl() : null)
                .postType(p.getPostType())
                .visibility(p.getVisibility())
                .mediaUrls(mediaUrls)
                .reactCount(p.getReactCount())
                .commentCount(p.getCommentCount())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private SessionSearchDoc mapToSessionDoc(Session s) {
        return SessionSearchDoc.builder()
                .id(s.getId())
                .title(s.getTitle())
                .content(s.getContent())
                .subjectId(s.getSubject() != null ? s.getSubject().getId() : null)
                .subjectName(s.getSubject() != null ? s.getSubject().getName() : null)
                .status(s.getStatus())
                .authorId(s.getProposer() != null ? s.getProposer().getId() : null)
                .questionCount(s.getQuestions() != null ? s.getQuestions().size() : 0)
                .reactCount(s.getReactCount())
                .commentCount(s.getCommentCount())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private QuestionSearchDoc mapToQuestionDoc(Question q) {
        // Tách các lựa chọn (key, text, mediaUrl) - không lưu isCorrect để bảo mật
        List<QuestionOptionDoc> options = q.getOptions() != null
                ? q.getOptions().stream().map(opt -> QuestionOptionDoc.builder()
                        .key(opt.getKey())
                        .text(opt.getText())
                        .mediaUrl(opt.getMediaUrl())
                        .build()
                    ).toList()
                : Collections.emptyList();

        // Vector hình ảnh nếu có (DINOv2 - 384 dims) và danh sách URL ảnh đính kèm
        List<float[]> imageVectors = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();

        if (q.getOwnedMedias() != null && !q.getOwnedMedias().isEmpty()) {
            for (QuestionMedia m : q.getOwnedMedias()) {
                if (MediaTarget.CONTENT.equals(m.getMediaTarget())) {
                    if (m.getUrl() != null) {
                        imageUrls.add(m.getUrl());
                    }
                    if (m.getMediaEmbedding() != null) {
                        imageVectors.add(m.getMediaEmbedding());
                    }
                }
            }
        }

        return QuestionSearchDoc.builder()
                .id(q.getId())
                .questionCode(q.getQuestionCode())
                .subjectId(q.getSession() != null && q.getSession().getSubject() != null ? q.getSession().getSubject().getId() : null)
                .subjectName(q.getSession() != null && q.getSession().getSubject() != null ? q.getSession().getSubject().getName() : null)
                .topicId(q.getTopic() != null ? q.getTopic().getId() : null)
                .topicName(q.getTopic() != null ? q.getTopic().getName() : null)
                .status(q.getStatus())
                .difficulty(q.getDifficulty())
                // Nội dung câu hỏi
                .content(q.getContent())
                .options(options)
                .explanation(q.getExplanation())
                // AI Vector Layer
                .textVector(q.getTextEmbedding())
                .imageVectors(imageVectors.isEmpty() ? null : imageVectors)
                .imageUrls(imageUrls)
                // Metrics
                .avgRating(q.getAvgRating())
                .ratingCount(q.getRatingCount())
                .reactCount(q.getReactCount())
                .commentCount(q.getCommentCount())
                .authorId(q.getSession() != null && q.getSession().getProposer() != null ? q.getSession().getProposer().getId() : null)
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }
}
