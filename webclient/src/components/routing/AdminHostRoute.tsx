import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { getAdminPageRedirect } from '@/lib/adminAccess';

export function AdminHostRoute() {
  const location = useLocation();
  const destination = getAdminPageRedirect(
    window.location.hostname, location.pathname, location.search, location.hash,
  );

  useEffect(() => {
    if (destination) window.location.replace(destination);
  }, [destination]);

  if (destination) return <div role="status">Đang mở trang quản trị…</div>;
  return <Outlet />;
}
