import type React from 'react';
import type { UserRole } from '@/stores/useAuthStore';
import {
  Bell,
  BookOpen,
  ClipboardCheck,
  Database,
  FileText,
  Home,
  MessageSquare,
  Monitor,
  Newspaper,
  PlusCircle,
  School,
  Settings,
  ShieldAlert,
  ShoppingBag,
  Trophy,
  Users,
} from 'lucide-react';

export interface NavItem {
  label: string;
  shortLabel?: string;
  path: string;
  icon: React.ComponentType<{ className?: string }>;
  activePrefixes?: string[];
  badge?: {
    text: string;
    variant: 'gold' | 'role' | 'success' | 'destructive' | 'info';
  };
}

export interface NavSection {
  label: string;
  items: NavItem[];
}

export const ROLE_NAV_SECTIONS: Record<UserRole, NavSection[]> = {
  STUDENT: [
    {
      label: 'Cộng đồng',
      items: [
        { label: 'Bảng tin', shortLabel: 'Trang chủ', path: '/feed', icon: Newspaper },
        { label: 'Bạn bè', path: '/friends', icon: Users },
        { label: 'Tin nhắn', path: '/messages', icon: MessageSquare },
      ],
    },
    {
      label: 'Học tập',
      items: [
        {
          label: 'Ngân hàng câu hỏi',
          shortLabel: 'Học tập',
          path: '/questions',
          icon: BookOpen,
          activePrefixes: ['/questions', '/sessions'],
        },
        { label: 'Đề xuất câu hỏi', shortLabel: 'Đề xuất', path: '/sessions/propose', icon: PlusCircle },
      ],
    },
    {
      label: 'Thành tích',
      items: [
        {
          label: 'Bảng xếp hạng',
          path: '/leaderboard',
          icon: Trophy,
          activePrefixes: ['/leaderboard', '/gamification'],
        },
        { label: 'Cửa hàng vật phẩm', path: '/shop', icon: ShoppingBag },
      ],
    },
    {
      label: 'Tài khoản',
      items: [
        { label: 'Cài đặt tài khoản', path: '/settings', icon: Settings },
        { label: 'Quản lý thiết bị', path: '/devices', icon: Monitor },
      ],
    },
  ],
  LECTURER: [
    {
      label: 'Cộng đồng',
      items: [
        { label: 'Bảng tin', path: '/feed', icon: Newspaper },
        { label: 'Bạn bè & Đồng nghiệp', path: '/friends', icon: Users },
        { label: 'Tin nhắn trao đổi', path: '/messages', icon: MessageSquare },
      ],
    },
    {
      label: 'Không gian giảng dạy',
      items: [
        {
          label: 'Hàng đợi kiểm duyệt',
          shortLabel: 'Kiểm duyệt',
          path: '/lecturer/sessions',
          icon: ClipboardCheck,
          activePrefixes: ['/lecturer/sessions', '/lecturer/reviews'],
        },
        { label: 'Ngân hàng môn học', path: '/lecturer/questions', icon: BookOpen },
        { label: 'Quản lý đề thi', path: '/lecturer/exams', icon: FileText },
        { label: 'Lớp học phần', shortLabel: 'Lớp học', path: '/lecturer/course-classes', icon: School },
      ],
    },
    {
      label: 'Tài khoản',
      items: [
        { label: 'Cài đặt tài khoản', path: '/settings', icon: Settings },
        { label: 'Quản lý thiết bị', path: '/devices', icon: Monitor },
      ],
    },
  ],
  ADMIN: [
    {
      label: 'Vận hành',
      items: [
        { label: 'Quản lý tài khoản', shortLabel: 'Tài khoản', path: '/admin/users', icon: Users },
        { label: 'Báo cáo vi phạm', shortLabel: 'Báo cáo', path: '/admin/reports', icon: ShieldAlert },
        { label: 'Lớp học phần', shortLabel: 'Lớp học', path: '/lecturer/course-classes', icon: School },
      ],
    },
    {
      label: 'Hệ thống',
      items: [
        { label: 'Đồng bộ bảng xếp hạng', path: '/admin/leaderboards', icon: Database },
        { label: 'Cấu hình hệ thống', shortLabel: 'Cấu hình', path: '/admin/settings', icon: Settings },
        { label: 'Quản lý thiết bị', path: '/devices', icon: Monitor },
      ],
    },
    {
      label: 'Cá nhân',
      items: [{ label: 'Cài đặt tài khoản', path: '/settings', icon: Settings }],
    },
  ],
};

export const ROLE_NAV_ITEMS: Record<UserRole, NavItem[]> = {
  STUDENT: ROLE_NAV_SECTIONS.STUDENT.flatMap((section) => section.items),
  LECTURER: ROLE_NAV_SECTIONS.LECTURER.flatMap((section) => section.items),
  ADMIN: ROLE_NAV_SECTIONS.ADMIN.flatMap((section) => section.items),
};

export const MOBILE_NAV_ITEMS: Record<UserRole, NavItem[]> = {
  STUDENT: [
    { label: 'Bảng tin', shortLabel: 'Trang chủ', path: '/feed', icon: Home },
    {
      label: 'Ngân hàng câu hỏi',
      shortLabel: 'Học tập',
      path: '/questions',
      icon: BookOpen,
      activePrefixes: ['/questions', '/sessions'],
    },
    { label: 'Đề xuất câu hỏi', shortLabel: 'Đề xuất', path: '/sessions/propose', icon: PlusCircle },
    { label: 'Thông báo', shortLabel: 'Thông báo', path: '/notifications', icon: Bell },
  ],
  LECTURER: [
    { label: 'Bảng tin', shortLabel: 'Trang chủ', path: '/feed', icon: Home },
    {
      label: 'Hàng đợi kiểm duyệt',
      shortLabel: 'Kiểm duyệt',
      path: '/lecturer/sessions',
      icon: ClipboardCheck,
      activePrefixes: ['/lecturer/sessions', '/lecturer/reviews'],
    },
    { label: 'Lớp học phần', shortLabel: 'Lớp học', path: '/lecturer/course-classes', icon: School },
    { label: 'Tin nhắn', shortLabel: 'Tin nhắn', path: '/messages', icon: MessageSquare },
  ],
  ADMIN: [
    { label: 'Quản lý tài khoản', shortLabel: 'Tài khoản', path: '/admin/users', icon: Users },
    { label: 'Báo cáo vi phạm', shortLabel: 'Báo cáo', path: '/admin/reports', icon: ShieldAlert },
    { label: 'Lớp học phần', shortLabel: 'Lớp học', path: '/lecturer/course-classes', icon: School },
    { label: 'Cấu hình hệ thống', shortLabel: 'Cấu hình', path: '/admin/settings', icon: Settings },
  ],
};

export function isNavItemActive(pathname: string, item: NavItem) {
  if (pathname === '/sessions/propose' && item.path !== '/sessions/propose') return false;
  const prefixes = item.activePrefixes ?? [item.path];
  return prefixes.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`));
}
