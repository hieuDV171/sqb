import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { userService } from '@/services/userService';
import { toast } from '@/stores/useToastStore';
import { ShieldAlert, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface BlockUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetUserId: number;
  targetUserName: string;
}

export function BlockUserModal({
  isOpen,
  onClose,
  targetUserId,
  targetUserName,
}: BlockUserModalProps) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const blockMutation = useMutation({
    mutationFn: async () => {
      return await userService.blockUser(targetUserId);
    },
    onSuccess: () => {
      toast.success(`Đã chặn người dùng ${targetUserName}`);
      queryClient.invalidateQueries({ queryKey: ['blockedUsers'] });
      queryClient.invalidateQueries({ queryKey: ['userProfile', targetUserId] });
      queryClient.invalidateQueries({ queryKey: ['followingList'] });
      queryClient.invalidateQueries({ queryKey: ['followersList'] });
      queryClient.invalidateQueries({ queryKey: ['myFriends'] });
      onClose();
      navigate('/feed');
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Không thể chặn người dùng này');
    },
  });

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in-50 duration-200">
      <div className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200/80 dark:border-slate-800 p-6 text-center">
        <div className="w-14 h-14 rounded-2xl bg-red-50 dark:bg-red-950/50 text-red-600 dark:text-red-400 flex items-center justify-center mx-auto mb-4 ring-8 ring-red-50/50 dark:ring-red-950/20">
          <ShieldAlert className="w-7 h-7" />
        </div>

        <h3 className="text-lg font-bold text-slate-900 dark:text-white">
          Chặn người dùng {targetUserName}?
        </h3>

        <p className="mt-2 text-xs sm:text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
          Khi chặn, người này sẽ không thể xem bài viết của bạn, không thể gửi tin nhắn hoặc lời mời kết bạn. Bạn cũng sẽ hủy kết bạn và hủy theo dõi người này nếu có.
        </p>

        <div className="mt-6 flex items-center justify-center gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={blockMutation.isPending}
            className="flex-1"
          >
            Hủy bỏ
          </Button>
          <Button
            type="button"
            variant="destructive"
            onClick={() => blockMutation.mutate()}
            disabled={blockMutation.isPending}
            className="flex-1 gap-2"
          >
            {blockMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <span>Xác nhận chặn</span>
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
