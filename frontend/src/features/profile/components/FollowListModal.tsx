import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { userService } from '@/services/userService';
import { toast } from '@/stores/useToastStore';
import { X, Search, UserCheck, UserPlus, Users, Loader2 } from 'lucide-react';
import { AvatarWithFrame } from './AvatarWithFrame';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { EmptyState } from '@/components/ui/empty-state';

interface FollowListModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialTab?: 'following' | 'followers';
  userId?: number;
}

export function FollowListModal({
  isOpen,
  onClose,
  initialTab = 'following',
}: FollowListModalProps) {
  const [activeTab, setActiveTab] = useState<'following' | 'followers'>(initialTab);
  const [searchQuery, setSearchQuery] = useState('');
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  // Queries
  const followingQuery = useQuery({
    queryKey: ['followingList'],
    queryFn: async () => {
      const res = await userService.getFollowing(undefined, 50);
      return res.data;
    },
    enabled: isOpen && activeTab === 'following',
  });

  const followersQuery = useQuery({
    queryKey: ['followersList'],
    queryFn: async () => {
      const res = await userService.getFollowers(undefined, 50);
      return res.data;
    },
    enabled: isOpen && activeTab === 'followers',
  });

  // Follow/Unfollow mutation
  const followMutation = useMutation({
    mutationFn: async ({ targetUserId, isCurrentlyFollowing }: { targetUserId: number; isCurrentlyFollowing: boolean }) => {
      if (isCurrentlyFollowing) {
        return await userService.unfollowUser(targetUserId);
      } else {
        return await userService.followUser(targetUserId);
      }
    },
    onSuccess: (_, variables) => {
      toast.success(variables.isCurrentlyFollowing ? 'Đã hủy theo dõi' : 'Đã theo dõi người dùng');
      queryClient.invalidateQueries({ queryKey: ['followingList'] });
      queryClient.invalidateQueries({ queryKey: ['followersList'] });
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Thao tác không thành công');
    },
  });

  if (!isOpen) return null;

  const currentQuery = activeTab === 'following' ? followingQuery : followersQuery;
  const rawItems = (activeTab === 'following' ? followingQuery.data?.items : followersQuery.data?.items) || [];

  const filteredItems = rawItems.filter((item) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      item.fullName?.toLowerCase().includes(q) ||
      item.schoolFaculty?.toLowerCase().includes(q) ||
      item.code?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in-50 duration-200">
      <div className="relative w-full max-w-lg bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200/80 dark:border-slate-800 overflow-hidden flex flex-col max-h-[85vh]">
        {/* Header with Tabs */}
        <div className="p-4 sm:p-5 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <Users className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
              <span>Mạng lưới kết nối</span>
            </h3>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Tab Selector */}
          <div className="grid grid-cols-2 p-1 bg-slate-200/60 dark:bg-slate-800/80 rounded-2xl gap-1">
            <button
              type="button"
              onClick={() => setActiveTab('following')}
              className={`py-2 px-3 text-xs sm:text-sm font-semibold rounded-xl transition-all cursor-pointer ${
                activeTab === 'following'
                  ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-xs'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
              }`}
            >
              Đang theo dõi ({followingQuery.data?.totalFollowing ?? 0})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('followers')}
              className={`py-2 px-3 text-xs sm:text-sm font-semibold rounded-xl transition-all cursor-pointer ${
                activeTab === 'followers'
                  ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-xs'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
              }`}
            >
              Người theo dõi ({followersQuery.data?.totalFollowers ?? 0})
            </button>
          </div>

          {/* Search bar inside modal */}
          <div className="relative mt-3">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo tên, mã số, khoa viện..."
              className="w-full pl-9 pr-4 py-2 text-xs sm:text-sm bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl outline-none focus:border-indigo-500 text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
            />
          </div>
        </div>

        {/* Content list */}
        <div className="overflow-y-auto p-4 sm:p-5 divide-y divide-slate-100 dark:divide-slate-800/60 flex-1">
          {currentQuery.isLoading ? (
            <div className="space-y-4">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="flex items-center gap-3 py-2">
                  <Skeleton className="w-12 h-12 rounded-full" />
                  <div className="flex-1 space-y-1.5">
                    <Skeleton className="h-4 w-36" />
                    <Skeleton className="h-3 w-24" />
                  </div>
                  <Skeleton className="h-8 w-20 rounded-xl" />
                </div>
              ))}
            </div>
          ) : filteredItems.length === 0 ? (
            <EmptyState
              icon={Users}
              title={
                searchQuery
                  ? 'Không tìm thấy kết quả phù hợp'
                  : activeTab === 'following'
                  ? 'Chưa theo dõi ai'
                  : 'Chưa có người theo dõi'
              }
              description={
                searchQuery
                  ? 'Thử thay đổi từ khóa tìm kiếm'
                  : activeTab === 'following'
                  ? 'Hãy kết nối và theo dõi các bạn sinh viên và giảng viên khác'
                  : 'Chia sẻ kiến thức để nhận được sự theo dõi từ cộng đồng SQB'
              }
              className="py-12 border-0 bg-transparent"
            />
          ) : (
            filteredItems.map((user) => {
              const isFollowingThisUser =
                activeTab === 'following' ? true : (user as any).isFollowingBack ?? false;

              return (
                <div
                  key={user.userId}
                  className="py-3 first:pt-0 last:pb-0 flex items-center justify-between gap-3 group"
                >
                  <div
                    onClick={() => {
                      onClose();
                      navigate(`/users/${user.userId}`);
                    }}
                    className="flex items-center gap-3 min-w-0 flex-1 cursor-pointer"
                  >
                    <AvatarWithFrame
                      avatarUrl={user.avatarUrl}
                      frameUrl={user.frameUrl}
                      name={user.fullName}
                      size="md"
                    />

                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <p className="text-sm font-bold text-slate-800 dark:text-slate-100 truncate group-hover:text-indigo-600 transition-colors">
                          {user.fullName}
                        </p>
                        <Badge variant="role" size="sm">
                          {user.role}
                        </Badge>
                      </div>

                      <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
                        {user.code ? `${user.code} • ` : ''}
                        {user.schoolFaculty || 'Đại học Bách Khoa Hà Nội'}
                      </p>
                    </div>
                  </div>

                  {/* Follow/Unfollow Toggle button */}
                  <Button
                    size="sm"
                    variant={isFollowingThisUser ? 'outline' : 'default'}
                    disabled={followMutation.isPending}
                    onClick={() =>
                      followMutation.mutate({
                        targetUserId: user.userId,
                        isCurrentlyFollowing: isFollowingThisUser,
                      })
                    }
                    className="gap-1.5 shrink-0 text-xs px-3"
                  >
                    {followMutation.isPending ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : isFollowingThisUser ? (
                      <>
                        <UserCheck className="w-3.5 h-3.5 text-indigo-500" />
                        <span>Đang theo dõi</span>
                      </>
                    ) : (
                      <>
                        <UserPlus className="w-3.5 h-3.5" />
                        <span>Theo dõi</span>
                      </>
                    )}
                  </Button>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}
