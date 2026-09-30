export interface GlobalResponse<T> {
  code: string;
  message: string;
  data: T;
}

export interface CursorPagination {
  before?: number | null;
  after?: number | null;
  hasPrev?: boolean;
  hasNext?: boolean;
}

export interface CursorResponse<T> {
  items: T[];
  pagination: CursorPagination;
}