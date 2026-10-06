package com.frozenheart.backend.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Thống kê số lượng câu hỏi theo độ khó")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Statistic {
    @Schema(description = "Số câu mức độ Dễ", example = "12")
    private Integer easyCount;

    @Schema(description = "Số câu mức độ Trung bình", example = "20")
    private Integer mediumCount;

    @Schema(description = "Số câu mức độ Khó", example = "8")
    private Integer hardCount;

    @Schema(description = "Số câu chưa phân loại độ khó", example = "0")
    private Integer unclassifiedCount;
}