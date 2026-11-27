import { useEffect, useRef, useState, type ReactNode } from 'react';
import { AUTH_EVENT_KEY, useAuthStore } from '@/stores/useAuthStore';
import { restoreWebSession } from '@/api/webSession';
import { getAccessGatewayError } from '@/api/accessSession';
import { reportApiConnectionFailure } from '@/api/connectivity';
import { queryClient } from '@/lib/queryClient';
import { stompClient } from '@/lib/stompClient';
import { clearFeedSnapshot } from '@/features/feed/services/feedSnapshot';
import { Button } from '@/components/ui/button';
import { SessionLoading } from './SessionLoading';
import { warmInitialRoute } from './routeModules';

export function AuthSessionGate({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const generation = useRef(0);

  useEffect(() => {
    let disposed = false;
    const restore = () => {
      const current = ++generation.current;
      setStatus('loading');
      void restoreWebSession().then((session) => {
        if (disposed || current !== generation.current) return;
        if (session) {
          // Load shell and page together, after auth traffic has finished. Starting
          // these modulepreloads earlier can starve refresh/profile on slow links.
          void warmInitialRoute(window.location.pathname).catch(() => undefined);
          // Header/feed reuse the profile already validated during restoration.
          queryClient.setQueryData(['userProfile', 'me'], session.profile);
          useAuthStore.getState().setAuth(session.accessToken, session.user, 'ACCESS', 'ACTIVE', false);
        } else {
          useAuthStore.getState().logout(false);
          void clearFeedSnapshot();
        }
        setStatus('ready');
      }).catch((failure: unknown) => {
        if (disposed || current !== generation.current) return;
        reportApiConnectionFailure(failure);
        setError(getAccessGatewayError(failure)?.message ?? 'Chưa kết nối được với máy chủ. Vui lòng kiểm tra kết nối và thử lại.');
        setStatus('error');
      });
    };
    const onStorage = (event: StorageEvent) => {
      if (event.key !== AUTH_EVENT_KEY || !event.newValue) return;
      let type: unknown;
      try { type = JSON.parse(event.newValue).type; } catch { return; }
      if (type !== 'login' && type !== 'logout') return;
      ++generation.current;
      stompClient.disconnect();
      useAuthStore.getState().logout(false);
      queryClient.clear();
      if (type === 'login') restore();
      else setStatus('ready');
    };
    restore();
    window.addEventListener('storage', onStorage);
    return () => { disposed = true; window.removeEventListener('storage', onStorage); };
  }, [attempt]);

  if (status === 'ready') return children;
  if (status === 'loading') return <SessionLoading />;
  return <main className="grid min-h-dvh place-items-center p-6">
    <div className="max-w-md space-y-4 text-center">
      <h1 className="text-xl font-semibold">Chưa thể khôi phục phiên</h1>
      <p role="alert" className="text-sm text-muted-foreground">{error}</p>
      <div className="flex flex-wrap justify-center gap-2">
        <Button onClick={() => setAttempt(attempt + 1)}>Thử lại</Button>
        <Button variant="outline" onClick={() => window.location.reload()}>Tải lại trang</Button>
      </div>
    </div>
  </main>;
}
