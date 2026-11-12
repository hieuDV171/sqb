import React, { useId, useState, useRef } from "react";
import { Combobox } from "@base-ui/react/combobox";
import { Select } from "@base-ui/react/select";
import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { DatePicker } from "@/components/ui/date-picker";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { AdminPageHeader } from "./AdminPageHeader";
import { adminService } from "@/services/adminService";
import { toast } from "@/stores/useToastStore";
import type {
  BulkImportResult,
  ExcelImportClassResult,
  Gender,
  LecturerSummary,
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
  Search,
  ShieldCheck,
  TriangleAlert,
  X,
  type LucideIcon,
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

type ResetResult = { email: string; error?: string };

const panelClass = "mx-auto w-full max-w-3xl rounded-2xl border-border";

function AccountPanelHeading({ icon: Icon, title, children }: { icon: LucideIcon; title: string; children: React.ReactNode }) {
  return (
    <CardHeader className="gap-1.5">
      <CardTitle className="flex items-center gap-2 text-lg font-semibold text-academic">
        <Icon aria-hidden="true" className="size-5" /> {title}
      </CardTitle>
      <CardDescription>{children}</CardDescription>
    </CardHeader>
  );
}

function AccountFormError({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <p role="alert" className="flex items-center gap-2.5 rounded-xl border border-destructive/25 bg-destructive/10 p-3.5 text-sm text-destructive">
      <AlertCircle aria-hidden="true" className="size-4 shrink-0" /> {message}
    </p>
  );
}

function LecturerPicker({ lecturers, value, onChange, disabled, pending }: {
  lecturers: LecturerSummary[];
  value: string;
  onChange: (value: string) => void;
  disabled: boolean;
  pending: boolean;
}) {
  const { contains } = Combobox.useFilter({ locale: "vi" });
  const selected = lecturers.find((lecturer) => String(lecturer.id) === value) ?? null;

  return (
    <Combobox.Root items={lecturers} value={selected} disabled={disabled}
      itemToStringLabel={(lecturer) => lecturer.fullName} itemToStringValue={(lecturer) => String(lecturer.id)}
      filter={(lecturer, query) => contains(`${lecturer.fullName} ${lecturer.studentLecturerCode ?? ""} ${lecturer.schoolFaculty ?? ""} ${lecturer.id}`, query)}
      onValueChange={(lecturer) => onChange(lecturer ? String(lecturer.id) : "")}>
      <Combobox.InputGroup className="relative">
        <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-muted-foreground" />
        <Combobox.Input id="lecturerId" aria-describedby="lecturer-hint"
          placeholder={pending ? "Đang tải danh sách giảng viên…" : "Tìm theo tên hoặc mã giảng viên"}
          render={<Input className="pr-20 pl-10" />} />
        <div className="absolute inset-y-0 right-1 flex items-center">
          <Combobox.Clear aria-label="Bỏ chọn giảng viên" render={<Button variant="ghost" size="icon-sm" type="button" />}>
            <X aria-hidden="true" />
          </Combobox.Clear>
          <Combobox.Trigger aria-label="Mở danh sách giảng viên" render={<Button variant="ghost" size="icon-sm" type="button" />}>
            <ChevronDown aria-hidden="true" />
          </Combobox.Trigger>
        </div>
      </Combobox.InputGroup>
      <Combobox.Portal>
        <Combobox.Positioner sideOffset={6} className="z-50 w-(--anchor-width) max-w-(--available-width)">
          <Combobox.Popup className="max-h-[min(20rem,var(--available-height))] overflow-y-auto rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
            <Combobox.Empty className="px-3 py-2 text-sm text-muted-foreground empty:p-0">Không tìm thấy giảng viên.</Combobox.Empty>
            <Combobox.List>
              {(lecturer: LecturerSummary) => (
                <Combobox.Item key={lecturer.id} value={lecturer}
                  className="flex min-h-11 cursor-pointer items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm outline-none data-highlighted:bg-accent">
                  <span className="min-w-0">
                    <span className="block font-medium">{lecturer.fullName}</span>
                    <span className="block text-xs text-muted-foreground">
                      {[lecturer.studentLecturerCode?.trim() || `ID ${lecturer.id}`, lecturer.schoolFaculty].filter(Boolean).join(" · ")}
                    </span>
                  </span>
                  <Combobox.ItemIndicator className="shrink-0 text-academic"><Check aria-hidden="true" className="size-4" /></Combobox.ItemIndicator>
                </Combobox.Item>
              )}
            </Combobox.List>
          </Combobox.Popup>
        </Combobox.Positioner>
      </Combobox.Portal>
    </Combobox.Root>
  );
}

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
    if (!isExcelUploading) setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    selectExcelFile(e.dataTransfer.files[0]);
  };

  function selectExcelFile(file?: File) {
    if (!file || isExcelUploading) return;
    if (!/\.xlsx?$/i.test(file.name)) {
      setExcelError("Chỉ chấp nhận tệp Excel (.xlsx, .xls).");
      return;
    }
    setSelectedFile(file);
    setExcelError(null);
    setExcelResult(null);
  }

  const handleExcelImport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isExcelUploading) return;
    if (!selectedFile) {
      setExcelError("Hãy chọn tệp Excel danh sách lớp.");
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
  const [resetPasswordConfirmation, setResetPasswordConfirmation] = useState("");
  const [isResetLoading, setIsResetLoading] = useState(false);
  const [resetLogs, setResetLogs] = useState<ResetResult[]>([]);
  const [resetError, setResetError] = useState<string | null>(null);
  const resetConfirmationError = resetPasswordConfirmation
    ? getPasswordConfirmationError(customNewPassword, resetPasswordConfirmation)
    : null;

  const handleAdminResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isResetLoading) return;
    setResetError(null);

    // Hỗ trợ nhập 1 email hoặc nhiều email ngăn cách bởi dấu phẩy/xuống dòng
    const rawEmails = resetEmailInput
      .split(/[\n,;]+/)
      .map((em) => em.trim())
      .filter((em) => em.length > 0);

    if (rawEmails.length === 0) {
      setResetError("Hãy nhập ít nhất một email tài khoản cần cấp lại mật khẩu.");
      return;
    }

    const passwordError = getPasswordConfirmationError(customNewPassword, resetPasswordConfirmation);
    if (passwordError) {
      setResetError(passwordError);
      document.getElementById("reset-confirm-password")?.focus();
      return;
    }

    setIsResetLoading(true);
    setResetLogs([]);
    const newLogs: ResetResult[] = [];

    for (const email of rawEmails) {
      try {
        await adminService.adminResetPassword({
          email,
          newPassword: customNewPassword.trim() || null,
        });
        newLogs.push({ email });
      } catch (err) {
        newLogs.push({ email, error: errorMessage(err, "Không thể cấp lại mật khẩu.") });
      }
    }

    setResetLogs(newLogs);
    setIsResetLoading(false);
    toast.info("Đã xử lý danh sách email.", "Cấp lại mật khẩu");
  };

  return (
    <div className="@container mx-auto max-w-6xl space-y-6 pb-10">
      <AdminPageHeader icon={Users} title="Quản lý tài khoản"
        description="Nhập sinh viên từ Excel, tạo tài khoản hàng loạt và cấp lại mật khẩu." />
      <div role="group" aria-label="Thao tác quản lý tài khoản"
        className="flex flex-wrap gap-2 rounded-2xl border border-border bg-card p-2">
        {accountTabs.map(({ value, label, icon: Icon }) => (
          <Button key={value} id={`admin-account-tab-${value}`} type="button" variant={activeTab === value ? "secondary" : "ghost"}
            aria-pressed={activeTab === value} className="min-h-11"
            onClick={() => setActiveTab(value)}>
            <Icon aria-hidden="true" /> {label}
          </Button>
        ))}
      </div>

      {activeTab === "excel" && (
        <Card className={panelClass}>
          <AccountPanelHeading icon={FileSpreadsheet} title="Nhập danh sách lớp từ Excel">
            Hệ thống ghi danh sinh viên vào lớp và tạo tài khoản cho những người chưa có.
          </AccountPanelHeading>
          <CardContent className="space-y-6">
            <AccountFormError message={excelError} />
            <form onSubmit={handleExcelImport} className="space-y-5">
              <div className="space-y-4 rounded-2xl border border-border bg-surface-subtle p-4 sm:p-5">
                <div className="space-y-1.5" onDragOver={handleDragOver} onDragLeave={handleDragLeave} onDrop={handleDrop}>
                  <Label htmlFor="excel-file">Tệp Excel danh sách lớp *</Label>
                  <input ref={fileInputRef} id="excel-file" type="file" accept=".xlsx,.xls"
                    className="hidden" disabled={isExcelUploading} onChange={(event) => selectExcelFile(event.target.files?.[0])} />
                  {selectedFile ? (
                    <div className={cn(
                      "flex flex-wrap items-center justify-between gap-3 rounded-xl border border-success/25 bg-success-soft p-3",
                      isDragging && "border-academic bg-academic-soft",
                    )}>
                      <div className="flex min-w-0 items-center gap-3">
                        <FileSpreadsheet aria-hidden="true" className="size-5 shrink-0 text-success" />
                        <div className="min-w-0">
                          <p className="wrap-anywhere font-medium">{selectedFile.name}</p>
                          <p className="text-xs text-muted-foreground">{(selectedFile.size / 1024).toFixed(1)} KB</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Button type="button" variant="outline" disabled={isExcelUploading}
                          onClick={() => fileInputRef.current?.click()}>Đổi tệp</Button>
                        <Button type="button" variant="ghost" size="icon" aria-label="Gỡ tệp Excel" disabled={isExcelUploading}
                          className="text-destructive hover:text-destructive" onClick={() => {
                            setSelectedFile(null);
                            setExcelError(null);
                            if (fileInputRef.current) fileInputRef.current.value = "";
                          }}>
                          <Trash2 aria-hidden="true" />
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <button type="button" disabled={isExcelUploading} onClick={() => fileInputRef.current?.click()}
                      className={cn(
                        "flex w-full cursor-pointer flex-col items-center gap-2 rounded-xl border-2 border-dashed border-input bg-card px-4 py-6 text-center transition-colors hover:border-academic hover:bg-academic-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:opacity-50",
                        isDragging && "border-academic bg-academic-soft",
                      )}>
                      <span className="grid size-10 place-items-center rounded-xl bg-academic-soft text-academic">
                        <Upload aria-hidden="true" className="size-5" />
                      </span>
                      <span className="font-semibold">Chọn hoặc kéo thả tệp Excel</span>
                      <span className="text-xs text-muted-foreground">Định dạng .xlsx hoặc .xls</span>
                    </button>
                  )}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="lecturerId">Giảng viên phụ trách lớp (tùy chọn)</Label>
                  <LecturerPicker lecturers={lecturersQuery.data ?? []} value={lecturerId} onChange={setLecturerId}
                    pending={lecturersQuery.isPending} disabled={isExcelUploading || lecturersQuery.isPending || lecturersQuery.isError} />
                  <p id="lecturer-hint" className="text-sm text-muted-foreground">
                    Để trống nếu muốn hệ thống đọc thông tin giảng viên từ tệp.
                  </p>
                  {lecturersQuery.isError && (
                    <div className="flex flex-wrap items-center gap-2">
                      <p role="alert" className="text-sm text-destructive">Không thể tải danh sách giảng viên.</p>
                      <Button type="button" variant="outline" disabled={isExcelUploading} onClick={() => void lecturersQuery.refetch()}>Thử lại</Button>
                    </div>
                  )}
                </div>
              </div>

              <div className="rounded-xl border border-academic/20 bg-academic-soft p-4">
                <h3 className="flex items-center gap-2 font-semibold text-academic">
                  <Info aria-hidden="true" className="size-4" /> Chuẩn bị tệp Excel
                </h3>
                <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-muted-foreground">
                  <li>Dùng mẫu chuẩn hoặc mẫu rút gọn có mã học phần, tên học phần, MSSV và họ tên.</li>
                  <li>Nếu thiếu cột mã lớp, hệ thống đọc mã từ tên tệp. Ví dụ <code>171146-IT4409.xlsx</code> có mã lớp <code>171146</code>.</li>
                  <li>Hệ thống tự sinh email theo chuẩn HUST và mật khẩu tạm nếu thiếu dữ liệu tương ứng.</li>
                  <li>Môn học phải có trong danh mục và học kỳ hiện tại phải được kích hoạt.</li>
                </ul>
              </div>

              <div className="flex justify-end">
                <Button type="submit" disabled={isExcelUploading || !selectedFile} className="min-h-11 w-full sm:w-auto">
                  {isExcelUploading ? <RefreshCw aria-hidden="true" className="motion-safe:animate-spin" /> : <Upload aria-hidden="true" />}
                  {isExcelUploading ? "Đang nhập danh sách…" : "Nhập danh sách lớp"}
                </Button>
              </div>
            </form>

            {excelResult && (
              <div className="space-y-3 rounded-xl border border-success/25 bg-success-soft p-4" role="status">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className="flex items-center gap-2 font-semibold text-success">
                    <CheckCircle2 aria-hidden="true" className="size-5 shrink-0" /> Đã nhập lớp {excelResult.classCode}
                  </h3>
                  <Badge variant="outline">{excelResult.subjectCode} · {excelResult.subjectName}</Badge>
                </div>
                <dl className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                  {[
                    ["Tài khoản mới", excelResult.newUsersCreated],
                    ["Đã có tài khoản", excelResult.existingUsersFound],
                    ["Ghi danh mới", excelResult.newEnrollments],
                    ["Tổng số dòng", excelResult.totalRowsInFile],
                  ].map(([label, count]) => (
                    <div key={label} className="rounded-lg border border-border bg-card p-3">
                      <dt className="text-xs text-muted-foreground">{label}</dt>
                      <dd className="mt-1 text-lg font-semibold tabular-nums">{count}</dd>
                    </div>
                  ))}
                </dl>
              </div>
            )}
          </CardContent>
        </Card>
      )}
      {activeTab === "bulk" && (
        <div className="grid items-start gap-4 @min-[64rem]:grid-cols-[minmax(0,1fr)_minmax(0,3.35fr)_minmax(0,1fr)]">
          <aside className="hidden space-y-3 rounded-2xl border border-academic/20 bg-linear-to-br from-academic-soft to-card p-4 @min-[64rem]:block">
            <span className="grid size-9 place-items-center rounded-xl bg-academic-soft text-academic">
              <FileSpreadsheet aria-hidden="true" className="size-5" />
            </span>
            <h2 className="text-sm font-semibold">Có danh sách lớp?</h2>
            <p className="text-sm leading-relaxed text-muted-foreground">Nhập tệp Excel để tạo tài khoản và ghi danh cả lớp trong một lần.</p>
            <Button type="button" variant="outline" size="sm" onClick={() => {
              setActiveTab("excel");
              document.getElementById("admin-account-tab-excel")?.focus();
            }}>
              Nhập Excel
            </Button>
          </aside>
          <Card className={panelClass}>
            <AccountPanelHeading icon={UserPlus} title="Tạo tài khoản">
              Điền thông tin từng người. Bạn có thể thêm nhiều tài khoản và tạo trong một lần.
            </AccountPanelHeading>
            <CardContent className="space-y-6">
              <AccountFormError message={bulkError} />

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
                            <DatePicker id={`${fieldPrefix}-birth-date`} value={user.dateOfBirth ?? ""} disabled={isBulkLoading}
                              allowClear allowYearJump onChange={(value) => handleUserFieldChange(id, "dateOfBirth", value)} />
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
          <aside className="hidden space-y-3 rounded-2xl border border-success/20 bg-linear-to-br from-success-soft to-card p-4 @min-[64rem]:block">
            <span className="grid size-9 place-items-center rounded-xl bg-success-soft text-success">
              <ShieldCheck aria-hidden="true" className="size-5" />
            </span>
            <h2 className="text-sm font-semibold">Lần đăng nhập đầu</h2>
            <p className="text-sm leading-relaxed text-muted-foreground">Người dùng đăng nhập bằng mật khẩu tạm, sau đó đổi mật khẩu và xác thực email.</p>
          </aside>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 3: RESET MẬT KHẨU VỀ MẶC ĐỊNH CHO SINH VIÊN / GIẢNG VIÊN              */}
      {/* ========================================================================= */}
      {activeTab === "reset" && (
        <Card className={panelClass}>
          <AccountPanelHeading icon={KeyRound} title="Cấp lại mật khẩu">
            Cấp lại mật khẩu cho một hoặc nhiều tài khoản theo địa chỉ email.
          </AccountPanelHeading>
          <CardContent className="space-y-6">
            <AccountFormError message={resetError} />
            <form onSubmit={handleAdminResetPassword} className="space-y-5">
              <fieldset disabled={isResetLoading} className="min-w-0 space-y-4 rounded-2xl border border-border bg-surface-subtle p-4 sm:p-5">
                <div className="space-y-1.5">
                  <Label htmlFor="resetEmails">Email tài khoản *</Label>
                  <textarea id="resetEmails" rows={4} value={resetEmailInput} required spellCheck={false}
                    autoCapitalize="none" aria-describedby="reset-emails-hint" placeholder="sv.k66@hust.edu.vn"
                    onChange={(event) => setResetEmailInput(event.target.value)}
                    className="block w-full resize-y rounded-xl border border-input bg-card px-3.5 py-2.5 text-base outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50 md:text-sm" />
                  <p id="reset-emails-hint" className="text-sm text-muted-foreground">Phân cách các email bằng dấu phẩy hoặc xuống dòng.</p>
                </div>
                <div className="space-y-2 border-t border-border pt-4">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <AccountPasswordField id="reset-new-password" label="Mật khẩu tùy chỉnh" value={customNewPassword}
                      describedBy="reset-password-hint" onChange={setCustomNewPassword} />
                    <AccountPasswordField id="reset-confirm-password" label="Xác nhận mật khẩu" value={resetPasswordConfirmation}
                      required={Boolean(customNewPassword.trim())} invalid={Boolean(resetConfirmationError)}
                      describedBy={resetConfirmationError ? "reset-password-error" : "reset-password-hint"}
                      onChange={setResetPasswordConfirmation} />
                  </div>
                  {resetConfirmationError && <p id="reset-password-error" className="text-sm text-destructive">{resetConfirmationError}</p>}
                  <p id="reset-password-hint" className="text-sm text-muted-foreground">
                    Để trống cả hai ô để dùng mật khẩu mặc định.
                  </p>
                </div>
              </fieldset>
              <p className="flex items-start gap-2 rounded-xl border border-warning/25 bg-warning-soft p-3.5 text-sm text-warning">
                <TriangleAlert aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
                Khi cấp lại mật khẩu, các phiên đăng nhập hiện tại của tài khoản sẽ bị thu hồi.
              </p>

              <div className="flex justify-end">
                <Button type="submit" variant="destructive" disabled={isResetLoading || !resetEmailInput.trim()} className="min-h-11 w-full sm:w-auto">
                  {isResetLoading ? <RefreshCw aria-hidden="true" className="motion-safe:animate-spin" /> : <KeyRound aria-hidden="true" />}
                  {isResetLoading ? "Đang cấp lại mật khẩu…" : "Cấp lại mật khẩu"}
                </Button>
              </div>
            </form>

            {resetLogs.length > 0 && (
              <div className="rounded-xl border border-border bg-surface-subtle p-4" role="status">
                <h3 className="font-semibold">Kết quả cấp lại mật khẩu</h3>
                <ul className="mt-2 max-h-56 divide-y divide-border overflow-y-auto">
                  {resetLogs.map(({ email, error }, index) => (
                    <li key={`${email}-${index}`} className="flex items-start gap-2 py-2">
                      {error ? <AlertCircle aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-destructive" />
                        : <CheckCircle2 aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-success" />}
                      <div className="min-w-0">
                        <p className="wrap-anywhere font-medium">{email}</p>
                        <p className={error ? "text-sm text-destructive" : "text-sm text-muted-foreground"}>{error ?? "Đã cấp lại mật khẩu"}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </CardContent>
        </Card>
      )}

    </div>
  );
}
