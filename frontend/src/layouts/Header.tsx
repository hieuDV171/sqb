import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Bell,
  ChevronDown,
  KeyRound,
  LogOut,
  Menu,
  Monitor,
  Moon,
  Search,
  Settings,
  ShieldAlert,
  Sun,
  User,
  Users,
  X,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { BrandMark } from '@/components/common/BrandMark';
import { ChangePasswordModal } from '@/components/auth/ChangePasswordModal';
import { NotificationDropdown } from '@/features/notification';
import { GlobalSearchModal } from '@/features/search';
import { useAuthStore } from '@/stores/useAuthStore';
import { useGamificationStore } from '@/stores/useGamificationStore';
import { useThemeStore } from '@/stores/useThemeStore';
import { useAuth } from '@/hooks/useAuth';
import { useStompStatus } from '@/lib/stompClient';
import { cn } from '@/lib/utils';

interface HeaderProps {
  isMobileMenuOpen: boolean;
  onToggleMobileMenu: () => void;
}

const ROLE_LABELS = {
  STUDENT: 'Sinh viên',
  LECTURER: 'Giảng viên',
  ADMIN: 'Quản trị viên',
} as const;

export function Header({ isMobileMenuOpen, onToggleMobileMenu }: HeaderProps) {
  const { user } = useAuthStore();
  const { unreadNotifications } = useGamificationStore();
  const { theme, toggleTheme } = useThemeStore();
  const { logout } = useAuth();
  const stompStatus = useStompStatus();
  const navigate = useNavigate();

  const [openPanel, setOpenPanel] = useState<'profile' | 'notifications' | null>(null);
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const profilePanelRef = useRef<HTMLDivElement>(null);
  const notificationPanelRef = useRef<HTMLDivElement>(null);
  const profileTriggerRef = useRef<HTMLButtonElement>(null);
  const notificationTriggerRef = useRef<HTMLButtonElement>(null);
  const desktopSearchTriggerRef = useRef<HTMLButtonElement>(null);
  const mobileSearchTriggerRef = useRef<HTMLButtonElement>(null);
  const activeSearchTriggerRef = useRef<HTMLElement | null>(null);
  const isProfileOpen = openPanel === 'profile';
  const isNotificationOpen = openPanel === 'notifications';

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        activeSearchTriggerRef.current = window.innerWidth >= 768
          ? desktopSearchTriggerRef.current
          : mobileSearchTriggerRef.current;
        setIsSearchOpen((current) => !current);
        setOpenPanel(null);
      }
    };

    window.addEventListener('keydown', handleKeyDown, { capture: true });
    return () => window.removeEventListener('keydown', handleKeyDown, { capture: true });
  }, []);

  useEffect(() => {
    if (!openPanel) return;
    const panelRef = openPanel === 'profile' ? profilePanelRef : notificationPanelRef;
    const triggerRef = openPanel === 'profile' ? profileTriggerRef : notificationTriggerRef;
    const handleOutsidePointer = (event: PointerEvent) => {
      if (event.target instanceof Node && !panelRef.current?.contains(event.target)) {
        setOpenPanel(null);
      }
    };
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key !== 'Escape' || event.defaultPrevented) return;
      setOpenPanel(null);
      triggerRef.current?.focus();
    };
    document.addEventListener('pointerdown', handleOutsidePointer, true);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('pointerdown', handleOutsidePointer, true);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [openPanel]);

  const openSearch = (trigger: HTMLElement | null) => {
    activeSearchTriggerRef.current = trigger;
    setOpenPanel(null);
    setIsSearchOpen(true);
  };

  return (
    <>
      <header className="sticky top-0 z-40 h-[var(--app-header-height)] border-b border-border bg-[var(--shell-header)] backdrop-blur-xl">
        <div className="mx-auto flex h-full w-full max-w-[var(--app-content-max-width)] items-center gap-3 px-3 sm:px-5 lg:px-6">
          <div className="flex min-w-0 items-center gap-2 md:w-[calc(var(--app-sidebar-width)-1.5rem)] md:shrink-0">
            <button
              type="button"
              onClick={onToggleMobileMenu}
              className="grid size-11 shrink-0 place-items-center rounded-xl text-muted-foreground transition-colors duration-200 hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:hidden"
              aria-label={isMobileMenuOpen ? 'Đóng menu' : 'Mở menu'}
              aria-expanded={isMobileMenuOpen}
              aria-controls="mobile-navigation-panel"
            >
              {isMobileMenuOpen ? <X className="size-5" /> : <Menu className="size-5" />}
            </button>
            <Link to="/feed" aria-label="SQB Campus Intelligence — về bảng tin" className="min-w-0 rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
              <BrandMark compact={false} className="hidden sm:inline-flex" />
              <BrandMark compact className="sm:hidden" />
            </Link>
          </div>

          <div className="hidden min-w-0 flex-1 justify-center md:flex">
            <button
              ref={desktopSearchTriggerRef}
              type="button"
              onClick={() => openSearch(desktopSearchTriggerRef.current)}
              aria-haspopup="dialog"
              aria-expanded={isSearchOpen}
              aria-controls="global-search-panel"
              className="group flex h-11 w-full max-w-xl items-center gap-3 rounded-2xl border border-border bg-surface-subtle px-4 text-left text-sm text-muted-foreground transition-[border-color,background-color,box-shadow] duration-200 hover:border-primary/25 hover:bg-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <Search className="size-4 shrink-0 transition-colors group-hover:text-primary" aria-hidden="true" />
              <span className="min-w-0 flex-1 truncate">Tìm người, môn học, bài viết hoặc câu hỏi</span>
              <kbd className="hidden rounded-lg border border-border bg-card px-2 py-1 font-mono text-[0.68rem] font-semibold text-muted-foreground lg:inline-flex">
                Ctrl K
              </kbd>
            </button>
          </div>

          <div className="ml-auto flex shrink-0 items-center gap-1 sm:gap-2">
            <button
              ref={mobileSearchTriggerRef}
              type="button"
              onClick={() => openSearch(mobileSearchTriggerRef.current)}
              className="grid size-11 place-items-center rounded-xl text-muted-foreground transition-colors duration-200 hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:hidden"
              aria-label="Mở tìm kiếm"
              aria-haspopup="dialog"
              aria-expanded={isSearchOpen}
            >
              <Search className="size-5" />
            </button>

            <button
              type="button"
              onClick={toggleTheme}
              className="hidden size-11 place-items-center rounded-xl text-muted-foreground transition-colors duration-200 hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:grid"
              aria-label={theme === 'dark' ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'}
            >
              {theme === 'dark' ? <Sun className="size-5 text-reward" /> : <Moon className="size-5" />}
            </button>

            <div ref={notificationPanelRef} className="relative">
              <button
                ref={notificationTriggerRef}
                type="button"
                onClick={() => setOpenPanel((current) => current === 'notifications' ? null : 'notifications')}
                className="relative grid size-11 place-items-center rounded-xl text-muted-foreground transition-colors duration-200 hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                aria-label={unreadNotifications > 0 ? `${unreadNotifications} thông báo chưa đọc` : 'Thông báo'}
                aria-haspopup="dialog"
                aria-expanded={isNotificationOpen}
                aria-controls={isNotificationOpen ? 'header-notifications' : undefined}
              >
                <Bell className="size-5" />
                {unreadNotifications > 0 && (
                  <span className="absolute right-1.5 top-1.5 grid min-h-4 min-w-4 place-items-center rounded-full border-2 border-card bg-destructive px-0.5 text-[0.58rem] font-bold leading-none text-white">
                    {unreadNotifications > 9 ? '9+' : unreadNotifications}
                  </span>
                )}
              </button>
              <NotificationDropdown isOpen={isNotificationOpen} onClose={() => setOpenPanel(null)} />
            </div>

            <div ref={profilePanelRef} className="relative">
              <button
                ref={profileTriggerRef}
                type="button"
                onClick={() => setOpenPanel((current) => current === 'profile' ? null : 'profile')}
                className="flex h-11 items-center gap-2 rounded-2xl border border-transparent px-1.5 transition-colors duration-200 hover:border-border hover:bg-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                aria-label="Mở menu tài khoản"
                aria-haspopup="menu"
                aria-expanded={isProfileOpen}
                aria-controls={isProfileOpen ? 'header-account-menu' : undefined}
              >
                <span className="relative grid size-9 shrink-0 place-items-center overflow-visible rounded-full bg-primary text-sm font-bold text-primary-foreground">
                  {user?.avatarUrl ? (
                    <img src={user.avatarUrl} alt="" className="size-full rounded-full object-cover" />
                  ) : (
                    user?.username?.charAt(0).toUpperCase() || 'S'
                  )}
                  {user?.frameUrl && <img src={user.frameUrl} alt="" className="pointer-events-none absolute -inset-1 size-[calc(100%+0.5rem)] object-contain" />}
                </span>
                <span className="hidden min-w-0 text-left lg:block">
                  <span className="block max-w-28 truncate text-sm font-semibold text-foreground">{user?.username || 'Tài khoản SQB'}</span>
                  <span className="mt-0.5 block text-[0.68rem] font-medium text-muted-foreground">{ROLE_LABELS[user?.role ?? 'STUDENT']}</span>
                </span>
                <ChevronDown className={cn('mr-1 hidden size-4 text-muted-foreground transition-transform lg:block', isProfileOpen && 'rotate-180')} />
              </button>

              {isProfileOpen && (
                  <div id="header-account-menu" role="menu" aria-label="Tài khoản" className="absolute right-0 z-50 mt-2 w-72 overflow-hidden rounded-2xl border border-border bg-popover text-popover-foreground shadow-[var(--shadow-primitive-md)]">
                    <div className="border-b border-border bg-surface-subtle px-4 py-4">
                      <p className="truncate text-sm font-semibold text-foreground">{user?.username || 'Tài khoản SQB'}</p>
                      <div className="mt-2 flex items-center gap-2">
                        <Badge variant="role" size="sm">{ROLE_LABELS[user?.role ?? 'STUDENT']}</Badge>
                        <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
                          <span className={cn('size-2 rounded-full', stompStatus === 'CONNECTED' ? 'bg-success' : stompStatus === 'CONNECTING' ? 'bg-warning' : 'bg-muted-foreground')} />
                          {stompStatus === 'CONNECTED' ? 'Realtime sẵn sàng' : stompStatus === 'CONNECTING' ? 'Đang kết nối' : 'Realtime gián đoạn'}
                        </span>
                      </div>
                    </div>

                    <div className="p-2">
                      {[
                        { label: 'Trang cá nhân', icon: User, path: '/profile' },
                        { label: 'Bạn bè & lời mời', icon: Users, path: '/friends' },
                        { label: 'Danh sách chặn', icon: ShieldAlert, path: '/blocked-users' },
                        { label: 'Thiết bị đăng nhập', icon: Monitor, path: '/devices' },
                        { label: 'Cài đặt tài khoản', icon: Settings, path: '/settings' },
                      ].map((item) => {
                        const Icon = item.icon;
                        return (
                          <button
                            key={item.path}
                            type="button"
                            role="menuitem"
                            onClick={() => {
                              setOpenPanel(null);
                              navigate(item.path);
                            }}
                            className="flex min-h-11 w-full items-center gap-3 rounded-xl px-3 text-left text-sm font-medium text-foreground transition-colors hover:bg-muted"
                          >
                            <Icon className="size-4 text-muted-foreground" />
                            {item.label}
                          </button>
                        );
                      })}
                      <button
                        type="button"
                        role="menuitem"
                        onClick={() => {
                          setOpenPanel(null);
                          setIsChangePasswordOpen(true);
                        }}
                        className="flex min-h-11 w-full items-center gap-3 rounded-xl px-3 text-left text-sm font-medium text-foreground transition-colors hover:bg-muted"
                      >
                        <KeyRound className="size-4 text-muted-foreground" />
                        Đổi mật khẩu
                      </button>
                    </div>

                    <div className="border-t border-border p-2">
                      <button
                        type="button"
                        role="menuitem"
                        onClick={() => {
                          setOpenPanel(null);
                          logout();
                        }}
                        className="flex min-h-11 w-full items-center gap-3 rounded-xl px-3 text-left text-sm font-semibold text-destructive transition-colors hover:bg-destructive/10"
                      >
                        <LogOut className="size-4" />
                        Đăng xuất
                      </button>
                    </div>
                  </div>
              )}
            </div>
          </div>
        </div>
      </header>

      <ChangePasswordModal
        isOpen={isChangePasswordOpen}
        onClose={() => setIsChangePasswordOpen(false)}
        onSuccess={() => useAuthStore.getState().updateUser({ profileCompleted: true })}
      />
      <GlobalSearchModal isOpen={isSearchOpen} onClose={() => setIsSearchOpen(false)} triggerRef={activeSearchTriggerRef} />
    </>
  );
}
