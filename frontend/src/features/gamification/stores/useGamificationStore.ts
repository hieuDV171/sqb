import { create } from 'zustand';

export type GamificationTab = 'leaderboard' | 'shop' | 'predictions' | 'badges' | 'error-hunter';

interface GamificationState {
  // Navigation
  activeTab: GamificationTab;
  setActiveTab: (tab: GamificationTab) => void;

  // Daily Check-in Modal (UI state)
  isCheckInModalOpen: boolean;
  setCheckInModalOpen: (open: boolean) => void;

}

export const useGamificationStore = create<GamificationState>((set) => ({
  activeTab: 'leaderboard',
  setActiveTab: (tab) => set({ activeTab: tab }),

  isCheckInModalOpen: false,
  setCheckInModalOpen: (open) => set({ isCheckInModalOpen: open }),

}));
