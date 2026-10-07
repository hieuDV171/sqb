import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';
import { useGamificationStore } from '@/stores/useGamificationStore';
import { useThemeStore } from '@/stores/useThemeStore';
import { useAuth } from '@/hooks/useAuth';
import { ChangePasswordModal } from '@/components/auth/ChangePasswordModal';
import { useStompStatus } from '@/lib/stompClient';
import { Badge } from '@/components/ui/badge';
import { NotificationDropdown } from '@/features/notification';
import { GlobalSearchModal } from '@/features/search';
import {
  GraduationCap,
  Coins,
  Zap,
  Bell,
  Search,
  Menu,
  X,
  ChevronDown,
  LogOut,
  KeyRound,
  Monitor,
  Sun,
  Moon,
  User,
  Users,
  ShieldAlert
} from 'lucide-react';

interface HeaderProps {
  isMobileMenuOpen: boolean;
  onToggleMobileMenu: () => void;
}

export function Header({ isMobileMenuOpen, onToggleMobileMenu }: HeaderProps) {
  const { user } = useAuthStore();
  const { coins, points, unreadNotifications } = useGamificationStore();
  const { logout } = useAuth();
  const { theme, toggleTheme } = useThemeStore();
  const stompStatus = useStompStatus();
  const navigate = useNavigate();

  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [isSearchModalOpen, setIsSearchModalOpen] = useState(false);

  // Phím tắt Ctrl + K / Cmd + K cho thanh tìm kiếm toàn cục
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && (e.key === 'k' || e.key === 'K' || e.code === 'KeyK')) {
        e.preventDefault();
        e.stopPropagation();
        setIsSearchModalOpen((prev) => !prev);
      }
    };

    window.addEventListener('keydown', handleKeyDown, { capture: true });
    return () => window.removeEventListener('keydown', handleKeyDown, { capture: true });
  }, []);

  return (
    <>
      <header className="sticky top-0 z-40 bg-white/85 dark:bg-slate-900/85 backdrop-blur-md border-b border-slate-200/80 dark:border-slate-800 shadow-xs transition-colors">
        <div className="w-full px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
          {/* Left: Mobile Toggle & Brand */}
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onToggleMobileMenu}
              className="md:hidden p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
              aria-label="Toggle Mobile Menu"
            >
              {isMobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>

            <Link to="/feed" className="flex items-center gap-2.5 group">
              <div className="w-10 h-10 rounded-xl bg-linear-to-tr from-indigo-600 to-violet-600 flex items-center justify-center text-white shadow-md shadow-indigo-500/20 group-hover:scale-105 transition-transform duration-200">
                <GraduationCap className="w-6 h-6" />
              </div>
              <div className="hidden sm:block">
                <span className="text-lg font-bold bg-linear-to-r from-indigo-600 to-violet-700 dark:from-indigo-400 dark:to-violet-400 bg-clip-text text-transparent">
                  SQB Network
                </span>
                <span className="block text-[10px] text-slate-400 dark:text-slate-500 font-medium leading-none">
                  HUST Academic & Social
                </span>
              </div>
            </Link>
          </div>

          {/* Center: Search Bar (Desktop) */}
          <div className="hidden md:flex flex-1 max-w-md mx-4">
            <div
              onClick={() => setIsSearchModalOpen(true)}
              className="relative w-full cursor-pointer group"
            >
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 dark:text-slate-500 group-hover:text-indigo-500 transition-colors" />
              <input
                type="text"
                readOnly
                placeholder="Tìm môn học, sinh viên, câu hỏi, đề thi... (Ctrl + K)"
                className="w-full pl-10 pr-16 py-2 text-sm bg-slate-100/70 dark:bg-slate-800/80 border border-transparent group-hover:border-slate-200 dark:group-hover:border-slate-700 focus:border-indigo-500 text-slate-900 dark:text-slate-100 rounded-xl outline-none transition-all placeholder:text-slate-400 dark:placeholder:text-slate-500 cursor-pointer"
              />
              <kbd className="absolute right-2.5 top-1/2 -translate-y-1/2 hidden lg:inline-flex items-center px-1.5 py-0.5 text-[10px] font-mono font-medium text-slate-500 dark:text-slate-400 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded shadow-2xs">
                Ctrl K
              </kbd>
            </div>
          </div>

          {/* Right: Gamification HUD, STOMP status, Theme Toggle & Profile */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Gamification Wallet: Coins HUD (dành cho Sinh viên) */}
            {user?.role !== 'ADMIN' && (
              <div
                title="Ví Xu Sinh Viên (Dùng để đổi vật phẩm)"
                className="flex items-center gap-1.5 px-3 py-1.5 bg-amber-50/80 dark:bg-amber-950/40 hover:bg-amber-100/80 dark:hover:bg-amber-900/50 border border-amber-200/80 dark:border-amber-800/60 rounded-full transition-all cursor-pointer shadow-2xs"
              >
                <div className="w-5 h-5 rounded-full bg-linear-to-tr from-amber-400 to-yellow-500 flex items-center justify-center text-white shadow-xs">
                  <Coins className="w-3.5 h-3.5" />
                </div>
                <span className="text-xs sm:text-sm font-bold text-amber-900 dark:text-amber-300 tracking-tight">
                  {coins.toLocaleString()}
                </span>
                <span className="text-[10px] font-semibold text-amber-700 dark:text-amber-400 hidden sm:inline">xu</span>
              </div>
            )}

            {/* Gamification Wallet: Points HUD */}
            {user?.role !== 'ADMIN' && (
              <div
                title="Điểm cống hiến học thuật (Tính vào Bảng xếp hạng)"
                className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 bg-indigo-50/80 dark:bg-indigo-950/40 hover:bg-indigo-100/80 dark:hover:bg-indigo-900/50 border border-indigo-200/80 dark:border-indigo-800/60 rounded-full transition-all cursor-pointer shadow-2xs"
              >
                <div className="w-5 h-5 rounded-full bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white shadow-xs">
                  <Zap className="w-3.5 h-3.5" />
                </div>
                <span className="text-xs sm:text-sm font-bold text-indigo-900 dark:text-indigo-300 tracking-tight">
                  {points.toLocaleString()}
                </span>
                <span className="text-[10px] font-semibold text-indigo-700 dark:text-indigo-400">pts</span>
              </div>
            )}

            {/* STOMP WebSocket Connection Status Dot */}
            <div
              title={`Trạng thái Realtime WebSocket: ${stompStatus}`}
              className="p-2 rounded-xl text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors flex items-center justify-center"
            >
              <div
                className={`w-2.5 h-2.5 rounded-full ring-2 ring-white dark:ring-slate-900 transition-colors ${
                  stompStatus === 'CONNECTED'
                    ? 'bg-emerald-500 shadow-xs shadow-emerald-500/50'
                    : stompStatus === 'CONNECTING'
                    ? 'bg-amber-500 animate-ping'
                    : 'bg-slate-400'
                }`}
              />
            </div>

            {/* Dark Mode Toggle Button */}
            <button
              type="button"
              onClick={toggleTheme}
              title={theme === 'dark' ? 'Chuyển sang giao diện Sáng' : 'Chuyển sang giao diện Tối'}
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer"
              aria-label="Chuyển giao diện sáng tối"
            >
              {theme === 'dark' ? (
                <Sun className="w-5 h-5 text-amber-400 animate-in spin-in-180 duration-200" />
              ) : (
                <Moon className="w-5 h-5 text-slate-600 hover:text-indigo-600 animate-in spin-in-180 duration-200" />
              )}
            </button>

            {/* Notification Bell */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setIsNotificationOpen(!isNotificationOpen)}
                className="relative p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-100 transition-colors cursor-pointer"
                aria-label="Thông báo"
              >
                <Bell className="w-5 h-5" />
                {unreadNotifications > 0 && (
                  <span className="absolute top-1.5 right-1.5 w-4 h-4 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center ring-2 ring-white dark:ring-slate-900 animate-pulse">
                    {unreadNotifications}
                  </span>
                )}
              </button>

              <NotificationDropdown
                isOpen={isNotificationOpen}
                onClose={() => setIsNotificationOpen(false)}
              />
            </div>

            {/* User Profile Menu */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                className="flex items-center gap-2 p-1 pl-1.5 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer border border-transparent hover:border-slate-200 dark:hover:border-slate-700"
              >
                {/* Avatar with optional frame */}
                <div className="relative">
                  <div className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white font-bold text-xs sm:text-sm shadow-xs overflow-hidden">
                    {user?.avatarUrl ? (
                      <img src={user.avatarUrl} alt={user.username} className="w-full h-full object-cover" />
                    ) : (
                      user?.username?.charAt(0).toUpperCase() || 'S'
                    )}
                  </div>
                  {user?.frameUrl && (
                    <img
                      src={user.frameUrl}
                      alt="Avatar Frame"
                      className="absolute -inset-1 w-[calc(100%+8px)] h-[calc(100%+8px)] object-contain pointer-events-none"
                    />
                  )}
                </div>

                <div className="hidden lg:block text-left pr-1">
                  <p className="text-xs font-bold text-slate-800 dark:text-slate-100 max-w-[100px] truncate leading-tight">
                    {user?.username || 'Sinh viên'}
                  </p>
                  <p className="text-[10px] font-semibold text-indigo-600 dark:text-indigo-400 capitalize">
                    {user?.role === 'LECTURER' ? 'Giảng viên' : user?.role === 'ADMIN' ? 'Admin' : 'Sinh viên'}
                  </p>
                </div>

                <ChevronDown className="w-4 h-4 text-slate-400 hidden sm:block" />
              </button>

              {/* Profile Dropdown Menu */}
              {isDropdownOpen && (
                <>
                  <div className="fixed inset-0 z-40" onClick={() => setIsDropdownOpen(false)} />
                  <div className="absolute right-0 mt-2 w-64 bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-slate-200/80 dark:border-slate-800 py-2 z-50 animate-in fade-in-50 zoom-in-95 duration-100">
                    <div className="px-4 py-3 border-b border-slate-100 dark:border-slate-800/80">
                      <p className="text-sm font-bold text-slate-800 dark:text-slate-100 truncate">
                        {user?.username || 'sinhvien.hust@edu.vn'}
                      </p>
                      <div className="flex items-center gap-2 mt-1">
                        <Badge variant="role" size="sm">
                          {user?.role || 'STUDENT'}
                        </Badge>
                        <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">● Đang hoạt động</span>
                      </div>
                    </div>

                    <div className="py-1">
                      <button
                        type="button"
                        onClick={() => {
                          setIsDropdownOpen(false);
                          navigate('/profile');
                        }}
                        className="w-full px-4 py-2.5 text-left text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800/60 flex items-center gap-2.5 transition-colors cursor-pointer"
                      >
                        <User className="w-4 h-4 text-indigo-500" />
                        <span>Trang cá nhân</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          setIsDropdownOpen(false);
                          navigate('/friends');
                        }}
                        className="w-full px-4 py-2.5 text-left text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800/60 flex items-center gap-2.5 transition-colors cursor-pointer"
                      >
                        <Users className="w-4 h-4 text-slate-400" />
                        <span>Bạn bè & Lời mời</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          setIsDropdownOpen(false);
                          navigate('/blocked-users');
                        }}
                        className="w-full px-4 py-2.5 text-left text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800/60 flex items-center gap-2.5 transition-colors cursor-pointer"
                      >
                        <ShieldAlert className="w-4 h-4 text-slate-400" />
                        <span>Danh sách chặn</span>
                      </button>

                      <div className="my-1 border-t border-slate-100 dark:border-slate-800/80" />

                      <button
                        type="button"
                        onClick={() => {
                          setIsDropdownOpen(false);
                          setIsChangePasswordOpen(true);
                        }}
                        className="w-full px-4 py-2.5 text-left text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800/60 flex items-center gap-2.5 transition-colors cursor-pointer"
                      >
                        <KeyRound className="w-4 h-4 text-slate-400" />
                        <span>Đổi mật khẩu</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          setIsDropdownOpen(false);
                          navigate('/devices');
                        }}
                        className="w-full px-4 py-2.5 text-left text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800/60 flex items-center gap-2.5 transition-colors cursor-pointer"
                      >
                        <Monitor className="w-4 h-4 text-slate-400" />
                        <span>Quản lý thiết bị đăng nhập</span>
                      </button>
                    </div>

                    <div className="border-t border-slate-100 dark:border-slate-800/80 pt-1">
                      <button
                        type="button"
                        onClick={() => {
                          setIsDropdownOpen(false);
                          logout();
                        }}
                        className="w-full px-4 py-2.5 text-left text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 flex items-center gap-2.5 transition-colors cursor-pointer font-medium"
                      >
                        <LogOut className="w-4 h-4" />
                        <span>Đăng xuất tài khoản</span>
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Modal đổi mật khẩu */}
      <ChangePasswordModal isOpen={isChangePasswordOpen} onClose={() => setIsChangePasswordOpen(false)} />

      {/* Modal tìm kiếm toàn cục (Ctrl + K) */}
      <GlobalSearchModal isOpen={isSearchModalOpen} onClose={() => setIsSearchModalOpen(false)} />
    </>
  );
}
