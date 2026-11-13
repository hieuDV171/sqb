package com.frozenheart.backend.modules.socialinteraction.service.helper;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.user.UserRole;
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
            case BADGE_AWARD -> throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE);
            case POST -> {
                var post = postRepository.findInteractionTarget(targetId)
                        .orElseThrow(() -> new AppException(ResponseCode.POST_NOT_FOUND));
                if (post.getPostType() == PostType.LEARNING_VIDEO) {
                    JwtPayload viewer = JwtPayload.getCurrentUserPayload();
                    boolean admin = UserRole.ADMIN.name().equalsIgnoreCase(viewer.getRole());
                    if (!postRepository.canViewLectureVideo(targetId, viewer.getUserId(), admin)) {
                        throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn chưa đăng ký học môn của video này");
                    }
                }
            }
            case SESSION -> {
                var session = sessionRepository.findByIdFetchProposer(targetId)
                        .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy phiên đề xuất câu hỏi"));
                if (!session.isPracticeVisibleTo(JwtPayload.getCurrentUserPayload().getUserId())) {
                    throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn chưa có quyền thảo luận về bộ câu hỏi này");
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
