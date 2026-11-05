import { useRef, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  ArrowRight,
  BookOpenCheck,
  Eye,
  EyeOff,
  HelpCircle,
  Moon,
  ShieldCheck,
  Sparkles,
  Sun,
  Users,
  X,
} from 'lucide-react';
import { BrandMark } from '@/components/common/BrandMark';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useModalFocusTrap } from '@/hooks/useModalFocusTrap';
import { useAuth } from '@/hooks/useAuth';
import { useThemeStore } from '@/stores/useThemeStore';
import { DevicePlatform } from '@/types/auth.types';
import { requestFid } from '@/lib/firebase';
import { getBrowserDeviceName } from '@/lib/utils';

function getOrCreateDeviceId() {
  let deviceId = localStorage.getItem('sqb_device_id');
  if (!deviceId) {
    deviceId = `web-browser-${crypto.randomUUID()}`;
    localStorage.setItem('sqb_device_id', deviceId);
  }
  return deviceId;
}

interface AccountHelpDialogProps {
  open: boolean;
  onClose: () => void;
}

function AccountHelpDialog({ open, onClose }: AccountHelpDialogProps) {
  const dialogRef = useModalFocusTrap(open, onClose);
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 p-4 backdrop-blur-[2px]" role="presentation">
      <button type="button" className="absolute inset-0" onClick={onClose} aria-label="Đóng hướng dẫn" />
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="account-help-title"
        className="relative z-10 w-full max-w-lg rounded-3xl border border-border bg-popover p-6 text-popover-foreground shadow-[var(--shadow-primitive-md)] sm:p-7"
      >
        <button
          type="button"
          onClick={onClose}
          data-autofocus
          className="absolute right-4 top-4 grid size-11 place-items-center rounded-xl text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          aria-label="Đóng hướng dẫn"
        >
          <X className="size-5" />
        </button>

        <div className="pr-12">
          <span className="grid size-11 place-items-center rounded-2xl bg-accent text-primary">
            <HelpCircle className="size-5" aria-hidden="true" />
          </span>
          <h2 id="account-help-title" className="mt-4 text-xl font-bold tracking-tight">Cấp lại quyền truy cập</h2>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            SQB dùng tài khoản do nhà trường quản lý, vì vậy mật khẩu không được đặt lại tự động qua email cá nhân.
          </p>
        </div>

        <div className="mt-5 rounded-2xl border border-border bg-surface-subtle p-4">
          <p className="text-sm font-semibold text-foreground">Dành cho sinh viên</p>
          <ol className="mt-3 space-y-3 text-sm leading-6 text-muted-foreground">
            <li className="flex gap-3"><span className="font-bold text-primary">01</span><span>Liên hệ giảng viên phụ trách lớp học phần để xác nhận danh tính.</span></li>
            <li className="flex gap-3"><span className="font-bold text-primary">02</span><span>Giảng viên gửi yêu cầu đến quản trị viên để cấp lại mật khẩu mặc định.</span></li>
            <li className="flex gap-3"><span className="font-bold text-primary">03</span><span>Đăng nhập và đổi mật khẩu ngay trong menu tài khoản.</span></li>
          </ol>
        </div>

        <Button type="button" onClick={onClose} className="mt-6 w-full sm:ml-auto sm:w-auto">
          Tôi đã hiểu
        </Button>
      </div>
    </div>
  );
}

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showHelpDialog, setShowHelpDialog] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);
  const { login, isLoading, error } = useAuth();
  const { theme, toggleTheme } = useThemeStore();
  const emailRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setValidationError(null);

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setValidationError('Vui lòng nhập địa chỉ email được nhà trường cấp.');
      emailRef.current?.focus();
      return;
    }
    if (!password) {
      setValidationError('Vui lòng nhập mật khẩu tài khoản.');
      passwordRef.current?.focus();
      return;
    }

    const fid = await requestFid().catch(() => null);
    login({
      email: trimmedEmail,
      password,
      deviceId: getOrCreateDeviceId(),
      platform: DevicePlatform.WEB,
      deviceName: getBrowserDeviceName(),
      fid: fid || undefined,
    });
  };

  const activeError = validationError || error;

  return (
    <main className="relative min-h-dvh overflow-hidden bg-background text-foreground">
      <div className="pointer-events-none absolute -left-32 -top-32 size-80 rounded-full bg-primary/8 blur-3xl" aria-hidden="true" />
      <div className="pointer-events-none absolute -bottom-40 right-0 size-96 rounded-full bg-academic/8 blur-3xl" aria-hidden="true" />

      <button
        type="button"
        onClick={toggleTheme}
        className="absolute right-4 top-4 z-20 grid size-11 place-items-center rounded-xl border border-border bg-card/90 text-muted-foreground shadow-[var(--card-shadow)] backdrop-blur transition-colors hover:text-foreground sm:right-6 sm:top-6"
        aria-label={theme === 'dark' ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'}
      >
        {theme === 'dark' ? <Sun className="size-5 text-reward" /> : <Moon className="size-5" />}
      </button>

      <div className="relative mx-auto grid min-h-dvh w-full max-w-[90rem] items-stretch lg:grid-cols-[1.05fr_0.95fr]">
        <section className="hidden border-r border-border px-10 py-12 lg:flex lg:flex-col lg:justify-between xl:px-16 xl:py-16" aria-labelledby="welcome-title">
          <BrandMark />

          <div className="max-w-xl py-12">
            <span className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-accent px-3 py-1.5 text-xs font-bold uppercase tracking-[0.11em] text-accent-foreground">
              <Sparkles className="size-3.5" aria-hidden="true" />
              Học cùng cộng đồng
            </span>
            <h1 id="welcome-title" className="mt-6 text-balance text-4xl font-bold leading-[1.12] tracking-[-0.04em] xl:text-5xl">
              Một không gian chung cho kiến thức, lớp học và cộng đồng HUST.
            </h1>
            <p className="mt-5 max-w-lg text-lg leading-8 text-muted-foreground">
              Chia sẻ câu hỏi, luyện tập theo môn học và cùng xây dựng ngân hàng học liệu đáng tin cậy.
            </p>

            <div className="mt-10 grid gap-4 sm:grid-cols-3">
              {[
                { icon: BookOpenCheck, title: 'Học tập', text: 'Luyện câu hỏi theo lớp và học kỳ' },
                { icon: Users, title: 'Cộng đồng', text: 'Trao đổi với bạn học và giảng viên' },
                { icon: ShieldCheck, title: 'Tin cậy', text: 'Nội dung có quy trình kiểm duyệt' },
              ].map((item) => {
                const Icon = item.icon;
                return (
                  <div key={item.title} className="rounded-2xl border border-border bg-card/75 p-4 shadow-[var(--card-shadow)] backdrop-blur">
                    <Icon className="size-5 text-primary" aria-hidden="true" />
                    <p className="mt-3 text-sm font-semibold">{item.title}</p>
                    <p className="mt-1 text-xs leading-5 text-muted-foreground">{item.text}</p>
                  </div>
                );
              })}
            </div>
          </div>

          <p className="text-xs text-muted-foreground">SQB Campus Intelligence · School Question Bank</p>
        </section>

        <section className="flex min-h-dvh items-center justify-center px-4 py-20 sm:px-8 lg:px-12" aria-labelledby="login-title">
          <div className="w-full max-w-md">
            <BrandMark className="mb-10 lg:hidden" />

            <div className="mb-7">
              <p className="text-sm font-semibold text-primary">Chào mừng trở lại</p>
              <h1 id="login-title" className="mt-2 text-3xl font-bold tracking-[-0.035em]">Đăng nhập vào SQB</h1>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">Sử dụng tài khoản sinh viên, giảng viên hoặc quản trị viên đã được cấp.</p>
            </div>

            <Card className="bg-card/90 backdrop-blur">
              <CardContent className="p-5 sm:p-6">
                <form onSubmit={handleSubmit} noValidate className="space-y-5">
                  {activeError && (
                    <div role="alert" className="flex items-start gap-3 rounded-2xl border border-destructive/20 bg-destructive/10 p-3.5 text-sm text-destructive">
                      <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                      <span>{activeError}</span>
                    </div>
                  )}

                  <div className="space-y-2">
                    <Label htmlFor="email">Email nhà trường</Label>
                    <Input
                      ref={emailRef}
                      id="email"
                      name="email"
                      type="email"
                      autoComplete="username"
                      inputMode="email"
                      placeholder="a.bc123456@hust.edu.vn"
                      value={email}
                      aria-invalid={Boolean(activeError && !email.trim())}
                      onChange={(event) => {
                        setEmail(event.target.value);
                        if (validationError) setValidationError(null);
                      }}
                    />
                  </div>

                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-3">
                      <Label htmlFor="password">Mật khẩu</Label>
                      <button type="button" onClick={() => setShowHelpDialog(true)} className="min-h-11 rounded-lg px-1 text-xs font-semibold text-primary transition-colors hover:text-[var(--primary-hover)]">
                        Cần hỗ trợ?
                      </button>
                    </div>
                    <div className="relative">
                      <Input
                        ref={passwordRef}
                        id="password"
                        name="password"
                        type={showPassword ? 'text' : 'password'}
                        autoComplete="current-password"
                        placeholder="Nhập mật khẩu"
                        value={password}
                        className="pr-12"
                        aria-invalid={Boolean(activeError && !password)}
                        onChange={(event) => {
                          setPassword(event.target.value);
                          if (validationError) setValidationError(null);
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword((current) => !current)}
                        className="absolute right-0 top-1/2 grid size-11 -translate-y-1/2 place-items-center rounded-xl text-muted-foreground transition-colors hover:text-foreground"
                        aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                        aria-pressed={showPassword}
                      >
                        {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                      </button>
                    </div>
                  </div>

                  <Button type="submit" disabled={isLoading} className="w-full" aria-busy={isLoading}>
                    {isLoading ? 'Đang xác thực…' : 'Đăng nhập'}
                    {!isLoading && <ArrowRight className="size-4" aria-hidden="true" />}
                  </Button>
                </form>

                <div className="mt-5 flex items-start gap-3 rounded-2xl bg-surface-subtle p-3.5 text-xs leading-5 text-muted-foreground">
                  <ShieldCheck className="mt-0.5 size-4 shrink-0 text-academic" aria-hidden="true" />
                  <p>Tài khoản được cấp theo danh sách lớp học phần. SQB không yêu cầu nhập mật khẩu tại bất kỳ trang nào khác.</p>
                </div>
              </CardContent>
            </Card>
          </div>
        </section>
      </div>

      <AccountHelpDialog open={showHelpDialog} onClose={() => setShowHelpDialog(false)} />
    </main>
  );
}
