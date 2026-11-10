import { Dialog } from '@base-ui/react/dialog';
import { Loader2, Mail, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useGroupInvitations, useRespondGroupInvitation } from '../hooks/useChat';

interface GroupInvitationsModalProps {
  invitations: ReturnType<typeof useGroupInvitations>;
  onClose: () => void;
  onJoined: (conversationId: number) => void;
}

export function GroupInvitationsModal({ invitations, onClose, onJoined }: GroupInvitationsModalProps) {
  const response = useRespondGroupInvitation();
  const items = invitations.data?.pages.flatMap((page) => page.items) ?? [];
  const respond = (invitationId: number, accept: boolean) => {
    response.mutate({ invitationId, accept }, {
      onSuccess: (conversationId) => {
        if (conversationId !== null) {
          onJoined(conversationId);
          onClose();
        }
      },
    });
  };

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !response.isPending) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-[70] bg-black/55" />
        <Dialog.Popup className="fixed top-1/2 left-1/2 z-[71] max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border border-border bg-card p-5 text-foreground shadow-xl">
          <header className="relative mb-4 border-b border-border pb-3 pr-12">
            <Dialog.Title className="flex items-center gap-2 text-lg font-semibold">
              <Mail aria-hidden="true" className="size-5 text-academic" /> Lời mời vào nhóm
            </Dialog.Title>
            <Dialog.Description className="mt-1 text-sm text-muted-foreground">
              Bạn có thể chấp nhận hoặc từ chối từng lời mời.
            </Dialog.Description>
            <Dialog.Close render={<Button variant="ghost" size="icon" aria-label="Đóng" />}
              className="absolute top-0 right-0" disabled={response.isPending}>
              <X aria-hidden="true" />
            </Dialog.Close>
          </header>
          {invitations.isPending ? (
            <p role="status" className="flex items-center gap-2 py-3 text-sm text-muted-foreground">
              <Loader2 aria-hidden="true" className="size-4 animate-spin" /> Đang tải lời mời…
            </p>
          ) : invitations.isLoadingError ? (
            <div className="space-y-2 py-3 text-sm">
              <p>Không thể tải lời mời.</p>
              <Button variant="outline" onClick={() => void invitations.refetch()}>Thử lại</Button>
            </div>
          ) : items.length === 0 ? (
            <p className="py-3 text-sm text-muted-foreground">Bạn chưa có lời mời nào đang chờ.</p>
          ) : (
            <ul className="space-y-2">
              {items.map((invitation) => (
                <li key={invitation.invitationId} className="space-y-2 rounded-xl border border-border bg-surface-subtle p-3">
                  <div className="flex items-center gap-2.5">
                    <UserAvatar name={invitation.groupName} avatarUrl={invitation.groupAvatarUrl} decorative />
                    <div className="min-w-0">
                      <p className="break-words text-sm font-semibold">{invitation.groupName}</p>
                      <p className="break-words text-sm text-muted-foreground">Lời mời từ {invitation.inviterName}</p>
                      <p className="text-xs text-muted-foreground">
                        Hết hạn {new Date(invitation.expiresAt).toLocaleDateString('vi-VN')}
                      </p>
                    </div>
                  </div>
                  <div className="flex flex-wrap justify-end gap-2">
                    <Button variant="outline" disabled={response.isPending}
                      onClick={() => respond(invitation.invitationId, false)}>Từ chối</Button>
                    <Button disabled={response.isPending}
                      onClick={() => respond(invitation.invitationId, true)}>Chấp nhận</Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
          {invitations.hasNextPage && (
            <Button variant="ghost" className="mt-2 w-full" disabled={invitations.isFetchingNextPage}
              onClick={() => void invitations.fetchNextPage()}>
              {invitations.isFetchingNextPage ? 'Đang tải…' : invitations.isFetchNextPageError ? 'Thử tải lại' : 'Xem thêm lời mời'}
            </Button>
          )}
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
