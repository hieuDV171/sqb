import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type UserRole = 'STUDENT' | 'LECTURER' | 'ADMIN';
export type AuthTokenType = 'ACCESS' | 'ONBOARDING';
export type AccountState = 'EMAIL_VERIFICATION_REQUIRED' | 'PASSWORD_CHANGE_REQUIRED' | 'ACTIVE';

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
  tokenType: AuthTokenType | null;
  accountState: AccountState | null;
  isAuthenticated: boolean;
  user: UserInfo | null;
  setAuth: (accessToken: string, user: UserInfo, tokenType: AuthTokenType, accountState: AccountState) => void;
  setTokens: (accessToken: string) => void;
  setAccountState: (accountState: AccountState) => void;
  setUserRole: (role: UserRole) => void;
  updateUser: (partial: Partial<UserInfo>) => void;
  logout: () => void;
}

type PersistedAuthState = Pick<AuthState, 'accessToken' | 'tokenType' | 'accountState' | 'isAuthenticated' | 'user'>;

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      tokenType: null,
      accountState: null,
      isAuthenticated: false,
      user: null,

      setAuth: (accessToken, user, tokenType, accountState) => {
        set({
          accessToken,
          tokenType,
          accountState,
          isAuthenticated: true,
          user: { ...user, role: user.role || 'STUDENT' },
        });
      },

      setTokens: (accessToken) =>
        set((state) => ({
          accessToken,
          isAuthenticated: true,
          user: state.user,
        })),

      setAccountState: (accountState) => set({ accountState }),

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
          tokenType: null,
          accountState: null,
          isAuthenticated: false,
          user: null,
        }),
    }),
    {
      name: 'sqb-auth-storage', // key in localStorage
      version: 2,
      // Refresh Token của web chỉ tồn tại trong HttpOnly cookie do Backend quản lý.
      partialize: (state): PersistedAuthState => ({
        accessToken: state.accessToken,
        tokenType: state.tokenType,
        accountState: state.accountState,
        isAuthenticated: state.isAuthenticated,
        user: state.user,
      }),
      migrate: (persistedState): PersistedAuthState => {
        const persisted = persistedState as Partial<PersistedAuthState>;
        const hasCurrentSessionContract = Boolean(persisted.tokenType && persisted.accountState);

        return {
          accessToken: hasCurrentSessionContract ? persisted.accessToken ?? null : null,
          tokenType: hasCurrentSessionContract ? persisted.tokenType ?? null : null,
          accountState: hasCurrentSessionContract ? persisted.accountState ?? null : null,
          isAuthenticated: Boolean(hasCurrentSessionContract && persisted.isAuthenticated && persisted.accessToken),
          user: hasCurrentSessionContract ? persisted.user ?? null : null,
        };
      },
    }
  )
);
