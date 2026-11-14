export type ViolationType =
  | 'SPAM'
  | 'HARASSMENT'
  | 'HATE_SPEECH'
  | 'INAPPROPRIATE_CONTENT'
  | 'COPYRIGHT'
  | 'OTHER';

export type ReportTargetType = 'USER' | 'POST' | 'COMMENT';

export type ReportStatus = 'PENDING' | 'RESOLVED';

export interface CreateReportRequestDto {
  violationType: ViolationType;
  description?: string;
  evidenceKeys?: string[];
  targetType: ReportTargetType;
  targetId: number;
}

export interface ReportResponseDto {
  reportId: number;
  targetType: ReportTargetType;
  targetId: number;
  status: ReportStatus;
  createdAt: string;
}

export type ReportOutcome = 'DISMISSED' | 'TARGET_UNAVAILABLE' | 'ALREADY_MODERATED' | 'ACTION_TAKEN';
export type ReportTargetState = 'AVAILABLE' | 'HIDDEN' | 'SUSPENDED' | 'UNAVAILABLE';
export type ReportDecision = 'DISMISS' | 'HIDE_CONTENT' | 'WARN_ACCOUNT' | 'SUSPEND_ACCOUNT';
export type ModerationActionType = Exclude<ReportDecision, 'DISMISS'> | 'RESTORE_CONTENT' | 'LIFT_SUSPENSION';
export type SuspensionDays = 1 | 7 | 30;

export interface ReportFilters {
  status: ReportStatus | 'ALL';
  targetType?: ReportTargetType;
  violationType?: ViolationType;
}

export interface AdminReportSummary {
  id: number;
  targetType: ReportTargetType | null;
  targetId: number | null;
  violationType: ViolationType | null;
  status: ReportStatus | null;
  outcome: ReportOutcome | null;
  createdAt: string | null;
  preview: string | null;
  targetState: ReportTargetState;
  pendingCount: number;
}

export interface ReportPerson {
  id: number;
  fullName: string | null;
}

export interface ReportSnapshot {
  authorId: number | null;
  content: string | null;
  updatedAt: string | null;
}

export interface ModerationAction {
  id: number;
  type: ModerationActionType;
  reason: string;
  createdAt: string;
  suspendedUntil: string | null;
  reversedActionId: number | null;
  reportId: number;
  admin: ReportPerson;
}

export interface AdminReportDetail {
  report: AdminReportSummary;
  version: number;
  description: string | null;
  reporter: ReportPerson | null;
  reviewer: ReportPerson | null;
  reviewedAt: string | null;
  resolutionNote: string | null;
  snapshot: ReportSnapshot | null;
  current: ReportSnapshot | null;
  suspendedUntil: string | null;
  activeActionId: number | null;
  allowedActions: ReportDecision[];
  history: ModerationAction[];
  hasEvidence: boolean;
  hasLegacyEvidence: boolean;
}

export interface ResolveReportRequest {
  version: number;
  action: ReportDecision;
  reason: string;
  suspensionDays?: SuspensionDays;
}
