package com.frozenheart.backend.core.entity.badge.criteria;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
public class SubjectMasteryCriteria implements BadgeCriteria {

    private Long subjectId;
    private String subjectCode;
    private int approvedQuestionsCount;

    @Override
    public CriteriaType getType() {
        return CriteriaType.SUBJECT_MASTERY;
    }
}
