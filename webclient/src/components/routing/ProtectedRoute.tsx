import { lazy, type ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';
import { RouteContent } from './RouteContent';
import { routeModules } from './routeModules';

const AppLayout = lazy(routeModules.AppLayout);

interface ProtectedRouteProps {
  children?: ReactNode;
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const accountState = useAuthStore((state) => state.accountState);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (accountState !== 'ACTIVE') {
    return <Navigate to="/account-setup" replace />;
  }

  return (
    <RouteContent routeKey={location.key}>
      <AppLayout>
        <RouteContent routeKey={location.key}>{children ? children : <Outlet />}</RouteContent>
      </AppLayout>
    </RouteContent>
  );
}
