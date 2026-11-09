import { useState, type ReactElement, type ReactNode } from 'react';
import { Menu } from '@base-ui/react/menu';
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
  Ellipsis,
  UserMinus,
} from 'lucide-react';

interface ProfileHeaderProps {
  profile: ProfileResponse;
  isOwnProfile: boolean;
}

const actionItemClass = 'flex min-h-11 cursor-pointer items-center gap-2 rounded-lg px-3 text-sm outline-none data-highlighted:bg-accent data-highlighted:text-accent-foreground data-disabled:cursor-default data-disabled:opacity-50';

function ProfileActionMenu({ trigger, children }: { trigger: ReactElement; children: ReactNode }) {
  return (
    <Menu.Root modal={false}>
      <Menu.Trigger render={trigger} />
      <Menu.Portal>
        <Menu.Positioner align="end" sideOffset={6} className="z-50 max-w-(--available-width)">
          <Menu.Popup className="min-w-44 rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
            {children}
          </Menu.Popup>
        </Menu.Positioner>
      </Menu.Portal>
    </Menu.Root>
  );
}

export function ProfileHeader({ profile, isOwnProfile }: ProfileHeaderProps) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const currentRole = useAuthStore((state) => state.user?.role);

  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isFollowModalOpen, setIsFollowModalOpen] = useState(false);
  const [followModalTab, setFollowModalTab] = useState<'following' | 'followers'>('following');
  const [isBlockModalOpen, setIsBlockModalOpen] = useState(false);

  const isPeer = Boolean(currentRole && currentRole === profile.role);
  const profileKey = ['userProfile', profile.userId] as const;

  const relationships = profile.relationships;
  const isFollowing = relationships?.isFollowed ?? false;
  const friendStatus = relationships?.friendRequestStatus ?? 'NONE';
  const isFriend = relationships?.isFriend ?? false;

  const setFollowState = (following: boolean, followersCount: number) => {
    queryClient.setQueryData<ProfileResponse>(profileKey, (old) => old && ({
      ...old,
      followersCount,
      relationships: {
        ...(old.relationships ?? { isFriend: false, isFollowing: false, friendRequestStatus: 'NONE' }),
        isFollowed: following,
      },
    }));
  };

  const followMutation = useMutation({
    mutationFn: (wasFollowing: boolean) => {
      return wasFollowing ? userService.unfollowUser(profile.userId) : userService.followUser(profile.userId);
    },
    onMutate: async (wasFollowing) => {
      await queryClient.cancelQueries({ queryKey: profileKey, exact: true });
      const previous = queryClient.getQueryData<ProfileResponse>(profileKey);
      if (previous) {
        setFollowState(!wasFollowing, Math.max(0, (previous.followersCount ?? 0) + (wasFollowing ? -1 : 1)));
      }
      return previous;
    },
    onError: (_error, _vars, previous) => {
      if (previous) {
        setFollowState(previous.relationships?.isFollowed ?? false, previous.followersCount);
      }
      toast.error('Không thể cập nhật theo dõi. Hãy thử lại.');
    },
    onSuccess: ({ data }, wasFollowing) => {
      if (data) setFollowState(!wasFollowing, data.newFollowersCount);
      toast.success(wasFollowing ? 'Đã hủy theo dõi' : 'Đã theo dõi người dùng');
    },
    onSettled: () => {
      return Promise.all([profileKey, ['userProfile', 'me'], ['followingList'], ['followersList']].map(
        (queryKey) => queryClient.invalidateQueries({ queryKey }),
      ));
    },
  });

  const friendMutation = useMutation({
    mutationFn: async (action: 'send' | 'unfriend') => {
      if (action === 'send') {
        const { data } = await userService.sendFriendRequest({ addresseeId: profile.userId });
        return data.status;
      }
      await userService.unfriend(profile.userId);
      return 'NONE' as const;
    },
    onSuccess: async (status, action) => {
      if (action === 'unfriend') toast.info('Đã hủy kết bạn');
      else toast.success(status === 'ACCEPTED' ? 'Hai bạn đã là bạn bè' : 'Đã gửi lời mời kết bạn');
      const refresh = [profileKey, ['userProfile', 'me'], ['myFriends'], ['receivedRequests'],
        ['sentRequests'], ['followingList'], ['followersList']].map(
        (queryKey) => queryClient.invalidateQueries({ queryKey }),
      );
      if (action === 'unfriend' || status === 'ACCEPTED') {
        refresh.push(queryClient.resetQueries({ queryKey: ['profile-posts', profile.userId], exact: true }));
      }
      await Promise.all(refresh);
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
  const relationshipBusy = followMutation.isPending || friendMutation.isPending;

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
                    <ProfileActionMenu trigger={<Button variant="outline"><UserCheck aria-hidden="true" /> Bạn bè</Button>}>
                      <Menu.Item className={actionItemClass} disabled={relationshipBusy}
                        onClick={() => friendMutation.mutate('unfriend')}>
                        <UserMinus className="size-4" aria-hidden="true" /> Hủy kết bạn
                      </Menu.Item>
                    </ProfileActionMenu>
                  ) : friendStatus === 'PENDING' ? (
                    <Button nativeButton={false} variant="outline" render={<Link to="/friends" />}>
                      <Clock aria-hidden="true" /> Lời mời đang chờ
                    </Button>
                  ) : (
                    <Button disabled={relationshipBusy} onClick={() => friendMutation.mutate('send')}>
                      {friendMutation.isPending ? <Loader2 className="motion-safe:animate-spin" aria-hidden="true" /> : <UserPlus aria-hidden="true" />}
                      Kết bạn
                    </Button>
                  ))}
                  {isPeer ? (
                    <Button variant="outline" disabled={messageMutation.isPending} onClick={() => messageMutation.mutate()}>
                      <MessageSquare aria-hidden="true" /> Nhắn tin
                    </Button>
                  ) : (
                    <Button variant={isFollowing ? 'outline' : 'default'} aria-pressed={isFollowing}
                      disabled={relationshipBusy} onClick={() => followMutation.mutate(isFollowing)}>
                      {isFollowing ? 'Đang theo dõi' : 'Theo dõi'}
                    </Button>
                  )}
                  <ProfileActionMenu trigger={(
                    <Button variant="outline" size="icon" className="size-11" aria-label="Tùy chọn hồ sơ">
                      <Ellipsis aria-hidden="true" />
                    </Button>
                  )}>
                    {isPeer ? (
                      <Menu.Item className={actionItemClass} disabled={relationshipBusy}
                        onClick={() => followMutation.mutate(isFollowing)}>
                        {isFollowing ? <UserMinus className="size-4" aria-hidden="true" /> : <UserPlus className="size-4" aria-hidden="true" />}
                        {isFollowing ? 'Hủy theo dõi' : 'Theo dõi'}
                      </Menu.Item>
                    ) : (
                      <Menu.Item className={actionItemClass} disabled={messageMutation.isPending}
                        onClick={() => messageMutation.mutate()}>
                        <MessageSquare className="size-4" aria-hidden="true" /> Nhắn tin
                      </Menu.Item>
                    )}
                    <Menu.Item className={`${actionItemClass} text-destructive`} onClick={() => setIsBlockModalOpen(true)}>
                      <ShieldAlert className="size-4" aria-hidden="true" /> Chặn người dùng
                    </Menu.Item>
                  </ProfileActionMenu>
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
