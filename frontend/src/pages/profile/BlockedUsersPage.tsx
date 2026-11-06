import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ArrowLeft, Loader2, Shield } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ConnectionList, ConnectionRow } from '@/features/profile/components/ConnectionList';
import { connectionDate, useBlockedList } from '@/features/profile/hooks/useConnections';
import { userService } from '@/services/userService';
import { toast } from '@/stores/useToastStore';

export function BlockedUsersPage() {
  const query = useBlockedList();
  const queryClient = useQueryClient();
  const unblock = useMutation({
    mutationFn: (id: number) => userService.unblockUser(id),
    onSuccess: async (_, id) => {
      toast.success('Đã bỏ chặn người dùng');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['blockedUsers'] }),
        queryClient.invalidateQueries({ queryKey: ['userProfile', id] }),
        queryClient.invalidateQueries({ queryKey: ['activity-feeds'] }),
      ]);
    },
    onError: () => toast.error('Không thể bỏ chặn. Hãy thử lại.'),
  });

  const people = query.data?.pages.flatMap((page) => page.items) ?? [];
  return (
    <div className="mx-auto max-w-4xl space-y-5 pb-10">
      <Button nativeButton={false} variant="ghost" render={<Link to="/friends" />}><ArrowLeft aria-hidden="true" /> Bạn bè</Button>
      <header className="space-y-2 rounded-2xl border border-border bg-card p-4 sm:p-6">
        <p className="flex items-center gap-2 text-sm font-semibold text-primary"><Shield className="size-4" aria-hidden="true" /> Quyền riêng tư</p>
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Danh sách chặn</h1>
        <p className="text-sm text-muted-foreground">Quản lý những tài khoản bạn đã chặn.</p>
      </header>
      <ConnectionList query={query} count={people.length} empty="Bạn chưa chặn ai">
        {people.map((person) => {
          const date = connectionDate(person.blockedAt);
          const pending = unblock.isPending && unblock.variables === person.userId;
          return <ConnectionRow key={person.userId} person={person} link={false} description={date && `Đã chặn ngày ${date}`}
            actions={
              <Button variant="outline" disabled={unblock.isPending} onClick={() => unblock.mutate(person.userId)}>
                {pending && <Loader2 className="motion-safe:animate-spin" aria-hidden="true" />}
                {pending ? 'Đang bỏ chặn…' : 'Bỏ chặn'}
              </Button>
            } />;
        })}
      </ConnectionList>
    </div>
  );
}
