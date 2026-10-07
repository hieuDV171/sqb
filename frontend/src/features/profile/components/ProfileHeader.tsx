import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { userService } from '@/services/userService';
import { chatService } from '@/features/chat';
import { useAuthStore } from '@/stores/useAuthStore';
import { toast } from '@/stores/useToastStore';
import type { ProfileResponse } from '@/types/user.types';
import { AvatarWithFrame } from './AvatarWithFrame';
import { EditProfileModal } from './EditProfileModal';
import { FollowListModal } from './FollowListModal';
import { BlockUserModal } from './BlockUserModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Edit3,
  UserPlus,
  UserCheck,
  UserMinus,
  UserX,
  MessageSquare,
  ShieldAlert,
  Sparkles,
  ShoppingBag,
  Loader2,
  Clock,
  MoreHorizontal,
} from 'lucide-react';

interface ProfileHeaderProps {
  profile: ProfileResponse;
  isOwnProfile: boolean;
}

export function ProfileHeader({ profile, isOwnProfile }: ProfileHeaderProps) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { user: currentUser } = useAuthStore();

  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isFollowModalOpen, setIsFollowModalOpen] = useState(false);
  const [followModalTab, setFollowModalTab] = useState<'following' | 'followers'>('following');
  const [isBlockModalOpen, setIsBlockModalOpen] = useState(false);
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false);

  // Đồng cấp: cùng là STUDENT hoặc cùng là LECTURER (hoặc cùng role)
  const isPeer = Boolean(currentUser?.role && profile.role && currentUser.role === profile.role);

  const relationships = profile.relationships;
  const isFollowing = relationships?.isFollowing ?? false;
  const friendStatus = relationships?.friendRequestStatus ?? 'NONE';
  const isFriend = relationships?.isFriend ?? false;

  // Follow / Unfollow mutation
  const followMutation = useMutation({
    mutationFn: async () => {
      if (isFollowing) {
        return await userService.unfollowUser(profile.userId);
      } else {
        return await userService.followUser(profile.userId);
      }
    },
    onSuccess: () => {
      toast.success(isFollowing ? 'Đã hủy theo dõi' : 'Đã theo dõi người dùng');
      queryClient.invalidateQueries({ queryKey: ['userProfile', profile.userId] });
      queryClient.invalidateQueries({ queryKey: ['relationshipStats', profile.userId] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Thao tác không thành công');
    },
  });

  // Friend Request mutation
  const friendMutation = useMutation({
    mutationFn: async (action: 'send' | 'accept' | 'decline' | 'unfriend') => {
      if (action === 'send') {
        return await userService.sendFriendRequest({ addresseeId: profile.userId });
      } else if (action === 'accept') {
        return await userService.acceptFriendRequest({ requesterId: profile.userId });
      } else if (action === 'decline') {
        return await userService.declineFriendRequest({ requesterId: profile.userId });
      } else {
        return await userService.unfriend(profile.userId);
      }
    },
    onSuccess: (_, action) => {
      if (action === 'send') toast.success('Đã gửi lời mời kết bạn');
      else if (action === 'accept') toast.success('Đã chấp nhận lời mời kết bạn');
      else if (action === 'decline') toast.info('Đã từ chối lời mời kết bạn');
      else if (action === 'unfriend') toast.info('Đã hủy kết bạn');

      queryClient.invalidateQueries({ queryKey: ['userProfile', profile.userId] });
      queryClient.invalidateQueries({ queryKey: ['myFriends'] });
      queryClient.invalidateQueries({ queryKey: ['receivedRequests'] });
      queryClient.invalidateQueries({ queryKey: ['sentRequests'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Thao tác kết bạn không thành công');
    },
  });

  const handleDirectMessage = async () => {
    try {
      const res = await chatService.createDirectConversation({ targetUserId: profile.userId });
      if (res?.data?.conversationId) {
        navigate(`/messages/${res.data.conversationId}`);
      }
    } catch (err: any) {
      toast.error(err?.message || 'Không thể mở cuộc trò chuyện với người dùng này');
    }
  };

  return (
    <>
      <div className="relative rounded-3xl overflow-hidden bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-md">
        {/* Cover Photo */}
        <div className="relative h-48 sm:h-64 lg:h-72 w-full bg-linear-to-r from-indigo-600 via-purple-600 to-pink-600 overflow-hidden">
          {profile.coverUrl ? (
            <img
              src={profile.coverUrl}
              alt="Cover"
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center opacity-30">
              <div className="w-96 h-96 rounded-full bg-white blur-3xl" />
            </div>
          )}

          {/* Quick Edit Cover for own profile */}
          {isOwnProfile && (
            <button
              type="button"
              onClick={() => setIsEditModalOpen(true)}
              className="absolute top-4 right-4 px-3 py-1.5 rounded-xl bg-black/40 hover:bg-black/60 text-white backdrop-blur-md text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer shadow-md"
            >
              <Edit3 className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Chỉnh sửa hồ sơ</span>
            </button>
          )}
        </div>

        {/* Profile Info Bar */}
        <div className="px-6 pb-6 pt-0">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-4">
            {/* Avatar with Frame + Identity */}
            <div className="flex flex-col sm:flex-row items-center sm:items-end gap-5 text-center sm:text-left">
              {/* Only the avatar hangs over the cover banner */}
              <div className="-mt-16 sm:-mt-20 shrink-0 z-10 flex justify-center sm:justify-start">
                <div className="relative">
                  <AvatarWithFrame
                    avatarUrl={profile.avatarUrl}
                    frameUrl={profile.frameUrl}
                    name={profile.fullName}
                    size="2xl"
                    showOnlineStatus={true}
                    className="shadow-2xl ring-4 ring-white dark:ring-slate-900 bg-white dark:bg-slate-900 rounded-full"
                  />
                </div>
              </div>

              {/* User text details sit strictly below the cover banner */}
              <div className="pt-2 sm:pt-0 sm:pb-2">
                <div className="flex items-center justify-center sm:justify-start gap-2 flex-wrap">
                  <h1 className="text-xl sm:text-2xl font-extrabold text-slate-900 dark:text-white">
                    {profile.fullName}
                  </h1>
                  <Badge variant="role" size="sm">
                    {profile.role === 'LECTURER' ? 'Giảng viên' : profile.role === 'ADMIN' ? 'Quản trị viên' : 'Sinh viên'}
                  </Badge>
                  {profile.verified && (
                    <span className="text-[11px] font-semibold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/50 px-2 py-0.5 rounded-full border border-emerald-200 dark:border-emerald-800">
                      ✓ Đã xác thực
                    </span>
                  )}
                </div>

                <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 font-medium mt-1">
                  {profile.studentLecturerCode && (
                    <span className="font-mono font-semibold text-slate-700 dark:text-slate-300">
                      {profile.studentLecturerCode} •{' '}
                    </span>
                  )}
                  {profile.schoolFaculty || 'Trường CNTT & TT (SoICT)'}
                  {profile.className ? ` • Lớp ${profile.className}` : ''}
                </p>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-2 flex-wrap justify-center sm:justify-end pb-2">
              {isOwnProfile ? (
                <>
                  <Button
                    onClick={() => setIsEditModalOpen(true)}
                    className="gap-2 text-xs sm:text-sm rounded-xl font-semibold shadow-xs"
                  >
                    <Edit3 className="w-4 h-4" />
                    <span>Sửa hồ sơ</span>
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => navigate('/shop')}
                    className="gap-2 text-xs sm:text-sm rounded-xl font-semibold border-amber-300 dark:border-amber-700 text-amber-600 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/30"
                  >
                    <ShoppingBag className="w-4 h-4" />
                    <span>Cửa hàng</span>
                  </Button>
                </>
              ) : isPeer ? (
                <>
                  {/* Người đồng cấp (GV-GV, SV-SV): Nút Thêm bạn ra ngoài, Theo dõi/Hủy theo dõi ẩn vào dấu ... */}
                  {isFriend ? (
                    <Button
                      variant="outline"
                      className="gap-1.5 text-xs sm:text-sm border-emerald-300 dark:border-emerald-700 text-emerald-600 dark:text-emerald-400 bg-emerald-50/50 dark:bg-emerald-950/30"
                    >
                      <UserCheck className="w-4 h-4" />
                      <span>Bạn bè</span>
                    </Button>
                  ) : friendStatus === 'PENDING' ? (
                    <Button
                      variant="outline"
                      disabled={friendMutation.isPending}
                      onClick={() => friendMutation.mutate('decline')}
                      className="gap-1.5 text-xs sm:text-sm text-slate-600 dark:text-slate-300 border-amber-300 dark:border-amber-700"
                      title="Nhấn để hủy lời mời đã gửi"
                    >
                      {friendMutation.isPending ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : (
                        <>
                          <Clock className="w-4 h-4 text-amber-500" />
                          <span>Đã gửi lời mời (Hủy)</span>
                        </>
                      )}
                    </Button>
                  ) : (
                    <Button
                      disabled={friendMutation.isPending}
                      onClick={() => friendMutation.mutate('send')}
                      className="gap-1.5 text-xs sm:text-sm bg-indigo-600 hover:bg-indigo-700 text-white shadow-xs"
                    >
                      {friendMutation.isPending ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : (
                        <>
                          <UserPlus className="w-4 h-4" />
                          <span>Thêm bạn</span>
                        </>
                      )}
                    </Button>
                  )}

                  {/* Nhắn tin */}
                  <Button
                    variant="outline"
                    onClick={handleDirectMessage}
                    className="gap-1.5 text-xs sm:text-sm cursor-pointer"
                  >
                    <MessageSquare className="w-4 h-4" />
                    <span>Nhắn tin</span>
                  </Button>

                  {/* Nút 3 chấm (...) Tùy chọn khác */}
                  <div className="relative">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setIsMoreMenuOpen(!isMoreMenuOpen)}
                      className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white"
                      title="Tùy chọn khác"
                    >
                      <MoreHorizontal className="w-4 h-4" />
                    </Button>

                    {isMoreMenuOpen && (
                      <>
                        <div className="fixed inset-0 z-30" onClick={() => setIsMoreMenuOpen(false)} />
                        <div className="absolute right-0 mt-1 w-48 bg-white dark:bg-slate-800 rounded-xl shadow-xl border border-slate-200 dark:border-slate-700 py-1.5 z-40 animate-in fade-in zoom-in-95 duration-100">
                          {/* Theo dõi / Hủy theo dõi ẩn vào dấu ... */}
                          <button
                            type="button"
                            onClick={() => {
                              setIsMoreMenuOpen(false);
                              followMutation.mutate();
                            }}
                            disabled={followMutation.isPending}
                            className="w-full px-3.5 py-2 text-left text-xs text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700/50 flex items-center gap-2.5 cursor-pointer font-medium transition-colors"
                          >
                            {isFollowing ? (
                              <>
                                <UserMinus className="w-4 h-4 text-slate-500" />
                                <span>Hủy theo dõi</span>
                              </>
                            ) : (
                              <>
                                <Sparkles className="w-4 h-4 text-indigo-500" />
                                <span>Theo dõi</span>
                              </>
                            )}
                          </button>

                          {/* Hủy kết bạn (nếu đã là bạn bè) */}
                          {isFriend && (
                            <button
                              type="button"
                              onClick={() => {
                                setIsMoreMenuOpen(false);
                                friendMutation.mutate('unfriend');
                              }}
                              disabled={friendMutation.isPending}
                              className="w-full px-3.5 py-2 text-left text-xs text-amber-600 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/40 flex items-center gap-2.5 cursor-pointer font-medium transition-colors"
                            >
                              <UserX className="w-4 h-4" />
                              <span>Hủy kết bạn</span>
                            </button>
                          )}

                          {/* Chặn người dùng */}
                          <button
                            type="button"
                            onClick={() => {
                              setIsMoreMenuOpen(false);
                              setIsBlockModalOpen(true);
                            }}
                            className="w-full px-3.5 py-2 text-left text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 flex items-center gap-2.5 cursor-pointer font-medium transition-colors"
                          >
                            <ShieldAlert className="w-4 h-4" />
                            <span>Chặn người dùng</span>
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                </>
              ) : (
                <>
                  {/* Người không cùng cấp (SV-GV, GV-SV): Chỉ có quan hệ theo dõi, hiển thị nút theo dõi ra bên ngoài */}
                  <Button
                    variant={isFollowing ? 'outline' : 'default'}
                    disabled={followMutation.isPending}
                    onClick={() => followMutation.mutate()}
                    className={`gap-1.5 text-xs sm:text-sm ${
                      isFollowing
                        ? 'border-indigo-300 dark:border-indigo-700 text-indigo-600 dark:text-indigo-400 bg-indigo-50/40 dark:bg-indigo-950/20'
                        : 'bg-indigo-600 hover:bg-indigo-700 text-white shadow-xs'
                    }`}
                  >
                    {followMutation.isPending ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : isFollowing ? (
                      <>
                        <UserCheck className="w-4 h-4" />
                        <span>Đang theo dõi</span>
                      </>
                    ) : (
                      <>
                        <Sparkles className="w-4 h-4" />
                        <span>Theo dõi</span>
                      </>
                    )}
                  </Button>

                  {/* Nhắn tin */}
                  <Button
                    variant="outline"
                    onClick={handleDirectMessage}
                    className="gap-1.5 text-xs sm:text-sm cursor-pointer"
                  >
                    <MessageSquare className="w-4 h-4" />
                    <span>Nhắn tin</span>
                  </Button>

                  {/* Nút 3 chấm (...) Tùy chọn khác */}
                  <div className="relative">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setIsMoreMenuOpen(!isMoreMenuOpen)}
                      className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white"
                      title="Tùy chọn khác"
                    >
                      <MoreHorizontal className="w-4 h-4" />
                    </Button>

                    {isMoreMenuOpen && (
                      <>
                        <div className="fixed inset-0 z-30" onClick={() => setIsMoreMenuOpen(false)} />
                        <div className="absolute right-0 mt-1 w-48 bg-white dark:bg-slate-800 rounded-xl shadow-xl border border-slate-200 dark:border-slate-700 py-1.5 z-40 animate-in fade-in zoom-in-95 duration-100">
                          {/* Chặn người dùng */}
                          <button
                            type="button"
                            onClick={() => {
                              setIsMoreMenuOpen(false);
                              setIsBlockModalOpen(true);
                            }}
                            className="w-full px-3.5 py-2 text-left text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 flex items-center gap-2.5 cursor-pointer font-medium transition-colors"
                          >
                            <ShieldAlert className="w-4 h-4" />
                            <span>Chặn người dùng</span>
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Bio text if provided */}
          {profile.bio && (
            <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-300 leading-relaxed max-w-3xl my-3 italic border-l-2 border-indigo-500 pl-3">
              "{profile.bio}"
            </p>
          )}

          {/* Social Stats Counters Bar */}
          <div className="flex items-center gap-6 pt-3 border-t border-slate-100 dark:border-slate-800 text-xs sm:text-sm flex-wrap">
            <button
              type="button"
              onClick={() => navigate('/friends')}
              className="group flex items-center gap-1.5 cursor-pointer hover:text-indigo-600 transition-colors"
            >
              <span className="font-bold text-slate-900 dark:text-white group-hover:text-indigo-600">
                {profile.friendsCount ?? 0}
              </span>
              <span className="text-slate-500 dark:text-slate-400">bạn bè</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setFollowModalTab('followers');
                setIsFollowModalOpen(true);
              }}
              className="group flex items-center gap-1.5 cursor-pointer hover:text-indigo-600 transition-colors"
            >
              <span className="font-bold text-slate-900 dark:text-white group-hover:text-indigo-600">
                {profile.followersCount ?? 0}
              </span>
              <span className="text-slate-500 dark:text-slate-400">người theo dõi</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setFollowModalTab('following');
                setIsFollowModalOpen(true);
              }}
              className="group flex items-center gap-1.5 cursor-pointer hover:text-indigo-600 transition-colors"
            >
              <span className="font-bold text-slate-900 dark:text-white group-hover:text-indigo-600">
                {profile.followingCount ?? 0}
              </span>
              <span className="text-slate-500 dark:text-slate-400">đang theo dõi</span>
            </button>

            {/* Gamification Points Chip */}
            <div className="ml-auto flex items-center gap-2 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/80 px-3 py-1 rounded-xl shadow-2xs">
              <Sparkles className="w-3.5 h-3.5 text-amber-500" />
              <span className="font-mono font-bold text-amber-700 dark:text-amber-400">
                {profile.gamificationPoints ?? 0}
              </span>
              <span className="text-[11px] text-amber-600 dark:text-amber-400 font-medium">XP</span>
            </div>
          </div>
        </div>
      </div>

      {/* Edit Profile Modal */}
      {isOwnProfile && (
        <EditProfileModal
          isOpen={isEditModalOpen}
          onClose={() => setIsEditModalOpen(false)}
          profile={profile}
        />
      )}

      {/* Follow List Modal */}
      <FollowListModal
        isOpen={isFollowModalOpen}
        onClose={() => setIsFollowModalOpen(false)}
        initialTab={followModalTab}
        userId={profile.userId}
      />

      {/* Block User Modal */}
      {!isOwnProfile && (
        <BlockUserModal
          isOpen={isBlockModalOpen}
          onClose={() => setIsBlockModalOpen(false)}
          targetUserId={profile.userId}
          targetUserName={profile.fullName}
        />
      )}
    </>
  );
}
