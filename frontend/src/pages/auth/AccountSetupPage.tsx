import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { AlertCircle, Check, Eye, EyeOff, KeyRound, LogOut, MailCheck, ShieldCheck } from 'lucide-react';
import { BrandMark } from '@/components/common/BrandMark';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { authService } from '@/services/authService';
import { useAuthStore } from '@/stores/useAuthStore';
import { toast } from '@/stores/useToastStore';

function errorMessage(error: unknown, fallback: string) {
  return typeof error === 'object' && error !== null && 'message' in error && typeof error.message === 'string'
    ? error.message
    : fallback;
}

export function AccountSetupPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user, accountState, updateUser, setAccountState, logout } = useAuthStore();
  const needsEmailVerification = accountState === 'EMAIL_VERIFICATION_REQUIRED';
  const [otp, setOtp] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPasswords, setShowPasswords] = useState(false);
  const [busyAction, setBusyAction] = useState<'send' | 'verify' | 'password' | 'logout' | null>(null);
  const [error, setError] = useState<string | null>(null);

  const sendOtp = async () => {
    setBusyAction('send');
    setError(null);
    try {
      await authService.requestEmailVerification();
      setOtpSent(true);
      toast.success('Mã OTP có hiệu lực trong 2 phút.', 'Đã gửi mã xác thực');
    } catch (caught) {
      setError(errorMessage(caught, 'Không thể gửi OTP. Vui lòng thử lại sau.'));
    } finally {
      setBusyAction(null);
    }
  };

  const verifyOtp = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    if (!/^\d{6}$/.test(otp)) {
      setError('OTP phải gồm đúng 6 chữ số.');
      return;
    }
    setBusyAction('verify');
    try {
      await authService.confirmEmailVerification(otp);
      updateUser({ verified: true });
      setAccountState('PASSWORD_CHANGE_REQUIRED');
      setOtp('');
      toast.success('Tiếp tục thiết lập mật khẩu riêng.', 'Email đã được xác thực');
    } catch (caught) {
      setError(errorMessage(caught, 'OTP không đúng hoặc đã hết hạn.'));
    } finally {
      setBusyAction(null);
    }
  };

  const changePassword = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    if (newPassword.length < 8) {
      setError('Mật khẩu mới phải có ít nhất 8 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Xác nhận mật khẩu mới không trùng khớp.');
      return;
    }
    if (oldPassword === newPassword) {
      setError('Mật khẩu mới phải khác mật khẩu tạm thời.');
      return;
    }

    setBusyAction('password');
    try {
      await authService.changePassword({ oldPassword, newPassword });
      await authService.logout().catch(() => undefined);
      logout();
      queryClient.clear();
      toast.success('Hãy đăng nhập lại bằng mật khẩu mới.', 'Thiết lập tài khoản hoàn tất');
      navigate('/login', { replace: true });
    } catch (caught) {
      setError(errorMessage(caught, 'Không thể đổi mật khẩu. Vui lòng kiểm tra mật khẩu hiện tại.'));
      setBusyAction(null);
    }
  };

  const leaveSetup = async () => {
    setBusyAction('logout');
    await authService.logout().catch(() => undefined);
    logout();
    queryClient.clear();
    navigate('/login', { replace: true });
  };

  return (
    <main className="min-h-screen bg-background px-4 py-6 text-foreground sm:px-6 sm:py-10">
      <div className="mx-auto w-full max-w-5xl">
        <header className="flex items-center justify-between gap-4">
          <BrandMark />
          <Button type="button" variant="ghost" onClick={leaveSetup} disabled={busyAction !== null}>
            <LogOut className="size-4" /> Đăng xuất
          </Button>
        </header>

        <div className="mt-8 grid gap-6 lg:grid-cols-[0.9fr_1.1fr] lg:items-start">
          <section className="rounded-3xl bg-primary p-6 text-primary-foreground shadow-[var(--shadow-primitive-md)] sm:p-8">
            <span className="grid size-12 place-items-center rounded-2xl bg-white/15"><ShieldCheck className="size-6" /></span>
            <p className="mt-7 text-sm font-semibold uppercase tracking-[0.16em] text-primary-foreground/70">Thiết lập lần đầu</p>
            <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">Bảo vệ tài khoản trước khi vào SQB</h1>
            <p className="mt-4 max-w-lg leading-7 text-primary-foreground/80">
              Tài khoản do nhà trường tạo cần xác thực email trước, sau đó thay mật khẩu tạm thời bằng mật khẩu riêng.
            </p>
            <div className="mt-8 space-y-4">
              <SetupStep number="1" title="Xác thực email bằng OTP" complete={!needsEmailVerification} active={needsEmailVerification} />
              <SetupStep number="2" title="Đặt mật khẩu riêng" complete={false} active={!needsEmailVerification} />
            </div>
            <p className="mt-8 rounded-2xl bg-black/10 p-4 text-sm leading-6 text-primary-foreground/80">
              Trong quá trình này, phiên đăng nhập không thể truy cập bảng tin, tin nhắn, API nghiệp vụ hoặc WebSocket.
            </p>
          </section>

          <Card className="rounded-3xl py-0">
            <CardContent className="p-5 sm:p-8">
              <p className="text-sm font-medium text-muted-foreground">{user?.username}</p>
              <h2 className="mt-2 text-2xl font-bold tracking-tight">
                {needsEmailVerification ? 'Xác thực email của bạn' : 'Tạo mật khẩu riêng'}
              </h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {needsEmailVerification
                  ? 'Nhận mã 6 chữ số qua email trường. Mỗi mã có hiệu lực 2 phút và tối đa 5 lần nhập.'
                  : 'Dùng ít nhất 8 ký tự và không sử dụng lại mật khẩu tạm thời.'}
              </p>

              {error && (
                <div role="alert" className="mt-5 flex gap-3 rounded-2xl border border-destructive/20 bg-destructive/10 p-4 text-sm text-destructive">
                  <AlertCircle className="mt-0.5 size-4 shrink-0" /> <span>{error}</span>
                </div>
              )}

              {needsEmailVerification ? (
                <form onSubmit={verifyOtp} className="mt-6 space-y-5">
                  <div className="space-y-2">
                    <Label htmlFor="setup-otp">Mã OTP</Label>
                    <Input
                      id="setup-otp"
                      inputMode="numeric"
                      autoComplete="one-time-code"
                      maxLength={6}
                      value={otp}
                      onChange={(event) => setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))}
                      placeholder="000000"
                      className="h-12 text-center text-lg tracking-[0.45em]"
                      required
                    />
                  </div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    <Button type="button" variant="outline" onClick={sendOtp} disabled={busyAction !== null}>
                      <MailCheck className="size-4" /> {otpSent ? 'Gửi lại OTP' : 'Gửi OTP'}
                    </Button>
                    <Button type="submit" disabled={busyAction !== null || otp.length !== 6}>
                      {busyAction === 'verify' ? 'Đang xác thực…' : 'Xác thực và tiếp tục'}
                    </Button>
                  </div>
                </form>
              ) : (
                <form onSubmit={changePassword} className="mt-6 space-y-4">
                  <PasswordField id="setup-current-password" label="Mật khẩu tạm thời" value={oldPassword} onChange={setOldPassword} visible={showPasswords} autoComplete="current-password" />
                  <PasswordField id="setup-new-password" label="Mật khẩu mới" value={newPassword} onChange={setNewPassword} visible={showPasswords} autoComplete="new-password" />
                  <PasswordField id="setup-confirm-password" label="Xác nhận mật khẩu mới" value={confirmPassword} onChange={setConfirmPassword} visible={showPasswords} autoComplete="new-password" />
                  <button type="button" onClick={() => setShowPasswords((value) => !value)} className="inline-flex min-h-11 items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground">
                    {showPasswords ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                    {showPasswords ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  </button>
                  <Button type="submit" className="w-full" disabled={busyAction !== null}>
                    <KeyRound className="size-4" /> {busyAction === 'password' ? 'Đang cập nhật…' : 'Đổi mật khẩu và đăng nhập lại'}
                  </Button>
                </form>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </main>
  );
}

function SetupStep({ number, title, complete, active }: { number: string; title: string; complete: boolean; active: boolean }) {
  return (
    <div className="flex items-center gap-3">
      <span className={`grid size-9 shrink-0 place-items-center rounded-full text-sm font-bold ${complete || active ? 'bg-white text-primary' : 'bg-white/10 text-primary-foreground/60'}`}>
        {complete ? <Check className="size-4" /> : number}
      </span>
      <span className={complete || active ? 'font-semibold' : 'text-primary-foreground/60'}>{title}</span>
    </div>
  );
}

function PasswordField({ id, label, value, onChange, visible, autoComplete }: { id: string; label: string; value: string; onChange: (value: string) => void; visible: boolean; autoComplete: string }) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} type={visible ? 'text' : 'password'} autoComplete={autoComplete} value={value} onChange={(event) => onChange(event.target.value)} minLength={8} required />
    </div>
  );
}
