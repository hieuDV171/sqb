import { axiosClient } from '@/api/axiosClient';
import { revokeAccessSession } from '@/api/accessSession';
import { useAuthStore } from '@/stores/useAuthStore';
import type { AuthResponse, ChangePasswordRequest, LoginRequest, RefreshTokenResponse } from '../types/auth.types';
import type { GlobalResponse } from '../types/response.types';

let pendingLogout: Promise<void> | undefined;

export const authService = {
  login: async (data: LoginRequest): Promise<GlobalResponse<AuthResponse>> => {
    await pendingLogout?.catch(() => undefined);
    return await axiosClient.post<GlobalResponse<AuthResponse>>('/auth/login', data, {
      skipToast: true,
      skipAuthRefresh: true,
    }) as unknown as GlobalResponse<AuthResponse>;
  },

  logout: (): Promise<void> => {
    if (pendingLogout) return pendingLogout;
    const { accessToken, tokenType } = useAuthStore.getState();
    if (!accessToken) return Promise.resolve();
    pendingLogout = revokeAccessSession(accessToken, tokenType === 'ACCESS').finally(() => {
      pendingLogout = undefined;
    });
    return pendingLogout;
  },

  changePassword: async (data: ChangePasswordRequest): Promise<GlobalResponse<void>> => {
    return await axiosClient.post<GlobalResponse<void>>('/auth/change-password', data) as unknown as GlobalResponse<void>;
  },

  requestEmailVerification: async (): Promise<GlobalResponse<void>> => {
    return await axiosClient.post<GlobalResponse<void>>('/auth/email-verification/request') as unknown as GlobalResponse<void>;
  },

  confirmEmailVerification: async (verifyCode: string): Promise<GlobalResponse<void>> => {
    return await axiosClient.post<GlobalResponse<void>>('/auth/email-verification/confirm', { verifyCode }) as unknown as GlobalResponse<void>;
  },

  refreshToken: async (): Promise<GlobalResponse<RefreshTokenResponse>> => {
    return await axiosClient.post<GlobalResponse<RefreshTokenResponse>>(
      '/auth/refresh',
      undefined,
      { skipAuthRefresh: true },
    ) as unknown as GlobalResponse<RefreshTokenResponse>;
  },
};
