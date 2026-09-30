import type { ReactNode } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';

interface PublicRouteProps {
  children?: ReactNode;
}

export function PublicRoute({ children }: PublicRouteProps) {
  const { isAuthenticated, user } = useAuthStore();

  if (isAuthenticated) {
    // Chuyển hướng người dùng về trang đích tương ứng với vai trò
    if (user?.role === 'ADMIN') {
      return <Navigate to="/admin/users" replace />;
    }
    return <Navigate to="/feed" replace />;
  }

  return children ? <>{children}</> : <Outlet />;
}
