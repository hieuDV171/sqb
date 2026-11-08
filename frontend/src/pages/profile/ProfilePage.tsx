import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, UserRound } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { ProfileAcademicInfo } from '@/features/profile/components/ProfileAcademicInfo';
import { ProfileGamificationStats } from '@/features/profile/components/ProfileGamificationStats';
import { ProfilePosts } from '@/features/profile/components/ProfilePosts';
import { useProfile } from '@/features/profile/hooks/useProfile';
import { useAuthStore } from '@/stores/useAuthStore';

export function ProfilePage() {
  const { userId } = useParams<{ userId?: string }>();
  const currentUserId = useAuthStore((state) => state.user?.id);
  const parsedId = Number(userId);
  const isValid = userId === undefined || (/^\d+$/.test(userId) && Number.isSafeInteger(parsedId) && parsedId > 0);
  const isOwnProfile = userId === undefined || parsedId === currentUserId;
  const profile = useProfile(isOwnProfile ? 'me' : parsedId, isValid);

  return (
    <div className="mx-auto max-w-5xl space-y-5 pb-10">
      {isOwnProfile && (
        <Button nativeButton={false} variant="ghost" render={<Link to="/feed" />}>
          <ArrowLeft aria-hidden="true" /> Bảng tin
        </Button>
      )}

      {!isValid ? (
        <EmptyState icon={UserRound} title="Đường dẫn hồ sơ không hợp lệ" description="Hãy mở lại hồ sơ từ bảng tin hoặc danh sách bạn bè." />
      ) : profile.isLoading ? (
        <div role="status" aria-label="Đang tải hồ sơ" className="space-y-5">
          <div className="rounded-2xl border border-border bg-card">
            <Skeleton className="h-36 rounded-t-2xl rounded-b-none sm:h-52" />
            <div className="relative space-y-4 px-4 pb-5 sm:px-6">
              <Skeleton className="-mt-10 size-24 rounded-full" />
              <Skeleton className="h-8 w-48 max-w-full" />
              <Skeleton className="h-5 w-64 max-w-full" />
            </div>
          </div>
          <div className="grid gap-5 lg:grid-cols-2">
            <Skeleton className="h-52 rounded-2xl" />
            <Skeleton className="h-52 rounded-2xl" />
          </div>
        </div>
      ) : profile.isError ? (
        <ErrorState title="Không thể tải hồ sơ" message="Hãy kiểm tra kết nối và quyền truy cập hồ sơ." onRetry={() => void profile.refetch()} />
      ) : profile.data ? (
        <>
          <ProfileHeader key={profile.data.userId} profile={profile.data} isOwnProfile={isOwnProfile} />
          <div className="grid items-start gap-5 lg:grid-cols-2">
            <ProfileAcademicInfo profile={profile.data} />
            <ProfileGamificationStats profile={profile.data} />
          </div>
          <ProfilePosts key={profile.data.userId} userId={profile.data.userId} isOwnProfile={isOwnProfile} />
        </>
      ) : (
        <EmptyState icon={UserRound} title="Không tìm thấy hồ sơ" description="Hồ sơ này hiện không có dữ liệu để hiển thị." />
      )}
    </div>
  );
}
