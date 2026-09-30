import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type UserRole = 'STUDENT' | 'LECTURER' | 'ADMIN';

export interface UserInfo {
  id: number;
  username: string;
  avatarUrl?: string;
  coverUrl?: string;
  frameUrl?: string;
  verified: boolean;
  profileCompleted: boolean;
  role?: UserRole;
}
 
interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  user: UserInfo | null;
  setAuth: (accessToken: string, refreshToken: string, user?: UserInfo) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  setUserRole: (role: UserRole) => void;
  updateUser: (partial: Partial<UserInfo>) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      user: null,

      setAuth: (accessToken, refreshToken, user) => {
        const resolvedUser: UserInfo = user
          ? { ...user, role: user.role || 'STUDENT' }
          : {
              id: 999,
              username: 'sinhvien.hust@edu.vn',
              verified: true,
              profileCompleted: true,
              role: 'STUDENT',
            };

        set({
          accessToken,
          refreshToken,
          isAuthenticated: true,
          user: resolvedUser,
        });
      },

      setTokens: (accessToken, refreshToken) =>
        set((state) => ({
          accessToken,
          refreshToken: refreshToken ?? state.refreshToken,
          isAuthenticated: true,
          user: state.user,
        })),

      setUserRole: (role: UserRole) =>
        set((state) => ({
          user: state.user ? { ...state.user, role } : null,
        })),

      updateUser: (partial: Partial<UserInfo>) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...partial } : null,
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
