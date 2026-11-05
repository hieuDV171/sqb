import { useState, type FormEvent } from 'react';
import { AlertTriangle, CheckCircle2, KeyRound, Mail, RefreshCw, ShieldCheck } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ChangePasswordModal } from '@/components/auth/ChangePasswordModal';
import { authService } from '@/services/authService';
import { useAuthStore } from '@/stores/useAuthStore';

function readError(error: unknown, fallback: string) {
  return typeof error === 'object' && error !== null && 'message' in error && typeof error.message === 'string'
    ? error.message
    : fallback;
}

export function AccountSettingsPage() {
  const { user, updateUser } = useAuthStore();
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);
  const [otpRequested, setOtpRequested] = useState(false);
  const [verifyCode, setVerifyCode] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const requestOtp = async () => {
    setIsSending(true);
    setError(null);
    setMessage(null);
    try {
      await authService.requestEmailVerification();
      setOtpRequested(true);
      setMessage('Mã OTP có hiệu lực trong 2 phút đã được gửi tới email của bạn.');
    } catch (caughtError) {
      setError(readError(caughtError, 'Không thể gửi mã xác thực. Vui lòng thử lại sau.'));
    } finally {
      setIsSending(false);
    }
  };

  const confirmOtp = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!/^\d{6}$/.test(verifyCode)) {
      setError('Vui lòng nhập đúng 6 chữ số trong email.');
      return;
    }

    setIsConfirming(true);
    setError(null);
    try {
      await authService.confirmEmailVerification(verifyCode);
      updateUser({ verified: true });
      setMessage('Email đã được xác thực thành công.');
      setOtpRequested(false);
      setVerifyCode('');
    } catch (caughtError) {
      setError(readError(caughtError, 'Mã OTP không đúng hoặc đã hết hạn.'));
    } finally {
      setIsConfirming(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <div className="flex items-center gap-2 text-sm font-semibold text-primary">
          <ShieldCheck className="size-4" aria-hidden="true" />
          Trung tâm tài khoản
        </div>
        <h1 className="mt-2 text-2xl font-bold tracking-tight sm:text-3xl">Cài đặt & bảo mật</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          Quản lý trạng thái email và thông tin đăng nhập cho tài khoản SQB của bạn.
        </p>
      </div>

      {!user?.verified ? (
        <section role="alert" className="rounded-3xl border border-destructive/30 bg-destructive/10 p-5 text-destructive shadow-[var(--shadow-primitive-sm)] sm:p-6">
          <div className="flex items-start gap-3">
            <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-destructive/15">
              <AlertTriangle className="size-5" aria-hidden="true" />
            </span>
            <div className="min-w-0 flex-1">
              <h2 className="font-bold">Email chưa được xác thực</h2>
              <p className="mt-1 text-sm leading-6 text-destructive/90">
                Hãy xác thực ngay để bảo vệ tài khoản và nhận đầy đủ thông báo từ nhà trường.
              </p>
              <p className="mt-2 break-all text-sm font-semibold">{user?.username}</p>
            </div>
          </div>

          {message && <p role="status" className="mt-4 rounded-2xl bg-card/80 p-3 text-sm text-foreground">{message}</p>}
          {error && <p role="alert" className="mt-4 rounded-2xl border border-destructive/20 bg-card/80 p-3 text-sm font-medium">{error}</p>}

          {!otpRequested ? (
            <Button className="mt-5" onClick={requestOtp} disabled={isSending}>
              {isSending ? <RefreshCw className="animate-spin" /> : <Mail />}
              {isSending ? 'Đang gửi mã…' : 'Xác thực email ngay'}
            </Button>
          ) : (
            <form onSubmit={confirmOtp} className="mt-5 max-w-sm space-y-3">
              <Label htmlFor="email-verification-code">Mã OTP gồm 6 chữ số</Label>
              <Input
                id="email-verification-code"
                value={verifyCode}
                onChange={(event) => setVerifyCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                inputMode="numeric"
                autoComplete="one-time-code"
                placeholder="000000"
                className="h-12 bg-card font-mono text-lg tracking-[0.35em]"
                maxLength={6}
                autoFocus
                required
              />
              <div className="flex flex-wrap gap-2">
                <Button type="submit" disabled={isConfirming || verifyCode.length !== 6}>
                  {isConfirming ? 'Đang xác nhận…' : 'Xác nhận mã'}
                </Button>
                <Button type="button" variant="outline" onClick={requestOtp} disabled={isSending}>
                  {isSending ? 'Đang gửi…' : 'Gửi lại mã'}
                </Button>
              </div>
            </form>
          )}
        </section>
      ) : (
        <section className="flex items-start gap-3 rounded-3xl border border-success/25 bg-success-soft p-5 text-success sm:p-6">
          <CheckCircle2 className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
          <div>
            <h2 className="font-bold">Email đã được xác thực</h2>
            <p className="mt-1 text-sm">Tài khoản của bạn đang sử dụng email đã xác minh.</p>
          </div>
        </section>
      )}

      <Card>
        <CardHeader>
          <div className="flex items-start justify-between gap-3">
            <div>
              <CardTitle className="flex items-center gap-2 text-lg"><KeyRound className="size-5 text-primary" /> Mật khẩu</CardTitle>
              <CardDescription className="mt-1">Đổi mật khẩu định kỳ và không dùng lại mật khẩu của dịch vụ khác.</CardDescription>
            </div>
            <Badge variant={user?.profileCompleted ? 'success' : 'warning'}>
              {user?.profileCompleted ? 'Đã thiết lập' : 'Cần cập nhật'}
            </Badge>
          </div>
        </CardHeader>
        <CardContent>
          <Button variant="outline" onClick={() => setIsChangePasswordOpen(true)}>Đổi mật khẩu</Button>
        </CardContent>
      </Card>

      <ChangePasswordModal
        isOpen={isChangePasswordOpen}
        onClose={() => setIsChangePasswordOpen(false)}
        onSuccess={() => updateUser({ profileCompleted: true })}
      />
    </div>
  );
}
