import axios, { type InternalAxiosRequestConfig } from "axios";
import humps from "humps";
import { useAuthStore } from "../stores/useAuthStore";
import { ACCESS_REQUEST_HEADERS, API_BASE_URL, getAccessGatewayError, isAccessTokenExpiring, refreshAccessToken } from "./accessSession";

declare module "axios" {
    export interface AxiosRequestConfig {
        skipAuthRefresh?: boolean;
        preserveEnumKeys?: boolean;
        _retry?: boolean;
    }
}

const preserveEnumKeys: humps.HumpsProcessor = (key, convert, options) =>
    /^[A-Z][A-Z0-9_]*$/.test(key) ? key : convert(key, options);

export const axiosClient = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    headers: {
        ...ACCESS_REQUEST_HEADERS,
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
        config.data = humps.decamelizeKeys(config.data, config.preserveEnumKeys ? preserveEnumKeys : undefined);
    }
    if (config.params) {
        config.params = humps.decamelizeKeys(config.params);
    }
    return config;
});

// Response Interceptor: Tự động Refresh Token khi 401 và chuyển snake_case -> camelCase bằng humps
axiosClient.interceptors.response.use(
    (response) => humps.camelizeKeys(
        response.data, response.config.preserveEnumKeys ? preserveEnumKeys : undefined
    ) as unknown as typeof response, // Trả về trực tiếp GlobalResponse<T> đã convert thành camelCase
    async (error) => {
        if (axios.isCancel(error)) return Promise.reject(error);

        const gatewayError = getAccessGatewayError(error);
        if (gatewayError) {
            return Promise.reject(gatewayError);
        }

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
            if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
                window.location.href = '/login?reason=setup-expired';
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

        const status = error.response?.status;
        if (!status || status >= 500) {
            console.warn('[API] Yêu cầu thất bại', { method: originalRequest?.method, status, code: error.code });
        }

        return Promise.reject(formattedError);
    },
);
