package com.frozenheart.backend.modules.report.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.socialinteraction.Report;
import com.frozenheart.backend.core.entity.socialinteraction.ReportStatus;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.report.constant.ReportTargetType;
import com.frozenheart.backend.modules.report.dto.CreateReportRequestDto;
import com.frozenheart.backend.modules.report.dto.ReportResponseDto;
import com.frozenheart.backend.modules.report.repository.ReportRepository;
import com.frozenheart.backend.modules.report.service.ReportService;
import com.frozenheart.backend.modules.socialinteraction.repository.CommentRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MediaService mediaService;

    @Override
    @Transactional
    public ReportResponseDto createReport(CreateReportRequestDto request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        validateTarget(request.getTargetType(), request.getTargetId(), currentUserId);

        List<MediaItem> mediaItems = null;
        if (request.getEvidenceUrls() != null && !request.getEvidenceUrls().isEmpty()) {
            List<String> nonBlankUrls = request.getEvidenceUrls().stream()
                    .filter(url -> url != null && !url.isBlank())
                    .collect(Collectors.toList());

            if (!nonBlankUrls.isEmpty()) {
                mediaService.confirmMediaPermanent(nonBlankUrls);
                mediaItems = nonBlankUrls.stream()
                        .map(url -> MediaItem.of(url))
                        .collect(Collectors.toList());
            }
        }

        Instant now = Instant.now();
        Report report = Report.builder()
                .targetType(request.getTargetType().name())
                .targetId(request.getTargetId())
                .violationType(request.getViolationType())
                .description(request.getDescription())
                .evidenceUrls(mediaItems)
                .status(ReportStatus.PENDING)
                .reporter(currentUser)
                .createdAt(now)
                .build();

        Report saved = reportRepository.save(report);

        return ReportResponseDto.builder()
                .reportId(saved.getId())
                .targetType(request.getTargetType())
                .targetId(saved.getTargetId())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private void validateTarget(ReportTargetType targetType, Long targetId, Long currentUserId) {
        if (targetType == null || targetId == null) {
            throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER, "Target type và target ID là bắt buộc");
        }

        switch (targetType) {
            case USER -> {
                if (Objects.equals(targetId, currentUserId)) {
                    throw new AppException(ResponseCode.CANNOT_INTERACT_WITH_SELF, "Không thể tự báo cáo chính mình");
                }
                if (!userRepository.existsById(targetId)) {
                    throw new AppException(ResponseCode.USER_NOT_FOUND, "Không tìm thấy người dùng được báo cáo");
                }
            }
            case POST -> {
                if (!postRepository.existsById(targetId)) {
                    throw new AppException(ResponseCode.POST_NOT_FOUND, "Không tìm thấy bài viết được báo cáo");
                }
            }
            case COMMENT -> {
                if (!commentRepository.existsById(targetId)) {
                    throw new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy bình luận được báo cáo");
                }
            }
            default ->
                throw new AppException(ResponseCode.REPORT_TARGET_NOT_FOUND, "Loại mục tiêu báo cáo không hợp lệ");
        }
    }
}
