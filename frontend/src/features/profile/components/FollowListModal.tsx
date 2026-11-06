import { useState } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Loader2, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { userService } from '@/services/userService';
import { toast } from '@/stores/useToastStore';
import { useFollowLists } from '../hooks/useConnections';
import { ConnectionList, ConnectionRow } from './ConnectionList';

type FollowTab = 'following' | 'followers';

export function FollowListModal({ isOpen, onClose, initialTab = 'following' }: {
  isOpen: boolean;
  onClose: () => void;
  initialTab?: FollowTab;
}) {
  const [tab, setTab] = useState<FollowTab>(initialTab);
  const [search, setSearch] = useState('');
  const queryClient = useQueryClient();
  const lists = useFollowLists(isOpen, tab);
  const mutation = useMutation({
    mutationFn: async ({ id, following }: { id: number; following: boolean }) => {
      if (following) await userService.unfollowUser(id);
      else await userService.followUser(id);
    },
    onSuccess: async (_, { following }) => {
      toast.success(following ? 'Đã hủy theo dõi' : 'Đã theo dõi');
      await Promise.all(['followingList', 'followersList', 'userProfile'].map(
        (key) => queryClient.invalidateQueries({ queryKey: [key] }),
      ));
    },
    onError: () => toast.error('Không thể cập nhật theo dõi. Hãy thử lại.'),
  });

  const rows = tab === 'following'
    ? (lists.following.data?.pages.flatMap((page) => page.items) ?? []).map((person) => ({ person, following: true }))
    : (lists.followers.data?.pages.flatMap((page) => page.items) ?? []).map((person) => ({ person, following: person.followingBack }));
  const term = search.trim().toLocaleLowerCase('vi-VN');
  const visible = rows.filter(({ person }) =>
    [person.fullName, person.code, person.schoolFaculty].some((value) => value?.toLocaleLowerCase('vi-VN').includes(term)),
  );

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-foreground/45 dark:bg-black/65" />
        <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-xl -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border border-border bg-card text-card-foreground shadow-xl">
          <header className="space-y-3 border-b border-border p-4">
            <div className="flex items-start justify-between gap-2">
              <div>
                <Dialog.Title className="text-xl font-semibold">Mạng lưới theo dõi</Dialog.Title>
                <Dialog.Description className="mt-1 text-sm text-muted-foreground">Quản lý các kết nối theo dõi của bạn.</Dialog.Description>
              </div>
              <Button variant="ghost" size="icon" aria-label="Đóng danh sách theo dõi" onClick={onClose}><X aria-hidden="true" /></Button>
            </div>
            <div className="flex flex-wrap gap-2" role="group" aria-label="Danh sách theo dõi">
              <Button aria-pressed={tab === 'following'} variant={tab === 'following' ? 'default' : 'outline'}
                onClick={() => { setTab('following'); setSearch(''); }}>Đang theo dõi</Button>
              <Button aria-pressed={tab === 'followers'} variant={tab === 'followers' ? 'default' : 'outline'}
                onClick={() => { setTab('followers'); setSearch(''); }}>Người theo dõi</Button>
            </div>
            <div className="space-y-2">
              <Label htmlFor="follow-search">Tìm tên, mã số hoặc khoa trong các mục đã tải</Label>
              <Input id="follow-search" type="search" value={search} onChange={(event) => setSearch(event.target.value)} />
            </div>
          </header>
          <div className="p-4">
            <ConnectionList query={lists[tab]} count={visible.length} searching={Boolean(term)}
              empty={tab === 'following' ? 'Bạn chưa theo dõi ai' : 'Bạn chưa có người theo dõi'}>
              {visible.map(({ person, following }) => {
                const pending = mutation.isPending && mutation.variables?.id === person.userId;
                return (
                  <ConnectionRow key={person.userId} person={person}
                    description={[person.code, person.schoolFaculty].filter(Boolean).join(' · ')}
                    actions={
                      <Button variant={following ? 'outline' : 'default'} disabled={mutation.isPending}
                        onClick={() => mutation.mutate({ id: person.userId, following })}>
                        {pending && <Loader2 className="motion-safe:animate-spin" aria-hidden="true" />}
                        {pending ? 'Đang cập nhật…' : following ? 'Hủy theo dõi' : tab === 'followers' ? 'Theo dõi lại' : 'Theo dõi'}
                      </Button>
                    } />
                );
              })}
            </ConnectionList>
          </div>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
