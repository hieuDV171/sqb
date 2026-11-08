import axios from 'axios';
import humps from 'humps';
import type { GlobalResponse } from '../types/response.types';
import { useAuthStore } from '../stores/useAuthStore';
import { toast } from '../stores/useToastStore';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

interface AccessClaims {
    userId?: number;
    deviceId?: string;
    exp?: number;
}

function readClaims(token: string): AccessClaims | null {
    try {
        const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(atob(payload)) as AccessClaims;
    } catch {
        return null;
    }
}

function readSessionIdentity(token: string): string | null {
    const claims = readClaims(token);
    return typeof claims?.userId === 'number' && typeof claims.deviceId === 'string' && claims.deviceId
        ? JSON.stringify([claims.userId, claims.deviceId])
        : null;
}

export function isAccessTokenExpiring(token: string): boolean {
    const expiresAt = readClaims(token)?.exp;
    // Allow time for the request or STOMP handshake to reach the server.
    return typeof expiresAt === 'number' && expiresAt * 1000 <= Date.now() + 5000;
}

let pendingRefresh: { token: string; result: Promise<string> } | undefined;

export async function refreshAccessToken(token: string): Promise<string> {
    const session = useAuthStore.getState();
    if (session.accessToken !== token || session.tokenType !== 'ACCESS') {
        throw new axios.CanceledError('Session changed before refresh');
    }
    if (pendingRefresh) {
        if (pendingRefresh.token !== token) {
            throw new axios.CanceledError('A previous session is still refreshing');
        }
        return pendingRefresh.result;
    }

    const result = requestRefresh(token).finally(() => {
        pendingRefresh = undefined;
    });
    pendingRefresh = { token, result };
    return result;
}

function expireSession(token: string): void {
    const session = useAuthStore.getState();
    if (session.accessToken !== token) return;

    session.logout();
    toast.warning('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.', 'Hết phiên đăng nhập');
    if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
        window.location.href = '/login';
    }
}

async function requestRefresh(token: string): Promise<string> {
    // Use the HttpOnly cookie with a plain Axios request to avoid the interceptors.
    const response = await axios.post<GlobalResponse<{ access_token: string }>>(
        `${API_BASE_URL.replace(/\/$/, '')}/auth/refresh`,
        undefined,
        { withCredentials: true, timeout: 15000 },
    ).catch((error: unknown) => {
        if (useAuthStore.getState().accessToken !== token) {
            throw new axios.CanceledError('Session changed during refresh');
        }
        if (axios.isAxiosError<GlobalResponse<null>>(error)) {
            const status = error.response?.status;
            if (status === 401 || status === 403 || error.response?.data?.code === '9995') {
                expireSession(token);
                throw new axios.CanceledError('Session expired');
            }
        }
        throw error;
    });

    const session = useAuthStore.getState();
    if (session.accessToken !== token) {
        throw new axios.CanceledError('Session changed during refresh');
    }

    const refreshData = humps.camelizeKeys(response.data) as GlobalResponse<{ accessToken: string }>;
    const newToken = refreshData?.data?.accessToken;
    const identity = readSessionIdentity(token);
    if (typeof newToken !== 'string' || !identity || readSessionIdentity(newToken) !== identity) {
        expireSession(token);
        throw new Error('Refresh response does not match the current session');
    }

    session.setTokens(newToken);
    return newToken;
}
