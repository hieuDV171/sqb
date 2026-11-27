import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';

export function InfiniteListFooter({ query, label = 'Tải thêm', auto = true, scrollRoot }: {
  query: {
    data?: { pages: readonly unknown[] };
    hasNextPage: boolean;
    isFetching: boolean;
    isFetchingNextPage: boolean;
    isFetchNextPageError: boolean;
    fetchNextPage: (options: { cancelRefetch: boolean }) => Promise<unknown>;
  };
  label?: string;
  auto?: boolean;
  scrollRoot?: { readonly current: HTMLElement | null };
}) {
  const { sentinel, supportsAutoLoad } = useInfiniteScroll(
    auto && query.hasNextPage && !query.isFetching && !query.isFetchNextPageError,
    query.fetchNextPage, query.data?.pages.length ?? 0, scrollRoot,
  );
  if (!query.hasNextPage && !query.isFetchNextPageError) return null;

  return (
    <div className="grid justify-items-center gap-2 text-center">
      <div ref={sentinel} className="h-px" aria-hidden="true" />
      {query.isFetchingNextPage ? (
        <p role="status" className="flex items-center gap-2 py-2 text-sm text-muted-foreground">
          <Loader2 aria-hidden="true" className="size-4 motion-safe:animate-spin" /> Đang tải thêm…
        </p>
      ) : (
        <Button type="button" variant="outline" disabled={query.isFetching}
          className={auto && supportsAutoLoad && !query.isFetchNextPageError ? 'sr-only focus-visible:not-sr-only' : 'min-h-11'}
          onClick={() => void query.fetchNextPage({ cancelRefetch: false })}>
          {query.isFetchNextPageError ? 'Thử tải lại' : label}
        </Button>
      )}
    </div>
  );
}
