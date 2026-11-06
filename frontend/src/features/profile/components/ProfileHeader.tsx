import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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
  MessageSquare,
  ShieldAlert,
  ShoppingBag,
  Loader2,
  Clock,
  BadgeCheck,
} from 'lucide-react';

interface ProfileHeaderProps {
  profile: ProfileResponse;
  isOwnProfile: boolean;
}

export function ProfileHeader({ profile, isOwnProfile }: ProfileHeaderProps) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const currentRole = useAuthStore((state) => state.user?.role);

  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isFollowModalOpen, setIsFollowModalOpen] = useState(false);
  const [followModalTab, setFollowModalTab] = useState<'following' | 'followers'>('following');
  const [isBlockModalOpen, setIsBlockModalOpen] = useState(false);

  // Đồng cấp: cùng là STUDENT hoặc cùng là LECTURER (hoặc cùng role)
  const isPeer = Boolean(currentRole && currentRole === profile.role);

  const relationships = profile.relationships;
  const isFollowing = relationships?.isFollowing ?? false;
  const friendStatus = relationships?.friendRequestStatus ?? 'NONE';
  const isFriend = relationships?.isFriend ?? false;

  // Follow / Unfollow mutation with Optimistic UI
  const followMutation = useMutation({
    mutationFn: async (wasFollowing: boolean) => {
      if (wasFollowing) {
        return await userService.unfollowUser(profile.userId);
      } else {
        return await userService.followUser(profile.userId);
      }
    },
    onMutate: async (wasFollowing) => {
      // 1. Dừng query profile đang chạy để tránh ghi đè dữ liệu cũ
      await queryClient.cancelQueries({ queryKey: ['userProfile', profile.userId] });

      // 2. Chụp snapshot cache hiện tại làm điểm phục hồi nếu có lỗi
      const previousProfile = queryClient.getQueryData<ProfileResponse>(['userProfile', profile.userId]);
      const nextFollowing = !wasFollowing;

      // 3. Optimistic Update: Đổi ngay trạng thái isFollowing và tăng/giảm followersCount (0ms delay)
      queryClient.setQueryData<ProfileResponse>(['userProfile', profile.userId], (old) => {
        if (!old) return old;
        return {
          ...old,
          followersCount: Math.max(0, (old.followersCount || 0) + (nextFollowing ? 1 : -1)),
          relationships: {
            ...(old.relationships || { isFriend: false, isFollowed: false, friendRequestStatus: 'NONE' }),
            isFollowing: nextFollowing,
          },
        };
      });

      return { previousProfile, wasFollowing };
    },
    onError: (_error, _vars, context) => {
      // 4. Phục hồi lại dữ liệu cũ nếu lỗi
      if (context?.previousProfile) {
        queryClient.setQueryData(['userProfile', profile.userId], context.previousProfile);
      }
      toast.error('Thao tác không thành công. Vui lòng thử lại!');
    },
    onSuccess: (_res, _vars, context) => {
      toast.success(context?.wasFollowing ? 'Đã hủy theo dõi' : 'Đã theo dõi người dùng');
    },
    onSettled: () => {
      // 5. Invalidate ngầm để đối soát với server
      queryClient.invalidateQueries({ queryKey: ['userProfile', profile.userId] });
      queryClient.invalidateQueries({ queryKey: ['userProfile', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['followingList'] });
      queryClient.invalidateQueries({ queryKey: ['followersList'] });
    },
  });

  // Friend Request mutation
  const friendMutation = useMutation({
    mutationFn: async (action: 'send' | 'unfriend') => {
      if (action === 'send') {
        return await userService.sendFriendRequest({ addresseeId: profile.userId });
      } else {
        return await userService.unfriend(profile.userId);
      }
    },
    onSuccess: (_, action) => {
      if (action === 'send') toast.success('Đã gửi lời mời kết bạn');
      else if (action === 'unfriend') toast.info('Đã hủy kết bạn');

      queryClient.invalidateQueries({ queryKey: ['userProfile', profile.userId] });
      queryClient.invalidateQueries({ queryKey: ['myFriends'] });
      queryClient.invalidateQueries({ queryKey: ['receivedRequests'] });
      queryClient.invalidateQueries({ queryKey: ['sentRequests'] });
    },
    onError: () => {
      toast.error('Thao tác kết bạn không thành công');
    },
  });

  const messageMutation = useMutation({
    mutationFn: async () => {
      const res = await chatService.createDirectConversation({ targetUserId: profile.userId });
      if (res?.data?.conversationId) {
        navigate(`/messages/${res.data.conversationId}`);
      }
    },
    onError: () => toast.error('Không thể mở cuộc trò chuyện với người dùng này'),
  });

  const roleLabel = profile.role === 'LECTURER' ? 'Giảng viên'
    : profile.role === 'ADMIN' ? 'Quản trị viên' : 'Sinh viên';
  const affiliation = [
    profile.schoolFaculty,
    profile.className && `Lớp ${profile.className}`,
  ].filter(Boolean).join(' · ');

  const openFollowList = (tab: 'followers' | 'following') => {
    setFollowModalTab(tab);
    setIsFollowModalOpen(true);
  };

  return (
    <>
      <section aria-labelledby="profile-name" className="rounded-2xl border border-border bg-card shadow-[var(--card-shadow)]">
        <div className="relative h-36 overflow-hidden rounded-t-2xl bg-accent sm:h-52">
          {profile.coverUrl ? (
            <img src={profile.coverUrl} alt="" className="size-full object-cover" />
          ) : (
            <div aria-hidden="true" className="absolute inset-x-0 bottom-0 h-1/2 border-t border-primary/15 bg-primary/5" />
          )}
        </div>

        <div className="relative px-4 pb-4 sm:px-6 sm:pb-6">
          <div className="-mt-10 w-fit rounded-full bg-card p-1">
            <AvatarWithFrame avatarUrl={profile.avatarUrl} frameUrl={profile.frameUrl} name={profile.fullName} size="xl" />
          </div>

          <div className="mt-3 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div className="min-w-0 space-y-2">
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant="secondary">{roleLabel}</Badge>
                {profile.verified && (
                  <Badge variant="outline" className="border-success/25 bg-success-soft text-success">
                    <BadgeCheck className="size-4" aria-hidden="true" /> Đã xác thực
                  </Badge>
                )}
              </div>
              <h1 id="profile-name" className="wrap-break-word text-2xl font-bold tracking-tight sm:text-3xl">{profile.fullName}</h1>
              {affiliation && <p className="wrap-break-word text-sm text-muted-foreground">{affiliation}</p>}
              {profile.bio && <p className="max-w-prose wrap-break-word whitespace-pre-wrap leading-relaxed">{profile.bio}</p>}
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {isOwnProfile ? (
                <>
                  <Button onClick={() => setIsEditModalOpen(true)}><Edit3 aria-hidden="true" /> Sửa hồ sơ</Button>
                  <Button nativeButton={false} variant="outline" render={<Link to="/shop" />}>
                    <ShoppingBag aria-hidden="true" /> Cửa hàng
                  </Button>
                </>
              ) : (
                <>
                  {isPeer && (isFriend ? (
                    <Badge variant="secondary"><UserCheck className="size-4" aria-hidden="true" /> Bạn bè</Badge>
                  ) : friendStatus === 'PENDING' ? (
                    <Button nativeButton={false} variant="outline" render={<Link to="/friends" />}>
                      <Clock aria-hidden="true" /> Lời mời đang chờ
                    </Button>
                  ) : (
                    <Button disabled={friendMutation.isPending} onClick={() => friendMutation.mutate('send')}>
                      {friendMutation.isPending ? <Loader2 className="motion-safe:animate-spin" aria-hidden="true" /> : <UserPlus aria-hidden="true" />}
                      Thêm bạn
                    </Button>
                  ))}
                  <Button variant={isPeer || isFollowing ? 'outline' : 'default'} aria-pressed={isFollowing}
                    disabled={followMutation.isPending} onClick={() => followMutation.mutate(isFollowing)}>
                    {isFollowing ? 'Đang theo dõi' : 'Theo dõi'}
                  </Button>
                  <Button variant="outline" disabled={messageMutation.isPending} onClick={() => messageMutation.mutate()}>
                    <MessageSquare aria-hidden="true" /> Nhắn tin
                  </Button>
                  <details className="basis-full">
                    <summary className="flex min-h-11 w-fit cursor-pointer list-none items-center rounded-xl px-3 text-sm font-semibold text-muted-foreground hover:bg-muted hover:text-foreground [&::-webkit-details-marker]:hidden">
                      Tùy chọn
                    </summary>
                    <div className="mt-1 flex flex-wrap gap-2 rounded-xl border border-border bg-surface-subtle p-2">
                      {isPeer && isFriend && (
                        <Button variant="ghost" disabled={friendMutation.isPending} onClick={() => friendMutation.mutate('unfriend')}>
                          Hủy kết bạn
                        </Button>
                      )}
                      <Button variant="destructive" onClick={() => setIsBlockModalOpen(true)}>
                        <ShieldAlert aria-hidden="true" /> Chặn người dùng
                      </Button>
                    </div>
                  </details>
                </>
              )}
            </div>
          </div>

          <div aria-label="Thống kê kết nối" className="mt-5 flex flex-wrap gap-x-3 gap-y-1 border-t border-border pt-2 text-sm">
            {isOwnProfile ? (
              <Button nativeButton={false} variant="ghost" render={<Link to="/friends" />}>
                <strong>{profile.friendsCount ?? 0}</strong> bạn bè
              </Button>
            ) : (
              <span className="inline-flex min-h-11 items-center gap-2 px-3"><strong>{profile.friendsCount ?? 0}</strong> bạn bè</span>
            )}
            {(['followers', 'following'] as const).map((tab) => {
              const count = tab === 'followers' ? profile.followersCount : profile.followingCount;
              const label = tab === 'followers' ? 'người theo dõi' : 'đang theo dõi';
              return isOwnProfile ? (
                <Button key={tab} variant="ghost" onClick={() => openFollowList(tab)}>
                  <strong>{count ?? 0}</strong> {label}
                </Button>
              ) : (
                <span key={tab} className="inline-flex min-h-11 items-center gap-2 px-3"><strong>{count ?? 0}</strong> {label}</span>
              );
            })}
          </div>
        </div>
      </section>

      {isOwnProfile && isEditModalOpen && (
        <EditProfileModal isOpen onClose={() => setIsEditModalOpen(false)} profile={profile} />
      )}
      {isOwnProfile && isFollowModalOpen && (
        <FollowListModal isOpen onClose={() => setIsFollowModalOpen(false)} initialTab={followModalTab} />
      )}
      {!isOwnProfile && (
        <BlockUserModal isOpen={isBlockModalOpen} onClose={() => setIsBlockModalOpen(false)}
          targetUserId={profile.userId} targetUserName={profile.fullName} />
      )}
    </>
  );
}
