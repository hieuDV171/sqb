import { InfiniteListFooter } from '@/components/ui/infinite-list-footer';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Users } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { AvatarWithFrame } from './AvatarWithFrame';

export interface ConnectionPerson {
  userId?: number | null;
  fullName: string;
  avatarUrl?: string | null;
  frameUrl?: string | null;
}

export function ConnectionRow({ person, description, actions, link = true }: {
  person: ConnectionPerson;
  description?: string;
  actions?: ReactNode;
  link?: boolean;
}) {
  const identity = (
    <>
      <AvatarWithFrame avatarUrl={person.avatarUrl} frameUrl={person.frameUrl} name={person.fullName} size="sm" />
      <span className="min-w-0">
        <span className="block wrap-break-word font-semibold">{person.fullName}</span>
        {description && <span className="block wrap-break-word text-sm text-muted-foreground">{description}</span>}
      </span>
    </>
  );
  return (
    <li className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
      {link && person.userId != null ? (
        <Link to={`/users/${person.userId}`} className="flex min-h-11 min-w-0 items-center gap-3 rounded-lg hover:text-primary">{identity}</Link>
      ) : (
        <div className="flex min-w-0 items-center gap-3">{identity}</div>
      )}
      {actions && <div className="flex flex-wrap items-center gap-2 sm:shrink-0">{actions}</div>}
    </li>
  );
}

interface ListQuery {
  isPending: boolean;
  isError: boolean;
  isFetchNextPageError: boolean;
  isFetchingNextPage: boolean;
  isFetching: boolean;
  data?: { pages: readonly unknown[] };
  hasNextPage: boolean;
  refetch: () => Promise<unknown>;
  fetchNextPage: (options: { cancelRefetch: boolean }) => Promise<unknown>;
}

export function ConnectionList({ query, count, searching = false, empty, children }: {
  query: ListQuery;
  count: number;
  searching?: boolean;
  empty: string;
  children: ReactNode;
}) {
  if (query.isPending) {
    return (
      <div role="status" aria-label="Đang tải danh sách" className="divide-y divide-border rounded-2xl border border-border bg-card">
        {[0, 1, 2].map((key) => (
          <div key={key} className="flex items-center gap-3 p-4">
            <Skeleton className="size-10 rounded-full" />
            <Skeleton className="h-5 w-40 max-w-1/2" />
          </div>
        ))}
      </div>
    );
  }
  if (query.isError && !query.isFetchNextPageError && count === 0) {
    return <ErrorState title="Không thể tải danh sách" onRetry={() => void query.refetch()} />;
  }
  return (
    <div className="space-y-3">
      {query.isError && !query.isFetchNextPageError && (
        <div className="flex flex-wrap items-center gap-2">
          <p role="alert" className="text-sm text-destructive">Không thể cập nhật danh sách.</p>
          <Button variant="outline" onClick={() => void query.refetch()}>Thử lại</Button>
        </div>
      )}
      {count > 0 ? (
        <ul className="divide-y divide-border rounded-2xl border border-border bg-card">{children}</ul>
      ) : (
        <EmptyState icon={Users} title={searching ? 'Chưa có kết quả khớp' : empty}
          description={searching ? 'Hãy thử từ khóa khác hoặc tải thêm kết quả nếu còn.' : undefined} />
      )}
      <InfiniteListFooter query={query} auto={count > 0 || !searching} />
    </div>
  );
}
