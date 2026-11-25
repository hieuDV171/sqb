import { create } from 'zustand';

const TOASTS_ENABLED = false;

export type ToastType = 'success' | 'info';

export interface ToastItem {
  id: string;
  type: ToastType;
  title?: string;
  message: string;
  duration?: number;
}

interface ToastStore {
  toasts: ToastItem[];
  addToast: (item: Omit<ToastItem, 'id'>) => string | undefined;
  removeToast: (id: string) => void;
  clearToasts: () => void;
}

export const useToastStore = create<ToastStore>((set) => ({
  toasts: [],
  addToast: (item) => {
    if (!TOASTS_ENABLED) return;

    const id = `toast-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
    const duration = item.duration ?? 4000;

    // Khi thông báo mới xuất hiện thì thông báo trước đó kết thúc ngay lập tức
    set(() => ({
      toasts: [{ ...item, id, duration }],
    }));

    if (duration > 0) {
      setTimeout(() => {
        set((state) => ({
          toasts: state.toasts.filter((t) => t.id !== id),
        }));
      }, duration);
    }

    return id;
  },
  removeToast: (id) =>
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    })),
  clearToasts: () => set({ toasts: [] }),
}));

// Tiện ích gọi nhanh từ bất kỳ đâu trong ứng dụng (kể cả ngoài React component)
export const toast = {
  success: (message: string, title?: string, duration?: number) => {
    return useToastStore.getState().addToast({
      type: 'success',
      title,
      message,
      duration,
    });
  },
  info: (message: string, title?: string, duration?: number) => {
    return useToastStore.getState().addToast({
      type: 'info',
      title,
      message,
      duration,
    });
  },
};
