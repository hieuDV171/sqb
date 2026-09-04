package com.frozenheart.backend.core.entity.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PushNotificationType {
    SOCIAL("Thông báo về tương tác mạng xã hội (bài viết, bình luận, kết bạn)"),
    ACADEMIC("Thông báo về đề thi, câu hỏi và duyệt bài"),
    GAMIFICATION("Thông báo về điểm thưởng, bảng xếp hạng và huy hiệu"),
    SYSTEM("Thông báo từ hệ thống và quản trị viên");

    private final String description;
}
