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
@JsonTypeName("MANUAL_GRANT")
@Schema(description = "Huy hiệu trao thủ công bởi ADMIN, ghép với trigger_event MANUAL_GRANT")
public class ManualGrantCriteria implements BadgeCriteria {

    @Schema(description = "Ghi chú tùy chọn cho tiêu chí trao thủ công", example = "Trao cho đóng góp nổi bật trong học kỳ")
    private String description;

    @Override
    @Schema(description = "Loại tiêu chí cố định", example = "MANUAL_GRANT")
    public CriteriaType getType() {
        return CriteriaType.MANUAL_GRANT;
    }
}
