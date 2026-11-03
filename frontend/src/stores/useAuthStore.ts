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
  isAuthenticated: boolean;
  user: UserInfo | null;
  setAuth: (accessToken: string, user?: UserInfo) => void;
  setTokens: (accessToken: string) => void;
  setUserRole: (role: UserRole) => void;
  updateUser: (partial: Partial<UserInfo>) => void;
  logout: () => void;
}

type PersistedAuthState = Pick<AuthState, 'accessToken' | 'isAuthenticated' | 'user'>;

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      isAuthenticated: false,
      user: null,

      setAuth: (accessToken, user) => {
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
          isAuthenticated: true,
          user: resolvedUser,
        });
      },

      setTokens: (accessToken) =>
        set((state) => ({
          accessToken,
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
          isAuthenticated: false,
          user: null,
        }),
    }),
    {
      name: 'sqb-auth-storage', // key in localStorage
      version: 1,
      // Refresh Token của web chỉ tồn tại trong HttpOnly cookie do Backend quản lý.
      partialize: (state): PersistedAuthState => ({
        accessToken: state.accessToken,
        isAuthenticated: state.isAuthenticated,
        user: state.user,
      }),
      migrate: (persistedState): PersistedAuthState => {
        const persisted = persistedState as Partial<PersistedAuthState>;

        return {
          accessToken: persisted.accessToken ?? null,
          isAuthenticated: Boolean(persisted.isAuthenticated && persisted.accessToken),
          user: persisted.user ?? null,
        };
      },
    }
  )
);
