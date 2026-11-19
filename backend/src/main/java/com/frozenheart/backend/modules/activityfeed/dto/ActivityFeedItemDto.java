package com.frozenheart.backend.modules.activityfeed.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.activityfeed.ActionType;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedTargetType;
import com.frozenheart.backend.modules.post.dto.AuthorDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Một sự kiện bảng tin; kiểu content phụ thuộc target_type/action_type")
public class ActivityFeedItemDto {
    @Schema(description = "ID sự kiện bảng tin, dùng theo cơ chế cursor của feed", example = "42")
    private Long feedId;
    @Schema(description = "Loại đối tượng được tham chiếu; dùng giá trị enum API công bố")
    private ActivityFeedTargetType targetType;
    @Schema(description = "ID đối tượng được tham chiếu; diễn giải cùng target_type/type của API", example = "42")
    private Long targetId;
    @Schema(description = "Loại sự kiện quyết định cách diễn giải content trên bảng tin")
    private ActionType actionType;
    @Schema(description = "Tài khoản thực hiện sự kiện, có thể không có với sự kiện hệ thống")
    private AuthorDto actor;
    @Schema(description = "Map metadata theo target_type/action_type, không phải PostResponseDto. Có thể chứa title/description/media_url/badge_name/cosmetic_name/subject_code/subject_name. POST/SESSION/QUESTION bổ sung comment_count/react_count/my_reaction/reacted_by_me; POST có thể thêm visibility/media_urls/lecturer_note; EARNED_BADGE có react_count/reacted_by_me.", example = "{\"title\":\"Bài viết mới\",\"description\":\"Cùng trao đổi bài học\",\"comment_count\":2,\"react_count\":3,\"my_reaction\":\"LIKE\",\"reacted_by_me\":true,\"visibility\":\"PUBLIC\"}")
    private Object content;

    @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
    private Instant createdAt;

    @Schema(description = "Trọng số sự kiện; kết quả mapping hiện trả 1", example = "1")
    private Integer weight;
}
