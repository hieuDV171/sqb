package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.modules.session.dto.ApproveQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.ApproveQuestionsResponse;
import com.frozenheart.backend.modules.session.dto.EditQuestionRequest;
import com.frozenheart.backend.modules.session.dto.EditQuestionResponse;
import com.frozenheart.backend.modules.session.dto.PendingSessionsResponse;
import com.frozenheart.backend.modules.session.dto.RejectQuestionsRequest;
import com.frozenheart.backend.modules.session.dto.SessionDetailReviewResponse;

public interface QuestionReviewService {

    PendingSessionsResponse getPendingSessions(Long after, Integer limit, Long subjectId, String sortBy);

    SessionDetailReviewResponse getSessionDetailForReview(Long sessionId);

    ApproveQuestionsResponse approveQuestions(ApproveQuestionsRequest request);

    void rejectQuestions(RejectQuestionsRequest request);

    EditQuestionResponse editQuestion(Long questionId, EditQuestionRequest request);

}
