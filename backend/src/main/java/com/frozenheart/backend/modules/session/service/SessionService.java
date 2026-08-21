package com.frozenheart.backend.modules.session.service;

import java.util.List;

import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.modules.session.dto.MySubmissionDetailResponse;
import com.frozenheart.backend.modules.session.dto.MySubmissionsResponse;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.ProposeSessionResponse;
import com.frozenheart.backend.modules.session.dto.SubjectResponse;
import com.frozenheart.backend.modules.session.dto.UpdateSubmissionSessionRequest;

public interface SessionService {

    ProposeSessionResponse proposeSession(ProposeSessionRequest request);

    MySubmissionsResponse getMySubmissions(Long after, Integer limit, Long subjectId, SessionStatus status);

    MySubmissionDetailResponse getMySubmissionDetail(Long sessionId);

    void updateMySubmissionSession(Long sessionId, UpdateSubmissionSessionRequest request);

    void deleteMySubmissionSession(Long sessionId);

    List<SubjectResponse> getSubjectList();

}


