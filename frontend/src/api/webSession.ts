import axios from 'axios';
import humps from 'humps';
import type { GlobalResponse } from '../types/response.types';
import type { ProfileResponse } from '../types/user.types';
import type { UserInfo } from '../stores/useAuthStore';
import { ACCESS_REQUEST_HEADERS, API_BASE_URL, getAccessGatewayError, withAuthSessionLock } from './accessSession';

let pendingRestore: Promise<{ accessToken: string; user: UserInfo } | null> | undefined;

export function restoreWebSession() {
  // React StrictMode and parallel consumers must not rotate the same cookie twice.
  pendingRestore ??= withAuthSessionLock(loadSession).finally(() => { pendingRestore = undefined; });
  return pendingRestore;
}

async function loadSession(): Promise<{ accessToken: string; user: UserInfo } | null> {
  const options = { withCredentials: true, timeout: 15000, headers: ACCESS_REQUEST_HEADERS };
  try {
    const refreshed = await axios.post<GlobalResponse<{ access_token: string }>>(`${API_BASE_URL}/auth/refresh`, undefined, options);
    const accessToken = refreshed.data.data?.access_token;
    if (!accessToken) throw new Error('Máy chủ chưa trả về phiên hợp lệ.');
    const response = await axios.get<GlobalResponse<ProfileResponse>>(`${API_BASE_URL}/users/me`, {
      ...options, headers: { ...ACCESS_REQUEST_HEADERS, Authorization: `Bearer ${accessToken}` },
    });
    const profile = humps.camelizeKeys(response.data.data) as ProfileResponse;
    if (!profile?.userId || !profile.email || !['STUDENT', 'LECTURER', 'ADMIN'].includes(profile.role)) {
      throw new Error('Không thể khôi phục thông tin tài khoản.');
    }
    return { accessToken, user: {
      id: profile.userId, username: profile.email, role: profile.role,
      avatarUrl: profile.avatarUrl ?? undefined, coverUrl: profile.coverUrl ?? undefined,
      frameUrl: profile.frameUrl ?? undefined, verified: profile.verified, profileCompleted: profile.profileCompleted,
    } };
  } catch (error) {
    const gatewayError = getAccessGatewayError(error);
    if (gatewayError) throw gatewayError;
    if (axios.isAxiosError(error) && [401, 403].includes(error.response?.status ?? 0)
      && error.response?.data && typeof error.response.data === 'object' && 'code' in error.response.data) return null;
    throw error;
  }
}
