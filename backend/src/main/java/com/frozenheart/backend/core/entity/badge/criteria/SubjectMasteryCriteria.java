package com.frozenheart.backend.core.entity.badge.criteria;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
import com.frozenheart.backend.core.entity.badge.CriteriaType;
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
@JsonTypeName("SUBJECT_MASTERY")
@Schema(description = "Ngưỡng câu hỏi được duyệt trong một môn, ghép với trigger_event APPROVED_QUESTIONS")
public class SubjectMasteryCriteria implements BadgeCriteria {

    @Schema(description = "ID môn học đang tồn tại; bắt buộc cho tiêu chí này", example = "42")
    private Long subjectId;
    @Schema(description = "Mã môn do server lấy từ subject_id, không dùng giá trị client để quyết định môn", example = "IT3290", accessMode = Schema.AccessMode.READ_ONLY)
    private String subjectCode;
    @Schema(description = "Số câu hỏi được duyệt cần đạt, >=1", example = "10", minimum = "1")
    private int approvedQuestionsCount;

    @Override
    @Schema(description = "Loại tiêu chí cố định", example = "SUBJECT_MASTERY")
    public CriteriaType getType() {
        return CriteriaType.SUBJECT_MASTERY;
    }
}
