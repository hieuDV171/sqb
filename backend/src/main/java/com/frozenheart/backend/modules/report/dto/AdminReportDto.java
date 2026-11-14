package com.frozenheart.backend.modules.report.dto;

import com.frozenheart.backend.core.entity.socialinteraction.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class AdminReportDto {
    private AdminReportDto() {}

    public enum TargetState { AVAILABLE, HIDDEN, SUSPENDED, UNAVAILABLE }
    public enum Decision { DISMISS, HIDE_CONTENT, WARN_ACCOUNT, SUSPEND_ACCOUNT }

    public record Summary(Long id, String targetType, Long targetId, ViolationType violationType, ReportStatus status,
                          ReportOutcome outcome, Instant createdAt, String preview, TargetState targetState,
                          long pendingCount) {}

    public record Person(Long id, String fullName) {}

    public record Action(Long id, ModerationActionType type, String reason, Instant createdAt, Instant suspendedUntil,
                         Long reversedActionId, Long reportId, Person admin) {}

    public record Detail(Summary report, Long version, String description, Person reporter, Person reviewer,
                         Instant reviewedAt, String resolutionNote, ReportSnapshot snapshot, ReportSnapshot current,
                         Instant suspendedUntil, Long activeActionId, List<Decision> allowedActions, List<Action> history,
                         boolean hasEvidence, boolean hasLegacyEvidence) {}

    public record ResolveRequest(@NotNull @PositiveOrZero Long version, @NotNull Decision action,
                                 @NotBlank @Size(max = 500) String reason, Integer suspensionDays) {}

    public record ReverseRequest(@NotBlank @Size(max = 500) String reason) {}

    public record Evidence(String url) {}
}
