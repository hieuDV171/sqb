import type { CursorPagination } from '@/types/response.types';

// ==========================================
// 1. DIFFICULTY & MATRIX TYPES
// ==========================================

export type DifficultyDto = 'UNCLASSIFIED' | 'EASY' | 'MEDIUM' | 'HARD';

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
  createdAt: string;
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
  paperSize?: 'A4' | 'Letter';
}

export interface ExportQuestionsParams {
  subjectId: number;
  format?: 'pdf' | 'excel';
  includeAnswer?: boolean;
}
