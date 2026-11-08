import axios, { type InternalAxiosRequestConfig } from "axios";
import humps from "humps";
import { useAuthStore } from "../stores/useAuthStore";
import { toast } from "../stores/useToastStore";
import { API_BASE_URL, isAccessTokenExpiring, refreshAccessToken } from "./accessSession";

declare module "axios" {
    export interface AxiosRequestConfig {
        skipToast?: boolean;
        skipAuthRefresh?: boolean;
        _retry?: boolean;
    }
}

export const axiosClient = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    headers: {
        "Content-Type": "application/json",
    },
});

// Request Interceptor: Tự động đính kèm Access Token và chuyển camelCase -> snake_case bằng humps
axiosClient.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
    const session = useAuthStore.getState();
    let token = session.accessToken;
    if (config._retry && (!token || config.headers.Authorization !== `Bearer ${token}`)) {
        throw new axios.CanceledError('Session changed before retry');
    }
    if (token && session.tokenType === 'ACCESS' && !config._retry && !config.skipAuthRefresh && isAccessTokenExpiring(token)) {
        token = await refreshAccessToken(token);
        if (useAuthStore.getState().accessToken !== token) {
            throw new axios.CanceledError('Session changed before request');
        }
    }
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
    (response) => humps.camelizeKeys(response.data) as unknown as typeof response, // Trả về trực tiếp GlobalResponse<T> đã convert thành camelCase
    async (error) => {
        if (axios.isCancel(error)) return Promise.reject(error);

        const originalRequest = error.config;
        const formattedError = error.response?.data ? humps.camelizeKeys(error.response.data) : error;
        const responseCode = formattedError?.code;
        const currentToken = useAuthStore.getState().accessToken;

        if (error.response?.status === 401 && originalRequest?.headers?.Authorization &&
            originalRequest.headers.Authorization !== `Bearer ${currentToken}`) {
            return Promise.reject(formattedError);
        }

        if (error.response?.status === 403 && responseCode === '1013') {
            if (typeof window !== 'undefined' && window.location.pathname !== '/account-setup') {
                window.location.href = '/account-setup';
            }
            return Promise.reject(formattedError);
        }

        if (error.response?.status === 401 && useAuthStore.getState().tokenType === 'ONBOARDING') {
            useAuthStore.getState().logout();
            toast.warning('Phiên thiết lập đã hết hạn. Vui lòng đăng nhập lại.', 'Hết phiên thiết lập');
            if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
                window.location.href = '/login';
            }
            return Promise.reject(formattedError);
        }

        // 1. Xử lý 401 (Unauthorized) - Silent Refresh Token
        // Web lưu Refresh Token trong HttpOnly cookie, không đọc token này từ JavaScript.
        if (
            error.response?.status === 401 &&
            originalRequest &&
            !originalRequest._retry &&
            !originalRequest.skipAuthRefresh &&
            currentToken &&
            useAuthStore.getState().tokenType === 'ACCESS'
        ) {
            const newAccessToken = await refreshAccessToken(currentToken);
            originalRequest._retry = true;
            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
            return axiosClient(originalRequest);
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
