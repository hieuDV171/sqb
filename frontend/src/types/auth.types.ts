export const DevicePlatform = {
    WEB: "WEB",
    IOS: "IOS",
    ANDROID: "ANDROID",
    DESKTOP: "DESKTOP",
} as const;

export type DevicePlatform = (typeof DevicePlatform)[keyof typeof DevicePlatform];

export type UserRole = "STUDENT" | "LECTURER" | "ADMIN";
export type Gender = "MALE" | "FEMALE" | "OTHER";

export interface LoginRequest {
    email: string;
    password: string;
    deviceId: string;
    platform: DevicePlatform;
    deviceName?: string;
    osVersion?: string;
    appVersion?: string;
    fcmToken?: string;
}

export interface AuthResponse {
    userId: number;
    username: string;
    role: UserRole;
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

// ==========================================
// ADMIN USER MANAGEMENT & IMPORT DTOS
// ==========================================

export interface SingleUserImportDto {
    email: string;
    fullName: string;
    password?: string;
    studentLecturerCode?: string;
    schoolFaculty?: string;
    major?: string;
    className?: string;
    gender?: Gender;
    dateOfBirth?: string; // Format: "YYYY-MM-DD"
    role?: UserRole;
    timezone?: string;
}

export interface BulkImportRequest {
    users: SingleUserImportDto[];
}

export interface BulkImportResult {
    totalSuccess: number;
    totalFailed: number;
    errors: string[];
}

export interface AdminResetPasswordRequest {
    email: string;
    newPassword?: string | null;
}

export interface ExcelImportClassResult {
    courseClassId: number;
    classCode: string;
    subjectCode: string;
    subjectName: string;
    semesterName: string;
    totalRowsInFile: number;
    newUsersCreated: number;
    existingUsersFound: number;
    newEnrollments: number;
    alreadyEnrolledCount: number;
}
