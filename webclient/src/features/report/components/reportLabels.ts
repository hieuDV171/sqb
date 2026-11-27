import type {
  ModerationActionType, ReportDecision, ReportOutcome, ReportTargetState, ReportTargetType, ViolationType,
} from '../types/report.types';

export const targetLabels: Record<ReportTargetType, string> = {
  POST: 'Bài viết', COMMENT: 'Bình luận', USER: 'Tài khoản',
};

export const violationLabels: Record<ViolationType, string> = {
  SPAM: 'Nội dung rác hoặc quảng cáo',
  HARASSMENT: 'Quấy rối hoặc xúc phạm',
  HATE_SPEECH: 'Ngôn từ thù địch',
  INAPPROPRIATE_CONTENT: 'Nội dung không phù hợp',
  COPYRIGHT: 'Vi phạm bản quyền',
  OTHER: 'Lý do khác',
};

export const targetStateLabels: Record<ReportTargetState, string> = {
  AVAILABLE: 'Đang hiển thị', HIDDEN: 'Đã ẩn', SUSPENDED: 'Đang đình chỉ', UNAVAILABLE: 'Không còn khả dụng',
};

export const decisionLabels: Record<ReportDecision | ModerationActionType, string> = {
  DISMISS: 'Đóng báo cáo',
  HIDE_CONTENT: 'Ẩn nội dung',
  WARN_ACCOUNT: 'Cảnh cáo tài khoản',
  SUSPEND_ACCOUNT: 'Đình chỉ tài khoản',
  RESTORE_CONTENT: 'Khôi phục nội dung',
  LIFT_SUSPENSION: 'Gỡ đình chỉ',
};

export const outcomeLabels: Record<ReportOutcome, string> = {
  DISMISSED: 'Đã đóng',
  TARGET_UNAVAILABLE: 'Đối tượng không khả dụng',
  ALREADY_MODERATED: 'Đã có biện pháp',
  ACTION_TAKEN: 'Đã áp dụng biện pháp',
};

const dateFormat = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh',
});

export function formatReportDate(value: string | null) {
  return value ? dateFormat.format(new Date(value)) : '—';
}
