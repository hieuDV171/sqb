import axios from 'axios';
import humps from 'humps';
import type { GlobalResponse } from '../types/response.types';
import type { ProfileResponse } from '../types/user.types';
import type { UserInfo } from '../stores/useAuthStore';
import { ACCESS_REQUEST_HEADERS, API_BASE_URL, getAccessGatewayError, withAuthSessionLock } from './accessSession';

interface RestoredWebSession {
  accessToken: string;
  user: UserInfo;
  profile: ProfileResponse;
}

let pendingRestore: Promise<RestoredWebSession | null> | undefined;

async function measureSessionStep<T>(name: string, run: () => Promise<T>): Promise<T> {
  const start = performance.now();
  try { return await run(); }
  finally { performance.measure(name, { start, end: performance.now() }); }
}

export function restoreWebSession() {
  // React StrictMode and parallel consumers must not rotate the same cookie twice.
  if (!pendingRestore) {
    const start = performance.now();
    // Local User Timing entries contain durations only, never tokens/profile data.
    pendingRestore = measureSessionStep('sqb:session-restore', () => withAuthSessionLock(() => {
      performance.measure('sqb:session-lock-wait', { start, end: performance.now() });
      return loadSession();
    })).finally(() => { pendingRestore = undefined; });
  }
  return pendingRestore;
}

async function loadSession(): Promise<RestoredWebSession | null> {
  const options = { withCredentials: true, timeout: 15000, headers: ACCESS_REQUEST_HEADERS };
  try {
    const refreshed = await measureSessionStep('sqb:auth-refresh', () =>
      axios.post<GlobalResponse<{ access_token: string }>>(`${API_BASE_URL}/auth/refresh`, undefined, options));
    const accessToken = refreshed.data.data?.access_token;
    if (!accessToken) throw new Error('Máy chủ chưa trả về phiên hợp lệ.');
    const response = await measureSessionStep('sqb:session-profile', () => axios.get<GlobalResponse<ProfileResponse>>(`${API_BASE_URL}/users/me`, {
      ...options, headers: { ...ACCESS_REQUEST_HEADERS, Authorization: `Bearer ${accessToken}` },
    }));
    const profile = humps.camelizeKeys(response.data.data) as ProfileResponse;
    if (!profile?.userId || !profile.email || !['STUDENT', 'LECTURER', 'ADMIN'].includes(profile.role)) {
      throw new Error('Không thể khôi phục thông tin tài khoản.');
    }
    return { accessToken, profile, user: {
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
