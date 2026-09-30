import axios, { type InternalAxiosRequestConfig } from "axios";
import humps from "humps";
import type { GlobalResponse } from "../types/response.types";
import { useAuthStore } from "../stores/useAuthStore";
import { toast } from "../stores/useToastStore";

declare module "axios" {
    export interface AxiosRequestConfig {
        skipToast?: boolean;
    }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

export const axiosClient = axios.create({
    baseURL: BASE_URL,
    withCredentials: true,
    headers: {
        "Content-Type": "application/json",
    },
});

// Flag và Queue để xử lý việc nhiều request cùng 401 một lúc
let isRefreshing = false;
let failedQueue: Array<{
    resolve: (value?: unknown) => void;
    reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
    failedQueue.forEach((prom) => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

// Request Interceptor: Tự động đính kèm Access Token và chuyển camelCase -> snake_case bằng humps
axiosClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;
    if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    if (config.data && !(config.data instanceof FormData)) {
        config.data = humps.decamelizeKeys(config.data);
    }
    if (config.params) {
        config.params = humps.decamelizeKeys(config.params);
    }
    return config;
});

// Response Interceptor: Tự động Refresh Token khi 401 và chuyển snake_case -> camelCase bằng humps
axiosClient.interceptors.response.use(
    (response) => humps.camelizeKeys(response.data) as any, // Trả về trực tiếp GlobalResponse<T> đã convert thành camelCase
    async (error) => {
        const originalRequest = error.config;
        const formattedError = error.response?.data ? humps.camelizeKeys(error.response.data) : error;

        // 1. Xử lý 401 (Unauthorized) - Silent Refresh Token
        if (error.response?.status === 401 && !originalRequest?._retry) {
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                    .then((token) => {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                        return axiosClient(originalRequest);
                    })
                    .catch((err) => Promise.reject(err));
            }

            originalRequest._retry = true;
            isRefreshing = true;

            const currentRefreshToken = useAuthStore.getState().refreshToken;

            if (!currentRefreshToken) {
                isRefreshing = false;
                useAuthStore.getState().logout();
                toast.warning("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.", "Hết phiên đăng nhập");
                return Promise.reject(formattedError);
            }

            try {
                // Gọi API /auth/refresh độc lập qua axios thuần
                const refreshResponse = await axios.post<
                    GlobalResponse<{
                        access_token: string;
                        refresh_token: string;
                    }>
                >(`${BASE_URL}/auth/refresh`, humps.decamelizeKeys({
                    refreshToken: currentRefreshToken,
                }));

                const formattedRefreshData = humps.camelizeKeys(refreshResponse.data) as GlobalResponse<{
                    accessToken: string;
                    refreshToken: string;
                }>;
                const newTokens = formattedRefreshData.data;
                const newAccessToken = newTokens.accessToken;
                const newRefreshToken = newTokens.refreshToken;

                // Cập nhật cặp Token mới vào Zustand Store
                useAuthStore
                    .getState()
                    .setTokens(newAccessToken, newRefreshToken);

                // Cập nhật Header cho request cũ bị thất bại và chạy lại các request trong hàng chờ
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                processQueue(null, newAccessToken);

                // Chạy lại request ban đầu
                return axiosClient(originalRequest);
            } catch (refreshError) {
                processQueue(refreshError, null);
                useAuthStore.getState().logout();
                toast.warning("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.", "Hết phiên đăng nhập");

                if (
                    typeof window !== "undefined" &&
                    !window.location.pathname.includes("/login")
                ) {
                    window.location.href = "/login";
                }

                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        // 2. Tự động hiển thị Toast Error cho các mã lỗi HTTP khác nếu không cấu hình skipToast
        if (!originalRequest?.skipToast) {
            const status = error.response?.status;
            const message = formattedError?.message || (typeof formattedError === 'string' ? formattedError : null);

            if (!status) {
                // Network error / Connection refused
                toast.error("Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.", "Lỗi kết nối");
            } else if (status === 400) {
                toast.error(message || "Yêu cầu không hợp lệ. Vui lòng kiểm tra lại thông tin.", "Lỗi dữ liệu (400)");
            } else if (status === 403) {
                toast.error(message || "Bạn không có quyền thực hiện thao tác này.", "Từ chối truy cập (403)");
            } else if (status === 404) {
                // Chỉ hiển thị toast 404 nếu có message cụ thể từ backend
                if (message) {
                    toast.warning(message, "Không tìm thấy dữ liệu (404)");
                }
            } else if (status >= 500) {
                toast.error(message || "Máy chủ gặp sự cố xử lý. Vui lòng thử lại sau.", "Lỗi hệ thống (500)");
            }
        }

        return Promise.reject(formattedError);
    },
);
