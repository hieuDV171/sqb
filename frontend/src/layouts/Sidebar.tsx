import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuthStore, type UserRole } from '@/stores/useAuthStore';
import {
  Newspaper,
  Trophy,
  BookOpen,
  MessageSquare,
  ShoppingBag,
  Monitor,
  ClipboardCheck,
  FileText,
  Users,
  ShieldAlert,
  Settings,
  ChevronLeft,
  ChevronRight,
  UserCheck
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

export interface NavItem {
  label: string;
  path: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: {
    text: string;
    variant: 'gold' | 'role' | 'success' | 'destructive' | 'info';
  };
  roles?: UserRole[];
}

export const ROLE_NAV_ITEMS: Record<UserRole, NavItem[]> = {
  STUDENT: [
    {
      label: 'Bảng tin',
      path: '/feed',
      icon: Newspaper,
    },
    {
      label: 'Bạn bè',
      path: '/friends',
      icon: Users,
    },
    {
      label: 'Luyện tập & Đề xuất',
      path: '/questions',
      icon: BookOpen,
    },
    {
      label: 'Bảng xếp hạng',
      path: '/leaderboard',
      icon: Trophy,
      badge: { text: 'Hot', variant: 'gold' },
    },
    {
      label: 'Tin nhắn',
      path: '/messages',
      icon: MessageSquare,
    },
    {
      label: 'Cửa hàng vật phẩm',
      path: '/shop',
      icon: ShoppingBag,
      badge: { text: 'Mới', variant: 'role' },
    },
    {
      label: 'Quản lý thiết bị',
      path: '/devices',
      icon: Monitor,
    },
  ],
  LECTURER: [
    {
      label: 'Bảng tin',
      path: '/feed',
      icon: Newspaper,
    },
    {
      label: 'Bạn bè & Đồng nghiệp',
      path: '/friends',
      icon: Users,
    },
    {
      label: 'Duyệt câu hỏi',
      path: '/lecturer/sessions',
      icon: ClipboardCheck,
      badge: { text: 'Cần duyệt', variant: 'destructive' },
    },
    {
      label: 'Ngân hàng môn học',
      path: '/lecturer/questions',
      icon: BookOpen,
    },
    {
      label: 'Quản lý đề thi',
      path: '/lecturer/exams',
      icon: FileText,
    },
    {
      label: 'Tin nhắn trao đổi',
      path: '/messages',
      icon: MessageSquare,
    },
    {
      label: 'Quản lý thiết bị',
      path: '/devices',
      icon: Monitor,
    },
  ],
  ADMIN: [
    {
      label: 'Quản lý tài khoản',
      path: '/admin/users',
      icon: Users,
    },
    {
      label: 'Báo cáo vi phạm',
      path: '/admin/reports',
      icon: ShieldAlert,
      badge: { text: 'Khẩn', variant: 'destructive' },
    },
    {
      label: 'Cấu hình hệ thống',
      path: '/admin/settings',
      icon: Settings,
    },
    {
      label: 'Quản lý thiết bị',
      path: '/devices',
      icon: Monitor,
    },
  ],
};

interface SidebarProps {
  isCollapsed: boolean;
  onToggleCollapse: () => void;
}

export function Sidebar({ isCollapsed, onToggleCollapse }: SidebarProps) {
  const { user } = useAuthStore();
  const location = useLocation();
  const currentRole: UserRole = user?.role || 'STUDENT';
  const navItems = ROLE_NAV_ITEMS[currentRole] || ROLE_NAV_ITEMS.STUDENT;

  return (
    <aside
      className={cn(
        'hidden md:flex flex-col sticky top-16 h-[calc(100dvh-4rem)] shrink-0 border-r border-slate-200/80 dark:border-slate-800 bg-white/70 dark:bg-slate-900/70 backdrop-blur-md transition-all duration-300 z-30',
        isCollapsed ? 'w-20' : 'w-64'
      )}
    >
      {/* Role Indicator Banner */}
      <div className={cn('p-3.5 border-b border-slate-200/60 dark:border-slate-800/60 shrink-0', isCollapsed && 'px-2 text-center')}>
        {!isCollapsed ? (
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-slate-400 dark:text-slate-500 uppercase tracking-wider">
                Không gian
              </span>
              <Badge variant="role">
                {currentRole === 'STUDENT' ? 'Sinh viên' : currentRole === 'LECTURER' ? 'Giảng viên' : 'Quản trị viên'}
              </Badge>
            </div>
          </div>
        ) : (
          <div className="flex justify-center" title={`Vai trò: ${currentRole}`}>
            <UserCheck className="w-5 h-5 text-indigo-500" />
          </div>
        )}
      </div>

      {/* Main Navigation Links */}
      <div className="flex-1 py-3 px-3 space-y-1 overflow-y-auto min-h-0">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path || location.pathname.startsWith(`${item.path}/`);

          return (
            <Link
              key={item.path}
              to={item.path}
              title={isCollapsed ? item.label : undefined}
              className={cn(
                'flex items-center gap-3 px-3 py-2 rounded-xl font-medium text-sm transition-all duration-150 group relative',
                isActive
                  ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 shadow-xs font-semibold'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100/80 dark:hover:bg-slate-800/60 hover:text-slate-900 dark:hover:text-slate-100',
                isCollapsed && 'justify-center px-0'
              )}
            >
              <Icon
                className={cn(
                  'w-5 h-5 shrink-0 transition-transform group-hover:scale-110 duration-200',
                  isActive ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400 dark:text-slate-500 group-hover:text-slate-600 dark:group-hover:text-slate-300'
                )}
              />

              {!isCollapsed && (
                <div className="flex-1 flex items-center justify-between truncate">
                  <span className="truncate">{item.label}</span>
                  {item.badge && (
                    <Badge variant={item.badge.variant} size="sm">
                      {item.badge.text}
                    </Badge>
                  )}
                </div>
              )}

              {/* Collapsed Active Indicator Pill */}
              {isCollapsed && isActive && (
                <div className="absolute right-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-indigo-600 dark:bg-indigo-400 rounded-l-full" />
              )}
            </Link>
          );
        })}
      </div>

      {/* Collapse Toggle Button */}
      <button
        type="button"
        onClick={onToggleCollapse}
        className="absolute -right-3.5 top-20 w-7 h-7 rounded-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center text-slate-500 hover:text-indigo-600 dark:hover:text-indigo-400 shadow-sm cursor-pointer z-40 transition-transform hover:scale-110"
        aria-label={isCollapsed ? 'Mở rộng menu' : 'Thu gọn menu'}
      >
        {isCollapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
      </button>
    </aside>
  );
}
