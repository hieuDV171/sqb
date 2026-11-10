import axios from 'axios';
import humps from 'humps';
import type { GlobalResponse } from '../types/response.types';
import { useAuthStore } from '../stores/useAuthStore';
import { toast } from '../stores/useToastStore';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

interface AccessClaims {
    userId?: number;
    deviceId?: string;
    sessionId?: string;
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

export function readAccessSessionId(token: string | null): string | undefined {
    return token ? readClaims(token)?.sessionId : undefined;
}

export function isAccessTokenExpiring(token: string): boolean {
    const expiresAt = readClaims(token)?.exp;
    // Allow time for the request or STOMP handshake to reach the server.
    return typeof expiresAt === 'number' && expiresAt * 1000 <= Date.now() + 5000;
}

let pendingRefresh: { token: string; request: Promise<string | null>; result: Promise<string> } | undefined;

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

    const request = requestRefresh(token);
    const result = applyRefresh(token, request).finally(() => {
        pendingRefresh = undefined;
    });
    pendingRefresh = { token, request, result };
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

async function requestRefresh(token: string): Promise<string | null> {
    const run = () => fetchRotatedToken(token);
    return typeof navigator !== 'undefined' && navigator.locks
        ? navigator.locks.request('sqb-refresh-token', run)
        : run();
}

async function fetchRotatedToken(token: string): Promise<string | null> {
    // Use the HttpOnly cookie with a plain Axios request to avoid the interceptors.
    const response = await axios.post<GlobalResponse<{ access_token: string }>>(
        `${API_BASE_URL.replace(/\/$/, '')}/auth/refresh`,
        undefined,
        { withCredentials: true, timeout: 15000 },
    );
    const refreshData = humps.camelizeKeys(response.data) as GlobalResponse<{ accessToken: string }>;
    const newToken = refreshData?.data?.accessToken;
    const identity = readSessionIdentity(token);
    const sessionId = readAccessSessionId(token);
    return typeof newToken === 'string' && identity && readSessionIdentity(newToken) === identity
        && (!sessionId || readAccessSessionId(newToken) === sessionId) ? newToken : null;
}

async function applyRefresh(token: string, request: Promise<string | null>): Promise<string> {
    const newToken = await request.catch((error: unknown) => {
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

    if (!newToken) {
        expireSession(token);
        throw new Error('Refresh response does not match the current session');
    }

    session.setTokens(newToken);
    return newToken;
}

export async function revokeAccessSession(token: string, canRefresh: boolean): Promise<void> {
    const logout = (accessToken: string) => axios.post(
        `${API_BASE_URL.replace(/\/$/, '')}/auth/logout`,
        undefined,
        { headers: { Authorization: `Bearer ${accessToken}` }, timeout: 15000 },
    );

    // Let a concurrent rotation finish before revoking the refresh token for this device.
    const refresh = pendingRefresh?.token === token ? pendingRefresh.request : undefined;
    const rotatedToken = await refresh?.catch(() => null);
    try {
        await logout(rotatedToken ?? token);
    } catch (error) {
        if (!canRefresh || refresh || !axios.isAxiosError(error) || error.response?.status !== 401) throw error;
        const freshToken = await requestRefresh(token);
        if (!freshToken) throw new Error('Refresh response does not match the session being signed out', { cause: error });
        await logout(freshToken);
    }
}
