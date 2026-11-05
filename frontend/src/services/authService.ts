import { axiosClient } from '@/api/axiosClient';
import type { AuthResponse, ChangePasswordRequest, LoginRequest, RefreshTokenResponse } from '../types/auth.types';
import type { GlobalResponse } from '../types/response.types';

export const authService = {
  login: async (data: LoginRequest): Promise<GlobalResponse<AuthResponse>> => {
    return await axiosClient.post<GlobalResponse<AuthResponse>>('/auth/login', data, {
      skipToast: true,
      skipAuthRefresh: true,
    }) as unknown as GlobalResponse<AuthResponse>;
  },

  logout: async (): Promise<GlobalResponse<void>> => {
    return await axiosClient.post<GlobalResponse<void>>('/auth/logout') as unknown as GlobalResponse<void>;
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
