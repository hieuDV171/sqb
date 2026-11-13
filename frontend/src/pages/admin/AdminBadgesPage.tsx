import { useState, type FormEvent } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Award, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { FilterSelect } from '@/components/ui/filter-select';
import { Textarea } from '@/components/ui/textarea';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from '@/components/ui/error-state';
import { EmptyState } from '@/components/ui/empty-state';
import { BadgeIcon } from '@/features/gamification/components/BadgesTab';
import { BadgeAccountPicker } from '@/features/gamification/components/BadgeAccountPicker';
import { gamificationService } from '@/features/gamification/services/gamificationService';
import { badgeTierLabels, type BadgeAccount, type BadgeCriteria, type BadgeResponseDto, type BadgeTargetRole, type BadgeTier, type BadgeTriggerEvent } from '@/features/gamification/types/gamification.types';
import { adminService } from '@/services/adminService';
import { mediaService } from '@/services/mediaService';
import { toast } from '@/stores/useToastStore';

const triggers: Record<BadgeTriggerEvent, { label: string; amountLabel?: string }> = {
  APPROVED_QUESTIONS: { label: 'Câu hỏi được duyệt', amountLabel: 'Số câu được duyệt tối thiểu' },
  PROPOSED_QUESTIONS: { label: 'Câu hỏi đã đề xuất', amountLabel: 'Số câu đã đề xuất tối thiểu' },
  REVIEWED_SESSIONS: { label: 'Hoàn tất duyệt phiên', amountLabel: 'Số phiên đã duyệt xong tối thiểu' },
  ACCEPTED_FRIENDS: { label: 'Kết bạn', amountLabel: 'Số bạn bè tối thiểu' },
  PUBLIC_POINTS: { label: 'Điểm công khai', amountLabel: 'Điểm công khai tối thiểu' },
  STUDY_STREAK: { label: 'Chuỗi học tập', amountLabel: 'Số ngày học liên tiếp tối thiểu' },
  LEADERBOARD_RANK: { label: 'Thứ hạng cuối học kỳ', amountLabel: 'Thứ hạng tối đa (1–10)' },
  MANUAL_GRANT: { label: 'Trao thủ công' },
};
const tierOptions = Object.entries(badgeTierLabels).map(([value, label]) => ({ value, label }));
const triggerOptions = Object.entries(triggers).map(([value, { label }]) => ({ value, label }));
const roleOptions = [
  { value: 'ALL', label: 'Tất cả' }, { value: 'STUDENT', label: 'Sinh viên' },
  { value: 'LECTURER', label: 'Giảng viên' }, { value: 'ADMIN', label: 'Admin' },
];
const adminKey = ['admin-badges'] as const;
type Action = { mode: 'edit' | 'grant' | 'revoke' | 'disable' | 'enable'; badge: BadgeResponseDto } | { mode: 'create'; badge?: undefined };
const titles = { create: 'Tạo huy hiệu', edit: 'Chỉnh sửa huy hiệu', grant: 'Trao huy hiệu', revoke: 'Thu hồi huy hiệu', disable: 'Vô hiệu hóa huy hiệu?', enable: 'Kích hoạt lại huy hiệu?' };
const submitLabels = { create: 'Lưu huy hiệu', edit: 'Lưu huy hiệu', grant: 'Xác nhận trao', revoke: 'Xác nhận thu hồi', disable: 'Vô hiệu hóa', enable: 'Kích hoạt lại' };
const successMessages = { create: 'Đã tạo huy hiệu.', edit: 'Đã lưu huy hiệu.', grant: 'Đã xử lý yêu cầu trao huy hiệu.', revoke: 'Đã thu hồi huy hiệu.', disable: 'Đã vô hiệu hóa huy hiệu.', enable: 'Đã kích hoạt lại huy hiệu.' };

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
              <p className="wrap-break-word text-sm text-muted-foreground">{badgeTierLabels[badge.badgeTier]} · {badge.active ? 'Đang hoạt động' : 'Đã vô hiệu hóa'} · {badge.totalEarnedUsers ?? 0} người đã đạt</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" onClick={() => setAction({ mode: 'edit', badge })}>Chỉnh sửa</Button>
              {(badge.totalEarnedUsers ?? 0) > 0 && <Button variant="outline" onClick={() => setAction({ mode: 'revoke', badge })}>Thu hồi</Button>}
              {badge.active ? <>
                <Button variant="outline" onClick={() => setAction({ mode: 'grant', badge })}>Trao huy hiệu</Button>
                <Button variant="destructive" onClick={() => setAction({ mode: 'disable', badge })}>Vô hiệu hóa</Button>
              </> : <Button variant="outline" onClick={() => setAction({ mode: 'enable', badge })}>Kích hoạt lại</Button>}
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
  const [tier, setTier] = useState<BadgeTier>(badge?.badgeTier ?? 'BRONZE');
  const [trigger, setTrigger] = useState<BadgeTriggerEvent>(badge?.badgeTriggerEvent ?? 'MANUAL_GRANT');
  const savedCriteria = badge?.criteria;
  const savedSubject = savedCriteria?.type === 'SUBJECT_MASTERY' ? savedCriteria : undefined;
  const [bySubject, setBySubject] = useState(!!savedSubject);
  const [subjectId, setSubjectId] = useState(String(savedSubject?.subjectId ?? ''));
  const [role, setRole] = useState<BadgeTargetRole>(savedCriteria?.type === 'COUNTER_THRESHOLD' ? savedCriteria.targetRole ?? 'ALL' : 'ALL');
  const [rankPeriod, setRankPeriod] = useState(savedCriteria?.type === 'LEADERBOARD_RANK' && savedCriteria.period === 'SUBJECT' ? '' : 'SEMESTER');
  const [iconUrl, setIconUrl] = useState(badge?.iconUrl ?? '');
  const [selected, setSelected] = useState<BadgeAccount[]>([]);
  const [validationError, setValidationError] = useState('');
  const subjectCriteria = trigger === 'APPROVED_QUESTIONS' && bySubject;
  const { amountLabel } = triggers[trigger];
  const isRank = trigger === 'LEADERBOARD_RANK';
  const isCounter = !!amountLabel && !isRank && trigger !== 'STUDY_STREAK' && !subjectCriteria;
  const editing = mode === 'edit' || mode === 'create';
  const assigning = mode === 'grant' || mode === 'revoke';
  const subjects = useQuery({ queryKey: ['subjects', 'admin'], enabled: editing && subjectCriteria,
    queryFn: () => adminService.getAllSubjects().then(({ data }) => data) });
  const upload = useMutation({
    mutationFn: (file: File) => mediaService.uploadMedia(file, 'BADGE_ICON'),
    onSuccess: ({ url }) => setIconUrl(url),
    onError: (error) => setValidationError(error.message || 'Không thể tải ảnh.'),
  });
  const mutation = useMutation({
    mutationFn: async (form: FormData) => {
      if (mode === 'disable') return gamificationService.deleteBadge(badge!.badgeId);
      if (mode === 'enable') return gamificationService.updateBadge(badge!.badgeId, { active: true });
      if (assigning) {
        const userIds = selected.map((account) => account.userId);
        if (!userIds.length) throw new Error('Hãy chọn ít nhất một tài khoản.');
        if (mode === 'revoke') return gamificationService.revokeBadge(badge!.badgeId, userIds);
        return gamificationService.grantManualBadge(badge!.badgeId, { userIds, reason: String(form.get('reason')).trim() || undefined });
      }
      const field = (name: string) => String(form.get(name) ?? '').trim();
      const name = field('name');
      if (!name) throw new Error('Nhập tên huy hiệu.');
      let criteria: BadgeCriteria;
      const amount = Number(field('amount'));
      if (amountLabel && (!Number.isInteger(amount) || amount < 1 || amount > 2_147_483_647)) throw new Error('Nhập ngưỡng là số nguyên dương hợp lệ.');
      if (subjectCriteria) {
        const subject = subjects.data?.find((item) => String(item.subjectId) === subjectId);
        if (!subject) throw new Error('Hãy chọn môn học.');
        criteria = { type: 'SUBJECT_MASTERY', subjectId: subject.subjectId, subjectCode: subject.code, approvedQuestionsCount: amount };
      } else if (trigger === 'STUDY_STREAK') criteria = { type: 'STREAK', streakDays: amount };
      else if (isRank) {
        if (rankPeriod !== 'SEMESTER') throw new Error('Hãy chọn phạm vi học kỳ.');
        if (amount > 10) throw new Error('Nhập thứ hạng từ 1 đến 10.');
        criteria = { type: 'LEADERBOARD_RANK', targetRank: amount, period: 'SEMESTER' };
      }
      else if (trigger === 'MANUAL_GRANT') criteria = { type: 'MANUAL_GRANT', description: field('criteriaDescription') };
      else criteria = { type: 'COUNTER_THRESHOLD', threshold: amount, targetRole: role };

      const data = { name, description: field('description'), badgeTier: tier,
        badgeTriggerEvent: trigger, iconUrl, criteria };
      return mode === 'edit' ? gamificationService.updateBadge(badge!.badgeId, { ...data, active: form.has('active') })
        : gamificationService.createBadge(data);
    },
    onSuccess: () => {
      void client.invalidateQueries({ predicate: ({ queryKey }) => ['admin-badges', 'admin-badge-accounts', 'gamification', 'badge-detail', 'userProfile', 'activity-feeds'].includes(String(queryKey[0])) });
      toast.success(successMessages[mode]);
      onClose();
    },
    onError: (error) => setValidationError(error.message || 'Không thể lưu. Hãy thử lại.'),
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (mutation.isPending || upload.isPending) return;
    setValidationError('');
    mutation.mutate(new FormData(event.currentTarget));
  }
  const original = trigger === badge?.badgeTriggerEvent ? savedCriteria : undefined;
  const defaultAmount = subjectCriteria && original?.type === 'SUBJECT_MASTERY' ? original.approvedQuestionsCount
    : isCounter && original?.type === 'COUNTER_THRESHOLD' ? original.threshold
    : isRank && original?.type === 'LEADERBOARD_RANK' ? original.targetRank
    : trigger === 'STUDY_STREAK' && original?.type === 'STREAK' ? original.streakDays : undefined;
  const busy = mutation.isPending || upload.isPending;

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !busy) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-lg -translate-y-1/2 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
          <Dialog.Title className="text-lg font-bold">{titles[mode]}</Dialog.Title>
          {badge && <Dialog.Description className="my-2 text-sm text-muted-foreground">{badge.name}</Dialog.Description>}
          <form className="mt-3 space-y-3" onSubmit={submit}>
            <fieldset disabled={busy} className="space-y-3">
              {editing && <>
                <label className="block space-y-1 text-sm">Tên huy hiệu<Input name="name" defaultValue={badge?.name} maxLength={100} required /></label>
                <label className="block space-y-1 text-sm">Mô tả<Textarea name="description" defaultValue={badge?.description} maxLength={500} /></label>
                <div className="grid gap-3 sm:grid-cols-2">
                  <FilterSelect label="Cấp huy hiệu" value={tier} options={tierOptions} onChange={(value) => setTier(value as BadgeTier)} disabled={busy} />
                  <FilterSelect label="Sự kiện xét thưởng" value={trigger} options={triggerOptions} onChange={(value) => setTrigger(value as BadgeTriggerEvent)} disabled={busy} />
                </div>
                <fieldset className="space-y-3 rounded-xl border border-border px-3 pb-3">
                  <legend className="px-1 text-sm font-medium">{amountLabel ? 'Tiêu chí xét tự động' : 'Trao thủ công'}</legend>
                  <p className="text-sm text-muted-foreground">
                    {amountLabel ? 'Áp dụng tiêu chí đã lưu từ sự kiện xét thưởng tiếp theo.' : 'Admin chọn người nhận ở mục Trao huy hiệu sau khi lưu.'}
                  </p>
                  {amountLabel && <p className="text-sm text-muted-foreground">Để hỗ trợ một loại điều kiện mới, cần bổ sung logic đánh giá trực tiếp ở backend.</p>}
                  {trigger === 'APPROVED_QUESTIONS' && <label className="flex min-h-11 items-center gap-2 text-sm"><input type="checkbox" checked={bySubject} onChange={(event) => setBySubject(event.target.checked)} /> Giới hạn theo môn học</label>}
                  {subjectCriteria && <div className="space-y-2">
                    <FilterSelect label="Môn học" value={subjectId} options={[
                      { value: '', label: subjects.isPending ? 'Đang tải môn học…' : 'Chọn môn học' },
                      ...(subjects.data ?? []).map((subject) => ({ value: String(subject.subjectId), label: `${subject.code} · ${subject.name}` })),
                    ]} onChange={setSubjectId} required disabled={busy || !subjects.data?.length} />
                    {subjects.isError && <Button type="button" size="sm" variant="outline" onClick={() => void subjects.refetch()}>Tải lại danh sách môn học</Button>}
                    {subjects.data?.length === 0 && <p className="text-sm text-muted-foreground">Chưa có môn học để chọn.</p>}
                  </div>}
                  {isRank && <FilterSelect label="Phạm vi xếp hạng" value={rankPeriod} options={[
                    ...(rankPeriod ? [] : [{ value: '', label: 'Chọn phạm vi được hỗ trợ' }]),
                    { value: 'SEMESTER', label: 'Toàn trường theo học kỳ' },
                  ]} onChange={setRankPeriod} required disabled={busy} />}
                  {amountLabel && <label key={`${trigger}-${subjectCriteria}`} className="block space-y-1 text-sm">{amountLabel}
                    <Input name="amount" type="number" min={1} max={isRank ? 10 : 2_147_483_647} step={1} defaultValue={defaultAmount} required />
                  </label>}
                  {isCounter && <FilterSelect label="Vai trò xét thưởng" value={role} options={roleOptions} onChange={(value) => setRole(value as BadgeTargetRole)} disabled={busy} />}
                  {trigger === 'MANUAL_GRANT' && <label className="block space-y-1 text-sm">Ghi chú trao thủ công (không bắt buộc)
                    <Input name="criteriaDescription" defaultValue={original?.type === 'MANUAL_GRANT' ? original.description : ''} />
                  </label>}
                </fieldset>
                <div className="space-y-2">
                  <label className="block space-y-1 text-sm">Ảnh biểu tượng
                    <Input type="file" accept="image/png,image/jpeg,image/gif,image/webp" onChange={(event) => {
                      const file = event.target.files?.[0];
                      event.target.value = '';
                      if (!file) return;
                      setValidationError('');
                      upload.mutate(file);
                    }} />
                  </label>
                  {upload.isPending && <p role="status" className="text-sm text-muted-foreground">Đang tải ảnh…</p>}
                  {iconUrl && <div className="flex items-center gap-2">
                    <BadgeIcon badge={{ iconUrl, badgeId: 0, name: '', description: '', badgeTier: tier }} />
                    <Button type="button" size="sm" variant="outline" onClick={() => setIconUrl('')}>Gỡ ảnh</Button>
                  </div>}
                </div>
                {mode === 'edit' && <label className="flex min-h-11 items-center gap-2 text-sm"><input type="checkbox" name="active" defaultChecked={badge?.active} /> Đang hoạt động</label>}
              </>}
              {assigning && badge && <>
                <BadgeAccountPicker badgeId={badge.badgeId} holders={mode === 'revoke'} selected={selected} onChange={setSelected} />
                {mode === 'grant' ? <label className="block space-y-1 text-sm">Lý do trao<Textarea name="reason" maxLength={255} /></label>
                  : <p className="text-sm text-muted-foreground">Chỉ Admin mới có thể trao lại huy hiệu đã thu hồi.</p>}
              </>}
              {mode === 'disable' && <p className="text-sm text-muted-foreground">Người đã đạt vẫn giữ huy hiệu. Hệ thống dừng trao mới.</p>}
              {mode === 'enable' && <p className="text-sm text-muted-foreground">Hệ thống tiếp tục xét và trao huy hiệu theo tiêu chí đã lưu.</p>}
            </fieldset>
            {validationError && <p role="alert" className="text-sm text-destructive">{validationError}</p>}
            <div className="flex flex-wrap justify-end gap-2">
              <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Quay lại</Button>
              <Button type="submit" variant={mode === 'disable' || mode === 'revoke' ? 'destructive' : 'default'}
                disabled={busy || (assigning && !selected.length) || (editing && subjectCriteria && !subjects.data?.length)}>
                {mutation.isPending ? 'Đang lưu…' : submitLabels[mode]}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
