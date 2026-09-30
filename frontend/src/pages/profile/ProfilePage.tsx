import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { userService } from '@/services/userService';
import { useAuthStore } from '@/stores/useAuthStore';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { ProfileAcademicInfo } from '@/features/profile/components/ProfileAcademicInfo';
import { ProfileGamificationStats } from '@/features/profile/components/ProfileGamificationStats';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from '@/components/ui/error-state';

export function ProfilePage() {
  const { userId } = useParams<{ userId?: string }>();
  const { user: currentUser } = useAuthStore();

  const isOwnProfile = !userId || Number(userId) === currentUser?.id;
  const targetUserId = isOwnProfile ? undefined : Number(userId);

  const {
    data: profileData,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['userProfile', isOwnProfile ? 'me' : targetUserId],
    queryFn: async () => {
      if (isOwnProfile) {
        const res = await userService.getMyProfile();
        return res.data;
      } else {
        const res = await userService.getUserProfile(targetUserId!);
        return res.data;
      }
    },
  });

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {isLoading ? (
        <div className="space-y-6">
          {/* Header Skeleton */}
          <div className="rounded-3xl overflow-hidden border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-md">
            <Skeleton className="h-56 sm:h-64 w-full" />
            <div className="px-6 pb-6 pt-0">
              <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-4">
                <div className="flex flex-col sm:flex-row items-center sm:items-end gap-5 text-center sm:text-left">
                  <div className="-mt-16 sm:-mt-20 shrink-0 z-10 flex justify-center sm:justify-start">
                    <Skeleton className="w-28 h-28 sm:w-32 sm:h-32 rounded-full ring-4 ring-white dark:ring-slate-900" />
                  </div>
                  <div className="space-y-2 flex-1 text-center sm:text-left pt-2 sm:pt-0 sm:pb-2">
                    <Skeleton className="h-6 w-48 mx-auto sm:mx-0" />
                    <Skeleton className="h-4 w-72 mx-auto sm:mx-0" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Content Skeletons */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Skeleton className="h-64 rounded-3xl" />
            <Skeleton className="h-64 rounded-3xl" />
          </div>
        </div>
      ) : isError || !profileData ? (
        <ErrorState
          title="Không thể tải hồ sơ người dùng"
          message={(error as any)?.message || 'Người dùng không tồn tại hoặc tài khoản đã bị khóa.'}
          onRetry={() => refetch()}
        />
      ) : (
        <>
          {/* Profile Header */}
          <ProfileHeader profile={profileData} isOwnProfile={isOwnProfile} />

          {/* Profile Body Columns */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Academic Metadata */}
            <ProfileAcademicInfo profile={profileData} />

            {/* Gamification and Stats */}
            <ProfileGamificationStats profile={profileData} />
          </div>
        </>
      )}
    </div>
  );
}
