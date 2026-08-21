package com.frozenheart.backend.core.entity.session;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionOption {
    private String key;
    private String text;
    private Boolean isCorrect;
    private String mediaUrl;
    private Long mediaId;
}
