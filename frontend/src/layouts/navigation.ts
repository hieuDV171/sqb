import type React from 'react';
import type { UserRole } from '@/stores/useAuthStore';
import {
  BookOpen,
  ClipboardCheck,
  Database,
  FileText,
  MessageSquare,
  Monitor,
  Newspaper,
  School,
  Settings,
  ShieldAlert,
  ShoppingBag,
  Trophy,
  Users,
} from 'lucide-react';

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
    { label: 'Bảng tin', path: '/feed', icon: Newspaper },
    { label: 'Bạn bè', path: '/friends', icon: Users },
    { label: 'Luyện tập & Đề xuất', path: '/questions', icon: BookOpen },
    {
      label: 'Bảng xếp hạng',
      path: '/leaderboard',
      icon: Trophy,
      badge: { text: 'Hot', variant: 'gold' },
    },
    { label: 'Tin nhắn', path: '/messages', icon: MessageSquare },
    {
      label: 'Cửa hàng vật phẩm',
      path: '/shop',
      icon: ShoppingBag,
      badge: { text: 'Mới', variant: 'role' },
    },
    { label: 'Quản lý thiết bị', path: '/devices', icon: Monitor },
  ],
  LECTURER: [
    { label: 'Bảng tin', path: '/feed', icon: Newspaper },
    { label: 'Bạn bè & Đồng nghiệp', path: '/friends', icon: Users },
    {
      label: 'Duyệt câu hỏi',
      path: '/lecturer/sessions',
      icon: ClipboardCheck,
      badge: { text: 'Cần duyệt', variant: 'destructive' },
    },
    { label: 'Ngân hàng môn học', path: '/lecturer/questions', icon: BookOpen },
    { label: 'Quản lý đề thi', path: '/lecturer/exams', icon: FileText },
    { label: 'Lớp học phần', path: '/lecturer/course-classes', icon: School },
    { label: 'Tin nhắn trao đổi', path: '/messages', icon: MessageSquare },
    { label: 'Quản lý thiết bị', path: '/devices', icon: Monitor },
  ],
  ADMIN: [
    { label: 'Quản lý tài khoản', path: '/admin/users', icon: Users },
    {
      label: 'Báo cáo vi phạm',
      path: '/admin/reports',
      icon: ShieldAlert,
      badge: { text: 'Khẩn', variant: 'destructive' },
    },
    { label: 'Lớp học phần', path: '/lecturer/course-classes', icon: School },
    { label: 'Đồng bộ BXH', path: '/admin/leaderboards', icon: Database },
    { label: 'Cấu hình hệ thống', path: '/admin/settings', icon: Settings },
    { label: 'Quản lý thiết bị', path: '/devices', icon: Monitor },
  ],
};
