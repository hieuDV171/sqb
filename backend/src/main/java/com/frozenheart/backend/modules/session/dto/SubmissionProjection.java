package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;

import com.frozenheart.backend.core.entity.session.SessionStatus;

public interface SubmissionProjection {

    Long getSessionId();

    String getSessionCode();

    String getTitle();

    String getContent();

    Long getSubjectId();

    String getSubjectName();

    String getSubjectCode();

    Long getCourseClassId();

    String getClassCode();

    Long getSemesterId();

    String getSemesterName();

    SessionStatus getStatus();

    Integer getQuestionCount();

    Instant getCreatedAt();

    Integer getReactCount();

    Integer getCommentCount();
}
