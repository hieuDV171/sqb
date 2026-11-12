import React, { useId, useState, useRef, useMemo } from "react";
import { Select } from "@base-ui/react/select";
import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { CustomSelect, type CustomSelectOption } from "@/components/ui/custom-select";
import { AdminPageHeader } from "./AdminPageHeader";
import { adminService } from "@/services/adminService";
import { toast } from "@/stores/useToastStore";
import type {
  BulkImportResult,
  ExcelImportClassResult,
  Gender,
  SingleUserImportDto,
  UserRole,
} from "@/types/auth.types";
import {
  FileSpreadsheet,
  Upload,
  UserPlus,
  Users,
  KeyRound,
  CheckCircle2,
  AlertCircle,
  Check,
  ChevronDown,
  Eye,
  EyeOff,
  RefreshCw,
  Trash2,
  PlusCircle,
  Info,
} from "lucide-react";

const accountTabs = [
  { value: "excel", label: "Nhập Excel", icon: FileSpreadsheet },
  { value: "bulk", label: "Tạo tài khoản", icon: UserPlus },
  { value: "reset", label: "Cấp lại mật khẩu", icon: KeyRound },
] as const;

type ActiveTab = typeof accountTabs[number]["value"];

type AccountDraft = {
  id: number;
  user: SingleUserImportDto;
  passwordConfirmation: string;
};

const roleOptions: { value: UserRole; label: string }[] = [
  { value: "STUDENT", label: "Sinh viên" },
  { value: "LECTURER", label: "Giảng viên" },
  { value: "ADMIN", label: "Quản trị viên" },
];

const genderOptions: { value: Gender; label: string }[] = [
  { value: "MALE", label: "Nam" },
  { value: "FEMALE", label: "Nữ" },
];

function AccountSelect<Value extends string>({ label, value, options, onChange, disabled }: {
  label: string;
  value: Value;
  options: { value: Value; label: string }[];
  onChange: (value: Value) => void;
  disabled: boolean;
}) {
  const labelId = useId();

  return (
    <div className="min-w-0 space-y-1.5">
      <Label id={labelId}>{label}</Label>
      <Select.Root items={options} value={value} disabled={disabled}
        onValueChange={(next) => { if (next !== null) onChange(next); }}>
        <Select.Trigger aria-labelledby={labelId}
          className="flex h-11 w-full min-w-0 items-center justify-between gap-2 rounded-xl border border-input bg-card px-3.5 text-left text-base outline-none transition-colors hover:bg-muted focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50 md:h-10 md:text-sm">
          <Select.Value className="min-w-0 truncate" />
          <Select.Icon className="shrink-0 text-muted-foreground"><ChevronDown aria-hidden="true" className="size-4" /></Select.Icon>
        </Select.Trigger>
        <Select.Portal>
          <Select.Positioner sideOffset={6} alignItemWithTrigger={false} className="z-50 w-(--anchor-width) max-w-(--available-width)">
            <Select.Popup className="max-h-(--available-height) overflow-y-auto rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
              <Select.List>
                {options.map((option) => (
                  <Select.Item key={option.value} value={option.value}
                    className="flex min-h-11 cursor-pointer items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm outline-none data-highlighted:bg-accent data-selected:font-semibold">
                    <Select.ItemText>{option.label}</Select.ItemText>
                    <Select.ItemIndicator><Check aria-hidden="true" className="size-4 text-academic" /></Select.ItemIndicator>
                  </Select.Item>
                ))}
              </Select.List>
            </Select.Popup>
          </Select.Positioner>
        </Select.Portal>
      </Select.Root>
    </div>
  );
}

function AccountPasswordField({ id, label, value, onChange, required, invalid, describedBy }: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  invalid?: boolean;
  describedBy?: string;
}) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="space-y-1.5">
      <Label htmlFor={id}>{label}</Label>
      <div className="relative">
        <Input id={id} type={visible ? "text" : "password"} value={value}
          autoComplete="new-password" autoCapitalize="none" spellCheck={false}
          required={required} aria-invalid={invalid} aria-describedby={describedBy}
          className="pr-12 [&::-ms-reveal]:hidden" onChange={(event) => onChange(event.target.value)} />
        <Button type="button" variant="ghost" size="icon" aria-controls={id}
          aria-label={`${visible ? "Ẩn" : "Hiện"} ${label.toLocaleLowerCase("vi-VN")}`}
          className="absolute inset-y-0 right-0 h-full rounded-l-none text-muted-foreground"
          onClick={() => setVisible((current) => !current)}>
          {visible ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
        </Button>
      </div>
    </div>
  );
}

function getPasswordConfirmationError(password: string | undefined, confirmation: string) {
  if (!password?.trim()) return confirmation ? "Hãy nhập mật khẩu tùy chỉnh trước." : null;
  if (!confirmation) return "Hãy nhập lại mật khẩu.";
  return password === confirmation ? null : "Mật khẩu xác nhận không khớp.";
}

function errorMessage(error: unknown, fallback: string) {
  return error && typeof error === "object" && "message" in error && typeof error.message === "string"
    ? error.message : fallback;
}

export function AdminUsersPage() {
  const [activeTab, setActiveTab] = useState<ActiveTab>("excel");

  // =========================================================================
  // TAB 1: EXCEL IMPORT STATE
  // =========================================================================
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [lecturerId, setLecturerId] = useState<string>("");
  const lecturersQuery = useQuery({
    queryKey: ["admin", "lecturers"],
    queryFn: async () => (await adminService.getLecturers()).data,
    enabled: activeTab === "excel",
  });
  const [isExcelUploading, setIsExcelUploading] = useState(false);
  const [excelResult, setExcelResult] = useState<ExcelImportClassResult | null>(null);
  const [excelError, setExcelError] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      if (file.name.endsWith(".xlsx") || file.name.endsWith(".xls")) {
        setSelectedFile(file);
        setExcelError(null);
        setExcelResult(null);
      } else {
        setExcelError("Chỉ chấp nhận file định dạng Excel (.xlsx, .xls)!");
      }
    }
  };

  const lecturerOptions: CustomSelectOption[] = useMemo(() => {
    return (lecturersQuery.data ?? []).map((lec) => {
      const cleanCode = lec.studentLecturerCode ? lec.studentLecturerCode.trim().replace(/^#+/, "") : "";
      return {
        value: lec.id.toString(),
        label: `${lec.fullName} (ID: ${lec.id})`,
        badge: cleanCode ? `#${cleanCode}` : undefined,
        subLabel: lec.schoolFaculty || undefined,
      };
    });
  }, [lecturersQuery.data]);

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
    } catch (err) {
      setExcelError(errorMessage(err, "Import file Excel thất bại. Vui lòng kiểm tra định dạng file."));
    } finally {
      setIsExcelUploading(false);
    }
  };

  // =========================================================================
  // TAB 2: BULK USER CREATION (TÙY CHỈNH) STATE
  // =========================================================================
  const nextAccountId = useRef(2);
  const [usersToImport, setUsersToImport] = useState<AccountDraft[]>([
    {
      id: 1,
      passwordConfirmation: "",
      user: {
        email: "",
        fullName: "",
        schoolFaculty: "Trường CNTT&TT",
        major: "Khoa học máy tính",
        className: "IT1-01",
        gender: "MALE",
        role: "STUDENT",
      },
    },
  ]);
  const [isBulkLoading, setIsBulkLoading] = useState(false);
  const [bulkResult, setBulkResult] = useState<BulkImportResult | null>(null);
  const [bulkError, setBulkError] = useState<string | null>(null);

  const handleAddUserRow = () => {
    const id = nextAccountId.current++;
    setUsersToImport((prev) => [
      ...prev,
      {
        id,
        passwordConfirmation: "",
        user: {
          email: "",
          fullName: "",
          schoolFaculty: "Trường CNTT&TT",
          gender: "MALE",
          role: "STUDENT",
        },
      },
    ]);
  };

  const handleRemoveUserRow = (id: number) => {
    setUsersToImport((prev) => prev.length === 1 ? prev : prev.filter((draft) => draft.id !== id));
  };

  const handleUserFieldChange = <Field extends keyof SingleUserImportDto>(
    id: number,
    field: Field,
    value: SingleUserImportDto[Field],
  ) => {
    setUsersToImport((prev) => prev.map((draft) => draft.id === id
      ? { ...draft, user: { ...draft.user, [field]: value } }
      : draft));
  };

  const handleBulkSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isBulkLoading) return;
    setBulkError(null);
    setBulkResult(null);

    // Validation sơ bộ
    for (let i = 0; i < usersToImport.length; i++) {
      const { id, user: u, passwordConfirmation } = usersToImport[i];
      if (!u.email || !u.fullName) {
        setBulkError(`Dòng #${i + 1}: Email và Họ tên là bắt buộc!`);
        return;
      }
      const passwordError = getPasswordConfirmationError(u.password, passwordConfirmation);
      if (passwordError) {
        setBulkError(`Tài khoản ${i + 1}. ${passwordError}`);
        document.getElementById(`account-${id}-confirm-password`)?.focus();
        return;
      }
    }

    // Chuẩn hóa payload: biến chuỗi rỗng thành undefined để backend tự động fallback mật khẩu & ngày sinh
    const sanitizedUsers = usersToImport.map(({ user: u }) => ({
      ...u,
      password: u.password?.trim() ? u.password.trim() : undefined,
      dateOfBirth: u.dateOfBirth?.trim() ? u.dateOfBirth.trim() : undefined,
      studentLecturerCode: u.studentLecturerCode?.trim() ? u.studentLecturerCode.trim() : undefined,
      className: u.className?.trim() ? u.className.trim() : undefined,
      schoolFaculty: u.schoolFaculty?.trim() ? u.schoolFaculty.trim() : undefined,
      major: u.major?.trim() ? u.major.trim() : undefined,
    }));

    setIsBulkLoading(true);
    try {
      const res = await adminService.bulkImportUsers({ users: sanitizedUsers });
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
    } catch (err) {
      setBulkError(errorMessage(err, "Tạo tài khoản thất bại."));
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
        newLogs.push(`✅ [${email}]: Đã cấp lại mật khẩu và thu hồi các phiên đăng nhập.`);
      } catch (err) {
        newLogs.push(`❌ [${email}]: ${errorMessage(err, "Không thể cấp lại mật khẩu.")}`);
      }
    }

    setResetLogs(newLogs);
    setIsResetLoading(false);
    toast.info("Đã hoàn tất quy trình xử lý danh sách reset mật khẩu.", "Kết quả Reset");
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-10">
      <AdminPageHeader icon={Users} title="Quản lý tài khoản"
        description="Nhập sinh viên từ Excel, tạo tài khoản hàng loạt và cấp lại mật khẩu." />
      <div role="group" aria-label="Thao tác quản lý tài khoản"
        className="flex flex-wrap gap-2 rounded-2xl border border-border bg-card p-2">
        {accountTabs.map(({ value, label, icon: Icon }) => (
          <Button key={value} type="button" variant={activeTab === value ? "secondary" : "ghost"}
            aria-pressed={activeTab === value} className="min-h-11"
            onClick={() => setActiveTab(value)}>
            <Icon aria-hidden="true" /> {label}
          </Button>
        ))}
      </div>

      {activeTab === "excel" && (
        <Card className="rounded-2xl border border-border bg-card">
          <CardHeader>
            <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400">
              <FileSpreadsheet className="w-5 h-5" />
              <CardTitle className="text-lg">Nhập Danh Sách Sinh Viên Lớp Học Phần (Excel)</CardTitle>
            </div>
            <CardDescription>
              Vào đầu kỳ học, Giảng viên gửi danh sách sinh viên theo lớp. Tải file Excel lên để hệ thống tạo tài khoản Sinh viên chưa xác thực, dùng mật khẩu tạm và yêu cầu đổi mật khẩu ở lần đăng nhập đầu tiên.
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
                  <Label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                    File Excel danh sách lớp (.xlsx, .xls) *
                  </Label>

                  {/* Hidden native input */}
                  <input
                    ref={fileInputRef}
                    id="excelFile"
                    type="file"
                    accept=".xlsx,.xls"
                    onChange={handleFileChange}
                    className="hidden"
                  />

                  {selectedFile ? (
                    <div className="p-3.5 rounded-2xl border border-emerald-200 dark:border-emerald-800/60 bg-emerald-50/50 dark:bg-emerald-950/20 flex items-center justify-between gap-3 animate-in fade-in duration-200">
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-10 h-10 rounded-xl bg-emerald-100 dark:bg-emerald-900/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
                          <FileSpreadsheet className="w-5 h-5" />
                        </div>
                        <div className="min-w-0">
                          <p className="text-xs font-bold text-slate-900 dark:text-slate-100 truncate">
                            {selectedFile.name}
                          </p>
                          <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                            {(selectedFile.size / 1024).toFixed(1)} KB • Sẵn sàng nạp dữ liệu
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-1.5 shrink-0">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => fileInputRef.current?.click()}
                          className="h-8 px-2.5 text-xs rounded-xl cursor-pointer"
                        >
                          Đổi file
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setSelectedFile(null);
                            if (fileInputRef.current) fileInputRef.current.value = "";
                          }}
                          className="h-8 px-2 text-xs text-rose-500 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40 rounded-xl cursor-pointer"
                          title="Gỡ bỏ file"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <div
                      role="button"
                      tabIndex={0}
                      onClick={() => fileInputRef.current?.click()}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();
                          fileInputRef.current?.click();
                        }
                      }}
                      onDragOver={handleDragOver}
                      onDragLeave={handleDragLeave}
                      onDrop={handleDrop}
                      className={`border-2 border-dashed rounded-2xl p-5 text-center cursor-pointer transition-all ${
                        isDragging
                          ? "border-indigo-500 bg-indigo-50/50 dark:bg-indigo-950/30"
                          : "border-slate-200 dark:border-slate-800 hover:border-indigo-400 dark:hover:border-indigo-600 bg-slate-50/40 dark:bg-slate-900/30"
                      }`}
                    >
                      <div className="w-9 h-9 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center mx-auto mb-2">
                        <Upload className="w-4 h-4" />
                      </div>
                      <p className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                        Nhấp để chọn hoặc kéo thả file Excel vào đây
                      </p>
                      <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-1">
                        Chấp nhận file định dạng .xlsx, .xls
                      </p>
                    </div>
                  )}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="lecturerId" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                    Giảng viên phụ trách lớp (Tùy chọn)
                  </Label>
                  <CustomSelect
                    id="lecturerId"
                    value={lecturerId}
                    onChange={setLecturerId}
                    options={lecturerOptions}
                    placeholder={
                      lecturersQuery.isPending
                        ? "Đang tải danh sách giảng viên..."
                        : "Chọn giảng viên từ hệ thống (hoặc để trống)..."
                    }
                    emptyLabel="-- Bỏ trống (Hệ thống tự tìm hoặc không chỉ định) --"
                    disabled={lecturersQuery.isPending || lecturersQuery.isError}
                  />
                  {lecturersQuery.isError && (
                    <div className="flex flex-wrap items-center gap-2">
                      <p role="alert" className="text-sm text-destructive">Không thể tải danh sách giảng viên.</p>
                      <Button type="button" variant="outline" onClick={() => void lecturersQuery.refetch()}>Thử lại</Button>
                    </div>
                  )}
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
        <Card className="mx-auto w-full max-w-3xl rounded-2xl border border-border bg-card">
          <CardHeader>
            <div className="flex items-center gap-2 text-academic">
              <UserPlus aria-hidden="true" className="size-5" />
              <CardTitle className="text-lg">Tạo tài khoản</CardTitle>
            </div>
            <CardDescription>
              Điền thông tin từng người. Bạn có thể thêm nhiều tài khoản và tạo trong một lần.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {bulkError && (
              <div role="alert" className="flex items-center gap-2.5 rounded-xl border border-destructive/25 bg-destructive/10 p-3.5 text-sm text-destructive">
                <AlertCircle aria-hidden="true" className="size-4 shrink-0" />
                <span>{bulkError}</span>
              </div>
            )}

            <form onSubmit={handleBulkSubmit} className="space-y-6">
              <div className="space-y-4">
                {usersToImport.map(({ id, user, passwordConfirmation }, index) => {
                  const fieldPrefix = `account-${id}`;
                  const confirmationError = passwordConfirmation
                    ? getPasswordConfirmationError(user.password, passwordConfirmation)
                    : null;

                  return (
                    <fieldset key={id} disabled={isBulkLoading} aria-labelledby={`${fieldPrefix}-heading`}
                      className="min-w-0 space-y-4 rounded-2xl border border-border bg-surface-subtle p-4 sm:p-5">
                      {usersToImport.length > 1 && (
                        <div className="flex items-center justify-between gap-2 border-b border-border pb-2">
                          <h3 id={`${fieldPrefix}-heading`} className="text-sm font-semibold">Tài khoản {index + 1}</h3>
                          <Button type="button" variant="ghost" size="sm" aria-label={`Xóa tài khoản ${index + 1} khỏi danh sách`}
                            className="text-destructive hover:text-destructive" onClick={() => handleRemoveUserRow(id)}>
                            <Trash2 aria-hidden="true" /> Xóa dòng
                          </Button>
                        </div>
                      )}
                      {usersToImport.length === 1 && <span id={`${fieldPrefix}-heading`} className="sr-only">Thông tin tài khoản</span>}

                      <div className="grid gap-4 sm:grid-cols-2">
                        <div className="space-y-1.5">
                          <Label htmlFor={`${fieldPrefix}-email`}>Email tài khoản *</Label>
                          <Input id={`${fieldPrefix}-email`} type="email" placeholder="sv.k66@hust.edu.vn"
                            value={user.email} required onChange={(event) => handleUserFieldChange(id, "email", event.target.value)} />
                        </div>
                        <div className="space-y-1.5">
                          <Label htmlFor={`${fieldPrefix}-full-name`}>Họ và tên *</Label>
                          <Input id={`${fieldPrefix}-full-name`} placeholder="Nguyễn Văn An" value={user.fullName}
                            required onChange={(event) => handleUserFieldChange(id, "fullName", event.target.value)} />
                        </div>

                        <AccountSelect label="Vai trò" value={user.role ?? "STUDENT"} options={roleOptions}
                          disabled={isBulkLoading} onChange={(value) => handleUserFieldChange(id, "role", value)} />
                        <AccountSelect label="Giới tính" value={user.gender ?? "MALE"} options={genderOptions}
                          disabled={isBulkLoading} onChange={(value) => handleUserFieldChange(id, "gender", value)} />

                        <div className="space-y-1.5">
                          <Label htmlFor={`${fieldPrefix}-code`}>MSSV / MSGV</Label>
                          <Input id={`${fieldPrefix}-code`} placeholder="20211234" value={user.studentLecturerCode ?? ""}
                            onChange={(event) => handleUserFieldChange(id, "studentLecturerCode", event.target.value)} />
                        </div>
                        <div className="space-y-1.5">
                          <Label htmlFor={`${fieldPrefix}-birth-date`}>Ngày sinh</Label>
                          <Input id={`${fieldPrefix}-birth-date`} type="date" value={user.dateOfBirth ?? ""}
                            onChange={(event) => handleUserFieldChange(id, "dateOfBirth", event.target.value)} />
                        </div>
                        <div className="space-y-1.5">
                          <Label htmlFor={`${fieldPrefix}-faculty`}>Khoa / Viện</Label>
                          <Input id={`${fieldPrefix}-faculty`} placeholder="Trường CNTT&TT" value={user.schoolFaculty ?? ""}
                            onChange={(event) => handleUserFieldChange(id, "schoolFaculty", event.target.value)} />
                        </div>
                        <div className="space-y-1.5">
                          <Label htmlFor={`${fieldPrefix}-class`}>Lớp sinh hoạt</Label>
                          <Input id={`${fieldPrefix}-class`} placeholder="IT1-02" value={user.className ?? ""}
                            onChange={(event) => handleUserFieldChange(id, "className", event.target.value)} />
                        </div>
                      </div>

                      <div className="space-y-2 border-t border-border pt-4">
                        <div className="grid gap-4 sm:grid-cols-2">
                          <AccountPasswordField id={`${fieldPrefix}-password`} label="Mật khẩu tùy chỉnh" value={user.password ?? ""}
                            describedBy={`${fieldPrefix}-password-hint`} onChange={(value) => handleUserFieldChange(id, "password", value)} />
                          <AccountPasswordField id={`${fieldPrefix}-confirm-password`} label="Xác nhận mật khẩu" value={passwordConfirmation}
                            required={Boolean(user.password?.trim())} invalid={Boolean(confirmationError)}
                            describedBy={confirmationError ? `${fieldPrefix}-password-error` : `${fieldPrefix}-password-hint`}
                            onChange={(value) => setUsersToImport((previous) => previous.map((draft) => draft.id === id
                              ? { ...draft, passwordConfirmation: value }
                              : draft))} />
                        </div>
                        {confirmationError && <p id={`${fieldPrefix}-password-error`} className="text-sm text-destructive">{confirmationError}</p>}
                        <p id={`${fieldPrefix}-password-hint`} className="text-sm text-muted-foreground">
                          Nếu để trống, hệ thống sẽ chọn mật khẩu theo thứ tự ưu tiên ngày sinh (ddMMyyyy), mã SV/CB, rồi mật khẩu mặc định.
                        </p>
                      </div>
                    </fieldset>
                  );
                })}
              </div>

              <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  disabled={isBulkLoading}
                  onClick={handleAddUserRow}
                  className="min-h-11 w-full sm:w-auto"
                >
                  <PlusCircle className="w-4 h-4" /> Thêm một dòng tài khoản
                </Button>

                <Button
                  type="submit"
                  disabled={isBulkLoading}
                  className="min-h-11 w-full sm:w-auto"
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
        <Card className="rounded-2xl border border-border bg-card">
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

    </div>
  );
}
