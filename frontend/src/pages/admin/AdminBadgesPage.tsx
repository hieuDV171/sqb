import { useState, type FormEvent } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Award, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from '@/components/ui/error-state';
import { EmptyState } from '@/components/ui/empty-state';
import { BadgeIcon } from '@/features/gamification/components/BadgesTab';
import { gamificationService } from '@/features/gamification/services/gamificationService';
import type { BadgeCriteria, BadgeResponseDto, BadgeTier, BadgeTriggerEvent } from '@/features/gamification/types/gamification.types';
import { toast } from '@/stores/useToastStore';

const tiers: Record<BadgeTier, string> = { BRONZE: 'Đồng', SILVER: 'Bạc', GOLD: 'Vàng', PLATINUM: 'Bạch kim', DIAMOND: 'Kim cương' };
const triggers: Record<BadgeTriggerEvent, string> = {
  APPROVED_QUESTIONS: 'Câu hỏi được duyệt', PROPOSED_QUESTIONS: 'Câu hỏi đã đề xuất', REVIEWED_SESSIONS: 'Phiên đã duyệt',
  ACCEPTED_FRIENDS: 'Kết bạn', PUBLIC_POINTS: 'Điểm công khai', STUDY_STREAK: 'Chuỗi học tập',
  LEADERBOARD_RANK: 'Thứ hạng', MANUAL_GRANT: 'Trao thủ công',
};
const selectStyle = 'min-h-11 w-full rounded-xl border border-input bg-background px-3 text-sm';
const adminKey = ['admin-badges'] as const;
type Action = { mode: 'edit' | 'grant' | 'disable'; badge: BadgeResponseDto } | { mode: 'create'; badge?: undefined };

export function AdminBadgesPage() {
  const [action, setAction] = useState<Action | null>(null);
  const badges = useQuery({ queryKey: adminKey, queryFn: () => gamificationService.getAdminBadges().then(({ data }) => data) });

  return (
    <div className="mx-auto max-w-5xl space-y-5 pb-10">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Quản lý huy hiệu</h1>
        <Button onClick={() => setAction({ mode: 'create' })}><Plus aria-hidden="true" /> Tạo huy hiệu</Button>
      </header>
      {badges.isPending ? <Skeleton className="h-40 rounded-2xl" aria-label="Đang tải huy hiệu" /> : badges.isError ? (
        <ErrorState title="Không thể tải huy hiệu" onRetry={() => void badges.refetch()} />
      ) : badges.data.length === 0 ? (
        <EmptyState title="Chưa có huy hiệu" icon={Award} />
      ) : (
        <ul className="space-y-2">
          {badges.data.map((badge) => <li key={badge.badgeId} className="flex flex-wrap items-center gap-3 rounded-2xl border border-border bg-card p-3 sm:p-4">
            <BadgeIcon badge={badge} />
            <div className="min-w-0 flex-1 basis-40">
              <h2 className="wrap-break-word font-semibold">{badge.name}</h2>
              <p className="wrap-break-word text-sm text-muted-foreground">{tiers[badge.badgeTier]} · {badge.active ? 'Đang hoạt động' : 'Đã vô hiệu hóa'} · {badge.totalEarnedUsers ?? 0} người đã đạt</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" onClick={() => setAction({ mode: 'edit', badge })}>Chỉnh sửa</Button>
              {badge.active && <>
                <Button variant="outline" onClick={() => setAction({ mode: 'grant', badge })}>Trao huy hiệu</Button>
                <Button variant="destructive" onClick={() => setAction({ mode: 'disable', badge })}>Vô hiệu hóa</Button>
              </>}
            </div>
          </li>)}
        </ul>
      )}
      {action && <AdminBadgeDialog action={action} onClose={() => setAction(null)} />}
    </div>
  );
}

function AdminBadgeDialog({ action, onClose }: { action: Action; onClose: () => void }) {
  const client = useQueryClient();
  const { mode, badge } = action;
  const [trigger, setTrigger] = useState<BadgeTriggerEvent>(badge?.badgeTriggerEvent ?? 'MANUAL_GRANT');
  const [bySubject, setBySubject] = useState(badge?.criteria?.type === 'SUBJECT_MASTERY');
  const [validationError, setValidationError] = useState('');
  const mutation = useMutation({
    mutationFn: async (form: FormData) => {
      if (mode === 'disable') return gamificationService.deleteBadge(badge!.badgeId);
      if (mode === 'grant') {
        const ids = String(form.get('users')).trim().split(/[\s,]+/).map(Number);
        const userIds = [...new Set(ids)];
        if (!userIds.length || userIds.length > 100 || userIds.some((id) => !Number.isSafeInteger(id) || id <= 0)) {
          throw new Error('Nhập từ 1 đến 100 ID tài khoản hợp lệ.');
        }
        return gamificationService.grantManualBadge(badge!.badgeId, { userIds, reason: String(form.get('reason')).trim() || undefined });
      }
      const field = (name: string) => String(form.get(name) ?? '').trim();
      const name = field('name');
      if (!name) throw new Error('Nhập tên huy hiệu.');
      let criteria: BadgeCriteria;
      const amount = Number(field('amount'));
      if (trigger !== 'MANUAL_GRANT' && (!Number.isSafeInteger(amount) || amount < 1)) throw new Error('Nhập ngưỡng lớn hơn 0.');
      if (trigger === 'APPROVED_QUESTIONS' && bySubject) {
        const subjectId = Number(field('subjectId'));
        if (!Number.isSafeInteger(subjectId) || subjectId <= 0) throw new Error('Nhập ID môn học hợp lệ.');
        criteria = { type: 'SUBJECT_MASTERY', subjectId, subjectCode: field('subjectCode') || undefined, approvedQuestionsCount: amount };
      } else if (trigger === 'STUDY_STREAK') criteria = { type: 'STREAK', streakDays: amount };
      else if (trigger === 'LEADERBOARD_RANK') criteria = { type: 'LEADERBOARD_RANK', targetRank: amount, period: field('period') };
      else if (trigger === 'MANUAL_GRANT') criteria = { type: 'MANUAL_GRANT', description: field('criteriaDescription') };
      else criteria = { type: 'COUNTER_THRESHOLD', threshold: amount, targetRole: field('role') };

      const data = { name, description: field('description'), badgeTier: field('tier') as BadgeTier,
        badgeTriggerEvent: trigger, iconUrl: field('icon'), criteria };
      return mode === 'edit' ? gamificationService.updateBadge(badge!.badgeId, { ...data, active: form.has('active') })
        : gamificationService.createBadge(data);
    },
    onSuccess: () => {
      void client.invalidateQueries({ predicate: ({ queryKey }) => ['admin-badges', 'gamification', 'badge-detail'].includes(String(queryKey[0])) });
      toast.success(mode === 'grant' ? 'Đã xử lý yêu cầu trao huy hiệu.' : mode === 'disable' ? 'Đã vô hiệu hóa huy hiệu.' : 'Đã lưu huy hiệu.');
      onClose();
    },
    onError: (error) => setValidationError(error.message || 'Không thể lưu. Hãy thử lại.'),
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setValidationError('');
    mutation.mutate(new FormData(event.currentTarget));
  }
  const original = trigger === badge?.badgeTriggerEvent ? badge?.criteria : undefined;
  const subjectCriteria = trigger === 'APPROVED_QUESTIONS' && bySubject;

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !mutation.isPending) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-lg -translate-y-1/2 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
          <Dialog.Title className="text-lg font-bold">{mode === 'create' ? 'Tạo huy hiệu' : mode === 'edit' ? 'Chỉnh sửa huy hiệu' : mode === 'grant' ? 'Trao huy hiệu' : 'Vô hiệu hóa huy hiệu?'}</Dialog.Title>
          {badge && <Dialog.Description className="my-2 text-sm text-muted-foreground">{badge.name}</Dialog.Description>}
          <form className="mt-3 space-y-3" onSubmit={submit}>
            <fieldset disabled={mutation.isPending} className="space-y-3">
              {(mode === 'create' || mode === 'edit') && <>
                <label className="block space-y-1 text-sm">Tên huy hiệu<Input name="name" defaultValue={badge?.name} maxLength={100} required /></label>
                <label className="block space-y-1 text-sm">Mô tả<Textarea name="description" defaultValue={badge?.description} maxLength={500} /></label>
                <div className="grid gap-3 sm:grid-cols-2">
                  <label className="block space-y-1 text-sm">Cấp huy hiệu<select name="tier" className={selectStyle} defaultValue={badge?.badgeTier ?? 'BRONZE'}>{Object.entries(tiers).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                  <label className="block space-y-1 text-sm">Sự kiện xét thưởng<select className={selectStyle} value={trigger} onChange={(event) => setTrigger(event.target.value as BadgeTriggerEvent)}>{Object.entries(triggers).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                </div>
                {trigger === 'APPROVED_QUESTIONS' && <label className="flex min-h-11 items-center gap-2 text-sm"><input type="checkbox" checked={bySubject} onChange={(event) => setBySubject(event.target.checked)} /> Giới hạn theo môn học</label>}
                {subjectCriteria && <div className="grid gap-3 sm:grid-cols-2">
                  <label className="block space-y-1 text-sm">ID môn học<Input name="subjectId" type="number" min={1} step={1} defaultValue={String(original?.subjectId ?? '')} required /></label>
                  <label className="block space-y-1 text-sm">Mã môn học<Input name="subjectCode" defaultValue={String(original?.subjectCode ?? '')} /></label>
                </div>}
                {trigger !== 'MANUAL_GRANT' && <label key={`${trigger}-${subjectCriteria}`} className="block space-y-1 text-sm">
                  {trigger === 'STUDY_STREAK' ? 'Số ngày liên tiếp' : trigger === 'LEADERBOARD_RANK' ? 'Thứ hạng tối đa' : 'Ngưỡng đạt'}
                  <Input name="amount" type="number" min={1} step={1} defaultValue={Number(original?.threshold ?? original?.streakDays ?? original?.targetRank ?? original?.approvedQuestionsCount ?? 1)} required />
                </label>}
                {trigger === 'MANUAL_GRANT' && <label className="block space-y-1 text-sm">Điều kiện trao<Input name="criteriaDescription" defaultValue={String(original?.description ?? '')} /></label>}
                {trigger === 'LEADERBOARD_RANK' && <label className="block space-y-1 text-sm">Phạm vi<select name="period" className={selectStyle} defaultValue={String(original?.period ?? 'SEMESTER')}><option value="SEMESTER">Học kỳ</option><option value="SUBJECT">Môn học</option></select></label>}
                {!subjectCriteria && trigger !== 'MANUAL_GRANT' && trigger !== 'STUDY_STREAK' && trigger !== 'LEADERBOARD_RANK' && <label className="block space-y-1 text-sm">Vai trò xét thưởng<select name="role" className={selectStyle} defaultValue={String(original?.targetRole ?? 'ALL')}><option value="ALL">Tất cả</option><option value="STUDENT">Sinh viên</option><option value="LECTURER">Giảng viên</option><option value="ADMIN">Admin</option></select></label>}
                <label className="block space-y-1 text-sm">URL biểu tượng<Input name="icon" defaultValue={badge?.iconUrl} /></label>
                {mode === 'edit' && <label className="flex min-h-11 items-center gap-2 text-sm"><input type="checkbox" name="active" defaultChecked={badge?.active} /> Đang hoạt động</label>}
              </>}
              {mode === 'grant' && <>
                <label className="block space-y-1 text-sm">ID tài khoản<Textarea name="users" required aria-describedby="badge-user-help" /></label>
                <p id="badge-user-help" className="text-sm text-muted-foreground">Phân cách bằng dấu phẩy, khoảng trắng hoặc xuống dòng.</p>
                <label className="block space-y-1 text-sm">Lý do trao<Textarea name="reason" maxLength={255} /></label>
              </>}
              {mode === 'disable' && <p className="text-sm text-muted-foreground">Người đã đạt vẫn giữ huy hiệu. Hệ thống dừng trao mới.</p>}
            </fieldset>
            {validationError && <p role="alert" className="text-sm text-destructive">{validationError}</p>}
            <div className="flex flex-wrap justify-end gap-2">
              <Button type="button" variant="outline" disabled={mutation.isPending} onClick={onClose}>Quay lại</Button>
              <Button type="submit" variant={mode === 'disable' ? 'destructive' : 'default'} disabled={mutation.isPending}>
                {mutation.isPending ? 'Đang lưu…' : mode === 'disable' ? 'Vô hiệu hóa' : mode === 'grant' ? 'Xác nhận trao' : 'Lưu huy hiệu'}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
