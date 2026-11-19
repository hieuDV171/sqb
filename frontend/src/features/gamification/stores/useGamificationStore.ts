import { create } from 'zustand';

export type GamificationTab = 'leaderboard' | 'shop' | 'predictions' | 'badges';

interface GamificationState {
  // Daily Check-in Modal (UI state)
  isCheckInModalOpen: boolean;
  setCheckInModalOpen: (open: boolean) => void;

}

export const useGamificationStore = create<GamificationState>((set) => ({
  isCheckInModalOpen: false,
  setCheckInModalOpen: (open) => set({ isCheckInModalOpen: open }),

}));
