import { Form, ValidatedField } from '@/components/ui/form';
import { useState, type SubmitEvent } from 'react';
import { AlertCircle, CheckCircle2, Eye, EyeOff, KeyRound, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useModalFocusTrap } from '@/hooks/useModalFocusTrap';
import { PasswordRequirements, PasswordConfirmation } from './PasswordRequirements';
import { getPasswordError, passwordsMatch } from '@/lib/passwordPolicy';
import { useAuth } from '@/hooks/useAuth';
import { toast } from '@/stores/useToastStore';
import { authService } from '@/services/authService';

interface ChangePasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
  required?: boolean;
  onSuccess?: () => void;
}

function getErrorMessage(error: unknown) {
  return typeof error === 'object' && error !== null && 'message' in error && typeof error.message === 'string'
    ? error.message
    : 'Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu hiện tại.';
}

export function ChangePasswordModal({ isOpen, onClose, required = false, onSuccess }: ChangePasswordModalProps) {
  const { finishPasswordChange } = useAuth();
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const dialogRef = useModalFocusTrap(isOpen, onClose, isLoading || required);

  if (!isOpen) return null;

  const handleSubmit = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSuccess(false);

    const passwordError = getPasswordError(newPassword, oldPassword);
    if (passwordError) {
      setError(passwordError);
      return;
    }
    if (!passwordsMatch(newPassword, confirmPassword)) {
      setError('Xác nhận mật khẩu mới không trùng khớp.');
      return;
    }

    setIsLoading(true);
    try {
      await authService.changePassword({ oldPassword, newPassword });
      setSuccess(true);
      onSuccess?.();
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      toast.success('Hãy đăng nhập lại bằng mật khẩu mới.', 'Đổi mật khẩu thành công');
      finishPasswordChange();
    } catch (caughtError: unknown) {
      setError(getErrorMessage(caughtError));
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    if (isLoading || required) return;
    setError(null);
    setSuccess(false);
    setOldPassword(''); setNewPassword(''); setConfirmPassword('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 p-4 backdrop-blur-[2px]" role="presentation">
      {!required && <button type="button" className="absolute inset-0" onClick={handleClose} aria-label="Đóng hộp thoại đổi mật khẩu" />}
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="change-password-title"
        className="relative z-10 max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto rounded-3xl border border-border bg-popover p-5 text-popover-foreground shadow-[var(--shadow-primitive-md)] sm:p-6"
      >
        {!required && (
          <button
            type="button"
            onClick={handleClose}
            disabled={isLoading}
            className="absolute right-4 top-4 grid size-11 place-items-center rounded-xl text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-50"
            aria-label="Đóng"
          >
            <X className="size-5" />
          </button>
        )}

        <div className="pr-12">
          <span className="grid size-11 place-items-center rounded-2xl bg-accent text-primary">
            <KeyRound className="size-5" aria-hidden="true" />
          </span>
          <h2 id="change-password-title" className="mt-4 text-xl font-bold tracking-tight">
            {required ? 'Thiết lập mật khẩu riêng' : 'Đổi mật khẩu'}
          </h2>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">
            {required
              ? 'Tài khoản của bạn được tạo bởi nhà trường. Hãy đổi mật khẩu tạm thời trước khi tiếp tục.'
              : 'Sử dụng ít nhất 12 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt. Tránh dùng lại mật khẩu ở dịch vụ khác.'}
          </p>
        </div>

        {error && (
          <div role="alert" className="mt-5 flex items-start gap-3 rounded-2xl border border-destructive/20 bg-destructive/10 p-3.5 text-sm text-destructive">
            <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <span>{error}</span>
          </div>
        )}
        {success && (
          <div role="status" className="mt-5 flex items-start gap-3 rounded-2xl border border-success/20 bg-success-soft p-3.5 text-sm text-success">
            <CheckCircle2 className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <span>Mật khẩu đã được cập nhật. Hãy đăng nhập lại bằng mật khẩu mới.</span>
          </div>
        )}

        <Form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div className="space-y-2">
            <Label htmlFor="oldPassword">Mật khẩu hiện tại</Label>
            <div className="relative">
              <ValidatedField>
                <Input
                  id="oldPassword"
                  name="current-password"
                  type={showOldPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={oldPassword}
                  onChange={(event) => setOldPassword(event.target.value)}
                  className="pr-12"
                  required
                  data-autofocus
                />
              </ValidatedField>
              <button
                type="button"
                onClick={() => setShowOldPassword((current) => !current)}
                className="absolute right-0 top-0 grid size-11 place-items-center rounded-xl text-muted-foreground hover:text-foreground md:h-10"
                aria-label={showOldPassword ? 'Ẩn mật khẩu hiện tại' : 'Hiện mật khẩu hiện tại'}
                aria-pressed={showOldPassword}
              >
                {showOldPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="newPassword">Mật khẩu mới</Label>
            <div className="relative">
              <ValidatedField>
                <Input
                  id="newPassword"
                  name="new-password"
                  type={showNewPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  className="pr-12"
                  autoCapitalize="none" spellCheck={false} aria-describedby="new-password-requirements"
                  required
                />
              </ValidatedField>
              <button
                type="button"
                onClick={() => setShowNewPassword((current) => !current)}
                className="absolute right-0 top-0 grid size-11 place-items-center rounded-xl text-muted-foreground hover:text-foreground md:h-10"
                aria-label={showNewPassword ? 'Ẩn mật khẩu mới' : 'Hiện mật khẩu mới'}
                aria-pressed={showNewPassword}
              >
                {showNewPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
          </div>

          <div className="space-y-2">
            <PasswordRequirements id="new-password-requirements" password={newPassword} currentPassword={oldPassword} />
            <Label htmlFor="confirmPassword">Xác nhận mật khẩu mới</Label>
            <ValidatedField>
              <Input
                id="confirmPassword"
                name="confirm-password"
                aria-describedby="confirm-password-status" autoCapitalize="none" spellCheck={false}
                type="password"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                required
              />
            </ValidatedField>
          </div>

          <PasswordConfirmation id="confirm-password-status" password={newPassword} confirmation={confirmPassword} />
          <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
            {!required && <Button type="button" variant="outline" onClick={handleClose} disabled={isLoading}>Hủy</Button>}
            <Button type="submit" disabled={isLoading || success} aria-busy={isLoading}>
              {isLoading ? 'Đang cập nhật…' : success ? 'Đã cập nhật' : 'Cập nhật mật khẩu'}
            </Button>
          </div>
        </Form>
      </div>
    </div>
  );
}
