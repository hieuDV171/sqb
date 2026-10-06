import { axiosClient } from '@/api/axiosClient';
import type { AuthResponse, ChangePasswordRequest, LoginRequest, RefreshTokenResponse } from '../types/auth.types';
import type { GlobalResponse } from '../types/response.types';

export const authService = {
  login: async (data: LoginRequest): Promise<GlobalResponse<AuthResponse>> => {
    return await axiosClient.post<GlobalResponse<AuthResponse>>('/auth/login', data, { skipToast: true } as any) as any;
  },

  logout: async (): Promise<GlobalResponse<void>> => {
    return await axiosClient.post<GlobalResponse<void>>('/auth/logout') as any;
  },

  changePassword: async (data: ChangePasswordRequest): Promise<GlobalResponse<void>> => {
    return await axiosClient.post<GlobalResponse<void>>('/auth/change-password', data) as any;
  },

  refreshToken: async (refreshToken?: string): Promise<GlobalResponse<RefreshTokenResponse>> => {
    return await axiosClient.post<GlobalResponse<RefreshTokenResponse>>('/auth/refresh', {
      refreshToken,
    }) as any;
  },
};
