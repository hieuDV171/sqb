package com.frozenheart.backend.modules.session.dto;

import java.time.LocalDateTime;

public interface MySubmissionProjection {

    Long getSessionId();

    Long getSubjectId();

    String getSubjectName();

    String getSubjectCode();

    Integer getQuestionCount();

    LocalDateTime getCreatedAt();

    Integer getReactCount();

    Integer getCommentCount();
}