import { useState } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { Bell, BookOpen, Loader2, Moon, Shield, Trophy, Users, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
import { usePushSettings, useUpdatePushSettings } from '../hooks/useNotification';
import type { PushSettingsResponseDto } from '../types/notification.types';

const categoryOptions = [
  { key: 'ACADEMIC', label: 'Học thuật', description: 'Kết quả xét duyệt và phản hồi về câu hỏi.', icon: BookOpen },
  { key: 'SOCIAL', label: 'Bạn bè và tương tác', description: 'Lời mời kết bạn và bình luận.', icon: Users },
  { key: 'GAMIFICATION', label: 'Thành tích', description: 'Huy hiệu và kết quả bảng xếp hạng.', icon: Trophy },
  { key: 'SYSTEM', label: 'Hệ thống', description: 'Cập nhật về tài khoản và hệ thống.', icon: Shield },
] as const;

function formatTime(hour: number, minute: number) {
  return String(hour).padStart(2, '0') + ':' + String(minute).padStart(2, '0');
}

function PushSettingsForm({ settings, onClose }: { settings: PushSettingsResponseDto; onClose: () => void }) {
  const [categories, setCategories] = useState(settings.categories);
  const [quietEnabled, setQuietEnabled] = useState(settings.quietHours.enabled);
  const [start, setStart] = useState(formatTime(settings.quietHours.startHour, settings.quietHours.startMinute));
  const [end, setEnd] = useState(formatTime(settings.quietHours.endHour, settings.quietHours.endMinute));
  const update = useUpdatePushSettings();

  return (
    <form className="flex min-h-0 flex-1 flex-col" onSubmit={(event) => {
      event.preventDefault();
      const [startHour, startMinute] = (start || formatTime(settings.quietHours.startHour, settings.quietHours.startMinute)).split(':').map(Number);
      const [endHour, endMinute] = (end || formatTime(settings.quietHours.endHour, settings.quietHours.endMinute)).split(':').map(Number);
      update.mutate({ categories, quietHours: { enabled: quietEnabled, startHour, startMinute, endHour, endMinute } },
        { onSuccess: onClose });
    }}>
      <div className="min-h-0 flex-1 space-y-4 overflow-y-auto overscroll-contain p-4">
        <fieldset className="space-y-2">
          <legend className="mb-2 text-sm font-semibold">Danh mục thông báo</legend>
          {categoryOptions.map(({ key, label, description, icon: Icon }) => (
            <label key={key} className="flex min-h-14 cursor-pointer items-center gap-3 rounded-xl border border-border bg-card p-3 hover:bg-muted">
              <Icon aria-hidden="true" className="size-5 shrink-0 text-academic" />
              <span className="min-w-0 flex-1">
                <span className="block text-sm font-semibold">{label}</span>
                <span className="block text-sm text-muted-foreground">{description}</span>
              </span>
              <input type="checkbox" className="size-5 shrink-0 accent-primary" checked={categories[key]?.enabled ?? true}
                aria-label={label}
                onChange={(event) => {
                  const enabled = event.target.checked;
                  setCategories((current) => ({ ...current, [key]: { ...current[key], enabled } }));
                }} />
            </label>
          ))}
        </fieldset>
        <fieldset className="space-y-3 rounded-xl border border-border p-3">
          <legend className="px-1 text-sm font-semibold">Giờ yên tĩnh</legend>
          <label className="flex min-h-11 cursor-pointer items-center gap-3 text-sm">
            <Moon aria-hidden="true" className="size-5 shrink-0 text-warning" />
            <span className="flex-1">Tạm dừng thông báo đẩy trong khung giờ đã chọn</span>
            <input type="checkbox" className="size-5 shrink-0 accent-primary" checked={quietEnabled}
              onChange={(event) => setQuietEnabled(event.target.checked)} />
          </label>
          {quietEnabled && (
            <div className="grid gap-2 sm:grid-cols-2">
              <label className="space-y-1 text-sm">Từ
                <input type="time" required value={start} onChange={(event) => setStart(event.target.value)}
                  className="block min-h-11 w-full rounded-lg border border-input bg-background px-2" />
              </label>
              <label className="space-y-1 text-sm">Đến
                <input type="time" required value={end} onChange={(event) => setEnd(event.target.value)}
                  className="block min-h-11 w-full rounded-lg border border-input bg-background px-2" />
              </label>
            </div>
          )}
        </fieldset>
      </div>
      <footer className="flex shrink-0 justify-end gap-2 border-t border-border p-4">
        <Dialog.Close render={<Button variant="ghost" className="min-h-11" />}>Hủy</Dialog.Close>
        <Button type="submit" className="min-h-11" disabled={update.isPending}>
          {update.isPending && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
          {update.isPending ? 'Đang lưu…' : 'Lưu cài đặt'}
        </Button>
      </footer>
    </form>
  );
}

export function PushSettingsModal({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
  const settings = usePushSettings(isOpen);
  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-[70] bg-black/55" />
        <Dialog.Popup className="fixed top-1/2 left-1/2 z-[71] flex max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 flex-col overflow-hidden rounded-2xl border border-border bg-card text-foreground shadow-xl">
          <header className="flex shrink-0 items-center gap-3 border-b border-border p-4">
            <Bell aria-hidden="true" className="size-5 shrink-0 text-academic" />
            <div className="min-w-0 flex-1">
              <Dialog.Title className="text-lg font-semibold">Cài đặt thông báo</Dialog.Title>
              <Dialog.Description className="text-sm text-muted-foreground">Chọn loại thông báo và khung giờ bạn muốn nhận.</Dialog.Description>
            </div>
            <Dialog.Close render={<Button variant="ghost" size="icon" className="size-11" />} aria-label="Đóng cài đặt"><X aria-hidden="true" /></Dialog.Close>
          </header>
          {settings.isLoading ? (
            <p role="status" className="p-6 text-center text-sm text-muted-foreground">Đang tải cài đặt…</p>
          ) : settings.data ? (
            <PushSettingsForm settings={settings.data} onClose={onClose} />
          ) : (
            <div className="p-4"><ErrorState title="Không thể tải cài đặt" onRetry={() => void settings.refetch()} /></div>
          )}
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
