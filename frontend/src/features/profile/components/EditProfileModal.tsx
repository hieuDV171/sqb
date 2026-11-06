import { useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Camera, Loader2, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { mediaService } from '@/services/mediaService';
import { userService } from '@/services/userService';
import { useAuthStore } from '@/stores/useAuthStore';
import { toast } from '@/stores/useToastStore';
import type { Gender, ProfileResponse, UpdateProfileRequest } from '@/types/user.types';
import { AvatarWithFrame } from './AvatarWithFrame';

interface EditProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
  profile: ProfileResponse;
}

const fieldClass = 'w-full rounded-xl border border-input bg-background px-3 py-2 text-sm';

export function EditProfileModal({ isOpen, onClose, profile }: EditProfileModalProps) {
  const queryClient = useQueryClient();
  const updateUser = useAuthStore((state) => state.updateUser);
  const [fullName, setFullName] = useState(profile.fullName ?? '');
  const [bio, setBio] = useState(profile.bio ?? '');
  const [gender, setGender] = useState<Gender>(profile.gender ?? 'OTHER');
  const [dateOfBirth, setDateOfBirth] = useState(profile.dateOfBirth ?? '');
  const [timezone, setTimezone] = useState(profile.timezone ?? 'Asia/Ho_Chi_Minh');
  const [avatarUrl, setAvatarUrl] = useState<string>();
  const [coverUrl, setCoverUrl] = useState<string>();
  const [uploading, setUploading] = useState<'AVATAR' | 'COVER'>();
  const [error, setError] = useState<string>();
  const [nameInvalid, setNameInvalid] = useState(false);
  const avatarInput = useRef<HTMLInputElement>(null);
  const coverInput = useRef<HTMLInputElement>(null);
  const nameInput = useRef<HTMLInputElement>(null);

  const updateMutation = useMutation({
    mutationFn: async (payload: UpdateProfileRequest) => (await userService.updateMyProfile(payload)).data,
    onSuccess: (updated) => {
      updateUser({
        username: updated.fullName || updated.email,
        avatarUrl: updated.avatarUrl ?? undefined,
        coverUrl: updated.coverUrl ?? undefined,
        frameUrl: updated.frameUrl ?? undefined,
        profileCompleted: updated.profileCompleted,
      });
      queryClient.setQueryData(['userProfile', 'me'], updated);
      void queryClient.invalidateQueries({ queryKey: ['userProfile', profile.userId] });
      toast.success('Đã cập nhật hồ sơ');
      onClose();
    },
    onError: (failure) => setError(readError(failure, 'Không thể lưu hồ sơ. Hãy thử lại.')),
  });
  const busy = Boolean(uploading) || updateMutation.isPending;

  async function uploadImage(event: ChangeEvent<HTMLInputElement>, purpose: 'AVATAR' | 'COVER') {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    setError(undefined);
    const limit = purpose === 'AVATAR' ? 10 : 20;
    if (!file.type.startsWith('image/') || file.size > limit * 1024 * 1024) {
      setError(`Hãy chọn tệp hình ảnh tối đa ${limit} MB.`);
      return;
    }

    setUploading(purpose);
    try {
      const uploaded = await mediaService.uploadViaPresign(file, purpose);
      if (purpose === 'AVATAR') setAvatarUrl(uploaded.publicUrl);
      else setCoverUrl(uploaded.publicUrl);
    } catch (failure) {
      setError(readError(failure, 'Không thể tải ảnh lên. Hãy thử lại.'));
    } finally {
      setUploading(undefined);
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setError(undefined);
    const trimmedName = fullName.trim();
    if (!trimmedName) {
      setNameInvalid(true);
      nameInput.current?.focus();
      return;
    }
    updateMutation.mutate({
      fullName: trimmedName,
      bio: bio.trim(),
      gender,
      dateOfBirth: dateOfBirth || undefined,
      timezone: timezone.trim(),
      avatarUrl,
      coverUrl,
    });
  }

  const cover = coverUrl ?? profile.coverUrl;

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => { if (!open && !busy) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-foreground/45 dark:bg-black/65" />
        <Dialog.Popup initialFocus={nameInput}
          className="fixed left-1/2 top-1/2 z-50 flex max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-xl -translate-x-1/2 -translate-y-1/2 flex-col overflow-hidden rounded-2xl border border-border bg-card text-card-foreground shadow-xl">
          <div className="flex items-start justify-between gap-3 border-b border-border p-4 sm:px-6">
            <div>
              <Dialog.Title className="text-xl font-semibold">Chỉnh sửa hồ sơ</Dialog.Title>
              <Dialog.Description className="mt-1 text-sm text-muted-foreground">Cập nhật ảnh và thông tin hiển thị trên SQB.</Dialog.Description>
            </div>
            <Button size="icon" variant="ghost" aria-label="Đóng chỉnh sửa hồ sơ" disabled={busy} onClick={onClose}>
              <X aria-hidden="true" />
            </Button>
          </div>

          <form onSubmit={submit} aria-busy={busy} className="space-y-5 overflow-y-auto p-4 sm:p-6">
            <div className="relative h-28 overflow-hidden rounded-xl bg-accent sm:h-36">
              {cover && <img src={cover} alt="Xem trước ảnh bìa" className="size-full object-cover" />}
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <AvatarWithFrame avatarUrl={avatarUrl ?? profile.avatarUrl} frameUrl={profile.frameUrl} name={fullName} size="lg" />
              <Button variant="outline" type="button" disabled={busy} onClick={() => avatarInput.current?.click()}>
                <Camera aria-hidden="true" /> {uploading === 'AVATAR' ? 'Đang tải ảnh…' : 'Đổi ảnh đại diện'}
              </Button>
              <Button variant="outline" type="button" disabled={busy} onClick={() => coverInput.current?.click()}>
                <Camera aria-hidden="true" /> {uploading === 'COVER' ? 'Đang tải ảnh…' : 'Đổi ảnh bìa'}
              </Button>
              <input ref={avatarInput} type="file" accept="image/*" hidden onChange={(event) => void uploadImage(event, 'AVATAR')} />
              <input ref={coverInput} type="file" accept="image/*" hidden onChange={(event) => void uploadImage(event, 'COVER')} />
            </div>

            <div className="space-y-2">
              <Label htmlFor="profile-full-name">Họ và tên hiển thị</Label>
              <Input id="profile-full-name" ref={nameInput} value={fullName} maxLength={100} required
                aria-invalid={nameInvalid} aria-describedby={nameInvalid ? 'profile-name-error' : undefined}
                onChange={(event) => { setFullName(event.target.value); setNameInvalid(false); }} />
              {nameInvalid && <p id="profile-name-error" role="alert" className="text-sm text-destructive">Hãy nhập họ và tên.</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="profile-bio">Giới thiệu</Label>
              <textarea id="profile-bio" rows={3} maxLength={500} value={bio} aria-describedby="profile-bio-limit"
                onChange={(event) => setBio(event.target.value)} className={fieldClass} />
              <p id="profile-bio-limit" className="text-sm text-muted-foreground">{bio.length}/500 ký tự</p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="profile-gender">Giới tính</Label>
                <select id="profile-gender" value={gender} onChange={(event) => setGender(event.target.value as Gender)}
                  className={`${fieldClass} min-h-11`}>
                  <option value="MALE">Nam</option>
                  <option value="FEMALE">Nữ</option>
                  <option value="OTHER">Khác / Không công khai</option>
                </select>
              </div>
              <div className="min-w-0 space-y-2">
                <Label htmlFor="profile-birth-date">Ngày sinh</Label>
                <Input id="profile-birth-date" type="date" value={dateOfBirth}
                  onChange={(event) => setDateOfBirth(event.target.value)} />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="profile-timezone">Múi giờ hoạt động</Label>
              <Input id="profile-timezone" value={timezone} onChange={(event) => setTimezone(event.target.value)} />
            </div>
            {error && <p role="alert" className="rounded-xl bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}
            <div className="flex flex-wrap justify-end gap-2 border-t border-border pt-4">
              <Button type="button" variant="outline" onClick={onClose} disabled={busy}>Hủy</Button>
              <Button type="submit" disabled={busy}>
                {updateMutation.isPending && <Loader2 className="motion-safe:animate-spin" aria-hidden="true" />}
                {updateMutation.isPending ? 'Đang lưu…' : 'Lưu thay đổi'}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

function readError(error: unknown, fallback: string) {
  return typeof error === 'object' && error !== null && 'message' in error && typeof error.message === 'string'
    ? error.message : fallback;
}
