import { AlertDialog } from '@base-ui/react/alert-dialog';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Loader2, ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { userService } from '@/services/userService';
import { toast } from '@/stores/useToastStore';

interface BlockUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetUserId: number;
  targetUserName: string;
}

export function BlockUserModal({ isOpen, onClose, targetUserId, targetUserName }: BlockUserModalProps) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const mutation = useMutation({
    mutationFn: () => userService.blockUser(targetUserId),
    onSuccess: () => {
      toast.success(`Đã chặn ${targetUserName}`);
      for (const key of ['blockedUsers', 'userProfile', 'followingList', 'followersList',
        'myFriends', 'receivedRequests', 'sentRequests', 'activity-feeds']) {
        void queryClient.invalidateQueries({ queryKey: [key] });
      }
      onClose();
      navigate('/feed');
    },
    onError: () => toast.error('Không thể chặn người dùng này. Hãy thử lại.'),
  });

  return (
    <AlertDialog.Root open={isOpen} onOpenChange={(open) => { if (!open && !mutation.isPending) onClose(); }}>
      <AlertDialog.Portal>
        <AlertDialog.Backdrop className="fixed inset-0 z-50 bg-foreground/45 dark:bg-black/65" />
        <AlertDialog.Popup className="fixed left-1/2 top-1/2 z-50 max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 text-card-foreground shadow-xl">
          <div className="grid size-11 place-items-center rounded-xl bg-destructive/10 text-destructive">
            <ShieldAlert aria-hidden="true" className="size-5" />
          </div>
          <AlertDialog.Title className="wrap-break-word text-lg font-semibold">Chặn {targetUserName}?</AlertDialog.Title>
          <AlertDialog.Description className="text-sm leading-relaxed text-muted-foreground">
            Khi chặn, các kết nối bạn bè và theo dõi giữa hai tài khoản sẽ bị gỡ. Bạn có thể bỏ chặn trong danh sách chặn.
          </AlertDialog.Description>
          <div className="flex flex-wrap justify-end gap-2">
            <Button variant="outline" disabled={mutation.isPending} onClick={onClose}>Quay lại</Button>
            <Button variant="destructive" disabled={mutation.isPending} onClick={() => mutation.mutate()}>
              {mutation.isPending && <Loader2 className="motion-safe:animate-spin" aria-hidden="true" />}
              {mutation.isPending ? 'Đang chặn…' : 'Chặn người dùng'}
            </Button>
          </div>
        </AlertDialog.Popup>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
