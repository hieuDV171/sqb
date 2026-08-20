export const DevicePlatform = {
    WEB: "WEB",
    IOS: "IOS",
    ANDROID: "ANDROID",
    DESKTOP: "DESKTOP",
} as const;

export type DevicePlatform = (typeof DevicePlatform)[keyof typeof DevicePlatform];

export interface LoginRequest {
    email: string;
    password: string;
    deviceId: string;
    fcmToken?: string;
    platform: DevicePlatform;
    deviceName?: string;
    osVersion?: string;
    appVersion?: string;
}

export interface AuthResponse {
    id: number;
    username: string;
    accessToken: string;
    refreshToken: string | null;
    avatarUrl?: string;
    coverUrl?: string;
    frameUrl?: string;
    verified: boolean;
    profileCompleted: boolean;
}

export interface ChangePasswordRequest {
    oldPassword: string;
    newPassword: string;
}

export interface RefreshTokenResponse {
    accessToken: string;
    refreshToken: string | null;
}
