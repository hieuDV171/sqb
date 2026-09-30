package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;

public interface SubmissionProjection {

    Long getSessionId();

    String getSessionCode();

    String getTitle();

    String getContent();

    Long getSubjectId();

    String getSubjectName();

    String getSubjectCode();

    Integer getQuestionCount();

    Instant getCreatedAt();

    Integer getReactCount();

    Integer getCommentCount();
}