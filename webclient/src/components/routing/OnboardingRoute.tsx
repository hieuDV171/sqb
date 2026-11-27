import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';

export function OnboardingRoute() {
  const { isAuthenticated, accountState } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (accountState === 'ACTIVE') {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
