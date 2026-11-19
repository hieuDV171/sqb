package com.frozenheart.backend.modules.socialinteraction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tổng hợp cảm xúc theo loại và cảm xúc hiện tại của người gọi")
public class ReactResponseDto {
    @Schema(description = "Cảm xúc hiện tại của người gọi; null nếu chưa thả cảm xúc", example = "LIKE")
    private String myReaction;
    @Schema(description = "Bản đồ LIKE/LOVE/WOW tới số lượt tương ứng", example = "{\"LIKE\":3,\"LOVE\":1,\"WOW\":0}")
    private Map<String, Integer> reactionCounts;
    @Schema(description = "Tổng số lượt cảm xúc trên đối tượng", example = "3")
    private int totalCount;
}
