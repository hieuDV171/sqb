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
  evidenceUrls?: string[];
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
