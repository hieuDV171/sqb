export interface CursorPaginationDto {
  after?: number | null;
  hasNext: boolean;
}

export type SessionStatus = 'PENDING' | 'REVIEWING' | 'RESOLVED';

export type QuestionSource = 'HOMO_SAPIENS' | 'LLM';

export interface QuestionOption {
  key: string;
  text: string;
  isCorrect: boolean;
  mediaUrl?: string;
  mediaId?: number;
}

export interface QuestionProposeDto {
  content: string;
  mediaUrls?: string[];
  options: QuestionOption[];
  explanation?: string;
  llmGenerated: boolean;
  confidence?: number; // 0.00 - 4.00
}

export interface ProposeSessionRequest {
  title: string;
  content?: string;
  sourceUrl?: string;
  subjectId: number;
  questions: QuestionProposeDto[];
}

export interface ProposeSessionResponse {
  sessionId: number;
  sessionCode: string;
  subjectId: number;
  questionCount: number;
  status: SessionStatus;
  createdAt: string;
}

export interface SubjectResponse {
  subjectId: number;
  code: string;
  name: string;
}

export interface SubmissionSessionSummaryDto {
  sessionId: number;
  sessionCode: string;
  title: string;
  content: string;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  questionCounts: number;
  createdAt: string;
  reactCount: number;
  commentCount: number;
  status?: SessionStatus;
}

export interface SubmissionsResponse {
  contents: SubmissionSessionSummaryDto[];
  pagination: CursorPaginationDto;
}

export interface MySubmissionQuestionDto {
  questionId: number;
  content: string;
  imageUrls: string[];
  options: QuestionOption[];
  explanation?: string;
  source?: string;
  confidenceScore?: number;
  commentCount: number;
  reactCount: number;
  ratingCount: number;
}

export interface MySubmissionDetailResponse {
  sessionId: number;
  sessionCode: string;
  title: string;
  content: string;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  commentCount: number;
  reactCount: number;
  reviewedByLecturer?: string | null;
  reviewedAt?: string | null;
  createdAt: string;
  status?: SessionStatus;
  questions: MySubmissionQuestionDto[];
}

export interface QuestionUpdateDto {
  questionId?: number;
  content: string;
  mediaUrls?: string[];
  options: QuestionOption[];
  explanation?: string;
  source?: QuestionSource;
  confidence?: number;
}

export interface UpdateSubmissionSessionRequest {
  title?: string;
  content?: string;
  sourceUrl?: string;
  subjectId?: number;
  questions?: QuestionUpdateDto[];
}

export interface GetMySubmissionsParams {
  after?: number;
  limit?: number;
  subject_id?: number;
  status?: SessionStatus;
}

// Game 2 Gamification types
export interface Game2PredictionRequest {
  sessionId: number;
  predictedLlmCount?: number | null;
  predictedHumanCount?: number | null;
}

export type GameType =
  | 'GAME_1_PARTICIPANTS'
  | 'GAME_2_APPROVED_QUESTIONS'
  | 'GAME_3_AUTHOR_CONFIDENCE'
  | 'GAME_4_SUBJECT_BANK_SIZE'
  | 'GAME_5_REPORT_ERROR'
  | 'GAME_6_LLM_IDENTIFICATION';

export type PredictionTargetType =
  | 'COURSE_CLASS'
  | 'SESSION'
  | 'SUBJECT'
  | 'GAME6_SESSION';

export type PredictionStatus = 'PENDING' | 'RESOLVED';

export interface GamePredictionResponse {
  predictionId: number;
  gameType: GameType;
  targetType: PredictionTargetType;
  targetId: number;
  predictionData: any;
  actualData: any;
  status: PredictionStatus;
  isCorrect: boolean;
  createdAt: string;
  resolvedAt: string | null;
}
