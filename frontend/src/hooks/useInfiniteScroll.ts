import { useEffect, useRef } from 'react';

export function useInfiniteScroll(
  enabled: boolean,
  fetchNextPage: (options: { cancelRefetch: boolean }) => Promise<unknown>,
  pageCount: number,
) {
  const sentinel = useRef<HTMLDivElement>(null);
  const supportsAutoLoad = typeof IntersectionObserver !== 'undefined';

  useEffect(() => {
    const target = sentinel.current;
    if (!enabled || !supportsAutoLoad || !target) return;

    const observer = new IntersectionObserver(([entry]) => {
      if (!entry?.isIntersecting) return;
      observer.disconnect();
      void fetchNextPage({ cancelRefetch: false });
    }, { rootMargin: '0px 0px 320px 0px' });
    observer.observe(target);
    return () => observer.disconnect();
  }, [enabled, fetchNextPage, pageCount, supportsAutoLoad]);

  return { sentinel, supportsAutoLoad };
}
