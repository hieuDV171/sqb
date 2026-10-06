import React, { useState, useEffect } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { adminService } from "@/services/adminService";
import { toast } from "@/stores/useToastStore";
import type {
  AdminSubjectResponse,
  BulkImportResult,
  ExcelImportClassResult,
  LecturerSummary,
  SemesterResponse,
  SingleUserImportDto,
  UserRole,
} from "@/types/auth.types";
import {
  FileSpreadsheet,
  Upload,
  UserPlus,
  KeyRound,
  CheckCircle2,
  AlertCircle,
  FileCheck,
  RefreshCw,
  ShieldCheck,
  Trash2,
  PlusCircle,
  Info,
  Calendar,
  BookOpen,
  Search,
  Award,
  Check,
  XCircle,
  X,
} from "lucide-react";

type ActiveTab = "excel" | "bulk" | "reset" | "semesters" | "subjects";

export function AdminUsersPage() {
  const [activeTab, setActiveTab] = useState<ActiveTab>("excel");

  // =========================================================================
  // TAB 1: EXCEL IMPORT STATE
  // =========================================================================
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [lecturerId, setLecturerId] = useState<string>("");
  const [lecturers, setLecturers] = useState<LecturerSummary[]>([]);
  const [isLoadingLecturers, setIsLoadingLecturers] = useState(false);
  const [isExcelUploading, setIsExcelUploading] = useState(false);
  const [excelResult, setExcelResult] = useState<ExcelImportClassResult | null>(null);
  const [excelError, setExcelError] = useState<string | null>(null);

  useEffect(() => {
    const fetchLecturers = async () => {
      try {
        setIsLoadingLecturers(true);
        const res = await adminService.getLecturers();
        setLecturers(res.data || []);
      } catch (err) {
        console.error("Lỗi khi tải danh sách giảng viên:", err);
      } finally {
        setIsLoadingLecturers(false);
      }
    };
    fetchLecturers();
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
      setExcelError(null);
      setExcelResult(null);
    }
  };

  const handleExcelImport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) {
      setExcelError("Vui lòng chọn file Excel danh sách lớp (.xlsx, .xls)");
      return;
    }

    setIsExcelUploading(true);
    setExcelError(null);
    setExcelResult(null);

    try {
      const parsedLecturerId = lecturerId ? parseInt(lecturerId, 10) : undefined;
      const res = await adminService.importExcelCourseClass(selectedFile, parsedLecturerId);
      setExcelResult(res.data);
      toast.success(
        `Import thành công lớp ${res.data.classCode}: Tạo mới ${res.data.newUsersCreated} SV, ghi danh ${res.data.newEnrollments} SV`,
        "Import Excel Thành Công"
      );
    } catch (err: any) {
      setExcelError(err?.message || "Import file Excel thất bại. Vui lòng kiểm tra định dạng file!");
    } finally {
      setIsExcelUploading(false);
    }
  };

  // =========================================================================
  // TAB 2: BULK USER CREATION (TÙY CHỈNH) STATE
  // =========================================================================
  const [usersToImport, setUsersToImport] = useState<SingleUserImportDto[]>([
    {
      email: "",
      fullName: "",
      password: "",
      studentLecturerCode: "",
      schoolFaculty: "Trường CNTT&TT",
      major: "Khoa học máy tính",
      className: "IT1-01",
      gender: "MALE",
      role: "STUDENT",
    },
  ]);
  const [isBulkLoading, setIsBulkLoading] = useState(false);
  const [bulkResult, setBulkResult] = useState<BulkImportResult | null>(null);
  const [bulkError, setBulkError] = useState<string | null>(null);

  const handleAddUserRow = () => {
    setUsersToImport((prev) => [
      ...prev,
      {
        email: "",
        fullName: "",
        password: "",
        studentLecturerCode: "",
        schoolFaculty: "Trường CNTT&TT",
        major: "",
        className: "",
        gender: "MALE",
        role: "STUDENT",
      },
    ]);
  };

  const handleRemoveUserRow = (index: number) => {
    if (usersToImport.length === 1) return;
    setUsersToImport((prev) => prev.filter((_, i) => i !== index));
  };

  const handleUserFieldChange = (index: number, field: keyof SingleUserImportDto, value: any) => {
    setUsersToImport((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const handleBulkSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBulkError(null);
    setBulkResult(null);

    // Validation sơ bộ
    for (let i = 0; i < usersToImport.length; i++) {
      const u = usersToImport[i];
      if (!u.email || !u.fullName) {
        setBulkError(`Dòng #${i + 1}: Email và Họ tên là bắt buộc!`);
        return;
      }
    }

    setIsBulkLoading(true);
    try {
      const res = await adminService.bulkImportUsers({ users: usersToImport });
      setBulkResult(res.data);
      if (res.data.totalSuccess > 0) {
        toast.success(
          `Đã tạo thành công ${res.data.totalSuccess} tài khoản!`,
          "Tạo tài khoản thành công"
        );
      }
      if (res.data.totalFailed > 0) {
        toast.warning(
          `Có ${res.data.totalFailed} tài khoản bị lỗi. Vui lòng xem danh sách chi tiết.`,
          "Một số dòng bị lỗi"
        );
      }
    } catch (err: any) {
      setBulkError(err?.message || "Tạo tài khoản thất bại!");
    } finally {
      setIsBulkLoading(false);
    }
  };

  // =========================================================================
  // TAB 3: RESET PASSWORD STATE
  // =========================================================================
  const [resetEmailInput, setResetEmailInput] = useState("");
  const [customNewPassword, setCustomNewPassword] = useState("");
  const [isResetLoading, setIsResetLoading] = useState(false);
  const [resetLogs, setResetLogs] = useState<string[]>([]);
  const [resetError, setResetError] = useState<string | null>(null);

  const handleAdminResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setResetError(null);

    // Hỗ trợ nhập 1 email hoặc nhiều email ngăn cách bởi dấu phẩy/xuống dòng
    const rawEmails = resetEmailInput
      .split(/[\n,;]+/)
      .map((em) => em.trim())
      .filter((em) => em.length > 0);

    if (rawEmails.length === 0) {
      setResetError("Vui lòng nhập ít nhất 1 email tài khoản cần reset!");
      return;
    }

    setIsResetLoading(true);
    const newLogs: string[] = [];

    for (const email of rawEmails) {
      try {
        await adminService.adminResetPassword({
          email,
          newPassword: customNewPassword ? customNewPassword.trim() : null,
        });
        newLogs.push(`✅ [${email}]: Đã reset về mật khẩu mặc định & thu hồi các phiên đăng nhập.`);
      } catch (err: any) {
        newLogs.push(`❌ [${email}]: Lỗi - ${err?.message || "Tài khoản không tồn tại"}`);
      }
    }

    setResetLogs(newLogs);
    setIsResetLoading(false);
    toast.info("Đã hoàn tất quy trình xử lý danh sách reset mật khẩu.", "Kết quả Reset");
  };

  // =========================================================================
  // TAB 4 & 5: CONFIRM MODAL STATE & ACTIONS
  // =========================================================================
  interface ConfirmModalState {
    isOpen: boolean;
    title: string;
    message: string;
    confirmText?: string;
    cancelText?: string;
    variant?: 'danger' | 'primary' | 'warning' | 'purple';
    onConfirm: () => void | Promise<void>;
  }

  const [confirmModal, setConfirmModal] = useState<ConfirmModalState>({
    isOpen: false,
    title: "",
    message: "",
    onConfirm: () => {},
  });

  const openConfirm = (opts: Omit<ConfirmModalState, 'isOpen'>) => {
    setConfirmModal({
      isOpen: true,
      ...opts,
    });
  };

  const closeConfirm = () => {
    setConfirmModal((prev) => ({ ...prev, isOpen: false }));
  };

  // =========================================================================
  // TAB 4: QUẢN LÝ HỌC KỲ (SEMESTERS) STATE & HANDLERS
  // =========================================================================
  const [semesters, setSemesters] = useState<SemesterResponse[]>([]);
  const [isLoadingSemesters, setIsLoadingSemesters] = useState(false);
  const [newSemesterName, setNewSemesterName] = useState("");
  const [isCreatingSemester, setIsCreatingSemester] = useState(false);
  const [activatingSemesterId, setActivatingSemesterId] = useState<number | null>(null);
  const [deletingSemesterId, setDeletingSemesterId] = useState<number | null>(null);
  const [isFinalizingSemester, setIsFinalizingSemester] = useState(false);

  const activeSemester = semesters.find((s) => s.active);

  const fetchSemesters = async () => {
    try {
      setIsLoadingSemesters(true);
      const res = await adminService.getAllSemesters();
      setSemesters(res.data || []);
    } catch (err: any) {
      console.error("Lỗi khi tải danh sách học kỳ:", err);
    } finally {
      setIsLoadingSemesters(false);
    }
  };

  const handleCreateSemester = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSemesterName.trim()) {
      toast.warning("Vui lòng nhập tên học kỳ!", "Dữ liệu thiếu");
      return;
    }
    try {
      setIsCreatingSemester(true);
      const res = await adminService.createSemester({ name: newSemesterName.trim() });
      toast.success(`Đã tạo học kỳ "${res.data.name}" thành công.`);
      setNewSemesterName("");
      fetchSemesters();
    } catch (err: any) {
      console.error("Lỗi khi tạo học kỳ:", err);
    } finally {
      setIsCreatingSemester(false);
    }
  };

  const handleActivateSemester = (sem: SemesterResponse) => {
    if (activeSemester) {
      toast.warning(
        `Không thể kích hoạt! Học kỳ "${activeSemester.name}" đang hoạt động. Bạn cần chốt sổ và đóng học kỳ này trước khi mở kỳ mới.`,
        "Cần hoàn tất kỳ trước"
      );
      return;
    }
    if (sem.isFinalize) {
      toast.warning(`Học kỳ "${sem.name}" đã được chốt sổ kết thúc, không thể kích hoạt lại.`, "Học kỳ đã đóng");
      return;
    }

    openConfirm({
      title: `Kích hoạt học kỳ "${sem.name}"`,
      message: `Bạn có chắc chắn muốn kích hoạt học kỳ "${sem.name}" làm học kỳ hoạt động chính thức? Toàn bộ các lớp học và dữ liệu sinh viên import sắp tới sẽ ghi nhận vào kỳ này.`,
      confirmText: "Kích hoạt ngay",
      variant: "primary",
      onConfirm: async () => {
        try {
          setActivatingSemesterId(sem.semesterId);
          await adminService.activateSemester(sem.semesterId);
          toast.success(`Đã kích hoạt học kỳ "${sem.name}" thành công!`);
          fetchSemesters();
        } catch (err) {
          console.error("Lỗi kích hoạt học kỳ:", err);
        } finally {
          setActivatingSemesterId(null);
        }
      },
    });
  };

  const handleDeactivateAllSemesters = () => {
    if (!activeSemester) {
      toast.warning("Hiện không có học kỳ nào đang được kích hoạt để hủy kích hoạt.", "Không có kỳ hoạt động");
      return;
    }
    if (!activeSemester.isFinalize) {
      toast.warning(
        `Học kỳ "${activeSemester.name}" đang hoạt động nhưng CHƯA ĐƯỢC CHỐT SỔ. Bạn bắt buộc phải thực hiện Chốt Sổ Học Kỳ trước khi đóng!`,
        "Yêu cầu chốt sổ trước"
      );
      return;
    }

    openConfirm({
      title: `Đóng và Hủy kích hoạt học kỳ "${activeSemester.name}"`,
      message: `Học kỳ "${activeSemester.name}" đã được chốt sổ. Bạn có chắc chắn muốn hủy kích hoạt kỳ này? Hệ thống sẽ chuyển về trạng thái không có học kỳ active cho tới khi bạn kích hoạt kỳ mới.`,
      confirmText: "Đóng học kỳ",
      variant: "warning",
      onConfirm: async () => {
        try {
          await adminService.deactivateAllSemesters();
          toast.success("Đã hủy kích hoạt toàn bộ học kỳ thành công.");
          fetchSemesters();
        } catch (err) {
          console.error("Lỗi hủy kích hoạt học kỳ:", err);
        }
      },
    });
  };

  const handleFinalizeSemester = () => {
    if (!activeSemester) {
      toast.warning("Hiện không có học kỳ nào đang được kích hoạt để thực hiện chốt sổ!", "Chưa kích hoạt học kỳ");
      return;
    }
    if (activeSemester.isFinalize) {
      toast.info(`Học kỳ "${activeSemester.name}" đang kích hoạt đã được chốt sổ trước đó.`, "Đã chốt sổ rồi");
      return;
    }

    openConfirm({
      title: `Xác nhận Chốt Sổ Học Kỳ "${activeSemester.name}"`,
      message: `Hệ thống sẽ thực hiện tính toán điểm Gamification, trao thưởng Top 3 môn học, lưu snapshot bảng xếp hạng và chuyển giao câu hỏi cho kỳ sau. Thao tác này không thể hoàn tác. Bạn có chắc chắn muốn tiếp tục?`,
      confirmText: "Đồng ý Chốt Sổ",
      variant: "purple",
      onConfirm: async () => {
        try {
          setIsFinalizingSemester(true);
          await adminService.finalizeSemester();
          toast.success(`Đã chốt sổ học kỳ "${activeSemester.name}" thành công!`);
          fetchSemesters();
        } catch (err) {
          console.error("Lỗi chốt sổ học kỳ:", err);
        } finally {
          setIsFinalizingSemester(false);
        }
      },
    });
  };

  const handleDeleteSemester = (sem: SemesterResponse) => {
    if (sem.active) {
      toast.warning("Không thể xóa học kỳ đang hoạt động! Vui lòng đóng hoặc chuyển học kỳ trước.", "Thao tác không được phép");
      return;
    }

    openConfirm({
      title: `Xác nhận xóa học kỳ "${sem.name}"`,
      message: `Bạn có chắc chắn muốn xóa vĩnh viễn học kỳ "${sem.name}" khỏi hệ thống? Thao tác này chỉ thành công nếu học kỳ chưa có lớp học phần trực thuộc.`,
      confirmText: "Xóa học kỳ",
      variant: "danger",
      onConfirm: async () => {
        try {
          setDeletingSemesterId(sem.semesterId);
          await adminService.deleteSemester(sem.semesterId);
          toast.success(`Đã xóa học kỳ "${sem.name}" thành công.`);
          fetchSemesters();
        } catch (err) {
          console.error("Lỗi xóa học kỳ:", err);
        } finally {
          setDeletingSemesterId(null);
        }
      },
    });
  };

  // =========================================================================
  // TAB 5: QUẢN LÝ MÔN HỌC (SUBJECTS) STATE & HANDLERS
  // =========================================================================
  const [subjects, setSubjects] = useState<AdminSubjectResponse[]>([]);
  const [isLoadingSubjects, setIsLoadingSubjects] = useState(false);
  const [newSubjectCode, setNewSubjectCode] = useState("");
  const [newSubjectName, setNewSubjectName] = useState("");
  const [isCreatingSubject, setIsCreatingSubject] = useState(false);
  const [subjectSearch, setSubjectSearch] = useState("");

  const fetchSubjects = async () => {
    try {
      setIsLoadingSubjects(true);
      const res = await adminService.getAllSubjects();
      setSubjects(res.data || []);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Không thể tải danh sách môn học");
    } finally {
      setIsLoadingSubjects(false);
    }
  };

  const handleCreateSubject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSubjectCode.trim() || !newSubjectName.trim()) {
      toast.error("Vui lòng nhập đầy đủ mã và tên môn học");
      return;
    }
    try {
      setIsCreatingSubject(true);
      const res = await adminService.createSubject({
        code: newSubjectCode.trim().toUpperCase(),
        name: newSubjectName.trim(),
      });
      toast.success(`Đã thêm môn học ${res.data.code} - ${res.data.name} thành công`);
      setNewSubjectCode("");
      setNewSubjectName("");
      fetchSubjects();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Lỗi khi tạo môn học");
    } finally {
      setIsCreatingSubject(false);
    }
  };

  useEffect(() => {
    if (activeTab === "semesters") {
      fetchSemesters();
    } else if (activeTab === "subjects") {
      fetchSubjects();
    }
  }, [activeTab]);

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-10">
      {/* Top Banner Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-3xl bg-linear-to-r from-indigo-900 to-slate-900 text-white shadow-xl relative overflow-hidden">
        <div className="space-y-1.5 z-10">
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold tracking-tight">
              Trung Tâm Quản Trị Tài Khoản & Người Dùng
            </h1>
            <Badge variant="role" className="bg-indigo-500/20 text-indigo-300 border-indigo-400/30">
              Admin Workspace
            </Badge>
          </div>
          <p className="text-sm text-slate-300 max-w-2xl leading-relaxed">
            Khởi tạo sinh viên đầu kỳ từ file Excel của Giảng viên, tạo tài khoản tùy chỉnh riêng biệt, và cấp lại mật khẩu mặc định.
          </p>
        </div>
        <div className="hidden lg:flex items-center gap-3 z-10">
          <div className="p-3 bg-white/10 rounded-2xl backdrop-blur-md flex items-center gap-3">
            <ShieldCheck className="w-8 h-8 text-emerald-400" />
            <div className="text-left">
              <p className="text-xs font-semibold text-slate-300">Bảo mật hệ thống</p>
              <p className="text-xs text-emerald-300 font-medium">Toàn quyền Quản trị</p>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex items-center gap-2 p-1.5 bg-slate-100 dark:bg-slate-900/80 rounded-2xl border border-slate-200 dark:border-slate-800">
        <button
          type="button"
          onClick={() => setActiveTab("excel")}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer ${
            activeTab === "excel"
              ? "bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs"
              : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200"
          }`}
        >
          <FileSpreadsheet className="w-4 h-4" />
          <span>Import Excel Đầu Kỳ</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab("bulk")}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer ${
            activeTab === "bulk"
              ? "bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs"
              : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200"
          }`}
        >
          <UserPlus className="w-4 h-4" />
          <span>Tạo Tùy Chỉnh (Bulk)</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab("reset")}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer ${
            activeTab === "reset"
              ? "bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs"
              : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200"
          }`}
        >
          <KeyRound className="w-4 h-4" />
          <span>Reset Mật Khẩu</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab("semesters")}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer ${
            activeTab === "semesters"
              ? "bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs"
              : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200"
          }`}
        >
          <Calendar className="w-4 h-4" />
          <span>Quản Lý Học Kỳ</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab("subjects")}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer ${
            activeTab === "subjects"
              ? "bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs"
              : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200"
          }`}
        >
          <BookOpen className="w-4 h-4" />
          <span>Quản Lý Môn Học</span>
        </button>
      </div>

      {/* ========================================================================= */}
      {/* TAB 1: IMPORT SINH VIÊN QUA FILE EXCEL                                    */}
      {/* ========================================================================= */}
      {activeTab === "excel" && (
        <Card className="rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <CardHeader>
            <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400">
              <FileSpreadsheet className="w-5 h-5" />
              <CardTitle className="text-lg">Nhập Danh Sách Sinh Viên Lớp Học Phần (Excel)</CardTitle>
            </div>
            <CardDescription>
              Vào đầu kỳ học, Giảng viên gửi danh sách sinh viên theo lớp. Tải file Excel lên để hệ thống tự động tạo tài khoản Sinh viên mới (với mật khẩu mặc định, verified = true) và ghi danh vào lớp học phần.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {excelError && (
              <div className="flex items-center gap-2.5 p-3.5 rounded-xl bg-red-50 dark:bg-red-950/40 text-sm text-red-600 dark:text-red-400 border border-red-200 dark:border-red-900/50">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{excelError}</span>
              </div>
            )}

            <form onSubmit={handleExcelImport} className="space-y-5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label htmlFor="excelFile" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                    File Excel danh sách lớp (.xlsx, .xls) *
                  </Label>
                  <Input
                    id="excelFile"
                    type="file"
                    accept=".xlsx,.xls"
                    onChange={handleFileChange}
                    required
                    className="cursor-pointer"
                  />
                  {selectedFile && (
                    <p className="text-xs text-emerald-600 dark:text-emerald-400 flex items-center gap-1 mt-1">
                      <FileCheck className="w-3.5 h-3.5" /> Đã chọn: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
                    </p>
                  )}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="lecturerId" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                    Giảng viên phụ trách lớp (Tùy chọn)
                  </Label>
                  <div className="relative">
                    <select
                      id="lecturerId"
                      value={lecturerId}
                      onChange={(e) => setLecturerId(e.target.value)}
                      disabled={isLoadingLecturers}
                      className="w-full h-10 px-3 py-2 text-xs bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 text-slate-900 dark:text-slate-100 cursor-pointer disabled:opacity-50"
                    >
                      <option value="">
                        {isLoadingLecturers
                          ? "-- Đang tải danh sách giảng viên... --"
                          : "-- Bỏ trống (Hệ thống tự tìm hoặc không chỉ định) --"}
                      </option>
                      {lecturers.map((lec) => {
                        const extraDetails = [
                          lec.studentLecturerCode ? `Mã CB: ${lec.studentLecturerCode}` : null,
                          lec.schoolFaculty ? lec.schoolFaculty : null,
                        ].filter(Boolean).join(" - ");

                        return (
                          <option key={lec.id} value={lec.id.toString()}>
                            {lec.fullName} (ID: {lec.id}){extraDetails ? ` | ${extraDetails}` : ""}
                          </option>
                        );
                      })}
                    </select>
                  </div>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400">
                    Chọn giảng viên từ hệ thống để phân công lớp hoặc để trống nếu tự động nhận diện từ file Excel.
                  </p>
                </div>
              </div>

              {/* Excel Guidelines Card */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-900/50 border border-slate-200/80 dark:border-slate-800 text-xs text-slate-600 dark:text-slate-400 space-y-1.5 leading-relaxed">
                <div className="flex items-center gap-1.5 font-semibold text-indigo-600 dark:text-indigo-400">
                  <Info className="w-4 h-4" /> Định dạng file Excel hợp lệ:
                </div>
                <p>• Hỗ trợ cả <strong>file mẫu chuẩn</strong> và <strong>file rút gọn</strong> (Mã học phần, Tên học phần, MSSV, Họ và tên SV).</p>
                <p>• Nếu file không có cột <em>Mã lớp</em>, hệ thống sẽ <strong>tự động trích xuất mã lớp từ tên file</strong> (VD: <code>171146-IT4409.xlsx</code> ➔ Mã lớp <code>171146</code>).</p>
                <p>• Email và mật khẩu sẽ tự động được sinh theo chuẩn HUST (<code>*@sis.hust.edu.vn</code>) nếu file không chứa cột Email / Ngày sinh.</p>
                <p>• Môn học (VD: <code>IT4409</code>) và Học kỳ hiện tại phải được tạo và kích hoạt sẵn trong hệ thống trước khi import.</p>
              </div>

              <div className="flex justify-end pt-2">
                <Button
                  type="submit"
                  disabled={isExcelUploading || !selectedFile}
                  className="gap-2 px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-semibold shadow-md shadow-indigo-200 dark:shadow-none"
                >
                  {isExcelUploading ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      Đang xử lý & khởi tạo tài khoản...
                    </>
                  ) : (
                    <>
                      <Upload className="w-4 h-4" />
                      Tiến hành Import & Khởi Tạo
                    </>
                  )}
                </Button>
              </div>
            </form>

            {/* Result Stats Display */}
            {excelResult && (
              <div className="mt-6 p-5 rounded-2xl bg-indigo-50/50 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/40 space-y-4 animate-in fade-in duration-200">
                <div className="flex items-center justify-between border-b border-indigo-100 dark:border-indigo-900/40 pb-3">
                  <div className="flex items-center gap-2 text-indigo-700 dark:text-indigo-300 font-bold">
                    <CheckCircle2 className="w-5 h-5 text-emerald-500" />
                    <span>Kết Quả Import Lớp Học Phần: {excelResult.classCode}</span>
                  </div>
                  <Badge variant="role">
                    {excelResult.subjectCode} - {excelResult.subjectName}
                  </Badge>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="p-3.5 bg-white dark:bg-slate-900 rounded-xl border border-slate-200/80 dark:border-slate-800 text-center">
                    <p className="text-xs text-slate-500 font-medium">Tài khoản mới tạo</p>
                    <p className="text-xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
                      {excelResult.newUsersCreated}
                    </p>
                  </div>
                  <div className="p-3.5 bg-white dark:bg-slate-900 rounded-xl border border-slate-200/80 dark:border-slate-800 text-center">
                    <p className="text-xs text-slate-500 font-medium">Đã có tài khoản</p>
                    <p className="text-xl font-bold text-indigo-600 dark:text-indigo-400 mt-1">
                      {excelResult.existingUsersFound}
                    </p>
                  </div>
                  <div className="p-3.5 bg-white dark:bg-slate-900 rounded-xl border border-slate-200/80 dark:border-slate-800 text-center">
                    <p className="text-xs text-slate-500 font-medium">Ghi danh mới vào lớp</p>
                    <p className="text-xl font-bold text-amber-600 dark:text-amber-400 mt-1">
                      {excelResult.newEnrollments}
                    </p>
                  </div>
                  <div className="p-3.5 bg-white dark:bg-slate-900 rounded-xl border border-slate-200/80 dark:border-slate-800 text-center">
                    <p className="text-xs text-slate-500 font-medium">Tổng số dòng đọc</p>
                    <p className="text-xl font-bold text-slate-700 dark:text-slate-200 mt-1">
                      {excelResult.totalRowsInFile}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: TẠO TÙY CHỈNH TỪNG TÀI KHOẢN (BULK-IMPORT)                        */}
      {/* ========================================================================= */}
      {activeTab === "bulk" && (
        <Card className="rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <CardHeader>
            <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400">
              <UserPlus className="w-5 h-5" />
              <CardTitle className="text-lg">Tạo Tài Khoản Tùy Chỉnh Riêng Biệt (Bulk Import)</CardTitle>
            </div>
            <CardDescription>
              Sử dụng khi Quản trị viên cần tạo từng tài khoản hoặc danh sách cá nhân hóa với thông tin cụ thể (MSSV, Họ tên, Khoa viện, Ngành, Lớp, Giới tính, Vai trò...) thay vì import qua file Excel.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {bulkError && (
              <div className="flex items-center gap-2.5 p-3.5 rounded-xl bg-red-50 dark:bg-red-950/40 text-sm text-red-600 dark:text-red-400 border border-red-200 dark:border-red-900/50">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{bulkError}</span>
              </div>
            )}

            <form onSubmit={handleBulkSubmit} className="space-y-6">
              <div className="space-y-4">
                {usersToImport.map((user, idx) => (
                  <div
                    key={idx}
                    className="p-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/40 relative space-y-3"
                  >
                    <div className="flex items-center justify-between pb-2 border-b border-slate-200/60 dark:border-slate-800/60">
                      <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                        Tài khoản #{idx + 1}
                      </span>
                      {usersToImport.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleRemoveUserRow(idx)}
                          className="text-xs text-rose-500 hover:text-rose-700 flex items-center gap-1 cursor-pointer font-medium"
                        >
                          <Trash2 className="w-3.5 h-3.5" /> Xóa dòng này
                        </button>
                      )}
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                      <div className="space-y-1">
                        <Label className="text-xs">Email tài khoản *</Label>
                        <Input
                          type="email"
                          placeholder="sv.k66@hust.edu.vn"
                          value={user.email}
                          onChange={(e) => handleUserFieldChange(idx, "email", e.target.value)}
                          required
                          className="text-xs"
                        />
                      </div>

                      <div className="space-y-1">
                        <Label className="text-xs">Họ và tên *</Label>
                        <Input
                          placeholder="Nguyễn Văn An"
                          value={user.fullName}
                          onChange={(e) => handleUserFieldChange(idx, "fullName", e.target.value)}
                          required
                          className="text-xs"
                        />
                      </div>

                      <div className="space-y-1">
                        <Label className="text-xs">Vai trò</Label>
                        <select
                          value={user.role}
                          onChange={(e) => handleUserFieldChange(idx, "role", e.target.value as UserRole)}
                          className="w-full h-8 px-2 text-xs rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                        >
                          <option value="STUDENT">🎓 Sinh viên (STUDENT)</option>
                          <option value="LECTURER">👨‍🏫 Giảng viên (LECTURER)</option>
                          <option value="ADMIN">🛡️ Quản trị viên (ADMIN)</option>
                        </select>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-1">
                      <div className="space-y-1">
                        <Label className="text-xs">MSSV / MSGV</Label>
                        <Input
                          placeholder="20211234"
                          value={user.studentLecturerCode || ""}
                          onChange={(e) => handleUserFieldChange(idx, "studentLecturerCode", e.target.value)}
                          className="text-xs"
                        />
                      </div>

                      <div className="space-y-1">
                        <Label className="text-xs">Khoa / Viện</Label>
                        <Input
                          placeholder="Trường CNTT&TT"
                          value={user.schoolFaculty || ""}
                          onChange={(e) => handleUserFieldChange(idx, "schoolFaculty", e.target.value)}
                          className="text-xs"
                        />
                      </div>

                      <div className="space-y-1">
                        <Label className="text-xs">Lớp sinh hoạt</Label>
                        <Input
                          placeholder="IT1-02"
                          value={user.className || ""}
                          onChange={(e) => handleUserFieldChange(idx, "className", e.target.value)}
                          className="text-xs"
                        />
                      </div>

                      <div className="space-y-1">
                        <Label className="text-xs">Mật khẩu (Trống = Mặc định)</Label>
                        <Input
                          type="password"
                          placeholder="Mặc định nếu để trống"
                          value={user.password || ""}
                          onChange={(e) => handleUserFieldChange(idx, "password", e.target.value)}
                          className="text-xs"
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleAddUserRow}
                  className="gap-2 text-xs w-full sm:w-auto"
                >
                  <PlusCircle className="w-4 h-4" /> Thêm một dòng tài khoản
                </Button>

                <Button
                  type="submit"
                  disabled={isBulkLoading}
                  className="gap-2 px-6 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-semibold text-xs sm:text-sm w-full sm:w-auto"
                >
                  {isBulkLoading ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" /> Đang tạo tài khoản...
                    </>
                  ) : (
                    <>
                      <UserPlus className="w-4 h-4" /> Xác nhận tạo {usersToImport.length} tài khoản
                    </>
                  )}
                </Button>
              </div>
            </form>

            {/* Bulk Result Details */}
            {bulkResult && (
              <div className="mt-6 p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/40 space-y-3">
                <div className="flex items-center gap-4 text-xs font-semibold">
                  <span className="text-emerald-600 dark:text-emerald-400">
                    ✅ Thành công: {bulkResult.totalSuccess}
                  </span>
                  <span className="text-rose-600 dark:text-rose-400">
                    ❌ Thất bại: {bulkResult.totalFailed}
                  </span>
                </div>
                {bulkResult.errors.length > 0 && (
                  <div className="p-3 bg-red-50 dark:bg-red-950/30 rounded-lg text-xs text-red-600 space-y-1">
                    <p className="font-bold">Chi tiết lỗi:</p>
                    {bulkResult.errors.map((err, i) => (
                      <p key={i}>• {err}</p>
                    ))}
                  </div>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* ========================================================================= */}
      {/* TAB 3: RESET MẬT KHẨU VỀ MẶC ĐỊNH CHO SINH VIÊN / GIẢNG VIÊN              */}
      {/* ========================================================================= */}
      {activeTab === "reset" && (
        <Card className="rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <CardHeader>
            <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400">
              <KeyRound className="w-5 h-5" />
              <CardTitle className="text-lg">Cấp Lại Mật Khẩu Mặc Định (Reset Password)</CardTitle>
            </div>
            <CardDescription>
              Khi Giảng viên tổng hợp danh sách Sinh viên quên mật khẩu gửi cho Quản trị viên, nhập danh sách email vào đây để khôi phục mật khẩu về mặc định của hệ thống.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {resetError && (
              <div className="flex items-center gap-2.5 p-3.5 rounded-xl bg-red-50 dark:bg-red-950/40 text-sm text-red-600 dark:text-red-400 border border-red-200 dark:border-red-900/50">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{resetError}</span>
              </div>
            )}

            <form onSubmit={handleAdminResetPassword} className="space-y-4">
              <div className="space-y-1.5">
                <Label htmlFor="resetEmails" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                  Danh sách Email cần reset (Nhập 1 hoặc nhiều email, ngăn cách bởi dấu phẩy hoặc xuống dòng) *
                </Label>
                <textarea
                  id="resetEmails"
                  rows={4}
                  placeholder={`sv1.k66@hust.edu.vn\nsv2.k66@hust.edu.vn, sv3.k66@hust.edu.vn`}
                  value={resetEmailInput}
                  onChange={(e) => setResetEmailInput(e.target.value)}
                  required
                  className="w-full p-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs sm:text-sm font-mono text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="customPass" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                  Mật khẩu mới chỉ định (Bỏ trống nếu muốn lấy mật khẩu mặc định của trường)
                </Label>
                <Input
                  id="customPass"
                  type="password"
                  placeholder="Để trống để reset về mật khẩu mặc định hệ thống"
                  value={customNewPassword}
                  onChange={(e) => setCustomNewPassword(e.target.value)}
                  className="text-xs sm:text-sm"
                />
              </div>

              <div className="p-3.5 rounded-xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-900/50 text-xs text-amber-800 dark:text-amber-300 space-y-1 leading-relaxed">
                <p className="font-semibold">⚠️ Cơ chế xử lý an toàn của Backend:</p>
                <p>• Khi reset mật khẩu, Backend sẽ tự động xóa sạch toàn bộ Refresh Token trong Redis (`rt:email:*`), buộc tài khoản phải đăng xuất khỏi tất cả các thiết bị đang đăng nhập.</p>
                <p>• Quản trị viên chỉ cần gửi lại mật khẩu mặc định cho Giảng viên để thông báo cho Sinh viên.</p>
              </div>

              <div className="flex justify-end pt-2">
                <Button
                  type="submit"
                  disabled={isResetLoading}
                  className="gap-2 px-6 py-2.5 bg-rose-600 hover:bg-rose-700 text-white rounded-xl font-semibold shadow-md shadow-rose-200 dark:shadow-none"
                >
                  {isResetLoading ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" /> Đang reset...
                    </>
                  ) : (
                    <>
                      <KeyRound className="w-4 h-4" /> Thực Hiện Reset Mật Khẩu
                    </>
                  )}
                </Button>
              </div>
            </form>

            {/* Execution Logs */}
            {resetLogs.length > 0 && (
              <div className="mt-4 p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-900 text-slate-100 font-mono text-xs space-y-1 max-h-48 overflow-y-auto">
                <p className="text-slate-400 font-semibold mb-2">Nhật ký xử lý reset mật khẩu:</p>
                {resetLogs.map((log, i) => (
                  <p key={i} className="leading-relaxed">{log}</p>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* ========================================================================= */}
      {/* TAB 4: QUẢN LÝ HỌC KỲ                                                    */}
      {/* ========================================================================= */}
      {activeTab === "semesters" && (
        <div className="space-y-6">
          {/* Create Semester Card */}
          <Card className="rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
            <CardHeader>
              <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400">
                <Calendar className="w-5 h-5" />
                <CardTitle className="text-lg">Tạo Học Kỳ Mới</CardTitle>
              </div>
              <CardDescription>
                Thêm học kỳ mới vào hệ thống (ví dụ: <code>2024.1</code>, <code>2024.2</code>). Sau khi tạo, bạn có thể kích hoạt học kỳ để làm việc chính thức.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateSemester} className="flex flex-col sm:flex-row items-stretch sm:items-end gap-3">
                <div className="flex-1 space-y-1.5">
                  <Label htmlFor="semesterName" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                    Tên học kỳ *
                  </Label>
                  <Input
                    id="semesterName"
                    placeholder="VD: 2024.1, 2024.2, 2024.3..."
                    value={newSemesterName}
                    onChange={(e) => setNewSemesterName(e.target.value)}
                    required
                    className="rounded-xl text-sm"
                  />
                </div>
                <Button
                  type="submit"
                  disabled={isCreatingSemester || !newSemesterName.trim()}
                  className="gap-2 px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-semibold shadow-md shadow-indigo-200 dark:shadow-none shrink-0"
                >
                  {isCreatingSemester ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" /> Đang tạo...
                    </>
                  ) : (
                    <>
                      <PlusCircle className="w-4 h-4" /> Thêm Học Kỳ
                    </>
                  )}
                </Button>
              </form>
            </CardContent>
          </Card>

          {/* Status Overview Banner */}
          <div className="p-4 rounded-2xl border bg-slate-50 dark:bg-slate-900/50 border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
            <div className="flex items-center gap-2.5">
              <div className={`w-3 h-3 rounded-full shrink-0 ${activeSemester ? (activeSemester.isFinalize ? 'bg-purple-500 animate-pulse' : 'bg-emerald-500 animate-pulse') : 'bg-slate-400'}`} />
              <div>
                <div className="font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                  {activeSemester ? (
                    <>
                      <span>Học kỳ đang hoạt động:</span>
                      <span className="font-bold text-indigo-600 dark:text-indigo-400 text-sm">{activeSemester.name}</span>
                      <span className="text-slate-500">
                        ({activeSemester.isFinalize ? "Đã chốt sổ - Sẵn sàng để đóng kỳ" : "Đang mở nhận đề xuất & làm bài"})
                      </span>
                    </>
                  ) : (
                    <span className="text-slate-500 dark:text-slate-400">
                      Hiện tại không có học kỳ nào đang được kích hoạt. Hãy kích hoạt một học kỳ để hệ thống hoạt động.
                    </span>
                  )}
                </div>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                  Quy trình chuẩn: Kích hoạt kỳ ➔ Thu thập câu hỏi & Làm bài ➔ Chốt sổ học kỳ ➔ Đóng kỳ ➔ Kích hoạt kỳ kế tiếp.
                </p>
              </div>
            </div>
            {activeSemester && (
              <Badge variant={activeSemester.isFinalize ? "gold" : "success"} className="shrink-0 self-start sm:self-auto">
                {activeSemester.isFinalize ? "Kỳ hiện tại đã chốt sổ" : "Kỳ hiện tại đang mở"}
              </Badge>
            )}
          </div>

          {/* Semesters List Card */}
          <Card className="rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
            <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3">
              <div>
                <CardTitle className="text-lg">Danh Sách Học Kỳ Trong Hệ Thống</CardTitle>
                <CardDescription>
                  Hệ thống chỉ cho phép duy nhất một học kỳ hoạt động (Active) tại một thời điểm.
                </CardDescription>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={!activeSemester || !activeSemester.isFinalize}
                  onClick={handleDeactivateAllSemesters}
                  title={
                    !activeSemester
                      ? "Hiện không có học kỳ nào đang hoạt động"
                      : !activeSemester.isFinalize
                      ? `Cần chốt sổ học kỳ "${activeSemester.name}" trước khi đóng`
                      : "Đóng học kỳ hiện tại"
                  }
                  className="text-xs text-amber-600 dark:text-amber-400 border-amber-200 dark:border-amber-800 hover:bg-amber-50 dark:hover:bg-amber-950/40 rounded-xl cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  <XCircle className="w-3.5 h-3.5 mr-1" /> Đóng kỳ hiện tại
                </Button>
                <Button
                  type="button"
                  size="sm"
                  disabled={!activeSemester || activeSemester.isFinalize || isFinalizingSemester}
                  onClick={handleFinalizeSemester}
                  title={
                    !activeSemester
                      ? "Hiện không có học kỳ nào đang hoạt động để chốt sổ"
                      : activeSemester.isFinalize
                      ? "Học kỳ hiện tại đã được chốt sổ rồi"
                      : "Chốt sổ điểm và xếp hạng học kỳ này"
                  }
                  className="text-xs bg-purple-600 hover:bg-purple-700 text-white rounded-xl shadow-xs cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  {isFinalizingSemester ? (
                    <RefreshCw className="w-3.5 h-3.5 mr-1 animate-spin" />
                  ) : (
                    <Award className="w-3.5 h-3.5 mr-1" />
                  )}
                  Chốt sổ học kỳ
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {isLoadingSemesters ? (
                <div className="flex items-center justify-center py-10 text-slate-500 gap-2">
                  <RefreshCw className="w-5 h-5 animate-spin text-indigo-500" />
                  <span className="text-sm">Đang tải danh sách học kỳ...</span>
                </div>
              ) : semesters.length === 0 ? (
                <div className="text-center py-10 text-slate-400 text-sm">
                  Chưa có học kỳ nào được tạo trong hệ thống.
                </div>
              ) : (
                <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-800">
                  <table className="w-full text-left text-xs sm:text-sm">
                    <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-500 uppercase tracking-wider font-semibold text-[11px] border-b border-slate-200 dark:border-slate-800">
                      <tr>
                        <th className="py-3 px-4">ID</th>
                        <th className="py-3 px-4">Tên học kỳ</th>
                        <th className="py-3 px-4">Trạng thái</th>
                        <th className="py-3 px-4">Chốt sổ</th>
                        <th className="py-3 px-4 text-right">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800 font-medium">
                      {semesters.map((s) => (
                        <tr key={s.semesterId} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition-colors">
                          <td className="py-3 px-4 font-mono text-slate-400">{s.semesterId}</td>
                          <td className="py-3 px-4 font-bold text-slate-900 dark:text-slate-100">
                            {s.name}
                          </td>
                          <td className="py-3 px-4">
                            {s.active ? (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800">
                                <Check className="w-3 h-3" /> Đang kích hoạt
                              </span>
                            ) : (
                              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs text-slate-400 bg-slate-100 dark:bg-slate-800">
                                Chưa kích hoạt
                              </span>
                            )}
                          </td>
                          <td className="py-3 px-4">
                            {s.isFinalize ? (
                              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-purple-50 dark:bg-purple-950/60 text-purple-600 dark:text-purple-400">
                                <Award className="w-3 h-3" /> Đã chốt sổ
                              </span>
                            ) : (
                              <span className="text-slate-400 text-xs">Chưa</span>
                            )}
                          </td>
                          <td className="py-3 px-4 text-right">
                            <div className="flex items-center justify-end gap-1.5">
                              {s.active ? (
                                <span className="text-xs text-emerald-600 dark:text-emerald-400 font-semibold px-2 py-1">
                                  Đang hoạt động
                                </span>
                              ) : s.isFinalize ? (
                                <span className="text-xs text-slate-400 italic px-2 py-1">
                                  Đã kết thúc
                                </span>
                              ) : (
                                <Button
                                  size="sm"
                                  variant="outline"
                                  disabled={Boolean(activeSemester) || activatingSemesterId === s.semesterId}
                                  onClick={() => handleActivateSemester(s)}
                                  title={
                                    activeSemester
                                      ? `Đang có kỳ "${activeSemester.name}" hoạt động. Cần chốt sổ và đóng kỳ hiện tại trước.`
                                      : "Kích hoạt học kỳ này"
                                  }
                                  className="text-xs h-8 px-3 rounded-lg text-indigo-600 hover:text-indigo-700 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 border-indigo-200 dark:border-indigo-900 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                                >
                                  {activatingSemesterId === s.semesterId ? (
                                    <RefreshCw className="w-3 h-3 animate-spin mr-1" />
                                  ) : (
                                    <Check className="w-3 h-3 mr-1" />
                                  )}
                                  Kích hoạt
                                </Button>
                              )}

                              {/* Delete Semester Button */}
                              <Button
                                size="sm"
                                variant="outline"
                                disabled={s.active || deletingSemesterId === s.semesterId}
                                onClick={() => handleDeleteSemester(s)}
                                title={s.active ? "Không thể xóa học kỳ đang hoạt động" : "Xóa học kỳ"}
                                className="text-xs h-8 px-2 rounded-lg text-rose-600 hover:text-rose-700 hover:bg-rose-50 dark:hover:bg-rose-950/40 border-rose-200 dark:border-rose-900 cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed"
                              >
                                {deletingSemesterId === s.semesterId ? (
                                  <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                                ) : (
                                  <Trash2 className="w-3.5 h-3.5" />
                                )}
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 5: QUẢN LÝ MÔN HỌC                                                    */}
      {/* ========================================================================= */}
      {activeTab === "subjects" && (
        <div className="space-y-6">
          {/* Create Subject Card */}
          <Card className="rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
            <CardHeader>
              <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400">
                <BookOpen className="w-5 h-5" />
                <CardTitle className="text-lg">Tạo Môn Học Mới</CardTitle>
              </div>
              <CardDescription>
                Khai báo mã môn học (ví dụ: <code>IT4409</code>, <code>IT3080</code>) và tên học phần đầy đủ trước khi import sinh viên và mở ngân hàng câu hỏi.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateSubject} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="subjectCode" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                      Mã môn học *
                    </Label>
                    <Input
                      id="subjectCode"
                      placeholder="VD: IT4409"
                      value={newSubjectCode}
                      onChange={(e) => setNewSubjectCode(e.target.value.toUpperCase())}
                      maxLength={10}
                      required
                      className="rounded-xl text-sm font-mono uppercase"
                    />
                  </div>
                  <div className="sm:col-span-2 space-y-1.5">
                    <Label htmlFor="subjectName" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                      Tên môn học / học phần *
                    </Label>
                    <Input
                      id="subjectName"
                      placeholder="VD: Công nghệ Web và dịch vụ trực tuyến"
                      value={newSubjectName}
                      onChange={(e) => setNewSubjectName(e.target.value)}
                      maxLength={100}
                      required
                      className="rounded-xl text-sm"
                    />
                  </div>
                </div>

                <div className="flex justify-end pt-1">
                  <Button
                    type="submit"
                    disabled={isCreatingSubject || !newSubjectCode.trim() || !newSubjectName.trim()}
                    className="gap-2 px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-semibold shadow-md shadow-indigo-200 dark:shadow-none cursor-pointer"
                  >
                    {isCreatingSubject ? (
                      <>
                        <RefreshCw className="w-4 h-4 animate-spin" /> Đang tạo môn học...
                      </>
                    ) : (
                      <>
                        <PlusCircle className="w-4 h-4" /> Thêm Môn Học
                      </>
                    )}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          {/* Subjects List Card */}
          <Card className="rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
            <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3">
              <div>
                <CardTitle className="text-lg">Danh Sách Môn Học Hệ Thống</CardTitle>
                <CardDescription>
                  Tổng số môn học đang quản lý: {subjects.length} môn
                </CardDescription>
              </div>
              <div className="relative w-full sm:w-64">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <Input
                  placeholder="Tìm theo mã hoặc tên..."
                  value={subjectSearch}
                  onChange={(e) => setSubjectSearch(e.target.value)}
                  className="pl-9 h-9 text-xs rounded-xl"
                />
              </div>
            </CardHeader>
            <CardContent>
              {isLoadingSubjects ? (
                <div className="flex items-center justify-center py-10 text-slate-500 gap-2">
                  <RefreshCw className="w-5 h-5 animate-spin text-indigo-500" />
                  <span className="text-sm">Đang tải danh sách môn học...</span>
                </div>
              ) : subjects.length === 0 ? (
                <div className="text-center py-10 text-slate-400 text-sm">
                  Chưa có môn học nào được tạo trong hệ thống.
                </div>
              ) : (
                <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-800">
                  <table className="w-full text-left text-xs sm:text-sm">
                    <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-500 uppercase tracking-wider font-semibold text-[11px] border-b border-slate-200 dark:border-slate-800">
                      <tr>
                        <th className="py-3 px-4">ID</th>
                        <th className="py-3 px-4">Mã môn</th>
                        <th className="py-3 px-4">Tên môn học</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800 font-medium">
                      {subjects
                        .filter(
                          (sub) =>
                            sub.code.toLowerCase().includes(subjectSearch.toLowerCase()) ||
                            sub.name.toLowerCase().includes(subjectSearch.toLowerCase())
                        )
                        .map((sub) => (
                          <tr key={sub.subjectId} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition-colors">
                            <td className="py-3 px-4 font-mono text-slate-400">{sub.subjectId}</td>
                            <td className="py-3 px-4">
                              <span className="font-mono font-bold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/60 px-2 py-0.5 rounded-lg border border-indigo-200 dark:border-indigo-900">
                                {sub.code}
                              </span>
                            </td>
                            <td className="py-3 px-4 font-medium text-slate-900 dark:text-slate-100">
                              {sub.name}
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Custom Confirmation Modal (Thay thế window.confirm / alert) */}
      {confirmModal.isOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-200"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="w-full max-w-md rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl p-6 transition-all"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start gap-4">
              <div
                className={`p-3 rounded-2xl shrink-0 ${
                  confirmModal.variant === "danger"
                    ? "bg-rose-50 text-rose-600 dark:bg-rose-950/40 dark:text-rose-400"
                    : confirmModal.variant === "warning"
                    ? "bg-amber-50 text-amber-600 dark:bg-amber-950/40 dark:text-amber-400"
                    : confirmModal.variant === "purple"
                    ? "bg-purple-50 text-purple-600 dark:bg-purple-950/40 dark:text-purple-400"
                    : "bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400"
                }`}
              >
                {confirmModal.variant === "danger" ? (
                  <AlertCircle className="w-6 h-6" />
                ) : confirmModal.variant === "warning" ? (
                  <AlertCircle className="w-6 h-6" />
                ) : confirmModal.variant === "purple" ? (
                  <Award className="w-6 h-6" />
                ) : (
                  <Info className="w-6 h-6" />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
                  {confirmModal.title}
                </h3>
                <p className="mt-2 text-sm text-slate-600 dark:text-slate-300 leading-relaxed whitespace-pre-line">
                  {confirmModal.message}
                </p>
              </div>
              <button
                type="button"
                onClick={closeConfirm}
                className="text-slate-400 hover:text-slate-500 dark:hover:text-slate-300 p-1 rounded-lg transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="mt-6 flex items-center justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={closeConfirm}
                className="rounded-xl px-4 py-2 text-xs font-medium cursor-pointer"
              >
                {confirmModal.cancelText || "Hủy bỏ"}
              </Button>
              <Button
                type="button"
                onClick={async () => {
                  const action = confirmModal.onConfirm;
                  closeConfirm();
                  await action();
                }}
                className={`rounded-xl px-4 py-2 text-xs font-medium text-white shadow-xs cursor-pointer ${
                  confirmModal.variant === "danger"
                    ? "bg-rose-600 hover:bg-rose-700"
                    : confirmModal.variant === "warning"
                    ? "bg-amber-600 hover:bg-amber-700"
                    : confirmModal.variant === "purple"
                    ? "bg-purple-600 hover:bg-purple-700"
                    : "bg-indigo-600 hover:bg-indigo-700"
                }`}
              >
                {confirmModal.confirmText || "Xác nhận"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
