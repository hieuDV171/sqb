import { Link, useLocation } from 'react-router-dom';
import { useAuthStore, type UserRole } from '@/stores/useAuthStore';
import { useAuth } from '@/hooks/useAuth';
import { ROLE_NAV_ITEMS } from './Sidebar';
import { Badge } from '@/components/ui/badge';
import {
  X,
  GraduationCap,
  LogOut,
  Sparkles,
  User
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface MobileNavProps {
  isOpen: boolean;
  onClose: () => void;
}

export function MobileNav({ isOpen, onClose }: MobileNavProps) {
  const { user, setUserRole } = useAuthStore();
  const { logout } = useAuth();
  const location = useLocation();

  if (!isOpen) return null;

  const currentRole: UserRole = user?.role || 'STUDENT';
  const navItems = ROLE_NAV_ITEMS[currentRole] || ROLE_NAV_ITEMS.STUDENT;

  return (
    <div className="fixed inset-0 z-50 md:hidden flex animate-in fade-in duration-200">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-black/60 backdrop-blur-xs" onClick={onClose} />

      {/* Drawer */}
      <div className="relative w-4/5 max-w-xs bg-white dark:bg-slate-900 h-full flex flex-col shadow-2xl border-r border-slate-200 dark:border-slate-800 z-10">
        {/* Drawer Header */}
        <div className="p-4 flex items-center justify-between border-b border-slate-200/80 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-linear-to-tr from-indigo-600 to-violet-600 flex items-center justify-center text-white shadow-xs">
              <GraduationCap className="w-5 h-5" />
            </div>
            <div>
              <span className="text-base font-bold bg-linear-to-r from-indigo-600 to-violet-700 dark:from-indigo-400 dark:to-violet-400 bg-clip-text text-transparent">
                SQB Network
              </span>
              <span className="block text-[10px] text-slate-400 leading-none">
                HUST Academic & Social
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* User Card */}
        <div className="p-4 bg-slate-50 dark:bg-slate-950/50 border-b border-slate-200/60 dark:border-slate-800/60 flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white font-bold text-sm shadow-xs">
            {user?.avatarUrl ? (
              <img src={user.avatarUrl} alt="" className="w-full h-full object-cover rounded-full" />
            ) : (
              <User className="w-5 h-5" />
            )}
          </div>
          <div className="flex-1 truncate">
            <p className="text-xs font-bold text-slate-800 dark:text-slate-100 truncate">
              {user?.username || 'Sinh viên'}
            </p>
            <Badge variant="role" size="sm" className="mt-1">
              {currentRole}
            </Badge>
          </div>
        </div>

        {/* Navigation Items */}
        <div className="flex-1 py-3 px-3 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path || location.pathname.startsWith(`${item.path}/`);

            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={onClose}
                className={cn(
                  'flex items-center gap-3 px-3 py-2.5 rounded-xl font-medium text-sm transition-colors',
                  isActive
                    ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 shadow-xs font-semibold'
                    : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800/60'
                )}
              >
                <Icon className={cn('w-5 h-5', isActive ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400')} />
                <span className="flex-1 truncate">{item.label}</span>
                {item.badge && (
                  <Badge variant={item.badge.variant} size="sm">
                    {item.badge.text}
                  </Badge>
                )}
              </Link>
            );
          })}
        </div>

        {/* Dev Role Quick Switcher */}
        <div className="p-3 border-t border-slate-200/60 dark:border-slate-800/60 bg-slate-50/50 dark:bg-slate-950/30">
          <div className="space-y-1.5">
            <div className="flex items-center gap-1 text-[11px] font-semibold text-slate-400">
              <Sparkles className="w-3 h-3 text-amber-500" /> Xem vai trò:
            </div>
            <div className="grid grid-cols-3 gap-1">
              {(['STUDENT', 'LECTURER', 'ADMIN'] as UserRole[]).map((r) => (
                <button
                  key={r}
                  type="button"
                  onClick={() => setUserRole(r)}
                  className={cn(
                    'px-1 py-1 text-[10px] font-bold rounded-lg transition-colors cursor-pointer text-center',
                    currentRole === r
                      ? 'bg-indigo-600 text-white shadow-xs'
                      : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-400'
                  )}
                >
                  {r === 'STUDENT' ? 'SV' : r === 'LECTURER' ? 'GV' : 'AD'}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Drawer Footer (Logout) */}
        <div className="p-3 border-t border-slate-200/80 dark:border-slate-800">
          <button
            type="button"
            onClick={() => {
              onClose();
              logout();
            }}
            className="w-full flex items-center justify-center gap-2 px-3 py-2 text-sm font-semibold text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 rounded-xl transition-colors cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Đăng xuất</span>
          </button>
        </div>
      </div>
    </div>
  );
}
