package com.frozenheart.backend.modules.session.dto;

public interface AnswerOptionCountProjection {
    String getOptionKey();

    long getSelectionCount();
}
