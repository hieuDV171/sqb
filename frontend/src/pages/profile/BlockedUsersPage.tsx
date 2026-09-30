import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { userService } from '@/services/userService';
import { toast } from '@/stores/useToastStore';
import { AvatarWithFrame } from '@/features/profile/components/AvatarWithFrame';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import {
  ShieldAlert,
  ShieldCheck,
  Loader2,
  Clock,
  ArrowLeft,
} from 'lucide-react';

export function BlockedUsersPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const {
    data: blockedData,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['blockedUsers'],
    queryFn: async () => {
      const res = await userService.getBlockedUsers(undefined, 50);
      return res.data;
    },
  });

  const unblockMutation = useMutation({
    mutationFn: async (targetUserId: number) => {
      return await userService.unblockUser(targetUserId);
    },
    onSuccess: () => {
      toast.success('Đã bỏ chặn người dùng');
      queryClient.invalidateQueries({ queryKey: ['blockedUsers'] });
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Không thể bỏ chặn người dùng này');
    },
  });

  const blockedList = blockedData?.items || [];

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-12">
        {/* Header */}
        <div className="flex items-center justify-between bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200/80 dark:border-slate-800 shadow-sm">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="p-2 rounded-xl text-slate-500 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <h1 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <ShieldAlert className="w-5 h-5 text-red-500" />
                <span>Danh sách Người dùng bị chặn</span>
              </h1>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
                Những người dùng này sẽ không thể liên hệ, xem thông tin hay tìm thấy trang cá nhân của bạn
              </p>
            </div>
          </div>

          <span className="text-xs font-mono font-bold px-3 py-1 bg-red-50 dark:bg-red-950/50 text-red-600 dark:text-red-400 rounded-full border border-red-200 dark:border-red-900/50">
            {blockedData?.totalBlocked ?? blockedList.length} bị chặn
          </span>
        </div>

        {/* Content list */}
        {isLoading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                className="p-4 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 flex items-center gap-3"
              >
                <Skeleton className="w-12 h-12 rounded-full" />
                <div className="space-y-2 flex-1">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-3 w-24" />
                </div>
                <Skeleton className="h-8 w-24 rounded-xl" />
              </div>
            ))}
          </div>
        ) : isError ? (
          <ErrorState
            title="Không thể tải danh sách bị chặn"
            message={(error as any)?.message}
            onRetry={() => refetch()}
          />
        ) : blockedList.length === 0 ? (
          <EmptyState
            icon={ShieldCheck}
            title="Danh sách trống"
            description="Bạn chưa chặn bất kỳ người dùng nào trên hệ thống."
            actionLabel="Về Bảng tin"
            onAction={() => navigate('/feed')}
          />
        ) : (
          <div className="divide-y divide-slate-100 dark:divide-slate-800/80 bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 overflow-hidden shadow-xs">
            {blockedList.map((user) => (
              <div
                key={user.userId}
                className="p-4 sm:p-5 flex items-center justify-between gap-4 transition-colors hover:bg-slate-50/50 dark:hover:bg-slate-800/30"
              >
                <div className="flex items-center gap-3.5 min-w-0 flex-1">
                  <AvatarWithFrame
                    avatarUrl={user.avatarUrl}
                    frameUrl={user.frameUrl}
                    name={user.fullName}
                    size="md"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-bold text-slate-800 dark:text-slate-100 truncate">
                      {user.fullName}
                    </p>
                    <p className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
                      <Clock className="w-3 h-3" />
                      <span>
                        Bị chặn từ {new Date(user.blockedAt).toLocaleDateString('vi-VN')}
                      </span>
                    </p>
                  </div>
                </div>

                <Button
                  size="sm"
                  variant="outline"
                  disabled={unblockMutation.isPending}
                  onClick={() => unblockMutation.mutate(user.userId)}
                  className="gap-1.5 text-xs rounded-xl border-slate-300 hover:border-indigo-500 hover:text-indigo-600"
                >
                  {unblockMutation.isPending ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <>
                      <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                      <span>Bỏ chặn</span>
                    </>
                  )}
                </Button>
              </div>
            ))}
          </div>
        )}
      </div>
  );
}
