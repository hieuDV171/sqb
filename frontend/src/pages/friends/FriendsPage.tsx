import { useState } from 'react';
import { AlertDialog } from '@base-ui/react/alert-dialog';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Check, Loader2, MessageSquare, Shield, Users, X } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { chatService } from '@/features/chat';
import { ConnectionList, ConnectionRow, type ConnectionPerson } from '@/features/profile/components/ConnectionList';
import { connectionDate, useFriendLists } from '@/features/profile/hooks/useConnections';
import { userService } from '@/services/userService';
import { toast } from '@/stores/useToastStore';

const tabs = [
  { id: 'friends', label: 'Bạn bè', empty: 'Chưa có bạn bè' },
  { id: 'received', label: 'Lời mời đến', empty: 'Chưa có lời mời đến' },
  { id: 'sent', label: 'Lời mời đã gửi', empty: 'Chưa có lời mời đã gửi' },
] as const;

type Action = 'accept' | 'decline' | 'cancel' | 'unfriend' | 'message';

export function FriendsPage() {
  const [params, setParams] = useSearchParams();
  const active = tabs.find((tab) => tab.id === params.get('tab')) ?? tabs[0];
  const [search, setSearch] = useState('');
  const [confirm, setConfirm] = useState<{ id: number; name: string }>();
  const lists = useFriendLists(active.id);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const mutation = useMutation({
    mutationFn: async ({ action, id }: { action: Action; id: number }) => {
      switch (action) {
        case 'accept': await userService.acceptFriendRequest({ requesterId: id }); break;
        case 'decline': await userService.declineFriendRequest({ requesterId: id }); break;
        case 'cancel': await userService.cancelSentFriendRequest(id); break;
        case 'unfriend': await userService.unfriend(id); break;
        case 'message': {
          const { data } = await chatService.createDirectConversation({ targetUserId: id });
          if (!data?.conversationId) throw new Error('Không nhận được cuộc trò chuyện');
          navigate(`/messages/${data.conversationId}`);
        }
      }
    },
    onSuccess: async (_, { action }) => {
      if (action === 'message') return;
      setConfirm(undefined);
      toast.success({ accept: 'Đã chấp nhận lời mời', decline: 'Đã từ chối lời mời', cancel: 'Đã rút lời mời', unfriend: 'Đã hủy kết bạn' }[action]);
      await Promise.all(['myFriends', 'receivedRequests', 'sentRequests', 'userProfile', 'followingList', 'followersList'].map(
        (key) => queryClient.invalidateQueries({ queryKey: [key] }),
      ));
    },
    onError: (error, { action }) => {
      toast.error(error.message || 'Không thể hoàn tất thao tác. Hãy thử lại.');
      if (action === 'accept' || action === 'decline' || action === 'cancel') {
        for (const key of ['myFriends', 'receivedRequests', 'sentRequests', 'userProfile']) {
          void queryClient.invalidateQueries({ queryKey: [key] });
        }
      }
    },
  });

  const rows: { person: ConnectionPerson; date: string; key: string }[] = active.id === 'friends'
    ? (lists.friends.data?.pages.flatMap((page) => page.items) ?? []).map((person) => ({
        person, date: person.friendsSince, key: String(person.userId),
      }))
    : active.id === 'received'
      ? (lists.received.data?.pages.flatMap((page) => page.items) ?? []).map(({ requester, createdAt }) => ({
          person: requester, date: createdAt, key: String(requester.userId ?? createdAt),
        }))
      : (lists.sent.data?.pages.flatMap((page) => page.items) ?? []).map(({ receiver, createdAt }) => ({
          person: receiver, date: createdAt, key: String(receiver.userId ?? createdAt),
        }));
  const term = search.trim().toLocaleLowerCase('vi-VN');
  const visible = rows.filter(({ person }) => person.fullName.toLocaleLowerCase('vi-VN').includes(term));
  const dateLabel = { friends: 'Kết bạn từ', received: 'Nhận ngày', sent: 'Gửi ngày' }[active.id];

  function actions(person: ConnectionPerson) {
    const id = person.userId;
    const pending = mutation.isPending && mutation.variables?.id === id ? mutation.variables?.action : undefined;
    if (active.id === 'sent') return (
      <>
        <Badge variant="secondary">Đang chờ phản hồi</Badge>
        {id != null && (
          <Button variant="outline" disabled={mutation.isPending}
            onClick={() => mutation.mutate({ action: 'cancel', id })}>
            {pending === 'cancel' ? 'Đang rút…' : 'Rút lời mời'}
          </Button>
        )}
      </>
    );
    if (id == null) return null;
    if (active.id === 'received') return (
      <>
        <Button disabled={mutation.isPending} onClick={() => mutation.mutate({ action: 'accept', id })}>
          {pending === 'accept' ? <Loader2 className="motion-safe:animate-spin" aria-hidden="true" /> : <Check aria-hidden="true" />}
          {pending === 'accept' ? 'Đang chấp nhận…' : 'Chấp nhận'}
        </Button>
        <Button variant="outline" disabled={mutation.isPending} onClick={() => mutation.mutate({ action: 'decline', id })}>
          <X aria-hidden="true" /> {pending === 'decline' ? 'Đang từ chối…' : 'Từ chối'}
        </Button>
      </>
    );
    return (
      <>
        <Button variant="outline" disabled={mutation.isPending} onClick={() => mutation.mutate({ action: 'message', id })}>
          <MessageSquare aria-hidden="true" /> {pending === 'message' ? 'Đang mở…' : 'Nhắn tin'}
        </Button>
        <Button variant="ghost" disabled={mutation.isPending} onClick={() => setConfirm({ id, name: person.fullName })}>
          Hủy kết bạn
        </Button>
      </>
    );
  }

  return (
    <div className="mx-auto max-w-4xl space-y-5 pb-10">
      <header className="flex flex-wrap items-start justify-between gap-4 rounded-2xl border border-border bg-card p-4 sm:p-6">
        <div className="space-y-2">
          <p className="flex items-center gap-2 text-sm font-semibold text-primary"><Users aria-hidden="true" className="size-4" /> Kết nối trong trường</p>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Bạn bè</h1>
          <p className="text-sm text-muted-foreground">Xem bạn bè và quản lý lời mời kết bạn.</p>
        </div>
        <Button nativeButton={false} variant="outline" render={<Link to="/blocked-users" />}><Shield aria-hidden="true" /> Danh sách chặn</Button>
      </header>
      <nav aria-label="Danh sách kết nối" className="flex flex-wrap gap-2">
        {tabs.map((tab) => (
          <Button key={tab.id} aria-pressed={tab.id === active.id} variant={tab.id === active.id ? 'default' : 'outline'}
            onClick={() => { setSearch(''); setParams(tab.id === 'friends' ? {} : { tab: tab.id }); }}>
            {tab.label}
          </Button>
        ))}
      </nav>
      <div className="max-w-sm space-y-2">
        <Label htmlFor="connection-search">Tìm theo tên trong các mục đã tải</Label>
        <Input id="connection-search" type="search" value={search} onChange={(event) => setSearch(event.target.value)} />
      </div>
      {active.id === 'sent' && <p className="text-sm text-muted-foreground">Bạn có thể rút lời mời khi người nhận chưa phản hồi.</p>}
      <ConnectionList query={lists[active.id]} count={visible.length} empty={active.empty} searching={Boolean(term)}>
        {visible.map(({ person, date, key }) => {
          const formatted = connectionDate(date);
          return <ConnectionRow key={key} person={person} description={formatted && `${dateLabel} ${formatted}`} actions={actions(person)} />;
        })}
      </ConnectionList>

      <AlertDialog.Root open={Boolean(confirm)} onOpenChange={(open) => { if (!open && !mutation.isPending) setConfirm(undefined); }}>
        <AlertDialog.Portal>
          <AlertDialog.Backdrop className="fixed inset-0 z-50 bg-foreground/45 dark:bg-black/65" />
          <AlertDialog.Popup className="fixed left-1/2 top-1/2 z-50 max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 text-card-foreground shadow-xl">
            <AlertDialog.Title className="wrap-break-word text-lg font-semibold">Hủy kết bạn với {confirm?.name}?</AlertDialog.Title>
            <AlertDialog.Description className="text-sm text-muted-foreground">Bạn có thể gửi lời mời kết bạn lại sau.</AlertDialog.Description>
            <div className="flex flex-wrap justify-end gap-2">
              <Button variant="outline" disabled={mutation.isPending} onClick={() => setConfirm(undefined)}>Quay lại</Button>
              <Button variant="destructive" disabled={!confirm || mutation.isPending}
                onClick={() => { if (confirm) mutation.mutate({ action: 'unfriend', id: confirm.id }); }}>
                {mutation.isPending ? 'Đang hủy…' : 'Hủy kết bạn'}
              </Button>
            </div>
          </AlertDialog.Popup>
        </AlertDialog.Portal>
      </AlertDialog.Root>
    </div>
  );
}
