import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface GamificationState {
  coins: number;
  points: number;
  unreadNotifications: number;
  addCoins: (amount: number) => void;
  deductCoins: (amount: number) => boolean;
  addPoints: (amount: number) => void;
  setCoins: (amount: number) => void;
  setPoints: (amount: number) => void;
  setUnreadNotifications: (count: number) => void;
}

export const useGamificationStore = create<GamificationState>()(
  persist(
    (set, get) => ({
      coins: 320, // Số dư xu khởi tạo mặc định cho demo sinh viên
      points: 1450, // Điểm cống hiến tích lũy
      unreadNotifications: 3,

      addCoins: (amount) => set((state) => ({ coins: state.coins + amount })),
      
      deductCoins: (amount) => {
        const current = get().coins;
        if (current < amount) return false;
        set({ coins: current - amount });
        return true;
      },

      addPoints: (amount) => set((state) => ({ points: state.points + amount })),
      setCoins: (coins) => set({ coins }),
      setPoints: (points) => set({ points }),
      setUnreadNotifications: (unreadNotifications) => set({ unreadNotifications }),
    }),
    {
      name: 'sqb-gamification-storage',
    }
  )
);
