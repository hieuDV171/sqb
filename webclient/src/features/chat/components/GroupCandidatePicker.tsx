import { InfiniteListFooter } from '@/components/ui/infinite-list-footer';
import { useEffect, useRef, useState } from 'react';
import { Loader2, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useGroupCandidates } from '../hooks/useChat';
import type { GroupCandidateDto } from '../types/chat.types';

interface GroupCandidatePickerProps {
  conversationId?: number;
  selected: GroupCandidateDto[];
  onSelectionChange: (selected: GroupCandidateDto[]) => void;
  disabled?: boolean;
}

export function GroupCandidatePicker({ conversationId, selected, onSelectionChange, disabled }: GroupCandidatePickerProps) {
  const listRef = useRef<HTMLDivElement>(null);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  useEffect(() => {
    const timer = window.setTimeout(() => setQuery(search.trim()), 200);
    return () => window.clearTimeout(timer);
  }, [search]);
  const candidates = useGroupCandidates(conversationId, query);
  const items = candidates.data?.pages.flatMap((page) => page.items) ?? [];
  const waiting = candidates.isPending || search.trim() !== query;
  const selectedIds = new Set(selected.map((person) => person.userId));

  return (
    <fieldset disabled={disabled} className="min-w-0 space-y-2">
      <legend className="text-base font-semibold">Chọn người ({selected.length} đã chọn)</legend>
      <p className="text-sm text-muted-foreground">Bạn bè được thêm ngay. Những người có quan hệ theo dõi sẽ nhận lời mời.</p>
      <label className="relative block">
        <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
        <span className="sr-only">Tìm người theo tên</span>
        <input type="search" value={search} onChange={(event) => setSearch(event.target.value)}
          placeholder="Tìm theo tên"
          className="min-h-11 w-full rounded-xl border border-input bg-background pr-3 pl-9 text-base focus-visible:outline-2 focus-visible:outline-ring" />
      </label>
      <div ref={listRef} aria-busy={waiting} className="max-h-52 overflow-y-auto rounded-xl border border-border p-1">
        {waiting ? (
          <p role="status" className="flex items-center justify-center gap-2 p-4 text-sm text-muted-foreground">
            <Loader2 aria-hidden="true" className="size-4 animate-spin" /> Đang tải danh sách…
          </p>
        ) : candidates.isLoadingError ? (
          <div className="space-y-2 p-3 text-center text-sm">
            <p>Không thể tải danh sách.</p>
            <Button type="button" variant="outline" onClick={() => void candidates.refetch()}>Thử lại</Button>
          </div>
        ) : items.length === 0 ? (
          <p className="p-4 text-center text-sm text-muted-foreground">
            {query ? 'Không tìm thấy người phù hợp.' : 'Chưa có người nào có thể thêm hoặc mời.'}
          </p>
        ) : (
          items.map((person) => {
            const checked = selectedIds.has(person.userId);
            const relationship = person.friend ? 'Bạn bè · Thêm ngay'
              : person.following && person.follower ? 'Theo dõi lẫn nhau · Gửi lời mời'
              : person.following ? 'Đang theo dõi · Gửi lời mời' : 'Người theo dõi · Gửi lời mời';
            return (
              <label key={person.userId} className="flex min-h-12 cursor-pointer items-center gap-2.5 rounded-lg px-2 py-1.5 hover:bg-accent has-checked:bg-academic-soft">
                <UserAvatar name={person.fullName} avatarUrl={person.avatarUrl} decorative className="size-8" />
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-base font-medium">{person.fullName}</span>
                  <span className="block text-sm text-muted-foreground">{relationship}</span>
                </span>
                <input type="checkbox" checked={checked} disabled={!checked && selected.length >= 199}
                  onChange={(event) => onSelectionChange(event.target.checked
                    ? [...selected, person]
                    : selected.filter((candidate) => candidate.userId !== person.userId))}
                  className="size-4 shrink-0 accent-primary" />
              </label>
            );
          })
        )}
        {!waiting && <InfiniteListFooter query={candidates} label="Xem thêm người" scrollRoot={listRef} />}
      </div>
    </fieldset>
  );
}
