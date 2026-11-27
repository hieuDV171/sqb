import { useEffect, useRef, useState } from 'react';
import { useInfiniteQuery } from '@tanstack/react-query';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { InfiniteListFooter } from '@/components/ui/infinite-list-footer';
import { gamificationService } from '../services/gamificationService';
import type { BadgeAccount } from '../types/gamification.types';

const roleLabels = { STUDENT: 'Sinh viên', LECTURER: 'Giảng viên', ADMIN: 'Admin' };

export function BadgeAccountPicker({ badgeId, holders, selected, onChange }: {
  badgeId: number;
  holders: boolean;
  selected: BadgeAccount[];
  onChange: (accounts: BadgeAccount[]) => void;
}) {
  const [input, setInput] = useState('');
  const [term, setTerm] = useState('');
  const resultsRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const timer = window.setTimeout(() => setTerm(input.trim()), 250);
    return () => window.clearTimeout(timer);
  }, [input]);

  const accounts = useInfiniteQuery({
    queryKey: ['admin-badge-accounts', badgeId, holders, term],
    initialPageParam: undefined as number | undefined,
    queryFn: ({ pageParam, signal }) => gamificationService.searchBadgeAccounts(badgeId, holders, term, pageParam, signal)
      .then(({ data }) => data),
    getNextPageParam: ({ pagination }) => pagination.hasNext ? pagination.after ?? undefined : undefined,
  });
  const rows = accounts.data?.pages.flatMap((page) => page.items) ?? [];
  const isSearching = input.trim() !== term || accounts.isPending;

  return (
    <div className="space-y-2">
      <label className="block space-y-1 text-sm">Tìm theo họ tên hoặc email
        <Input type="search" value={input} onChange={(event) => setInput(event.target.value)} maxLength={100} />
      </label>
      {selected.length > 0 && <ul aria-label="Tài khoản đã chọn" className="flex flex-wrap gap-1.5">
        {selected.map((account) => <li key={account.userId}>
          <Button type="button" size="sm" variant="secondary" aria-label={`Bỏ chọn ${account.fullName}`}
            onClick={() => onChange(selected.filter((item) => item.userId !== account.userId))}>
            {account.fullName} <span aria-hidden="true">×</span>
          </Button>
        </li>)}
      </ul>}
      {isSearching ? <p role="status" className="text-sm text-muted-foreground">Đang tìm tài khoản…</p> : accounts.isLoadingError ? (
        <div role="alert" className="flex flex-wrap items-center gap-2 text-sm">Không thể tải tài khoản.
          <Button type="button" size="sm" variant="outline" onClick={() => void accounts.refetch()}>Thử lại</Button>
        </div>
      ) : rows.length === 0 ? (
        <p className="text-sm text-muted-foreground">{holders && !term ? 'Chưa có người giữ huy hiệu này.' : 'Không tìm thấy tài khoản phù hợp.'}</p>
      ) : (
        <div ref={resultsRef} className="max-h-52 space-y-1 overflow-y-auto rounded-xl border border-border p-1">
          {rows.map((account) => {
            const checked = selected.some((item) => item.userId === account.userId);
            return <label key={account.userId} className="flex min-h-11 cursor-pointer items-center gap-3 rounded-lg px-2 py-1.5 text-sm hover:bg-muted">
              <input type="checkbox" checked={checked} disabled={!checked && selected.length >= 100}
                onChange={() => onChange(checked ? selected.filter((item) => item.userId !== account.userId) : [...selected, account])} />
              <span className="min-w-0 wrap-break-word"><span className="block font-medium">{account.fullName}</span>
                <span className="text-xs text-muted-foreground">{roleLabels[account.role]}{account.email && ` · ${account.email}`}</span>
              </span>
            </label>;
          })}
          <InfiniteListFooter query={accounts} scrollRoot={resultsRef} label="Tải thêm tài khoản" />
        </div>
      )}
      {selected.length > 0 && <p role="status" className="text-xs text-muted-foreground">Đã chọn {selected.length}/100 tài khoản</p>}
    </div>
  );
}
