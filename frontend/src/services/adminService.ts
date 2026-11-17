import { axiosClient } from '@/api/axiosClient';
import type {
  AdminResetPasswordRequest,
  AdminSubjectResponse,
  BulkImportRequest,
  BulkImportResult,
  CreateSemesterRequest,
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
    return axiosClient.get<unknown, GlobalResponse<LecturerSummary[]>>(
      '/lecturer/course-classes/lecturers'
    );
  },

  /**
   * Import danh sách lớp và sinh viên đầu kỳ từ file Excel
   * Tự động tạo tài khoản SV mới (chưa xác thực, bắt buộc đổi mật khẩu tạm) và ghi danh vào lớp
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

    return axiosClient.post<unknown, GlobalResponse<ExcelImportClassResult>>(
      '/admin/course-classes/import-excel',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
  },


  /**
   * Tạo tài khoản tùy chỉnh hàng loạt hoặc đơn lẻ
   */
  bulkImportUsers: async (
    data: BulkImportRequest
  ): Promise<GlobalResponse<BulkImportResult>> => {
    return axiosClient.post<unknown, GlobalResponse<BulkImportResult>>(
      '/auth/admin/bulk-import',
      data
    );
  },

  /**
   * Reset mật khẩu sinh viên / giảng viên về mật khẩu mặc định (hoặc mật khẩu chỉ định)
   * Đồng thời xóa sạch refresh token để thu hồi phiên trên toàn bộ thiết bị
   */
  adminResetPassword: async (
    data: AdminResetPasswordRequest
  ): Promise<GlobalResponse<void>> => {
    return axiosClient.post<unknown, GlobalResponse<void>>(
      '/auth/admin/reset-password',
      data
    );
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
    return axiosClient.post<unknown, GlobalResponse<void>>(
      '/auth/admin/register',
      data
    );
  },

  /**
   * Xác thực mã OTP nội bộ Admin
   */
  adminVerifyOtp: async (data: {
    email: string;
    verifyCode: string;
    deviceId: string;
  }): Promise<GlobalResponse<void>> => {
    return axiosClient.post<unknown, GlobalResponse<void>>(
      '/auth/admin/verify',
      data
    );
  },

  /**
   * Tạo học kỳ mới
   */
  createSemester: async (data: CreateSemesterRequest): Promise<GlobalResponse<SemesterResponse>> => {
    return axiosClient.post<unknown, GlobalResponse<SemesterResponse>>(
      '/admin/semesters',
      data
    );
  },

  /**
   * Lấy danh sách tất cả học kỳ
   */
  getAllSemesters: async (): Promise<GlobalResponse<SemesterResponse[]>> => {
    return axiosClient.get<unknown, GlobalResponse<SemesterResponse[]>>(
      '/admin/semesters'
    );
  },

  updateSemesterStartDate: async (semesterId: number, startDate: string): Promise<GlobalResponse<SemesterResponse>> => {
    const response = await axiosClient.put<GlobalResponse<SemesterResponse>>(
      `/admin/semesters/${semesterId}/start-date`, { startDate },
    );
    return response as unknown as GlobalResponse<SemesterResponse>;
  },

  /**
   * Kích hoạt một học kỳ
   */
  activateSemester: async (semesterId: number): Promise<GlobalResponse<SemesterResponse>> => {
    return axiosClient.put<unknown, GlobalResponse<SemesterResponse>>(
      `/admin/semesters/${semesterId}/activate`
    );
  },

  /**
   * Hủy kích hoạt toàn bộ học kỳ
   */
  deactivateAllSemesters: async (): Promise<GlobalResponse<string>> => {
    return axiosClient.put<unknown, GlobalResponse<string>>(
      '/admin/semesters/deactive-all'
    );
  },

  /**
   * Chốt sổ học kỳ (finalize semester)
   */
  finalizeSemester: async (): Promise<GlobalResponse<string>> => {
    return axiosClient.post<unknown, GlobalResponse<string>>(
      '/admin/semesters/finalize-semester'
    );
  },

  /**
   * Xóa một học kỳ
   */
  deleteSemester: async (semesterId: number): Promise<GlobalResponse<string>> => {
    return axiosClient.delete<unknown, GlobalResponse<string>>(
      `/admin/semesters/${semesterId}`
    );
  },

  /**
   * Tạo môn học mới
   */
  createSubject: async (data: { code: string; name: string }): Promise<GlobalResponse<AdminSubjectResponse>> => {
    return axiosClient.post<unknown, GlobalResponse<AdminSubjectResponse>>(
      '/admin/subjects',
      data
    );
  },

  /**
   * Lấy danh sách tất cả môn học (danh mục dùng chung toàn trường)
   */
  getAllSubjects: async (): Promise<GlobalResponse<AdminSubjectResponse[]>> => {
    return axiosClient.get<unknown, GlobalResponse<AdminSubjectResponse[]>>(
      '/subjects'
    );
  },
};
