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
  fullName?: string;
  studentLecturerCode?: string;
  email?: string;
  bio?: string;
  avatarUrl?: string;
  avatarFrameUrl?: string;
  schoolFaculty?: string;
  major?: string;
  role?: string;
  totalProposedQuestions?: number;
  badgesCount?: number;
  followersCount?: number;
}

export interface QuestionOptionDto {
  key: string;
  text: string;
  mediaUrl?: string;
}

export interface QuestionSearchResultDto {
  questionId: number;
  questionCode?: string;
  content: string;
  subjectId?: number;
  subjectName?: string;
  topicId?: number;
  topicName?: string;
  status?: string;
  difficulty?: string;
  options?: QuestionOptionDto[];
  explanation?: string;
  imageUrls?: string[];
  avgRating?: number;
  ratingCount?: number;
  reactCount?: number;
  commentCount?: number;
  createdAt?: string;
}

export interface SessionSearchResultDto {
  sessionId: number;
  title: string;
  content?: string;
  subjectId?: number;
  subjectName?: string;
  status?: string;
  questionCount?: number;
  reactCount?: number;
  commentCount?: number;
  createdAt?: string;
}

export interface SubjectSearchResultDto {
  subjectId: number;
  subjectCode: string;
  subjectName: string;
}

export interface RecordSearchRequest {
  query: string;
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
}
