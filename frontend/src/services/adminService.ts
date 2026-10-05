import { axiosClient } from '@/api/axiosClient';
import type {
  AdminResetPasswordRequest,
  BulkImportRequest,
  BulkImportResult,
  ExcelImportClassResult,
  LecturerSummary,
  UserRole,
} from '@/types/auth.types';
import type { GlobalResponse } from '@/types/response.types';

export const adminService = {
  /**
   * Lấy danh sách Giảng viên hoạt động trong hệ thống
   */
  getLecturers: async (): Promise<GlobalResponse<LecturerSummary[]>> => {
    return (await axiosClient.get<GlobalResponse<LecturerSummary[]>>(
      '/lecturer/course-classes/lecturers'
    )) as any;
  },

  /**
   * Import danh sách lớp và sinh viên đầu kỳ từ file Excel
   * Tự động tạo tài khoản SV mới (verified = true, pass mặc định) và ghi danh vào lớp
   */
  importExcelCourseClass: async (
    file: File,
    lecturerId?: number
  ): Promise<GlobalResponse<ExcelImportClassResult>> => {
    const formData = new FormData();
    formData.append('file', file);
    if (lecturerId) {
      formData.append('lecturerId', lecturerId.toString());
    }

    return (await axiosClient.post<GlobalResponse<ExcelImportClassResult>>(
      '/lecturer/course-classes/import-excel',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    )) as any;
  },


  /**
   * Tạo tài khoản tùy chỉnh hàng loạt hoặc đơn lẻ
   */
  bulkImportUsers: async (
    data: BulkImportRequest
  ): Promise<GlobalResponse<BulkImportResult>> => {
    return (await axiosClient.post<GlobalResponse<BulkImportResult>>(
      '/auth/admin/bulk-import',
      data
    )) as any;
  },

  /**
   * Reset mật khẩu sinh viên / giảng viên về mật khẩu mặc định (hoặc mật khẩu chỉ định)
   * Đồng thời xóa sạch refresh token để thu hồi phiên trên toàn bộ thiết bị
   */
  adminResetPassword: async (
    data: AdminResetPasswordRequest
  ): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>(
      '/auth/admin/reset-password',
      data
    )) as any;
  },

  /**
   * Đăng ký tài khoản nội bộ Admin (gửi OTP kích hoạt)
   */
  adminRegister: async (data: {
    email: string;
    password: string;
    deviceId: string;
    role: UserRole;
    timezone?: string;
  }): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>(
      '/auth/admin/register',
      data
    )) as any;
  },

  /**
   * Xác thực mã OTP nội bộ Admin
   */
  adminVerifyOtp: async (data: {
    email: string;
    verifyCode: string;
    deviceId: string;
  }): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>(
      '/auth/admin/verify',
      data
    )) as any;
  },
};
