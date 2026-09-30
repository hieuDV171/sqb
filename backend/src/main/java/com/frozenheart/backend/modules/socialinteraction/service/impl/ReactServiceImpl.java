package com.frozenheart.backend.modules.socialinteraction.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.socialinteraction.React;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.socialinteraction.dto.ReactRequest;
import com.frozenheart.backend.modules.socialinteraction.dto.ReactResponseDto;
import com.frozenheart.backend.modules.socialinteraction.repository.ReactRepository;
import com.frozenheart.backend.modules.socialinteraction.service.ReactService;
import com.frozenheart.backend.modules.socialinteraction.service.helper.InteractionTargetValidator;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReactServiceImpl implements ReactService {

    private final ReactRepository reactRepository;
    private final UserRepository userRepository;
    private final InteractionTargetValidator targetValidator;
    private final CounterMetricsService counterMetricsService;

    /**
     * Xử lý tương tác cảm xúc đa hình (Toggle Reaction) trên các thực thể (Post, Comment, Question...).
     * Triết lý giáo dục: Hệ thống tuân thủ chuẩn EdTech/LinkedIn, chỉ duy trì 3 cảm xúc tích cực:
     * - LIKE: Đồng tình / Đánh giá tốt
     * - LOVE: Tâm đắc / Rất hữu ích
     * - WOW: Khâm phục / Trầm trồ
     * Tuyệt đối không chứa cảm xúc tiêu cực (Dislike, Angry) để loại bỏ nỗi sợ bị đánh giá tiêu cực
     * (Fear of Negative Evaluation), bảo vệ tâm lý người học và thúc đẩy văn hóa chia sẻ tích cực.
     */
    @Override
    @Transactional
    public ReactResponseDto toggleReact(ReactRequest request) {
        targetValidator.validateTargetExists(request.getTargetType(), request.getTargetId());

        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        InteractionTargetType targetType = request.getTargetType();
        Long targetId = request.getTargetId();

        Optional<React> existingOpt = reactRepository.findByUserIdAndTargetTypeAndTargetId(
                currentUserId, targetType, targetId);

        String myReaction;

        if (existingOpt.isPresent()) {
            React existing = existingOpt.get();
            if (existing.getReactionType() == request.getReactionType()) {
                // Same reaction type -> Cancel react
                reactRepository.delete(existing);
                incrementTargetReactCounter(request.getTargetType(), targetId, -1);
                myReaction = null;
            } else {
                // Different reaction type -> Update react
                existing.setReactionType(request.getReactionType());
                reactRepository.save(existing);
                myReaction = request.getReactionType().name();
            }
        } else {
            // New react
            React newReact = new React();
            newReact.setTargetType(targetType);
            newReact.setTargetId(targetId);
            newReact.setReactionType(request.getReactionType());
            newReact.setUser(currentUser);
            newReact.setCreatedAt(Instant.now());

            reactRepository.save(newReact);
            incrementTargetReactCounter(request.getTargetType(), targetId, 1);
            myReaction = request.getReactionType().name();
        }

        // Calculate reaction counts breakdown and total
        List<React> allReacts = reactRepository.findByTargetTypeAndTargetId(targetType, targetId);

        Map<String, Integer> countsMap = new LinkedHashMap<>();
        for (ReactionType type : ReactionType.values()) {
            countsMap.put(type.name(), 0);
        }

        for (React r : allReacts) {
            if (r.getReactionType() != null) {
                String typeKey = r.getReactionType().name();
                countsMap.put(typeKey, countsMap.getOrDefault(typeKey, 0) + 1);
            }
        }

        return ReactResponseDto.builder()
                .myReaction(myReaction)
                .reactionCounts(countsMap)
                .totalCount(allReacts.size())
                .build();
    }

    private void incrementTargetReactCounter(InteractionTargetType targetType, Long targetId, int delta) {
        if (targetType == null || targetId == null)
            return;
        switch (targetType) {
            case POST -> counterMetricsService.incrementPostReacts(targetId, delta);
            case SESSION -> counterMetricsService.incrementSessionReacts(targetId, delta);
            case QUESTION -> counterMetricsService.incrementQuestionReacts(targetId, delta);
        }
    }
}
