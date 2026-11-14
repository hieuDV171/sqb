package com.frozenheart.backend.modules.report.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.notification.*;
import com.frozenheart.backend.core.entity.socialinteraction.*;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.security.AccountWebSocketSessions;
import com.frozenheart.backend.modules.device.repository.UserDeviceRepository;
import com.frozenheart.backend.modules.notification.service.NotificationWriter;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.report.constant.ReportTargetType;
import com.frozenheart.backend.modules.report.dto.AdminReportDto.*;
import com.frozenheart.backend.modules.report.repository.ModerationActionRepository;
import com.frozenheart.backend.modules.report.repository.ReportRepository;
import com.frozenheart.backend.modules.report.service.ReportTargets.Key;
import com.frozenheart.backend.modules.report.service.ReportTargets.View;
import com.frozenheart.backend.modules.socialinteraction.repository.CommentRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminReportService {
    private static final Set<Integer> SUSPENSION_DAYS = Set.of(1, 7, 30);

    private final ReportRepository reports;
    private final ModerationActionRepository actions;
    private final ReportTargets targets;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final CounterMetricsService counters;
    private final UserDeviceRepository devices;
    private final NotificationWriter notifications;
    private final ReportEvidenceStorage evidence;
    private final R2Properties storageProperties;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public CursorResponse<Summary> queue(ReportStatus status, ReportTargetType targetType, ViolationType violation,
                                         Long after, Integer limit) {
        requireAdmin();
        int size = pageSize(limit);
        return page(reports.findQueue(status, targetType == null ? null : targetType.name(), violation,
                cursor(after), PageRequest.of(0, size + 1)), size);
    }

    @Transactional(readOnly = true)
    public CursorResponse<Summary> related(Long id, Long after, Integer limit) {
        requireAdmin();
        Report report = reports.findById(id).orElseThrow(AdminReportService::missing);
        int size = pageSize(limit);
        return page(reports.findRelated(report.getTargetType(), report.getTargetId(), id, cursor(after),
                PageRequest.of(0, size + 1)), size);
    }

    @Transactional(readOnly = true)
    public Detail detail(Long id) {
        requireAdmin();
        return loadDetail(id);
    }

    @Transactional(readOnly = true)
    public Evidence evidence(Long id) {
        requireAdmin();
        Report report = reports.findById(id).orElseThrow(AdminReportService::missing);
        if (report.getPrivateEvidenceKey() == null) throw missing();
        return new Evidence(evidence.readUrl(report.getPrivateEvidenceKey()));
    }

    @Transactional
    public Evidence importEvidence(Long id) {
        requireAdmin();
        Report report = reports.findForReview(id).orElseThrow(AdminReportService::missing);
        if (report.getPrivateEvidenceKey() != null) return new Evidence(evidence.readUrl(report.getPrivateEvidenceKey()));
        if (!canImportEvidence(report) || report.getReporter() == null) throw missing();
        String sourceUrl = report.getEvidenceUrls().getFirst().url();
        String key = evidence.attachHistorical(report.getReporter().getId(), sourceUrl);
        report.setPrivateEvidenceKey(key);
        return new Evidence(evidence.readUrl(key));
    }

    private boolean canImportEvidence(Report report) {
        return report.getEvidenceUrls() != null && !report.getEvidenceUrls().isEmpty()
                && (report.getReviewedAt() == null || !report.getReviewedAt().isBefore(Instant.now()
                    .minus(Math.max(1, storageProperties.getReportEvidenceRetentionDays()), ChronoUnit.DAYS)));
    }

    @Transactional
    public Detail resolve(Long id, ResolveRequest request) {
        Long adminId = requireAdmin();
        // Lock the report before touching its target. No target mutation locks a second report.
        Report report = reports.findForReview(id).orElseThrow(AdminReportService::missing);
        if (report.getStatus() != ReportStatus.PENDING || !Objects.equals(report.getVersion(), request.version())) {
            throw conflict("Báo cáo đã thay đổi. Hãy xem trạng thái mới trước khi xử lý.");
        }
        if (request.action() == Decision.SUSPEND_ACCOUNT) {
            if (request.suspensionDays() == null || !SUSPENSION_DAYS.contains(request.suspensionDays())) {
                throw invalid("Chọn thời hạn đình chỉ 24 giờ, 7 ngày hoặc 30 ngày.");
            }
        } else if (request.suspensionDays() != null) {
            throw invalid("Chỉ nhập thời hạn khi đình chỉ tài khoản.");
        }

        Instant now = Instant.now();
        User admin = users.getReferenceById(adminId);
        String reason = request.reason().strip();
        Key key = key(report);
        var notices = new ArrayList<Notification>();
        ReportOutcome outcome = switch (request.action()) {
            case DISMISS -> {
                View view = targets.load(List.of(key)).getOrDefault(key, View.unavailable());
                yield switch (view.state()) {
                    case UNAVAILABLE -> ReportOutcome.TARGET_UNAVAILABLE;
                    case HIDDEN, SUSPENDED -> ReportOutcome.ALREADY_MODERATED;
                    case AVAILABLE -> ReportOutcome.DISMISSED;
                };
            }
            case HIDE_CONTENT -> {
                changeContent(report, admin, reason, now, null, notices);
                yield ReportOutcome.ACTION_TAKEN;
            }
            case WARN_ACCOUNT, SUSPEND_ACCOUNT -> {
                changeAccount(report, admin, reason, now, request.suspensionDays(), null, notices);
                yield ReportOutcome.ACTION_TAKEN;
            }
        };

        report.setStatus(ReportStatus.RESOLVED);
        report.setOutcome(outcome);
        report.setReviewer(admin);
        report.setReviewedAt(now);
        report.setResolutionNote(reason);
        notice(notices, report.getReporter(), NotificationType.REPORT_REVIEWED, "Báo cáo đã được xem xét",
                "Ban quản trị đã xử lý báo cáo #" + id + ".", now);
        notifications.createAll(notices);
        reports.flush();
        return loadDetail(id);
    }

    @Transactional
    public Detail reverse(Long actionId, ReverseRequest request) {
        Long adminId = requireAdmin();
        ModerationAction action = actions.findById(actionId).orElseThrow(AdminReportService::missing);
        Report report = reports.findForReview(action.getReport().getId()).orElseThrow(AdminReportService::missing);
        var notices = new ArrayList<Notification>();
        Instant now = Instant.now();
        User admin = users.getReferenceById(adminId);
        switch (action.getType()) {
            case HIDE_CONTENT -> changeContent(report, admin, request.reason().strip(), now, actionId, notices);
            case SUSPEND_ACCOUNT -> changeAccount(report, admin, request.reason().strip(), now, null, actionId, notices);
            default -> throw invalid("Biện pháp này không hỗ trợ hoàn tác.");
        }
        notifications.createAll(notices);
        actions.flush();
        return loadDetail(report.getId());
    }

    private void changeContent(Report report, User admin, String reason, Instant now, Long reverseId,
                               List<Notification> notices) {
        boolean restore = reverseId != null;
        var type = restore ? ModerationActionType.RESTORE_CONTENT : ModerationActionType.HIDE_CONTENT;
        User author;
        String targetType = report.getTargetType();
        if (ReportTargetType.POST.name().equals(targetType)) {
            var post = posts.findForModeration(report.getTargetId())
                    .filter(item -> item.getDeletedAt() == null).orElseThrow(AdminReportService::targetChanged);
            requireExpectedAction(post.getActiveModerationAction(), reverseId);
            var action = recordAction(report, admin, type, reason, now, null, reverseId);
            post.setActiveModerationAction(restore ? null : action);
            author = post.getPoster();
            events.publishEvent(new ModerationPostSync.Requested(action.getId()));
        } else if (ReportTargetType.COMMENT.name().equals(targetType)) {
            var comment = comments.findForModeration(report.getTargetId())
                    .filter(item -> item.getDeletedAt() == null).orElseThrow(AdminReportService::targetChanged);
            requireExpectedAction(comment.getActiveModerationAction(), reverseId);
            var action = recordAction(report, admin, type, reason, now, null, reverseId);
            comment.setActiveModerationAction(restore ? null : action);
            author = comment.getUser();
            int delta = restore ? 1 : -1;
            if (comment.getTargetType() != null && comment.getTargetId() != null) {
                switch (comment.getTargetType()) {
                    case "POST" -> counters.incrementPostComments(comment.getTargetId(), delta);
                    case "QUESTION" -> counters.incrementQuestionComments(comment.getTargetId(), delta);
                    case "SESSION" -> counters.incrementSessionComments(comment.getTargetId(), delta);
                    default -> { }
                }
            }
        } else {
            throw invalid("Hành động chỉ áp dụng cho bài viết hoặc bình luận.");
        }
        String subject = ReportTargetType.POST.name().equals(targetType) ? "Bài viết" : "Bình luận";
        notice(notices, author, NotificationType.MODERATION_NOTICE, restore ? "Đã khôi phục nội dung" : "Đã ẩn nội dung",
                subject + " #" + report.getTargetId() + ". Lý do: " + reason, now);
    }

    private void changeAccount(Report report, User admin, String reason, Instant now, Integer days, Long reverseId,
                               List<Notification> notices) {
        if (!ReportTargetType.USER.name().equals(report.getTargetType())) {
            throw invalid("Hành động chỉ áp dụng cho tài khoản.");
        }
        User target = users.findByIdForUpdate(report.getTargetId()).filter(User::isActive)
                .orElseThrow(AdminReportService::targetChanged);
        if (reverseId == null && (target.getRole() == UserRole.ADMIN || Objects.equals(target.getId(), admin.getId()))) {
            throw invalid("Tài khoản quản trị chỉ có thể được xem xét và đóng đơn.");
        }
        if (reverseId != null) {
            requireExpectedAction(target.isSuspendedAt(now) ? target.getActiveModerationAction() : null, reverseId);
            recordAction(report, admin, ModerationActionType.LIFT_SUSPENSION, reason, now, null, reverseId);
            target.setSuspendedUntil(null);
            target.setActiveModerationAction(null);
            notice(notices, target, NotificationType.MODERATION_NOTICE, "Đã gỡ đình chỉ", reason, now);
        } else if (days != null) {
            if (target.isSuspendedAt(now)) throw targetChanged();
            Instant until = now.plus(days, ChronoUnit.DAYS);
            var action = recordAction(report, admin, ModerationActionType.SUSPEND_ACCOUNT, reason, now, until, null);
            target.setSuspendedUntil(until);
            target.setActiveModerationAction(action);
            target.setSessionEpoch(target.getSessionEpoch() == null ? 1L : target.getSessionEpoch() + 1);
            devices.deactivateForAccounts(List.of(target.getId()));
            events.publishEvent(new AccountWebSocketSessions.AccountSuspended(target.getId()));
            notice(notices, target, NotificationType.MODERATION_NOTICE, "Tài khoản bị đình chỉ",
                    "Thời hạn " + (days == 1 ? "24 giờ" : days + " ngày") + ". Lý do: " + reason, now);
        } else {
            recordAction(report, admin, ModerationActionType.WARN_ACCOUNT, reason, now, null, null);
            notice(notices, target, NotificationType.MODERATION_NOTICE, "Cảnh cáo tài khoản", reason, now);
        }
    }

    private ModerationAction recordAction(Report report, User admin, ModerationActionType type, String reason,
                                          Instant now, Instant until, Long reverseId) {
        return actions.save(ModerationAction.builder().report(report).admin(admin).targetType(report.getTargetType())
                .targetId(report.getTargetId()).type(type).reason(reason).createdAt(now)
                .suspendedUntil(until).reversedActionId(reverseId).build());
    }

    private Detail loadDetail(Long id) {
        Report report = reports.findDetails(id).orElseThrow(AdminReportService::missing);
        Key key = key(report);
        View current = targets.load(List.of(key)).getOrDefault(key, View.unavailable());
        var history = new ArrayList<>(actions.findHistory(key.type(), key.id(), PageRequest.of(0, 50)));
        // Keep the reversible action available even if newer warnings have filled the history preview.
        if (current.actionId() != null && history.stream().noneMatch(action -> current.actionId().equals(action.getId()))) {
            actions.findById(current.actionId()).ifPresent(history::add);
        }
        var peopleIds = Stream.concat(Stream.of(report.getReporter(), report.getReviewer()),
                history.stream().map(ModerationAction::getAdmin)).filter(Objects::nonNull).map(User::getId).distinct().toList();
        Map<Long, UserProfile> people = peopleIds.isEmpty() ? Map.of() : profiles.findAllById(peopleIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, profile -> profile));
        List<Action> actionHistory = history.stream().map(a -> new Action(a.getId(), a.getType(), a.getReason(),
                a.getCreatedAt(), a.getSuspendedUntil(), a.getReversedActionId(), a.getReport().getId(),
                person(a.getAdmin(), people))).toList();
        return new Detail(summary(report, current, pendingCounts(List.of(key)).getOrDefault(key, 0L)), report.getVersion(),
                report.getDescription(), person(report.getReporter(), people), person(report.getReviewer(), people),
                report.getReviewedAt(), report.getResolutionNote(), report.getSnapshot(), current.snapshot(),
                current.suspendedUntil(), current.actionId(), allowed(report, current), actionHistory,
                report.getPrivateEvidenceKey() != null, canImportEvidence(report));
    }

    private static List<Decision> allowed(Report report, View target) {
        if (report.getStatus() != ReportStatus.PENDING) return List.of();
        if (!ReportTargetType.POST.name().equals(report.getTargetType())
                && !ReportTargetType.COMMENT.name().equals(report.getTargetType())
                && !ReportTargetType.USER.name().equals(report.getTargetType())) return List.of(Decision.DISMISS);
        if (target.state() != TargetState.AVAILABLE && target.state() != TargetState.SUSPENDED) {
            return List.of(Decision.DISMISS);
        }
        if (ReportTargetType.USER.name().equals(report.getTargetType())) {
            if (target.role() == UserRole.ADMIN) return List.of(Decision.DISMISS);
            return target.state() == TargetState.SUSPENDED ? List.of(Decision.DISMISS, Decision.WARN_ACCOUNT)
                    : List.of(Decision.DISMISS, Decision.WARN_ACCOUNT, Decision.SUSPEND_ACCOUNT);
        }
        return List.of(Decision.DISMISS, Decision.HIDE_CONTENT);
    }

    private CursorResponse<Summary> page(List<Report> rows, int size) {
        boolean more = rows.size() > size;
        var page = rows.subList(0, Math.min(rows.size(), size));
        var keys = page.stream().map(AdminReportService::key).filter(key -> key.id() != null && key.type() != null)
                .distinct().toList();
        var current = targets.load(keys);
        var pending = pendingCounts(keys);
        var items = page.stream().map(report -> {
            var key = key(report);
            return summary(report, current.getOrDefault(key, View.unavailable()), pending.getOrDefault(key, 0L));
        }).toList();
        return new CursorResponse<>(items, CursorPaginationDto.builder().hasNext(more)
                .after(more ? page.getLast().getId() : null).build());
    }

    private Map<Key, Long> pendingCounts(List<Key> keys) {
        var valid = keys.stream().filter(key -> key.id() != null && key.type() != null).toList();
        if (valid.isEmpty()) return Map.of();
        return reports.countPendingForTargets(valid.stream().map(Key::id).distinct().toList(),
                        valid.stream().map(Key::type).distinct().toList()).stream()
                .collect(Collectors.toMap(row -> new Key(row.getTargetType(), row.getTargetId()), ReportRepository.PendingCount::getTotal));
    }

    private static Summary summary(Report report, View target, long pending) {
        var snapshot = report.getSnapshot() != null ? report.getSnapshot() : target.snapshot();
        String preview = snapshot == null ? null : snapshot.content();
        if (preview != null && preview.codePointCount(0, preview.length()) > 160) {
            preview = preview.substring(0, preview.offsetByCodePoints(0, 160)) + "…";
        }
        return new Summary(report.getId(), report.getTargetType(), report.getTargetId(), report.getViolationType(),
                report.getStatus(), report.getOutcome(), report.getCreatedAt(), preview, target.state(), pending);
    }

    private static void notice(List<Notification> notices, User receiver, NotificationType type, String title,
                               String body, Instant now) {
        if (receiver == null || !receiver.isActive()) return;
        notices.add(Notification.builder().receiver(receiver).type(type).category(NotificationCategory.SYSTEM)
                .title(title).body(body).creadtedAt(now).build());
    }

    private static Person person(User user, Map<Long, UserProfile> people) {
        if (user == null) return null;
        var profile = people.get(user.getId());
        return new Person(user.getId(), profile == null ? null : profile.getFullName());
    }

    private static Key key(Report report) {
        return new Key(report.getTargetType(), report.getTargetId());
    }

    private static Long requireAdmin() {
        JwtPayload actor = JwtPayload.getCurrentUserPayload();
        if (!UserRole.ADMIN.name().equals(actor.getRole())) throw new AppException(ResponseCode.ACCESS_DENIED);
        return actor.getUserId();
    }

    private static void requireExpectedAction(ModerationAction actual, Long expected) {
        if (!Objects.equals(ReportTargets.actionId(actual), expected)) throw targetChanged();
    }

    private static int pageSize(Integer size) {
        return size == null ? 20 : Math.clamp(size, 1, 50);
    }

    private static long cursor(Long after) {
        return after == null ? Long.MAX_VALUE : after;
    }

    private static AppException invalid(String message) {
        return new AppException(ResponseCode.INVALID_PARAMETER_VALUE, message);
    }

    private static AppException conflict(String message) {
        return new AppException(ResponseCode.REPORT_CONFLICT, message);
    }

    private static AppException targetChanged() {
        return conflict("Trạng thái đối tượng đã thay đổi. Hãy tải lại chi tiết.");
    }

    private static AppException missing() {
        return new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy báo cáo, biện pháp hoặc ảnh bằng chứng.");
    }
}
