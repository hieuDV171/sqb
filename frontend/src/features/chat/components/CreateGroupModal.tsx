import { useState, useEffect } from 'react';
import { useCreateGroupConversation } from '../hooks/useChat';
import { userService } from '@/services/userService';
import { mediaService } from '@/services/mediaService';
import {
  Users,
  Search,
  X,
  Camera,
  Check,
  Loader2,
} from 'lucide-react';
import { toast } from '@/stores/useToastStore';
import type { FriendDto } from '@/types/user.types';

interface CreateGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated?: (conversationId: number) => void;
}

export function CreateGroupModal({
  isOpen,
  onClose,
  onCreated,
}: CreateGroupModalProps) {
  const [name, setName] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string>('');
  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [friends, setFriends] = useState<FriendDto[]>([]);
  const [isLoadingFriends, setIsLoadingFriends] = useState(false);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);

  const createGroupMutation = useCreateGroupConversation();

  useEffect(() => {
    if (isOpen) {
      setName('');
      setAvatarUrl('');
      setAvatarFile(null);
      setAvatarPreview('');
      setSelectedUserIds([]);
      setSearchTerm('');

      // Tải danh sách bạn bè để chọn
      setIsLoadingFriends(true);
      userService
        .getMyFriends(undefined, 50)
        .then((res) => {
          if (res?.data?.items) {
            setFriends(res.data.items);
          }
        })
        .finally(() => setIsLoadingFriends(false));
    }
  }, [isOpen]);

  if (!isOpen) return null;

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

  const toggleSelectUser = (userId: number) => {
    setSelectedUserIds((prev) =>
      prev.includes(userId) ? prev.filter((id) => id !== userId) : [...prev, userId]
    );
  };

  const filteredFriends = friends.filter((f) =>
    f.fullName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedName = name.trim();
    if (!trimmedName) {
      toast.warning('Vui lòng nhập tên nhóm');
      return;
    }

    if (selectedUserIds.length < 2) {
      toast.warning('Vui lòng chọn tối thiểu 2 thành viên để tạo nhóm');
      return;
    }

    try {
      let finalAvatarUrl = avatarUrl;
      if (avatarFile) {
        setIsUploadingAvatar(true);
        const uploadRes = await mediaService.uploadViaPresign(avatarFile, 'AVATAR');
        finalAvatarUrl = uploadRes.publicUrl;
      }

      createGroupMutation.mutate(
        {
          name: trimmedName,
          avatarUrl: finalAvatarUrl || undefined,
          memberIds: selectedUserIds,
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in">
      <div
        className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl p-6 relative animate-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
                Tạo nhóm trò chuyện
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Tối thiểu 3 thành viên bao gồm cả bạn
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
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
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                Tên nhóm <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                maxLength={100}
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Ví dụ: Hội ôn thi Giải tích 1"
                className="w-full px-3.5 py-2.5 text-sm bg-slate-100 dark:bg-slate-800/80 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
              />
            </div>
          </div>

          {/* Member Selection Section */}
          <div className="space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-slate-700 dark:text-slate-300">
                Thêm thành viên ({selectedUserIds.length} đã chọn)
              </span>
              <span className="text-slate-400">Cần chọn ít nhất 2 bạn</span>
            </div>

            {/* Friend Search Box */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm bạn bè theo tên..."
                className="w-full pl-9 pr-3 py-2 text-xs bg-slate-100 dark:bg-slate-800/80 rounded-xl outline-none text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
              />
            </div>

            {/* Friend List */}
            <div className="max-h-48 overflow-y-auto space-y-1 pr-1 border border-slate-100 dark:border-slate-800 rounded-xl p-1.5">
              {isLoadingFriends ? (
                <div className="py-6 text-center text-xs text-slate-400 flex items-center justify-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Đang tải danh sách bạn bè...
                </div>
              ) : filteredFriends.length === 0 ? (
                <div className="py-6 text-center text-xs text-slate-400">
                  {searchTerm ? 'Không tìm thấy bạn bè phù hợp' : 'Bạn chưa có người bạn nào'}
                </div>
              ) : (
                filteredFriends.map((friend) => {
                  const isSelected = selectedUserIds.includes(friend.userId);
                  return (
                    <div
                      key={friend.userId}
                      onClick={() => toggleSelectUser(friend.userId)}
                      className={`flex items-center justify-between p-2 rounded-xl cursor-pointer transition-colors ${
                        isSelected
                          ? 'bg-indigo-50 dark:bg-indigo-950/40 text-indigo-900 dark:text-indigo-200'
                          : 'hover:bg-slate-100 dark:hover:bg-slate-800/60 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      <div className="flex items-center gap-2.5 truncate">
                        <div className="w-8 h-8 rounded-full overflow-hidden bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-xs font-bold shrink-0">
                          {friend.avatarUrl ? (
                            <img src={friend.avatarUrl} alt={friend.fullName} className="w-full h-full object-cover" />
                          ) : (
                            friend.fullName.charAt(0).toUpperCase()
                          )}
                        </div>
                        <span className="text-xs font-medium truncate">{friend.fullName}</span>
                      </div>

                      <div
                        className={`w-5 h-5 rounded-lg flex items-center justify-center border transition-colors ${
                          isSelected
                            ? 'bg-indigo-600 border-indigo-600 text-white'
                            : 'border-slate-300 dark:border-slate-600'
                        }`}
                      >
                        {isSelected && <Check className="w-3.5 h-3.5" />}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Submit Button */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting || !name.trim() || selectedUserIds.length < 2}
              className="w-full py-2.5 rounded-xl bg-linear-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white font-semibold text-sm shadow-md transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {isSubmitting && <Loader2 className="w-4 h-4 animate-spin" />}
              Tạo nhóm ngay
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
