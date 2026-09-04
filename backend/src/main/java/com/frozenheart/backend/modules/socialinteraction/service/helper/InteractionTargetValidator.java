package com.frozenheart.backend.modules.socialinteraction.service.helper;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InteractionTargetValidator {

    private final PostRepository postRepository;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;

    public void validateTargetExists(InteractionTargetType targetType, Long targetId) {
        if (targetType == null || targetId == null) return;
        switch (targetType) {
            case POST -> {
                if (!postRepository.existsById(targetId)) {
                    throw new AppException(ResponseCode.POST_NOT_FOUND);
                }
            }
            case SESSION -> {
                if (!sessionRepository.existsById(targetId)) {
                    throw new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy phiên đề xuất câu hỏi");
                }
            }
            case QUESTION -> {
                if (!questionRepository.existsById(targetId)) {
                    throw new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy câu hỏi");
                }
            }
        }
    }
}
