package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.modules.session.dto.DuplicateMatchResponse;
import java.util.List;
import com.frozenheart.backend.modules.session.dto.ApproveQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.ApproveQuestionsResponse;
import com.frozenheart.backend.modules.session.dto.EditQuestionRequest;
import com.frozenheart.backend.modules.session.dto.EditQuestionResponse;
import com.frozenheart.backend.modules.session.dto.ReviewableSessionsResponse;
import com.frozenheart.backend.modules.session.dto.RejectQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.SessionDetailReviewResponse;

public interface QuestionReviewService {

    ReviewableSessionsResponse getReviewableSessions(Long after, Long before, Integer limit, Long courseClassId, String sortBy, String keyword);

    SessionDetailReviewResponse getSessionDetailForReview(Long sessionId);

    List<DuplicateMatchResponse> getDuplicateMatches(Long sessionId, Long questionId);

    ApproveQuestionsResponse approveQuestions(ApproveQuestionsRequest request);

    void rejectQuestions(RejectQuestionsRequest request);

    EditQuestionResponse editQuestion(Long questionId, EditQuestionRequest request);

    void retryDuplicateCheck(Long questionId);

    void completeSessionReview(Long sessionId);

}
