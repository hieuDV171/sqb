import { create } from 'zustand';

export const AUTH_EVENT_KEY = 'sqb-auth-event';

function publishAuthEvent(type: 'login' | 'logout') {
  try {
    localStorage.setItem(AUTH_EVENT_KEY, JSON.stringify({ type, id: crypto.randomUUID() }));
  } catch { /* Storage may be disabled; authentication remains in memory. */ }
}

// Remove credentials persisted by previous versions; never hydrate them into memory.
try { localStorage.removeItem('sqb-auth-storage'); } catch { /* Storage may be disabled. */ }

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
  setAuth: (accessToken: string, user: UserInfo, tokenType: AuthTokenType, accountState: AccountState, notify?: boolean) => void;
  setTokens: (accessToken: string) => void;
  setAccountState: (accountState: AccountState) => void;
  setUserRole: (role: UserRole) => void;
  updateUser: (partial: Partial<UserInfo>) => void;
  logout: (notify?: boolean) => void;
}

export const useAuthStore = create<AuthState>()(
    (set) => ({
      accessToken: null,
      tokenType: null,
      accountState: null,
      isAuthenticated: false,
      user: null,

      setAuth: (accessToken, user, tokenType, accountState, notify = true) => {
        set({
          accessToken,
          tokenType,
          accountState,
          isAuthenticated: true,
          user: { ...user, role: user.role || 'STUDENT' },
        });
        if (notify) publishAuthEvent('login');
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

      logout: (notify = true) => {
        set({
          accessToken: null,
          tokenType: null,
          accountState: null,
          isAuthenticated: false,
          user: null,
        });
        if (notify) publishAuthEvent('logout');
      },
    }),
);
