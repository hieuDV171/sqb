import { create } from 'zustand';

export type GamificationTab = 'leaderboard' | 'shop' | 'predictions' | 'badges' | 'error-hunter';

interface GamificationState {
  // Navigation
  activeTab: GamificationTab;
  setActiveTab: (tab: GamificationTab) => void;

  // Daily Check-in Modal
  isCheckInModalOpen: boolean;
  setCheckInModalOpen: (open: boolean) => void;
  currentStreak: number;
  hasCheckedInToday: boolean;
  setCheckInResult: (streak: number, date?: string) => void;

  // Fitting Room (Live Preview for Cosmetics)
  previewCosmeticId: number | null;
  previewAssetUrl: string | null;
  previewCosmeticName: string | null;
  setPreviewCosmetic: (id: number | null, assetUrl: string | null, name: string | null) => void;
  clearPreview: () => void;
}

const getStoredStreak = (): { streak: number; checkedInToday: boolean } => {
  try {
    const now = new Date();
    const today = now.toISOString().slice(0, 10);

    const yesterdayDate = new Date(now);
    yesterdayDate.setDate(yesterdayDate.getDate() - 1);
    const yesterday = yesterdayDate.toISOString().slice(0, 10);

    const lastDate = localStorage.getItem('sqb_checkin_last_date');
    const savedStreak = parseInt(localStorage.getItem('sqb_checkin_streak') || '0', 10);

    if (!lastDate) {
      return { streak: 0, checkedInToday: false };
    }

    const checkedInToday = lastDate === today;
    const checkedInYesterday = lastDate === yesterday;

    // Nếu không điểm danh hôm nay và cũng không điểm danh hôm qua -> Chuỗi đã đứt về 0
    if (!checkedInToday && !checkedInYesterday) {
      return { streak: 0, checkedInToday: false };
    }

    return {
      streak: isNaN(savedStreak) ? 0 : savedStreak,
      checkedInToday,
    };
  } catch {
    return { streak: 0, checkedInToday: false };
  }
};

const initialCheckIn = getStoredStreak();

export const useGamificationStore = create<GamificationState>((set) => ({
  activeTab: 'leaderboard',
  setActiveTab: (tab) => set({ activeTab: tab }),

  isCheckInModalOpen: false,
  setCheckInModalOpen: (open) => set({ isCheckInModalOpen: open }),
  currentStreak: initialCheckIn.streak,
  hasCheckedInToday: initialCheckIn.checkedInToday,
  setCheckInResult: (streak: number, date?: string) => {
    const today = date || new Date().toISOString().slice(0, 10);
    try {
      localStorage.setItem('sqb_checkin_streak', streak.toString());
      localStorage.setItem('sqb_checkin_last_date', today);
    } catch {}
    set({
      currentStreak: streak,
      hasCheckedInToday: true,
    });
  },

  previewCosmeticId: null,
  previewAssetUrl: null,
  previewCosmeticName: null,
  setPreviewCosmetic: (id, assetUrl, name) =>
    set({
      previewCosmeticId: id,
      previewAssetUrl: assetUrl,
      previewCosmeticName: name,
    }),
  clearPreview: () =>
    set({
      previewCosmeticId: null,
      previewAssetUrl: null,
      previewCosmeticName: null,
    }),
}));
