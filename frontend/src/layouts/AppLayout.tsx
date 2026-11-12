import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/useAuthStore';
import { stompClient } from '@/lib/stompClient';
import { onForegroundMessage, syncCurrentFidIfAuthenticated } from '@/lib/firebase';
import { toast } from '@/stores/useToastStore';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { MobileNav } from './MobileNav';
import { useChatRealtimeListener, FloatingChatWidget } from '@/features/chat';
import { NOTIFICATION_KEYS } from '@/features/notification/hooks/useNotification';

const SIDEBAR_COLLAPSED_STORAGE_KEY = 'sqb-sidebar-collapsed';

interface AppLayoutProps {
  children?: React.ReactNode;
}

interface PushMessageShape {
  notification?: { title?: string; body?: string };
  data?: { title?: string; body?: string };
}

function readPushMessage(payload: unknown): PushMessageShape {
  return payload && typeof payload === 'object' ? (payload as PushMessageShape) : {};
}

export function AppLayout({ children }: AppLayoutProps) {
  const { isAuthenticated } = useAuthStore();
  const queryClient = useQueryClient();
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(() => {
    try {
      return window.localStorage.getItem(SIDEBAR_COLLAPSED_STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  });
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  function toggleSidebar() {
    const collapsed = !isSidebarCollapsed;
    setIsSidebarCollapsed(collapsed);
    try {
      window.localStorage.setItem(SIDEBAR_COLLAPSED_STORAGE_KEY, String(collapsed));
    } catch {
      // Tùy chọn vẫn áp dụng cho trang hiện tại nếu trình duyệt chặn lưu trữ.
    }
  }

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
      stompClient.disconnect();
    };
  }, [isAuthenticated]);

  // Đồng bộ FID và lắng nghe Push Notification Foreground khi đã xác thực
  useEffect(() => {
    if (!isAuthenticated) return;

    // Xử lý cả yêu cầu hủy đang chờ khi quyền thông báo đã bị tắt.
    syncCurrentFidIfAuthenticated().catch(() => {});

    // 2. Lắng nghe thông báo khi người dùng đang mở tab (Foreground Toast)
    let unsubscribe: (() => void) | undefined;
    onForegroundMessage((payload: unknown) => {
      const message = readPushMessage(payload);
      const title = message.notification?.title || message.data?.title || 'Thông báo mới từ SQB';
      const body = message.notification?.body || message.data?.body || '';
      toast.info(body || title, title);
      void queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEYS.lists });
    }).then((unsub) => {
      unsubscribe = unsub;
    }).catch(() => {});

    return () => {
      if (unsubscribe) unsubscribe();
    };
  }, [isAuthenticated, queryClient]);

  return (
    <div className="flex min-h-screen flex-col bg-background font-sans text-foreground transition-colors duration-200">
      <Header
        isMobileMenuOpen={isMobileMenuOpen}
        onToggleMobileMenu={() => setIsMobileMenuOpen((prev) => !prev)}
      />

      <div className="relative flex w-full flex-1">
        <Sidebar
          isCollapsed={isSidebarCollapsed}
          onToggleCollapse={toggleSidebar}
        />

        <MobileNav
          isOpen={isMobileMenuOpen}
          onOpen={() => setIsMobileMenuOpen(true)}
          onClose={() => setIsMobileMenuOpen(false)}
        />

        <main className="min-w-0 flex-1 overflow-x-hidden pb-[calc(var(--app-mobile-nav-height)+env(safe-area-inset-bottom)+var(--space-4))] md:pb-0">
          <div className="mx-auto w-full max-w-[var(--app-content-max-width)] px-4 py-5 sm:px-5 sm:py-6 lg:px-8 lg:py-8">
            {children ? children : <Outlet />}
          </div>
        </main>
      </div>

      <FloatingChatWidget />
    </div>
  );
}
