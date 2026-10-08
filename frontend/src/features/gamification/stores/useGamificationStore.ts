import { create } from 'zustand';

export type GamificationTab = 'leaderboard' | 'shop' | 'predictions' | 'badges' | 'error-hunter';

interface GamificationState {
  // Navigation
  activeTab: GamificationTab;
  setActiveTab: (tab: GamificationTab) => void;

  // Daily Check-in Modal (UI state)
  isCheckInModalOpen: boolean;
  setCheckInModalOpen: (open: boolean) => void;

  // Fitting Room (Live Preview for Cosmetics)
  previewCosmeticId: number | null;
  previewAssetUrl: string | null;
  previewCosmeticName: string | null;
  setPreviewCosmetic: (id: number | null, assetUrl: string | null, name: string | null) => void;
  clearPreview: () => void;
}

export const useGamificationStore = create<GamificationState>((set) => ({
  activeTab: 'leaderboard',
  setActiveTab: (tab) => set({ activeTab: tab }),

  isCheckInModalOpen: false,
  setCheckInModalOpen: (open) => set({ isCheckInModalOpen: open }),

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
