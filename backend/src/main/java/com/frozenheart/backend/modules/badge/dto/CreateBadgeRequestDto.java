package com.frozenheart.backend.modules.badge.dto;

import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
import com.frozenheart.backend.core.entity.badge.BadgeTier;
import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBadgeRequestDto {

    @NotBlank(message = "Tên huy hiệu không được để trống")
    @Size(max = 100, message = "Tên huy hiệu tối đa 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @NotNull(message = "Cấp bậc huy hiệu không được để trống")
    private BadgeTier badgeTier;

    @NotNull(message = "Loại sự kiện kích hoạt (badgeTriggerEvent) không được để trống")
    private BadgeTriggerEvent badgeTriggerEvent;

    private String iconUrl;

    @NotNull(message = "Tiêu chí huy hiệu không được để trống")
    private BadgeCriteria criteria;
}
