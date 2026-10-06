package com.frozenheart.backend.modules.badge.dto;

import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
import com.frozenheart.backend.core.entity.badge.BadgeTier;
import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Yêu cầu cập nhật thông tin huy hiệu danh hiệu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBadgeRequestDto {

    @Schema(description = "Tên huy hiệu mới", example = "Chuyên Gia Đề Xuất Cấp Cao")
    @Size(max = 100, message = "Tên huy hiệu tối đa 100 ký tự")
    private String name;

    @Schema(description = "Mô tả ý nghĩa của huy hiệu", example = "Đề xuất thành công 100 câu hỏi được duyệt vào ngân hàng đề")
    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @Schema(description = "Cấp bậc huy hiệu", example = "PLATINUM")
    private BadgeTier badgeTier;

    @Schema(description = "Sự kiện kích hoạt trao huy hiệu", example = "PROPOSE_QUESTION")
    private BadgeTriggerEvent badgeTriggerEvent;

    @Schema(description = "URL hình ảnh biểu tượng huy hiệu", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/badges/proposer_platinum.png")
    private String iconUrl;

    @Schema(description = "Tiêu chí điều kiện xét duyệt huy hiệu")
    private BadgeCriteria criteria;

    @Schema(description = "Trạng thái kích hoạt của huy hiệu", example = "true")
    private Boolean active;
}
