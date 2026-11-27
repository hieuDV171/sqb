import { Check, Circle } from 'lucide-react';
import { getPasswordRules, normalizePassword, passwordsMatch } from '@/lib/passwordPolicy';

export function PasswordRequirements({ password, currentPassword, id }: {
  password: string; currentPassword?: string; id?: string;
}) {
  const rules = getPasswordRules(password);
  if (currentPassword !== undefined) rules.push({ id: 'different', label: 'Khác mật khẩu hiện tại',
    met: normalizePassword(password) !== normalizePassword(currentPassword) });
  const count = password ? rules.filter(rule => rule.met).length : 0;
  return (
    <div id={id} className="space-y-2 text-sm">
      <p className="text-muted-foreground" role="status" aria-live="polite" aria-atomic="true">
        Yêu cầu mật khẩu · {count}/{rules.length} điều kiện
      </p>
      <ul className="grid gap-1.5 sm:grid-cols-2">
        {rules.map(rule => {
          const met = Boolean(password) && rule.met;
          const Icon = met ? Check : Circle;
          return <li key={rule.id} className={`flex items-start gap-2 ${met ? 'text-success' : 'text-muted-foreground'}`}>
            <Icon aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
            <span><span className="sr-only">{met ? 'Đã đạt: ' : 'Chưa đạt: '}</span>{rule.label}</span>
          </li>;
        })}
      </ul>
    </div>
  );
}

export function PasswordConfirmation({ password, confirmation, id }: {
  password: string; confirmation: string; id?: string;
}) {
  if (!confirmation) return null;
  const match = Boolean(password) && passwordsMatch(password, confirmation);
  const Icon = match ? Check : Circle;
  return <p id={id} role="status" aria-live="polite"
    className={`flex items-center gap-2 text-sm ${match ? 'text-success' : 'text-muted-foreground'}`}>
    <Icon aria-hidden="true" className="size-4 shrink-0" />
    {match ? 'Mật khẩu xác nhận khớp' : 'Mật khẩu xác nhận chưa khớp'}
  </p>;
}
