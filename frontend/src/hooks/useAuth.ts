import { useNavigate } from 'react-router-dom';
import { useAuthStore } from "@/stores/useAuthStore";
import { useState } from "react";
import { useQueryClient } from '@tanstack/react-query';
import type { LoginRequest } from '@/types/auth.types';
import { authService } from '@/services/authService';

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

            // Lưu Token và Thông tin User vào Zustand Store (LocalStorage)
            setAuth(authData.accessToken, authData.refreshToken || '', {
                id: authData.userId ?? 0,
                username: authData.username,
                role: authData.role,
                avatarUrl: authData.avatarUrl,
                coverUrl: authData.coverUrl,
                frameUrl: authData.frameUrl,
                verified: authData.verified,
                profileCompleted: authData.profileCompleted
            })

            // Đăng nhập thành công -> Điều hướng về trang mặc định theo vai trò (Role-based Navigation)
            if (authData.role === 'ADMIN') {
                navigate('/admin/users');
            } else if (authData.role === 'LECTURER') {
                navigate('/lecturer/sessions');
            } else {
                navigate('/feed');
            }
        } catch (err: any) {
            setError(
                err?.message || "Đăng nhập thất bại. Vui lòng kiểm tra lại!",
            );
        } finally {
            setIsLoading(false);
        }
    };

    const logout = async () => {
        try {
            await authService.logout();
        } catch (e) {
            console.error('Logout error', e);
        } finally {
            logoutStore();
            queryClient.clear();
            navigate('/login');
        }
    };

    return { login, logout, isLoading, error }

}