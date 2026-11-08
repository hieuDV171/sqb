import { useNavigate } from 'react-router-dom';
import { useAuthStore } from "@/stores/useAuthStore";
import { useState } from "react";
import { useQueryClient } from '@tanstack/react-query';
import type { LoginRequest } from '@/types/auth.types';
import { authService } from '@/services/authService';
import { toast } from '@/stores/useToastStore';

import { stompClient } from '@/lib/stompClient';

export function useAuth() {

    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const setAuth = useAuthStore((state) => state.setAuth);
    const logoutStore = useAuthStore((state) => state.logout);
    const queryClient = useQueryClient();
    const navigate = useNavigate();

    const login = async (data: LoginRequest) => {

        setIsLoading(true);
        setError(null);

        try {
            const response = await authService.login(data);
            const authData = response.data;

            // Dọn dẹp cache của tài khoản trước đó để chống rò rỉ dữ liệu giữa các phiên
            queryClient.clear();

            // Chỉ lưu Access Token và thông tin User; Refresh Token web ở HttpOnly cookie.
            setAuth(authData.accessToken, {
                id: authData.userId ?? 0,
                username: authData.username,
                role: authData.role,
                avatarUrl: authData.avatarUrl,
                coverUrl: authData.coverUrl,
                frameUrl: authData.frameUrl,
                verified: authData.verified,
                profileCompleted: authData.profileCompleted
            }, authData.tokenType, authData.accountState)

            if (authData.accountState !== 'ACTIVE') {
                navigate('/account-setup');
            } else if (authData.role === 'ADMIN') {
                navigate('/admin/users');
            } else if (authData.role === 'LECTURER') {
                navigate('/lecturer/sessions');
            } else {
                navigate('/feed');
            }
        } catch (err: unknown) {
            const message =
                typeof err === 'object' &&
                err !== null &&
                'message' in err &&
                typeof err.message === 'string'
                    ? err.message
                    : null;
            setError(
                message || "Đăng nhập thất bại. Vui lòng kiểm tra lại!",
            );
        } finally {
            setIsLoading(false);
        }
    };

    const logout = () => {
        void authService.logout().catch(() => {
            toast.warning('Đã thoát trên trình duyệt. Máy chủ chưa xác nhận thu hồi phiên.');
        });
        stompClient.disconnect();
        logoutStore();
        queryClient.clear();
        navigate('/login', { replace: true });
    };

    return { login, logout, isLoading, error }

}
