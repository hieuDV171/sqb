package com.frozenheart.backend.modules.badge.evaluator;

import com.frozenheart.backend.core.entity.badge.Badge;
import com.frozenheart.backend.core.entity.badge.CriteriaType;
import com.frozenheart.backend.core.entity.badge.criteria.SubjectMasteryCriteria;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubjectMasteryEvaluator implements BadgeEvaluator<SubjectMasteryCriteria> {

    private final QuestionRepository questionRepository;

    @Override
    public CriteriaType getSupportedType() {
        return CriteriaType.SUBJECT_MASTERY;
    }

    @Override
    public boolean evaluate(User user, Badge badge, SubjectMasteryCriteria criteria, Object context) {
        if (user == null || criteria == null || criteria.getSubjectId() == null) {
            return false;
        }

        long approvedCount = questionRepository.countApprovedQuestionsByUserAndSubject(user.getId(), criteria.getSubjectId());
        return approvedCount >= criteria.getApprovedQuestionsCount();
    }
}
