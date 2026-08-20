import { axiosClient } from '../api/axiosClient';
import type { AuthResponse, ChangePasswordRequest, LoginRequest } from '../types/auth.types';
import type { GlobalResponse } from '../types/response.types';

export const authService = {
  login: async (data: LoginRequest): Promise<GlobalResponse<AuthResponse>> => {
    return axiosClient.post('/auth/login', data);
  },

  logout: async (): Promise<GlobalResponse<void>> => {
    return axiosClient.post('/auth/logout');
  },

  changePassword: async (data: ChangePasswordRequest): Promise<GlobalResponse<void>> => {
    return axiosClient.post('/auth/change-password', data);
  },
};
