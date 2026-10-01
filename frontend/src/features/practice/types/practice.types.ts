import type { QuestionOption, CursorPaginationDto, SubmissionSessionSummaryDto } from '@/features/session/types/session.types';

export type QuestionSource = 'HOMO_SAPIENS' | 'LLM';

export interface MyInteractionDto {
  answered: boolean;
  rated: boolean;
}

export interface HiddenFieldsDto {
  correctAnswer?: string;
  explanation?: string;
}

export interface PracticeQuestionDto {
  questionId: number;
  questionCode?: string;
  content: string;
  imageUrls?: string[];
  options: QuestionOption[];
  source: QuestionSource;
  createdAt: string;
  updatedAt?: string;
  myInteraction: MyInteractionDto;
  hiddenFields?: HiddenFieldsDto | null;
  reactCount: number;
  commentCount: number;
  ratingCount: number;
}

export interface SessionQuestionsResponse {
  sessionId: number;
  sessionCode?: string;
  title: string;
  content?: string;
  subjectId?: number;
  subjectName?: string;
  subjectCode?: string;
  createdAt: string;
  totalQuestions: number;
  questions: PracticeQuestionDto[];
}

export interface AnswerQuestionRequest {
  selectedOptions: string[];
  questionUpdatedAt?: string;
}

export interface AnswerQuestionResponse {
  isCorrect: boolean;
  correctAnswer: string;
  explanation?: string;
}

export interface RatingSummaryDto {
  avgRating: number;
  totalRatings: number;
  distribution: Record<string, number>;
}

export interface QuestionStatisticsResponse {
  totalAnswer: number;
  correctRate: number; // 0.0 to 1.0 (or percentage)
  optionDistribution: Record<string, number>;
  ratingSummary: RatingSummaryDto;
}

export interface RateQuestionRequest {
  rating: number; // 0 to 4
  isError?: boolean;
  comment?: string;
}

export interface RateQuestionResponse {
  newAvgRating: number;
  newRatingCount: number;
}

export interface RatingItemDto {
  userId: number;
  rating: number;
  isError: boolean;
  comment?: string;
  createdAt: string;
}

export interface QuestionRatingsResponse {
  items: RatingItemDto[];
  pagination: CursorPaginationDto;
}

export interface GetUserSessionsParams {
  after?: number;
  limit?: number;
  subject_id?: number;
  status?: string;
}

export interface UserSubmissionsResponse {
  contents: SubmissionSessionSummaryDto[];
  pagination: CursorPaginationDto;
}
