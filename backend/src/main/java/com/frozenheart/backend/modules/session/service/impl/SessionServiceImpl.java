package com.frozenheart.backend.modules.session.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.ProcessingStatus;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionDifficulty;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest.QuestionProposeDto;
import com.frozenheart.backend.modules.session.dto.ProposeSessionResponse;
import com.frozenheart.backend.modules.session.repository.QuestionMediaRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.session.service.DuplicateDetectionService;
import com.frozenheart.backend.modules.session.service.SessionService;
import com.frozenheart.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final AnonymizerUtil anonymizerUtil;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionMediaRepository questionMediaRepository;
    private final DuplicateDetectionService duplicateDetectionService;

    @Transactional
    @Override
    public ProposeSessionResponse proposeSession(ProposeSessionRequest request) {

        // Hiện tại, nội dung câu hỏi có thể có nhiều ảnh, nhưng đáp án chỉ được 1 ảnh

        // Lấy thông tin User nộp bài & Mã môn học
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        User proposer = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy môn học"));

        // Mã ẩn danh 6 ký tự của tác giả
        String authorCode = anonymizerUtil.encodeUserId(userId);

        // Khởi tạo & Lưu Session
        Session session = Session.builder()
                .title(request.title())
                .content(request.content())
                .sourceUrl(request.sourceUrl())
                .proposer(proposer)
                .subject(subject)
                .status(SessionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        session = sessionRepository.save(session);

        // {subjectCode}_{authorCode}_{timestamp}_{sessionId}
        String sessionCode = subject.getCode() + "_" + authorCode + "_" + System.currentTimeMillis() + "_" + session.getId();
        session.setSessionCode(sessionCode);

        // Chuyển đổi danh sách DTO sang danh sách Question Entity
        List<QuestionProposeDto> requestedQuestions = request.questions();
        Session finalSession = session;

        List<Question> questions = requestedQuestions.stream()
                .map(q -> {
                    String textContent = q.content();
                    return Question.builder()
                            .session(finalSession) // Gán mối quan hệ với Session

                            .originalContent(textContent)
                            .content(textContent)
                            
                            .difficulty(QuestionDifficulty.UNCLASSIFIED)
                            .status(QuestionStatus.PENDING)

                            // Gán cả bản gốc và bản làm việc
                            .originalOptions(q.options())
                            .options(q.options())

                            .originalCorrectAnswer(q.correctAnswer())
                            .correctAnswer(q.correctAnswer())

                            .originalExplanation(q.explanation())
                            .explanation(q.explanation())

                            .llmGenerated(q.llmGenerated())
                            .confidenceScore(q.confidence() != null ? q.confidence() : 0.0)
                            .createdAt(LocalDateTime.now())
                            .build();
                }).toList();

        // Lưu câu hỏi vào DB
        List<Question> savedQuestions = questionRepository.saveAll(questions);

        // Cập nhật displayOrder, questionCode và Lưu QuestionMedia đính kèm
        List<QuestionMedia> mediaListToSave = new ArrayList<>();

        // Cập nhật questionCode cho từng câu hỏi
        int size = savedQuestions.size();
        for (int i = 0; i < size; i++) {
            Question q = savedQuestions.get(i);
            QuestionProposeDto dto = requestedQuestions.get(i);

            q.setDisplayOrder(i + 1);
            q.setQuestionCode(subject.getCode() + "_" + authorCode + "_" + q.getId());

            // Ảnh của Đề bài (CONTENT)
            if (dto.mediaUrls() != null && !dto.mediaUrls().isEmpty()) {
                for (String mediaUrl : dto.mediaUrls()) {
                    if (mediaUrl != null && !mediaUrl.isBlank()) {
                        QuestionMedia media = QuestionMedia.builder()
                                .url(mediaUrl)
                                .mediaTarget(MediaTarget.CONTENT)
                                .question(q)
                                .owner(proposer)
                                .processingStatus(ProcessingStatus.PENDING)
                                .uploadedAt(LocalDateTime.now())
                                .build();

                        mediaListToSave.add(media);
                    }
                }
            }

            // Ảnh của các Đáp án (OPTION)
            if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                for (var opt : q.getOptions()) {
                    if (opt != null && opt.getMediaUrl() != null && !opt.getMediaUrl().isBlank()) {
                        QuestionMedia media = QuestionMedia.builder()
                                .url(opt.getMediaUrl())
                                .mediaTarget(MediaTarget.OPTION)
                                .question(q)
                                .owner(proposer)
                                .processingStatus(ProcessingStatus.PENDING)
                                .uploadedAt(LocalDateTime.now())
                                .build();

                        mediaListToSave.add(media);
                    }
                }
            }
        }
        
        if (!mediaListToSave.isEmpty()) {
            List<QuestionMedia> savedMedias = questionMediaRepository.saveAll(mediaListToSave);
            
            // Tạo Map tra cứu siêu nhanh O(1) trên RAM: Key = questionId_mediaUrl -> Value = mediaId
            Map<String, Long> mediaIdMap = savedMedias.stream()
                    .collect(Collectors.toMap(
                            m -> m.getQuestion().getId() + "_" + m.getUrl(), // key
                            q -> q.getId(), // value
                            (existing, replacement) -> existing // xử lý đụng độ
                    ));

            // 2. Tra cứu O(1) để gán mediaId ngược lại cho từng QuestionOption
            for (Question q : savedQuestions) {
                if (q.getOptions() != null) {
                    for (var opt : q.getOptions()) {
                        if (opt != null && opt.getMediaUrl() != null) {
                            Long mediaId = mediaIdMap.get(q.getId() + "_" + opt.getMediaUrl());
                            if (mediaId != null) {
                                opt.setMediaId(mediaId);
                            }
                        }
                    }
                }
            }
        }

        // BẮN TASK CHẠY NGẦM CHECK TRÙNG 3 TẦNG
        duplicateDetectionService.asyncCheckDuplicates(session.getId());

        return ProposeSessionResponse.builder()
                .sessionId(session.getId())
                .sessionCode(session.getSessionCode())
                .subjectId(subject.getId())
                .questionCount(request.questions().size())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .build();
    }

}
