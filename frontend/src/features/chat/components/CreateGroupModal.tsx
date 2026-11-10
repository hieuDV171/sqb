import { useState, useEffect } from 'react';
import { useCreateGroupConversation } from '../hooks/useChat';
import { Dialog } from '@base-ui/react/dialog';
import { GroupCandidatePicker } from './GroupCandidatePicker';
import type { GroupCandidateDto } from '../types/chat.types';
import { mediaService } from '@/services/mediaService';
import {
  Users,
  X,
  Camera,
  Loader2,
} from 'lucide-react';
import { toast } from '@/stores/useToastStore';

interface CreateGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated?: (conversationId: number) => void;
}

export function CreateGroupModal({ isOpen, ...props }: CreateGroupModalProps) {
  return isOpen ? <CreateGroupForm {...props} /> : null;
}

function CreateGroupForm({ onClose, onCreated }: Omit<CreateGroupModalProps, 'isOpen'>) {
  const [name, setName] = useState('');
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string>('');
  const [selectedPeople, setSelectedPeople] = useState<GroupCandidateDto[]>([]);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);

  const createGroupMutation = useCreateGroupConversation();
  const directCount = selectedPeople.filter((person) => person.friend).length;
  const invitationCount = selectedPeople.length - directCount;

  useEffect(() => {
    return () => { if (avatarPreview) URL.revokeObjectURL(avatarPreview); };
  }, [avatarPreview]);

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      toast.warning('Chỉ hỗ trợ tệp hình ảnh');
      return;
    }
    setAvatarFile(file);
    setAvatarPreview(URL.createObjectURL(file));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedName = name.trim();
    if (!trimmedName) {
      toast.warning('Vui lòng nhập tên nhóm');
      return;
    }

    if (selectedPeople.length < 2) {
      toast.warning('Vui lòng chọn ít nhất 2 người để tạo nhóm');
      return;
    }

    try {
      let finalAvatarUrl: string | undefined;
      if (avatarFile) {
        setIsUploadingAvatar(true);
        const uploadRes = await mediaService.uploadViaPresign(avatarFile, 'AVATAR');
        finalAvatarUrl = uploadRes.publicUrl;
      }

      createGroupMutation.mutate(
        {
          name: trimmedName,
          avatarUrl: finalAvatarUrl || undefined,
          memberIds: selectedPeople.map((person) => person.userId),
        },
        {
          onSuccess: (res) => {
            onCreated?.(res.data.conversationId);
            onClose();
          },
        }
      );
    } catch {
      toast.error('Có lỗi xảy ra khi tải ảnh đại diện nhóm');
    } finally {
      setIsUploadingAvatar(false);
    }
  };

  const isSubmitting = createGroupMutation.isPending || isUploadingAvatar;

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !isSubmitting) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-[70] bg-black/55" />
        <Dialog.Popup className="fixed top-1/2 left-1/2 z-[71] flex max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 flex-col rounded-3xl border border-border bg-card p-5 text-foreground shadow-xl sm:p-6">
          {/* Header */}
          <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 shrink-0">
            <div className="flex items-center gap-2.5">
              <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
                <Users className="w-5 h-5" />
              </div>
              <div>
                <Dialog.Title className="text-lg font-bold">
                  Tạo nhóm trò chuyện
                </Dialog.Title>
                <Dialog.Description className="text-sm text-muted-foreground">
                  Chọn ít nhất 2 người để thêm hoặc mời vào nhóm.
                </Dialog.Description>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              aria-label="Đóng"
              disabled={isSubmitting}
              className="p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form Body */}
          <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto py-4 space-y-4">
            {/* Avatar & Group Name */}
            <div className="flex items-center gap-3">
              <label className="relative w-16 h-16 rounded-2xl bg-slate-100 dark:bg-slate-800 border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-indigo-500 flex flex-col items-center justify-center cursor-pointer overflow-hidden shrink-0 group transition-colors">
                {avatarPreview ? (
                  <img src={avatarPreview} alt="Group Avatar" className="w-full h-full object-cover" />
                ) : (
                  <Camera className="w-6 h-6 text-slate-400 group-hover:text-indigo-500 transition-colors" />
                )}
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={handleAvatarChange}
                />
              </label>

              <div className="flex-1">
                <label htmlFor="new-group-name" className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                  Tên nhóm <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  id="new-group-name"
                  required
                  maxLength={100}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Ví dụ: Hội ôn thi Giải tích 1"
                  className="w-full px-3.5 py-2.5 text-sm bg-slate-100 dark:bg-slate-800/80 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
                />
              </div>
            </div>

            <GroupCandidatePicker selected={selectedPeople} onSelectionChange={setSelectedPeople} disabled={isSubmitting} />

            {/* Submit Button */}
            <div className="space-y-2 pt-2">
              <p role="status" className="rounded-xl border border-border bg-surface-subtle px-3 py-2 text-sm text-muted-foreground empty:sr-only">
                {selectedPeople.length > 0 && `${directCount} người được thêm ngay · ${invitationCount} người sẽ nhận lời mời`}
              </p>
              <button
                type="submit"
                disabled={isSubmitting || !name.trim() || selectedPeople.length < 2}
                className="w-full py-2.5 rounded-xl bg-linear-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white font-semibold text-sm shadow-md transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {isSubmitting && <Loader2 className="w-4 h-4 animate-spin" />}
                Tạo nhóm ngay
              </button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
