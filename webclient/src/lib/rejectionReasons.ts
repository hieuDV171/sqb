export const REJECTION_REASON_LABELS: Record<string, string> = {
  DUPLICATE: 'Trùng lặp',
  INCORRECT_ANSWER: 'Sai hoặc thiếu đáp án đúng',
  AMBIGUOUS_OR_INCOMPLETE: 'Đề bài mơ hồ hoặc thiếu dữ kiện',
  OUT_OF_SCOPE: 'Ngoài phạm vi môn học',
  LOW_QUALITY: 'Chất lượng chưa đạt',
  OTHER: 'Lý do khác',
  UNSPECIFIED: 'Không nêu lý do',
  AUTO_REJECTED_ON_COMPLETION: 'Chưa được chọn khi hoàn tất phiên duyệt',
};

export function getRejectionReasonLabel(code?: string | null) {
  if (!code) return 'Không nêu lý do';
  return REJECTION_REASON_LABELS[code] ?? 'Lý do khác';
}
