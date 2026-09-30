import type { CursorPaginationDto, QuestionOption } from '@/features/session/types/session.types';

// ==========================================
// 16.1 GET /api/v1/sessions/pending
// ==========================================

export interface PendingSessionAuthorDto {
  userId?: number;
  fullName: string;
  avatarUrl?: string;
  frameUrl?: string;
}

export interface PendingSessionItemDto {
  sessionId: number;
  topic?: string;
  title: string;
  content: string;
  author: PendingSessionAuthorDto;
  status: string; // PENDING, REVIEWING, RESOLVED
  createdAt: string;
}

export interface PendingSessionsResponse {
  items: PendingSessionItemDto[];
  pagination: CursorPaginationDto;
}

export interface GetPendingSessionsParams {
  after?: number;
  limit?: number;
  subject_id?: number;
  sort_by?: string;
}

// ==========================================
// 16.2 GET /api/v1/sessions/{sessionId}
// ==========================================

export type DuplicateDetectionTier =
  | 'RULE_BASED'
  | 'TRIGRAM'
  | 'P_HASH'
  | 'TEXT_EMBEDDING'
  | 'IMAGE_EMBEDDING';

export interface DuplicateWarning {
  questionIndex: number;
  similarQuestionId: number;
  similarityScore: number;
  tier: DuplicateDetectionTier;
  matchedSnipet?: string;
  mediaUrl?: string;
}

export interface QuestionEditLogDto {
  editLogId: number;
  actorType: 'SYSTEM' | 'LECTURER' | 'ADMIN' | string;
  actorName: string;
  status: string;
  beforeState?: unknown;
  afterState?: unknown;
  hallucinationAudit?: unknown;
  createdAt: string;
}

export interface SessionQuestionReviewDto {
  questionId: number;
  content: string;
  imageUrls?: string[];
  options: QuestionOption[];
  correctAnswer: string;
  explanation?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EDITING' | string;
  editLog?: QuestionEditLogDto | null;
}

export interface SessionDetailReviewResponse {
  items: SessionQuestionReviewDto[];
  duplicateWarnings: DuplicateWarning[];
}

// ==========================================
// 16.3 POST /api/v1/sessions/approve
// ==========================================

export interface ApproveQuestionsRequest {
  questionIds: number[];
}

export interface ApproveQuestionsResponse {
  pointsEarned: number;
}

// ==========================================
// 16.4 POST /api/v1/sessions/reject
// ==========================================

export interface RejectQuestionsRequest {
  questionIds: number[];
  reason?: string;
}

// ==========================================
// 16.5 PUT /api/v1/questions/{questionId}
// ==========================================

export interface EditQuestionRequest {
  content?: string;
  options?: QuestionOption[];
  explanation?: string;
  autoApprove?: boolean;
}

export interface EditQuestionResponse {
  questionId: number;
  status: string;
  reviewedBy: number;
  reviewedAt: string;
}
