import { axiosClient } from '@/api/axiosClient';
import type {
  AdminResetPasswordRequest,
  AdminSubjectResponse,
  BulkImportRequest,
  BulkImportResult,
  ExcelImportClassResult,
  LecturerSummary,
  SemesterResponse,
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
      formData.append('lecturer_id', lecturerId.toString());
    }

    return (await axiosClient.post<GlobalResponse<ExcelImportClassResult>>(
      '/admin/course-classes/import-excel',
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

  /**
   * Tạo học kỳ mới
   */
  createSemester: async (data: { name: string }): Promise<GlobalResponse<SemesterResponse>> => {
    return (await axiosClient.post<GlobalResponse<SemesterResponse>>(
      '/admin/semesters',
      data
    )) as any;
  },

  /**
   * Lấy danh sách tất cả học kỳ
   */
  getAllSemesters: async (): Promise<GlobalResponse<SemesterResponse[]>> => {
    return (await axiosClient.get<GlobalResponse<SemesterResponse[]>>(
      '/admin/semesters'
    )) as any;
  },

  /**
   * Kích hoạt một học kỳ
   */
  activateSemester: async (semesterId: number): Promise<GlobalResponse<SemesterResponse>> => {
    return (await axiosClient.put<GlobalResponse<SemesterResponse>>(
      `/admin/semesters/${semesterId}/activate`
    )) as any;
  },

  /**
   * Hủy kích hoạt toàn bộ học kỳ
   */
  deactivateAllSemesters: async (): Promise<GlobalResponse<string>> => {
    return (await axiosClient.put<GlobalResponse<string>>(
      '/admin/semesters/deactive-all'
    )) as any;
  },

  /**
   * Chốt sổ học kỳ (finalize semester)
   */
  finalizeSemester: async (): Promise<GlobalResponse<string>> => {
    return (await axiosClient.post<GlobalResponse<string>>(
      '/admin/semesters/finalize-semester'
    )) as any;
  },

  /**
   * Xóa một học kỳ
   */
  deleteSemester: async (semesterId: number): Promise<GlobalResponse<string>> => {
    return (await axiosClient.delete<GlobalResponse<string>>(
      `/admin/semesters/${semesterId}`
    )) as any;
  },

  /**
   * Tạo môn học mới
   */
  createSubject: async (data: { code: string; name: string }): Promise<GlobalResponse<AdminSubjectResponse>> => {
    return (await axiosClient.post<GlobalResponse<AdminSubjectResponse>>(
      '/admin/subjects',
      data
    )) as any;
  },

  /**
   * Lấy danh sách tất cả môn học (danh mục dùng chung toàn trường)
   */
  getAllSubjects: async (): Promise<GlobalResponse<AdminSubjectResponse[]>> => {
    return (await axiosClient.get<GlobalResponse<AdminSubjectResponse[]>>(
      '/subjects'
    )) as any;
  },
};
