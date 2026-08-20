import { useNavigate } from 'react-router-dom'
import { useAuthStore } from "@/stores/useAuthStore";
import { useState } from "react";
import type { LoginRequest } from '@/types/auth.types';
import { authService } from '@/services/authService';

export function useAuth() {

    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const setAuth = useAuthStore((state) => state.setAuth);
    const logoutStore = useAuthStore((state) => state.logout);
    const navigate = useNavigate();

    const login = async (data: LoginRequest) => {

        setIsLoading(true);
        setError(null);

        try {
            const response = await authService.login(data);
            const authData = response.data;

            // Lưu Token và Thông tin User vào Zustand Store (LocalStorage)
            setAuth(authData.accessToken, authData.refreshToken || '', {
                id: authData.id,
                username: authData.username,
                avatarUrl: authData.avatarUrl,
                coverUrl: authData.coverUrl,
                frameUrl: authData.frameUrl,
                verified: authData.verified,
                profileCompleted: authData.profileCompleted
            })

            // Đăng nhập thành công -> TẠM THỜI chuyển sang màn hình Quản lý thiết bị
            navigate('/devices');
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
            navigate('/login');
        }
    };

    return { login, logout, isLoading, error }

}