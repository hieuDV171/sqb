package com.frozenheart.backend.modules.badge.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualGrantBadgeRequestDto {

    @NotEmpty(message = "Danh sách user_ids không được để trống")
    private List<Long> userIds;

    private String reason;
}
