import type { CursorPagination } from '@/types/response.types';
import type { QuestionDifficulty } from '@/lib/questionDifficulty';

// ==========================================
// 1. DIFFICULTY & MATRIX TYPES
// ==========================================

export type DifficultyDto = QuestionDifficulty;

export interface DifficultyDistribution {
  easy?: number;
  medium?: number;
  hard?: number;
  unclassified?: number;
}

export interface Statistic {
  easyCount?: number;
  mediumCount?: number;
  hardCount?: number;
  unclassifiedCount?: number;
}

export interface ExamBankSummary {
  subjectId: number;
  totalQuestions: number;
  statistic: Statistic;
  topics: { topicId?: number | null; topicName: string; totalQuestions: number; statistic: Statistic }[];
}

// ==========================================
// 2. EXAM GENERATION & DETAIL
// ==========================================

export interface GenerateExamRequest {
  subjectId: number;
  title?: string;
  questionCount?: number;
  difficultyDistribution?: DifficultyDistribution;
  topicWeights?: Record<string, number>;
  shuffleOptions?: boolean;
  includeAnswerKey?: boolean;
  classIds: number[];
}

export interface OptionDto {
  key: string;
  text: string;
  mediaUrl?: string;
  isCorrect?: boolean;
}

export interface ExamQuestionDto {
  order: number;
  questionId: number;
  content: string;
  imageUrls?: string[];
  options: OptionDto[];
  difficulty?: DifficultyDto;
  topic?: string;
}

export interface ExamDetailResponse {
  examId: number;
  title: string;
  subjectName: string;
  questionCount: number;
  questions: ExamQuestionDto[];
  statistic?: Statistic;
  difficultyMatrix?: { requested?: DifficultyDistribution | null; actual: Statistic } | null;
  topicMatrix?: { rows: ExamTopicMatrixRow[] } | null;
  createdAt: string;
}

export interface ExamTopicMatrixRow {
  topicId?: number | null;
  topicName: string;
  requestedWeight?: number | null;
  requestedCount?: number | null;
  actualCount: number;
  actualByDifficulty: Statistic;
}

// ==========================================
// 3. EXAM LISTING & EXPORT
// ==========================================

export interface ExamItem {
  examId: number;
  title: string;
  subjectName: string;
  questionCount: number;
  createdAt: string;
  statistic?: Statistic;
  downloadUrl?: string;
  subjectCode?: string;
}

export type ExamSummaryDto = ExamItem;

export interface ExamListResponse {
  items: ExamItem[];
  pagination?: CursorPagination;
}

export interface ExportResponse {
  downloadUrl: string;
  fileName?: string;
  fileSizeMb?: number;
  questionsCount?: number;
  expiresAt?: string;
}

export interface ExportExamParams {
  format?: 'pdf' | 'excel';
  includeAnswerKey?: boolean;
}

export interface ExportQuestionsParams {
  subjectId: number;
  format?: 'pdf' | 'excel';
  includeAnswer?: boolean;
}
