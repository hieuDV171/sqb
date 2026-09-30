import type { ReactNode } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore, type UserRole } from '@/stores/useAuthStore';
import { toast } from '@/stores/useToastStore';

interface RoleGuardProps {
  allowedRoles: UserRole[];
  children?: ReactNode;
}

export function RoleGuard({ allowedRoles, children }: RoleGuardProps) {
  const { user, isAuthenticated } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const currentRole = user?.role || 'STUDENT';

  if (!allowedRoles.includes(currentRole)) {
    toast.error(
      `Tài khoản (${currentRole}) không có quyền truy cập vào tài nguyên này.`,
      'Không có quyền (403)'
    );
    return <Navigate to="/403" replace />;
  }

  return children ? <>{children}</> : <Outlet />;
}
