import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import type { CursorPagination as Pagination } from '@/types/response.types';

export type PageCursor = { before?: number; after?: number };

export function CursorPagination({ pagination, cursor, busy, onChange }: {
  pagination?: Pagination;
  cursor: PageCursor;
  busy: boolean;
  onChange: (cursor: PageCursor) => void;
}) {
  const previous = pagination?.hasPrev ? pagination.before : undefined;
  const next = pagination?.hasNext ? pagination.after : undefined;
  const onFirstPage = cursor.after == null && cursor.before == null;
  const unavailable = !pagination?.hasPrev && !pagination?.hasNext && !onFirstPage;
  if (onFirstPage && previous == null && next == null) return null;

  return (
    <nav aria-label="Phân trang" aria-busy={busy} className="flex justify-center gap-2 p-3">
      <Button type="button" variant="outline" className="min-h-11"
        disabled={busy || (previous == null && !unavailable)}
        onClick={() => onChange(previous == null ? {} : { before: previous })}>
        <ChevronLeft aria-hidden="true" /> Trang trước
      </Button>
      <Button type="button" variant="outline" className="min-h-11"
        disabled={busy || next == null}
        onClick={() => { if (next != null) onChange({ after: next }); }}>
        Trang sau <ChevronRight aria-hidden="true" />
      </Button>
    </nav>
  );
}
