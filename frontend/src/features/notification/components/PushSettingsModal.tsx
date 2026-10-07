import { useState, useEffect } from 'react';
import { X, Bell, Moon, BookOpen, Users, Trophy, Shield, Check, Loader2 } from 'lucide-react';
import { usePushSettings, useUpdatePushSettings } from '../hooks/useNotification';
import type { PushNotificationType } from '../types/notification.types';

interface PushSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function PushSettingsModal({ isOpen, onClose }: PushSettingsModalProps) {
  const { data: currentSettings, isLoading } = usePushSettings();
  const updateMutation = useUpdatePushSettings();

  const [categories, setCategories] = useState<Record<PushNotificationType, { enabled: boolean; emailEnabled?: boolean }>>({
    ACADEMIC: { enabled: true, emailEnabled: false },
    SOCIAL: { enabled: true, emailEnabled: false },
    GAMIFICATION: { enabled: true, emailEnabled: false },
    SYSTEM: { enabled: true, emailEnabled: true },
  });

  const [quietHours, setQuietHours] = useState({
    enabled: false,
    startHour: 22,
    startMinute: 0,
    endHour: 7,
    endMinute: 0,
  });

  useEffect(() => {
    if (currentSettings) {
      if (currentSettings.categories) {
        setCategories({
          ACADEMIC: currentSettings.categories.ACADEMIC || { enabled: true },
          SOCIAL: currentSettings.categories.SOCIAL || { enabled: true },
          GAMIFICATION: currentSettings.categories.GAMIFICATION || { enabled: true },
          SYSTEM: currentSettings.categories.SYSTEM || { enabled: true },
        });
      }
      if (currentSettings.quietHours) {
        setQuietHours(currentSettings.quietHours);
      }
    }
  }, [currentSettings]);

  if (!isOpen) return null;

  const handleToggleCategory = (cat: PushNotificationType) => {
    setCategories((prev) => ({
      ...prev,
      [cat]: {
        ...prev[cat],
        enabled: !prev[cat]?.enabled,
      },
    }));
  };

  const handleSave = async () => {
    await updateMutation.mutateAsync({
      categories,
      quietHours,
    });
    onClose();
  };

  const categoryConfigs: {
    key: PushNotificationType;
    label: string;
    description: string;
    icon: typeof BookOpen;
    color: string;
  }[] = [
    {
      key: 'ACADEMIC',
      label: 'Học thuật & Ngân hàng đề',
      description: 'Thông báo câu hỏi được duyệt, duyệt phiên đề xuất, phản hồi thẩm định.',
      icon: BookOpen,
      color: 'text-blue-500 bg-blue-50 dark:bg-blue-900/30',
    },
    {
      key: 'SOCIAL',
      label: 'Mạng xã hội & Tương tác',
      description: 'Lời mời kết bạn, bình luận bài viết, nhắc đến bạn trong thảo luận.',
      icon: Users,
      color: 'text-emerald-500 bg-emerald-50 dark:bg-emerald-900/30',
    },
    {
      key: 'GAMIFICATION',
      label: 'Gamification & Vinh danh',
      description: 'Nhận huy hiệu mới, quà tặng điểm danh, lọt top bảng xếp hạng vinh danh.',
      icon: Trophy,
      color: 'text-amber-500 bg-amber-50 dark:bg-amber-900/30',
    },
    {
      key: 'SYSTEM',
      label: 'Hệ thống & An toàn tài khoản',
      description: 'Thông báo bảo mật, đăng nhập thiết bị mới, cập nhật chính sách.',
      icon: Shield,
      color: 'text-purple-500 bg-purple-50 dark:bg-purple-900/30',
    },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-900/40 text-indigo-600 dark:text-indigo-400">
              <Bell className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-slate-900 dark:text-slate-100 text-base">
                Cài Đặt Nhận Thông Báo
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Tùy chỉnh các danh mục thông báo đẩy và giờ không làm phiền
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          {isLoading ? (
            <div className="flex items-center justify-center py-12 text-slate-400">
              <Loader2 className="w-6 h-6 animate-spin mr-2" />
              <span>Đang tải cấu hình thông báo...</span>
            </div>
          ) : (
            <>
              {/* Category Toggles */}
              <div>
                <h3 className="text-xs font-bold text-slate-400 dark:text-slate-500 uppercase tracking-wider mb-3">
                  Danh mục thông báo
                </h3>
                <div className="space-y-3">
                  {categoryConfigs.map((cfg) => {
                    const Icon = cfg.icon;
                    const isEnabled = categories[cfg.key]?.enabled ?? true;

                    return (
                      <div
                        key={cfg.key}
                        onClick={() => handleToggleCategory(cfg.key)}
                        className={`flex items-start justify-between gap-3 p-3.5 rounded-2xl border transition-all cursor-pointer select-none ${
                          isEnabled
                            ? 'border-indigo-200 dark:border-indigo-800/60 bg-indigo-50/20 dark:bg-indigo-950/20'
                            : 'border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 opacity-70'
                        }`}
                      >
                        <div className="flex items-start gap-3">
                          <div className={`p-2 rounded-xl mt-0.5 ${cfg.color}`}>
                            <Icon className="w-4 h-4" />
                          </div>
                          <div>
                            <p className="text-sm font-bold text-slate-900 dark:text-slate-100">
                              {cfg.label}
                            </p>
                            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                              {cfg.description}
                            </p>
                          </div>
                        </div>

                        <div
                          className={`w-6 h-6 rounded-full border flex items-center justify-center transition-colors ${
                            isEnabled
                              ? 'bg-indigo-600 border-indigo-600 text-white'
                              : 'border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800'
                          }`}
                        >
                          {isEnabled && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Quiet Hours Section */}
              <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="flex items-center gap-2">
                    <div className="p-2 rounded-xl bg-amber-50 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400">
                      <Moon className="w-4 h-4" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">
                        Khung Giờ Yên Tĩnh (Quiet Hours)
                      </h3>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        Tắt toàn bộ chuông và pop-up trong khung giờ nghỉ ngơi
                      </p>
                    </div>
                  </div>

                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={quietHours.enabled}
                      onChange={(e) =>
                        setQuietHours((prev) => ({ ...prev, enabled: e.target.checked }))
                      }
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-slate-600 peer-checked:bg-indigo-600"></div>
                  </label>
                </div>

                {quietHours.enabled && (
                  <div className="grid grid-cols-2 gap-3 mt-3 p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800">
                    <div>
                      <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                        Từ (Bắt đầu):
                      </label>
                      <input
                        type="time"
                        value={`${String(quietHours.startHour).padStart(2, '0')}:${String(
                          quietHours.startMinute
                        ).padStart(2, '0')}`}
                        onChange={(e) => {
                          const [h, m] = e.target.value.split(':').map(Number);
                          setQuietHours((prev) => ({ ...prev, startHour: h, startMinute: m }));
                        }}
                        className="w-full px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-indigo-500"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                        Đến (Kết thúc):
                      </label>
                      <input
                        type="time"
                        value={`${String(quietHours.endHour).padStart(2, '0')}:${String(
                          quietHours.endMinute
                        ).padStart(2, '0')}`}
                        onChange={(e) => {
                          const [h, m] = e.target.value.split(':').map(Number);
                          setQuietHours((prev) => ({ ...prev, endHour: h, endMinute: m }));
                        }}
                        className="w-full px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-indigo-500"
                      />
                    </div>
                  </div>
                )}
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 p-4 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-xs font-semibold rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={updateMutation.isPending || isLoading}
            className="px-5 py-2 text-xs font-bold rounded-xl text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 transition-colors shadow-sm flex items-center gap-1.5 cursor-pointer"
          >
            {updateMutation.isPending ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                Đang lưu...
              </>
            ) : (
              'Lưu Cài Đặt'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
