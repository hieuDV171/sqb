import type { CursorPagination } from '@/types/response.types';

export type SearchType =
  | 'ALL'
  | 'POST'
  | 'USER'
  | 'QUESTION'
  | 'SESSION'
  | 'SUBJECT';

export interface PostSearchResultDto {
  postId: number;
  content: string;
  authorId: number;
  authorName: string;
  authorAvatarUrl?: string;
  authorFrameUrl?: string;
  postType?: string;
  mediaUrls?: string[];
  reactCount?: number;
  commentCount?: number;
  createdAt?: string;
}

export interface UserSearchResultDto {
  userId: number;
  username: string;
  fullName?: string;
  avatarUrl?: string;
  frameUrl?: string;
  role?: string;
  schoolFaculty?: string;
  studentLecturerCode?: string;
  relationshipStatus?: string;
}

export interface QuestionSearchResultDto {
  questionId: number;
  content: string;
  subjectId?: number;
  subjectName?: string;
  difficulty?: string;
  topic?: string;
  totalAnswers?: number;
  correctAnswersRate?: number;
}

export interface SessionSearchResultDto {
  sessionId: number;
  title: string;
  subjectName?: string;
  creatorName?: string;
  status?: string;
  questionCount?: number;
  createdAt?: string;
}

export interface SubjectSearchResultDto {
  subjectId: number;
  code: string;
  name: string;
  department?: string;
  creditCount?: number;
}

export interface SearchResultItemDto {
  type: SearchType;
  relevanceScore?: number;
  post?: PostSearchResultDto;
  user?: UserSearchResultDto;
  question?: QuestionSearchResultDto;
  session?: SessionSearchResultDto;
  subject?: SubjectSearchResultDto;
}

export interface GlobalSearchResponseDto {
  items: SearchResultItemDto[];
  pagination?: CursorPagination;
  totalHits?: number;
}

export interface SavedSearchDto {
  savedSearchId: number;
  queryText: string;
  lastSearchedAt: string;
}

export interface SavedAndTrendingSearchesResponseDto {
  savedSearches: SavedSearchDto[];
  trendingSearches: string[];
}

export interface DeleteSavedSearchResponseDto {
  savedSearchId: number;
  deleted: boolean;
}
