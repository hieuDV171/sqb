import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface UserInfo {
  id: number;
  username: string;
  avatarUrl?: string;
  coverUrl?: string;
  frameUrl?: string;
  verified: boolean;
  profileCompleted: boolean;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  user: UserInfo | null;
  setAuth: (accessToken: string, refreshToken: string, user?: UserInfo) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      user: null,

      setAuth: (accessToken, refreshToken, user) =>
        set({
          accessToken,
          refreshToken,
          isAuthenticated: true,
          user: user || null,
        }),

      setTokens: (accessToken, refreshToken) =>
        set((state) => ({
          accessToken,
          refreshToken: refreshToken ?? state.refreshToken,
          isAuthenticated: true,
          user: state.user,
        })),

      logout: () =>
        set({
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
          user: null,
        }),
    }),
    {
      name: 'sqb-auth-storage', // key in localStorage
    }
  )
);
