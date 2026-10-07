import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';
import { stompClient } from '@/lib/stompClient';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { MobileNav } from './MobileNav';
import { useChatRealtimeListener, FloatingChatWidget } from '@/features/chat';

interface AppLayoutProps {
  children?: React.ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const { isAuthenticated } = useAuthStore();
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // Lắng nghe các sự kiện WebSocket STOMP toàn cục (tin nhắn mới, biên nhận đọc)
  useChatRealtimeListener();

  // Khởi tạo kết nối STOMP WebSocket khi người dùng đã xác thực
  useEffect(() => {
    if (isAuthenticated) {
      stompClient.connect();
    } else {
      stompClient.disconnect();
    }

    return () => {
      // Dọn dẹp khi unmount toàn bộ layout
    };
  }, [isAuthenticated]);

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-800 dark:text-slate-100 flex flex-col font-sans transition-colors duration-200">
      {/* Top Header HUD */}
      <Header
        isMobileMenuOpen={isMobileMenuOpen}
        onToggleMobileMenu={() => setIsMobileMenuOpen((prev) => !prev)}
      />

      {/* Body: Sidebar + Main Content */}
      <div className="flex-1 flex w-full relative">
        {/* Desktop Collapsible Sidebar */}
        <Sidebar
          isCollapsed={isSidebarCollapsed}
          onToggleCollapse={() => setIsSidebarCollapsed((prev) => !prev)}
        />

        {/* Mobile Slide-over Drawer */}
        <MobileNav
          isOpen={isMobileMenuOpen}
          onClose={() => setIsMobileMenuOpen(false)}
        />

        {/* Main Content Area */}
        <main className="flex-1 w-full overflow-x-hidden min-w-0">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
            {children ? children : <Outlet />}
          </div>
        </main>
      </div>

      {/* Floating Chat Widget (Bong bóng chat mini góc phải màn hình) */}
      <FloatingChatWidget />
    </div>
  );
}
