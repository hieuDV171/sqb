package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;

public interface MySubmissionProjection {

    Long getSessionId();

    Long getSubjectId();

    String getSubjectName();

    String getSubjectCode();

    Integer getQuestionCount();

    Instant getCreatedAt();

    Integer getReactCount();

    Integer getCommentCount();
}