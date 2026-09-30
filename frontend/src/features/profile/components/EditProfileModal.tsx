import React, { useState, useRef } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { userService } from '@/services/userService';
import { mediaService } from '@/services/mediaService';
import { useAuthStore } from '@/stores/useAuthStore';
import { toast } from '@/stores/useToastStore';
import type { Gender, ProfileResponse, UpdateProfileRequest } from '@/types/user.types';
import {
  X,
  Camera,
  Loader2,
  Check,
  User,
  Calendar,
  Globe,
  Sparkles,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { AvatarWithFrame } from './AvatarWithFrame';

interface EditProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
  profile: ProfileResponse;
}

export function EditProfileModal({ isOpen, onClose, profile }: EditProfileModalProps) {
  const queryClient = useQueryClient();
  const { updateUser } = useAuthStore();

  const [fullName, setFullName] = useState(profile.fullName || '');
  const [bio, setBio] = useState(profile.bio || '');
  const [gender, setGender] = useState<Gender>(profile.gender || 'OTHER');
  const [dateOfBirth, setDateOfBirth] = useState(profile.dateOfBirth || '');
  const [timezone, setTimezone] = useState(profile.timezone || 'Asia/Ho_Chi_Minh');

  // Preview & Upload states
  const [avatarPreview, setAvatarPreview] = useState<string | null>(profile.avatarUrl || null);
  const [coverPreview, setCoverPreview] = useState<string | null>(profile.coverUrl || null);
  const [avatarUrlToSubmit, setAvatarUrlToSubmit] = useState<string | undefined>(undefined);
  const [coverUrlToSubmit, setCoverUrlToSubmit] = useState<string | undefined>(undefined);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [isUploadingCover, setIsUploadingCover] = useState(false);

  const avatarInputRef = useRef<HTMLInputElement>(null);
  const coverInputRef = useRef<HTMLInputElement>(null);

  // Handle avatar upload via Presigned URL
  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      toast.error('Vui lòng chọn tệp định dạng hình ảnh (PNG, JPG, WEBP)');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      toast.error('Kích thước ảnh đại diện không được vượt quá 10MB');
      return;
    }

    try {
      setIsUploadingAvatar(true);
      // Local preview immediately
      const localUrl = URL.createObjectURL(file);
      setAvatarPreview(localUrl);

      // Upload via Presigned URL (MinIO S3) with fallback
      const uploadRes = await mediaService.uploadViaPresign(file, 'AVATAR');
      setAvatarUrlToSubmit(uploadRes.publicUrl);
      toast.success('Tải ảnh đại diện lên thành công');
    } catch (err: any) {
      toast.error(err?.message || 'Không thể tải ảnh đại diện lên máy chủ');
    } finally {
      setIsUploadingAvatar(false);
    }
  };

  // Handle cover upload via Presigned URL
  const handleCoverChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      toast.error('Vui lòng chọn tệp định dạng hình ảnh (PNG, JPG, WEBP)');
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      toast.error('Kích thước ảnh bìa không được vượt quá 20MB');
      return;
    }

    try {
      setIsUploadingCover(true);
      const localUrl = URL.createObjectURL(file);
      setCoverPreview(localUrl);

      // Upload via Presigned URL (MinIO S3) with fallback
      const uploadRes = await mediaService.uploadViaPresign(file, 'COVER');
      setCoverUrlToSubmit(uploadRes.publicUrl);
      toast.success('Tải ảnh bìa lên thành công');
    } catch (err: any) {
      toast.error(err?.message || 'Không thể tải ảnh bìa lên máy chủ');
    } finally {
      setIsUploadingCover(false);
    }
  };

  const updateMutation = useMutation({
    mutationFn: async (payload: UpdateProfileRequest) => {
      const res = await userService.updateMyProfile(payload);
      return res.data;
    },
    onSuccess: (updated) => {
      toast.success('Cập nhật hồ sơ thành công!');
      // Update global auth store
      updateUser({
        username: updated.fullName || updated.email,
        avatarUrl: updated.avatarUrl || undefined,
        coverUrl: updated.coverUrl || undefined,
        frameUrl: updated.frameUrl || undefined,
        profileCompleted: updated.profileCompleted,
      });

      // Invalidate queries so Profile & Header re-render fresh
      queryClient.invalidateQueries({ queryKey: ['userProfile', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['userProfile', profile.userId] });
      onClose();
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Lỗi khi lưu thông tin hồ sơ. Vui lòng thử lại!');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      toast.error('Họ và tên không được để trống');
      return;
    }

    const payload: UpdateProfileRequest = {
      fullName: fullName.trim(),
      bio: bio.trim(),
      gender,
      dateOfBirth: dateOfBirth || undefined,
      timezone,
      avatarUrl: avatarUrlToSubmit !== undefined ? avatarUrlToSubmit : profile.avatarUrl || undefined,
      coverUrl: coverUrlToSubmit !== undefined ? coverUrlToSubmit : profile.coverUrl || undefined,
    };

    updateMutation.mutate(payload);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in-50 duration-200">
      <div className="relative w-full max-w-2xl bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200/80 dark:border-slate-800 overflow-hidden max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-white">Chỉnh sửa hồ sơ cá nhân</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">Cập nhật thông tin hiển thị trên SQB Network</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Form Body */}
        <form onSubmit={handleSubmit} className="overflow-y-auto p-6 space-y-6 flex-1">
          {/* Cover & Avatar Upload Preview */}
          <div className="relative">
            {/* Cover Photo */}
            <div className="relative h-36 sm:h-44 w-full rounded-2xl overflow-hidden bg-linear-to-r from-indigo-500 via-purple-500 to-pink-500 shadow-inner group">
              {coverPreview ? (
                <img src={coverPreview} alt="Cover Preview" className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center text-white/70 text-sm font-medium">
                  Chưa có ảnh bìa
                </div>
              )}

              {/* Cover overlay button */}
              <button
                type="button"
                onClick={() => coverInputRef.current?.click()}
                disabled={isUploadingCover}
                className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2 text-white font-medium text-xs sm:text-sm cursor-pointer backdrop-blur-2xs"
              >
                {isUploadingCover ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <>
                    <Camera className="w-5 h-5" />
                    <span>Thay đổi ảnh bìa</span>
                  </>
                )}
              </button>
              <input
                ref={coverInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleCoverChange}
              />
            </div>

            {/* Avatar Photo with Frame */}
            <div className="absolute -bottom-8 left-6 flex items-end gap-3">
              <div className="relative group">
                <AvatarWithFrame
                  avatarUrl={avatarPreview}
                  frameUrl={profile.frameUrl}
                  name={fullName || profile.fullName}
                  size="xl"
                  className="shadow-xl"
                />

                <button
                  type="button"
                  onClick={() => avatarInputRef.current?.click()}
                  disabled={isUploadingAvatar}
                  className="absolute inset-0 rounded-full bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white cursor-pointer z-20"
                >
                  {isUploadingAvatar ? (
                    <Loader2 className="w-5 h-5 animate-spin" />
                  ) : (
                    <Camera className="w-5 h-5" />
                  )}
                </button>
                <input
                  ref={avatarInputRef}
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={handleAvatarChange}
                />
              </div>

              <div className="mb-2 hidden sm:block">
                <p className="text-xs font-semibold text-slate-700 dark:text-slate-300">Ảnh đại diện & Khung</p>
                <p className="text-[10px] text-slate-400">Click vào ảnh để đổi avatar</p>
              </div>
            </div>
          </div>

          <div className="pt-8 grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Full Name */}
            <div className="sm:col-span-2 space-y-1.5">
              <Label htmlFor="fullName" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                Họ và tên hiển thị <span className="text-red-500">*</span>
              </Label>
              <div className="relative">
                <User className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                <Input
                  id="fullName"
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Nhập họ và tên của bạn"
                  className="pl-10"
                  maxLength={100}
                  required
                />
              </div>
            </div>

            {/* Bio */}
            <div className="sm:col-span-2 space-y-1.5">
              <div className="flex justify-between items-center">
                <Label htmlFor="bio" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                  Tiểu sử (Bio)
                </Label>
                <span className="text-[10px] text-slate-400">{bio.length}/500</span>
              </div>
              <textarea
                id="bio"
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                rows={3}
                maxLength={500}
                placeholder="Chia sẻ đôi điều về bạn, sở thích hoặc định hướng học tập..."
                className="w-full px-3.5 py-2.5 text-sm bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl focus:border-indigo-500 focus:bg-white dark:focus:bg-slate-900 outline-none text-slate-900 dark:text-slate-100 transition-all resize-none placeholder:text-slate-400"
              />
            </div>

            {/* Gender */}
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-700 dark:text-slate-300">Giới tính</Label>
              <select
                value={gender}
                onChange={(e) => setGender(e.target.value as Gender)}
                className="w-full px-3.5 py-2.5 text-sm bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl focus:border-indigo-500 focus:bg-white dark:focus:bg-slate-900 outline-none text-slate-900 dark:text-slate-100 transition-all"
              >
                <option value="MALE">Nam</option>
                <option value="FEMALE">Nữ</option>
                <option value="OTHER">Khác / Không công khai</option>
              </select>
            </div>

            {/* Date of Birth */}
            <div className="space-y-1.5">
              <Label htmlFor="dateOfBirth" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                Ngày sinh
              </Label>
              <div className="relative">
                <Calendar className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                <Input
                  id="dateOfBirth"
                  type="date"
                  value={dateOfBirth}
                  onChange={(e) => setDateOfBirth(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>

            {/* Timezone */}
            <div className="sm:col-span-2 space-y-1.5">
              <Label htmlFor="timezone" className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                Múi giờ hoạt động
              </Label>
              <div className="relative">
                <Globe className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                <Input
                  id="timezone"
                  type="text"
                  value={timezone}
                  onChange={(e) => setTimezone(e.target.value)}
                  placeholder="Asia/Ho_Chi_Minh"
                  className="pl-10"
                />
              </div>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="pt-4 flex items-center justify-end gap-3 border-t border-slate-100 dark:border-slate-800">
            <Button type="button" variant="outline" onClick={onClose} disabled={updateMutation.isPending}>
              Hủy bỏ
            </Button>
            <Button
              type="submit"
              disabled={updateMutation.isPending || isUploadingAvatar || isUploadingCover}
              className="gap-2 min-w-[120px]"
            >
              {updateMutation.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang lưu...</span>
                </>
              ) : (
                <>
                  <Check className="w-4 h-4" />
                  <span>Lưu thay đổi</span>
                </>
              )}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
