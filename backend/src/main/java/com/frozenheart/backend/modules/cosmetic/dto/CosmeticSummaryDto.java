package com.frozenheart.backend.modules.cosmetic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Schema(description = "Thống kê tổng quan kho vật phẩm trang trí của người dùng")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmeticSummaryDto {

    @Schema(description = "Tổng số vật phẩm đã mở khóa", example = "12")
    private int totalUnlocked;

    @Schema(description = "Số lượng vật phẩm đã mở khóa phân theo độ hiếm", example = "{\"COMMON\": 6, \"RARE\": 4, \"EPIC\": 2, \"LEGENDARY\": 0}")
    private Map<String, Integer> byRarity;

    @Schema(description = "Tỷ lệ % hoàn thành bộ sưu tập vật phẩm", example = "35.5")
    private double collectionCompletionPercent;
}
