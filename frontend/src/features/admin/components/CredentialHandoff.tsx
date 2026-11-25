import { useState } from 'react';
import { Copy, Download, Eye, EyeOff, KeyRound, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ActionError } from '@/components/ui/action-error';
import { toast } from '@/stores/useToastStore';
import type { IssuedAccountCredential } from '@/types/auth.types';

function CredentialPassword({ value }: { value: string }) {
  const [visible, setVisible] = useState(false);
  const [copyError, setCopyError] = useState<string>();
  return <div className="flex flex-wrap items-center gap-1">
    <code className="min-w-40 whitespace-pre">{visible ? value : '••••••••••••••••'}</code>
    <Button type="button" variant="ghost" size="icon" aria-label={visible ? 'Ẩn mật khẩu tạm' : 'Hiện mật khẩu tạm'}
      aria-pressed={visible} onClick={() => setVisible(!visible)}>{visible ? <EyeOff /> : <Eye />}</Button>
    <Button type="button" variant="ghost" size="icon" aria-label="Sao chép mật khẩu tạm" onClick={async () => {
      setCopyError(undefined);
      try { await navigator.clipboard.writeText(value); toast.success('Đã sao chép mật khẩu tạm.'); }
      catch { setCopyError('Không thể sao chép. Hãy tải Excel hoặc hiện mật khẩu để sao chép.'); }
    }}><Copy /></Button>
    <ActionError className="basis-full" message={copyError} />
  </div>;
}

export function CredentialHandoff({ credentials, onClose }: {
  credentials: readonly IssuedAccountCredential[]; onClose: () => void;
}) {
  const [downloading, setDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string>();
  return <section aria-labelledby="credential-handoff-title" className="space-y-4 rounded-2xl border border-success/30 bg-card p-4 sm:p-5">
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h2 id="credential-handoff-title" className="flex items-center gap-2 text-lg font-semibold"><KeyRound className="size-5 text-success" aria-hidden="true" />
          Bàn giao {credentials.length} tài khoản thành công
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">Tải Excel trước khi đóng hoặc rời màn này. Mật khẩu tạm chỉ hiển thị trong lần thao tác này.</p>
      </div>
      <Button type="button" variant="ghost" size="icon" aria-label="Đóng danh sách mật khẩu tạm" onClick={onClose}><X /></Button>
    </div>
    <div className="max-h-96 overflow-auto rounded-xl border border-border" tabIndex={0} role="region" aria-label="Danh sách tài khoản và mật khẩu tạm">
      <table className="w-full text-left text-sm">
        <thead className="sticky top-0 bg-muted"><tr>{['Họ và tên', 'MSSV / MSGV', 'Email đăng nhập', 'Mật khẩu tạm'].map(label =>
          <th scope="col" key={label} className="whitespace-nowrap px-4 py-3 font-semibold">{label}</th>)}</tr></thead>
        <tbody>{credentials.map(item => <tr key={item.userId} className="border-t border-border">
          <td className="px-4 py-2">{item.fullName || '—'}</td><td className="px-4 py-2">{item.studentLecturerCode || '—'}</td>
          <td className="px-4 py-2">{item.email}</td><td className="px-4 py-2"><CredentialPassword value={item.temporaryPassword} /></td>
        </tr>)}</tbody>
      </table>
    </div>
    <div className="flex flex-wrap items-center justify-between gap-3">
      <p className="text-sm text-muted-foreground">Người dùng phải đổi mật khẩu tạm khi đăng nhập.</p>
      <Button type="button" disabled={downloading} onClick={async () => {
        setDownloadError(undefined);
        setDownloading(true);
        try { const { downloadCredentialWorkbook } = await import('../services/credentialWorkbook'); downloadCredentialWorkbook(credentials); }
        catch { setDownloadError('Không thể tạo Excel. Hãy thử lại.'); }
        finally { setDownloading(false); }
      }}><Download aria-hidden="true" />{downloading ? 'Đang tạo Excel…' : 'Tải Excel tài khoản và mật khẩu tạm'}</Button>
    </div>
    <ActionError message={downloadError} />
  </section>;
}
