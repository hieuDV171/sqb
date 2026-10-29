package com.frozenheart.backend.core.entity.session;

/**
 * Lý do từ chối chuẩn hóa để giảng viên có thể kiểm duyệt nhanh, trong khi
 * sinh viên vẫn nhận được phản hồi nhất quán.
 */
public enum QuestionRejectionCode {
    DUPLICATE,
    INCORRECT_ANSWER,
    AMBIGUOUS_OR_INCOMPLETE,
    OUT_OF_SCOPE,
    LOW_QUALITY,
    OTHER,
    UNSPECIFIED,
    AUTO_REJECTED_ON_COMPLETION
}
