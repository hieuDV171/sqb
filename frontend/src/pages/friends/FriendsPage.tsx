import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { userService } from '@/services/userService';
import { chatService } from '@/features/chat';
import { toast } from '@/stores/useToastStore';
import { AvatarWithFrame } from '@/features/profile/components/AvatarWithFrame';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import {
  Users,
  UserPlus,
  Send,
  Search,
  MessageSquare,
  UserX,
  Check,
  X,
  Loader2,
  Clock,
} from 'lucide-react';

export function FriendsPage() {
  const [activeTab, setActiveTab] = useState<'friends' | 'received' | 'sent'>('friends');
  const [searchQuery, setSearchQuery] = useState('');
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  // Queries
  const friendsQuery = useQuery({
    queryKey: ['myFriends'],
    queryFn: async () => {
      const res = await userService.getMyFriends(undefined, 50);
      return res.data;
    },
  });

  const receivedQuery = useQuery({
    queryKey: ['receivedRequests'],
    queryFn: async () => {
      const res = await userService.getReceivedFriendRequests(undefined, 50);
      return res.data;
    },
  });

  const sentQuery = useQuery({
    queryKey: ['sentRequests'],
    queryFn: async () => {
      const res = await userService.getSentFriendRequests(undefined, 50);
      return res.data;
    },
  });

  // Mutations
  const acceptMutation = useMutation({
    mutationFn: async (requesterId: number) => {
      return await userService.acceptFriendRequest({ requesterId });
    },
    onSuccess: () => {
      toast.success('Đã chấp nhận lời mời kết bạn');
      queryClient.invalidateQueries({ queryKey: ['myFriends'] });
      queryClient.invalidateQueries({ queryKey: ['receivedRequests'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Không thể chấp nhận lời mời');
    },
  });

  const declineMutation = useMutation({
    mutationFn: async (requesterId: number) => {
      return await userService.declineFriendRequest({ requesterId });
    },
    onSuccess: () => {
      toast.info('Đã từ chối lời mời kết bạn');
      queryClient.invalidateQueries({ queryKey: ['receivedRequests'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Không thể từ chối lời mời');
    },
  });

  const cancelSentMutation = useMutation({
    mutationFn: async (receiverId: number) => {
      // In Backend, decline endpoint or unfriend can cancel outgoing request
      return await userService.declineFriendRequest({ requesterId: receiverId });
    },
    onSuccess: () => {
      toast.info('Đã hủy lời mời kết bạn');
      queryClient.invalidateQueries({ queryKey: ['sentRequests'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Không thể hủy lời mời');
    },
  });

  const unfriendMutation = useMutation({
    mutationFn: async (friendId: number) => {
      return await userService.unfriend(friendId);
    },
    onSuccess: () => {
      toast.info('Đã hủy kết bạn');
      queryClient.invalidateQueries({ queryKey: ['myFriends'] });
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Không thể hủy kết bạn');
    },
  });

  const friendsList = friendsQuery.data?.items || [];
  const receivedList = receivedQuery.data?.items || [];
  const sentList = sentQuery.data?.items || [];

  const filteredFriends = friendsList.filter((f) => {
    if (!searchQuery.trim()) return true;
    return f.fullName.toLowerCase().includes(searchQuery.toLowerCase());
  });

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
        {/* Page Title & Search Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200/80 dark:border-slate-800 shadow-sm">
          <div>
            <div className="flex items-center gap-2.5">
              <div className="w-10 h-10 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center shadow-xs">
                <Users className="w-5 h-5" />
              </div>
              <div>
                <h1 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white">
                  Quản lý Bạn bè
                </h1>
                <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
                  Kết nối và mở rộng mạng lưới học thuật cùng sinh viên HUST
                </p>
              </div>
            </div>
          </div>

          {/* Search Box */}
          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo tên bạn bè..."
              className="w-full pl-10 pr-4 py-2 text-xs sm:text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl outline-none focus:border-indigo-500 text-slate-900 dark:text-slate-100"
            />
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-2 p-1.5 bg-slate-200/60 dark:bg-slate-800/80 rounded-2xl w-fit">
          <button
            type="button"
            onClick={() => setActiveTab('friends')}
            className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-2 transition-all cursor-pointer ${
              activeTab === 'friends'
                ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            <Users className="w-4 h-4" />
            <span>Tất cả bạn bè</span>
            <span className="text-[11px] px-1.5 py-0.5 rounded-full bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 font-mono">
              {friendsQuery.data?.totalFriends ?? friendsList.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('received')}
            className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-2 transition-all cursor-pointer ${
              activeTab === 'received'
                ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            <UserPlus className="w-4 h-4" />
            <span>Lời mời nhận được</span>
            {receivedList.length > 0 && (
              <span className="text-[11px] px-1.5 py-0.5 rounded-full bg-red-500 text-white font-mono animate-pulse">
                {receivedQuery.data?.totalPending ?? receivedList.length}
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('sent')}
            className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-2 transition-all cursor-pointer ${
              activeTab === 'sent'
                ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            <Send className="w-4 h-4" />
            <span>Đã gửi đi</span>
            <span className="text-[11px] px-1.5 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-mono">
              {sentQuery.data?.totalSent ?? sentList.length}
            </span>
          </button>
        </div>

        {/* Tab 1: All Friends */}
        {activeTab === 'friends' && (
          <div>
            {friendsQuery.isLoading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {[1, 2, 3, 4, 5, 6].map((i) => (
                  <div key={i} className="p-4 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 flex items-center gap-3">
                    <Skeleton className="w-14 h-14 rounded-full" />
                    <div className="space-y-2 flex-1">
                      <Skeleton className="h-4 w-28" />
                      <Skeleton className="h-3 w-20" />
                    </div>
                  </div>
                ))}
              </div>
            ) : friendsQuery.isError ? (
              <ErrorState
                title="Không thể tải danh sách bạn bè"
                onRetry={() => friendsQuery.refetch()}
              />
            ) : filteredFriends.length === 0 ? (
              <EmptyState
                icon={Users}
                title={searchQuery ? 'Không tìm thấy bạn bè nào' : 'Bạn chưa có người bạn nào'}
                description={
                  searchQuery
                    ? 'Hãy kiểm tra lại từ khóa tìm kiếm'
                    : 'Ghé thăm trang Bảng tin hoặc Bảng xếp hạng để làm quen và kết bạn cùng các bạn sinh viên khác!'
                }
                actionLabel="Khám phá Bảng tin"
                onAction={() => navigate('/feed')}
              />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {filteredFriends.map((friend) => (
                  <div
                    key={friend.userId}
                    className="p-4 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-2xs hover:shadow-md transition-all group flex flex-col justify-between"
                  >
                    <div
                      onClick={() => navigate(`/users/${friend.userId}`)}
                      className="flex items-center gap-3 cursor-pointer"
                    >
                      <AvatarWithFrame
                        avatarUrl={friend.avatarUrl}
                        frameUrl={friend.frameUrl}
                        name={friend.fullName}
                        size="lg"
                      />
                      <div className="min-w-0 flex-1">
                        <h4 className="text-sm font-bold text-slate-900 dark:text-white truncate group-hover:text-indigo-600 transition-colors">
                          {friend.fullName}
                        </h4>
                        {friend.mutualFriendsCount !== undefined && friend.mutualFriendsCount > 0 && (
                          <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                            {friend.mutualFriendsCount} bạn chung
                          </p>
                        )}
                        <p className="text-[10px] text-slate-400 mt-0.5 flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          <span>
                            Từ {new Date(friend.friendsSince).toLocaleDateString('vi-VN')}
                          </span>
                        </p>
                      </div>
                    </div>

                    <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800/80 flex items-center gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={async () => {
                          try {
                            const res = await chatService.createDirectConversation({ targetUserId: friend.userId });
                            if (res?.data?.conversationId) {
                              navigate(`/messages/${res.data.conversationId}`);
                            }
                          } catch (err: any) {
                            toast.error(err?.message || 'Không thể mở cuộc trò chuyện');
                          }
                        }}
                        className="flex-1 gap-1.5 text-xs rounded-xl cursor-pointer"
                      >
                        <MessageSquare className="w-3.5 h-3.5" />
                        <span>Nhắn tin</span>
                      </Button>

                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => {
                          if (confirm(`Bạn có chắc chắn muốn hủy kết bạn với ${friend.fullName}?`)) {
                            unfriendMutation.mutate(friend.userId);
                          }
                        }}
                        className="text-slate-400 hover:text-red-600 rounded-xl px-2.5"
                        title="Hủy kết bạn"
                      >
                        <UserX className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab 2: Received Requests */}
        {activeTab === 'received' && (
          <div>
            {receivedQuery.isLoading ? (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="p-4 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 flex items-center gap-3">
                    <Skeleton className="w-12 h-12 rounded-full" />
                    <div className="space-y-2 flex-1">
                      <Skeleton className="h-4 w-32" />
                      <Skeleton className="h-3 w-20" />
                    </div>
                    <Skeleton className="h-9 w-24 rounded-xl" />
                  </div>
                ))}
              </div>
            ) : receivedList.length === 0 ? (
              <EmptyState
                icon={UserPlus}
                title="Không có lời mời kết bạn nào"
                description="Khi có người gửi lời mời kết bạn cho bạn, yêu cầu sẽ xuất hiện tại đây."
              />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {receivedList.map((req, idx) => {
                  const author = req.requester;
                  const userId = author.userId ?? idx;
                  return (
                    <div
                      key={userId}
                      className="p-5 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-2xs flex flex-col justify-between"
                    >
                      <div
                        onClick={() => author.userId && navigate(`/users/${author.userId}`)}
                        className="flex items-center gap-3.5 cursor-pointer group"
                      >
                        <AvatarWithFrame
                          avatarUrl={author.avatarUrl}
                          frameUrl={author.frameUrl}
                          name={author.fullName}
                          size="lg"
                        />
                        <div className="min-w-0 flex-1">
                          <h4 className="text-sm font-bold text-slate-900 dark:text-white truncate group-hover:text-indigo-600 transition-colors">
                            {author.fullName}
                          </h4>
                          {req.mutualFriendsCount !== undefined && req.mutualFriendsCount > 0 && (
                            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                              {req.mutualFriendsCount} bạn chung
                            </p>
                          )}
                          <p className="text-[10px] text-slate-400 mt-0.5">
                            Gửi lúc {new Date(req.createdAt).toLocaleDateString('vi-VN')}
                          </p>
                        </div>
                      </div>

                      <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800/80 flex items-center gap-2">
                        <Button
                          size="sm"
                          disabled={acceptMutation.isPending || declineMutation.isPending}
                          onClick={() => author.userId && acceptMutation.mutate(author.userId)}
                          className="flex-1 gap-1.5 text-xs rounded-xl"
                        >
                          <Check className="w-3.5 h-3.5" />
                          <span>Chấp nhận</span>
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={acceptMutation.isPending || declineMutation.isPending}
                          onClick={() => author.userId && declineMutation.mutate(author.userId)}
                          className="flex-1 gap-1.5 text-xs rounded-xl text-slate-600 hover:text-red-600"
                        >
                          <X className="w-3.5 h-3.5" />
                          <span>Từ chối</span>
                        </Button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Tab 3: Sent Requests */}
        {activeTab === 'sent' && (
          <div>
            {sentQuery.isLoading ? (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="p-4 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 flex items-center gap-3">
                    <Skeleton className="w-12 h-12 rounded-full" />
                    <div className="space-y-2 flex-1">
                      <Skeleton className="h-4 w-32" />
                      <Skeleton className="h-3 w-20" />
                    </div>
                  </div>
                ))}
              </div>
            ) : sentList.length === 0 ? (
              <EmptyState
                icon={Send}
                title="Chưa gửi lời mời kết bạn nào"
                description="Bạn chưa gửi lời mời kết bạn nào đang chờ phản hồi."
              />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {sentList.map((req, idx) => {
                  const receiver = req.receiver;
                  const userId = receiver.userId ?? idx;
                  return (
                    <div
                      key={userId}
                      className="p-5 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-2xs flex items-center justify-between gap-3"
                    >
                      <div
                        onClick={() => receiver.userId && navigate(`/users/${receiver.userId}`)}
                        className="flex items-center gap-3.5 cursor-pointer group min-w-0 flex-1"
                      >
                        <AvatarWithFrame
                          avatarUrl={receiver.avatarUrl}
                          frameUrl={receiver.frameUrl}
                          name={receiver.fullName}
                          size="md"
                        />
                        <div className="min-w-0 flex-1">
                          <h4 className="text-sm font-bold text-slate-900 dark:text-white truncate group-hover:text-indigo-600 transition-colors">
                            {receiver.fullName}
                          </h4>
                          <p className="text-[10px] text-slate-400 mt-0.5">
                            Đã gửi ngày {new Date(req.createdAt).toLocaleDateString('vi-VN')}
                          </p>
                        </div>
                      </div>

                      <Button
                        size="sm"
                        variant="outline"
                        disabled={cancelSentMutation.isPending}
                        onClick={() => receiver.userId && cancelSentMutation.mutate(receiver.userId)}
                        className="gap-1.5 text-xs rounded-xl shrink-0 text-slate-500 hover:text-red-600"
                      >
                        {cancelSentMutation.isPending ? (
                          <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        ) : (
                          <>
                            <X className="w-3.5 h-3.5" />
                            <span>Hủy lời mời</span>
                          </>
                        )}
                      </Button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>
  );
}
