import type { UserInfo } from '@/stores/useAuthStore';
import { useProfile } from './useProfile';

export function useCurrentUserAvatarName(user: UserInfo | null): string | undefined {
  const { data: profile } = useProfile('me', Boolean(user?.id));
  const fullName = profile?.userId === user?.id ? profile?.fullName?.trim() : undefined;
  return fullName || user?.username;
}
