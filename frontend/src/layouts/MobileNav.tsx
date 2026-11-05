import { Link, useLocation } from 'react-router-dom';
import { LogOut, Menu, UserRound, X } from 'lucide-react';
import { useAuthStore, type UserRole } from '@/stores/useAuthStore';
import { useAuth } from '@/hooks/useAuth';
import { useModalFocusTrap } from '@/hooks/useModalFocusTrap';
import { cn } from '@/lib/utils';
import { MOBILE_NAV_ITEMS, ROLE_NAV_SECTIONS, isNavItemActive } from './navigation';

interface MobileNavProps {
  isOpen: boolean;
  onOpen: () => void;
  onClose: () => void;
}

export function MobileNav({ isOpen, onOpen, onClose }: MobileNavProps) {
  const { user } = useAuthStore();
  const { logout } = useAuth();
  const location = useLocation();
  const currentRole: UserRole = user?.role || 'STUDENT';
  const mobileItems = MOBILE_NAV_ITEMS[currentRole] ?? MOBILE_NAV_ITEMS.STUDENT;
  const sections = ROLE_NAV_SECTIONS[currentRole] ?? ROLE_NAV_SECTIONS.STUDENT;
  const panelRef = useModalFocusTrap(isOpen, onClose);

  return (
    <>
      <nav
        className="fixed inset-x-0 bottom-0 z-40 grid h-[calc(var(--app-mobile-nav-height)+env(safe-area-inset-bottom))] grid-cols-5 border-t border-border bg-card/95 px-1 pb-[env(safe-area-inset-bottom)] shadow-[0_-8px_24px_-20px_rgb(15_23_42_/_0.45)] backdrop-blur-xl md:hidden"
        aria-label="Điều hướng di động"
      >
        {mobileItems.map((item) => {
          const Icon = item.icon;
          const active = isNavItemActive(location.pathname, item);
          return (
            <Link
              key={item.path}
              to={item.path}
              aria-current={active ? 'page' : undefined}
              className={cn(
                'relative flex min-w-0 flex-col items-center justify-center gap-1 rounded-xl px-1 py-2 text-[0.68rem] font-semibold transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                active ? 'text-primary' : 'text-muted-foreground hover:text-foreground'
              )}
            >
              <span className={cn('grid size-8 place-items-center rounded-xl', active && 'bg-accent')}>
                <Icon className="size-5" aria-hidden="true" />
              </span>
              <span className="max-w-full truncate">{item.shortLabel ?? item.label}</span>
              {active && <span className="absolute top-1 h-0.5 w-5 rounded-full bg-primary" aria-hidden="true" />}
            </Link>
          );
        })}

        <button
          type="button"
          onClick={isOpen ? onClose : onOpen}
          className={cn(
            'flex min-w-0 flex-col items-center justify-center gap-1 rounded-xl px-1 py-2 text-[0.68rem] font-semibold text-muted-foreground transition-colors duration-200 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
            isOpen && 'text-primary'
          )}
          aria-label="Mở toàn bộ menu"
          aria-expanded={isOpen}
          aria-controls="mobile-navigation-panel"
          data-mobile-more-trigger
        >
          <span className={cn('grid size-8 place-items-center rounded-xl', isOpen && 'bg-accent')}>
            <Menu className="size-5" aria-hidden="true" />
          </span>
          <span>Thêm</span>
        </button>
      </nav>

      {isOpen && (
        <div className="fixed inset-0 z-50 md:hidden" role="presentation">
          <button type="button" className="absolute inset-0 bg-slate-950/55 backdrop-blur-[2px]" onClick={onClose} aria-label="Đóng menu" />
          <div
            ref={panelRef}
            id="mobile-navigation-panel"
            role="dialog"
            aria-modal="true"
            aria-labelledby="mobile-navigation-title"
            className="absolute inset-x-0 bottom-0 max-h-[88dvh] overflow-hidden rounded-t-3xl border border-border bg-card shadow-2xl"
          >
            <div className="mx-auto mt-2 h-1 w-10 rounded-full bg-border" aria-hidden="true" />
            <div className="flex items-center justify-between border-b border-border px-5 py-4">
              <div className="min-w-0">
                <p id="mobile-navigation-title" className="text-lg font-bold tracking-tight text-foreground">Không gian của bạn</p>
                <p className="mt-0.5 truncate text-sm text-muted-foreground">{user?.username ?? 'Tài khoản SQB'}</p>
              </div>
              <button
                type="button"
                onClick={onClose}
                data-autofocus
                className="grid size-11 place-items-center rounded-xl text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                aria-label="Đóng menu"
              >
                <X className="size-5" />
              </button>
            </div>

            <div className="max-h-[calc(88dvh-9rem)] overflow-y-auto px-4 py-5">
              <Link
                to="/profile"
                onClick={onClose}
                className="mb-6 flex min-h-14 items-center gap-3 rounded-2xl border border-border bg-surface-subtle px-4 py-3 text-sm font-semibold text-foreground"
              >
                <span className="grid size-10 place-items-center overflow-hidden rounded-full bg-primary text-primary-foreground">
                  {user?.avatarUrl ? <img src={user.avatarUrl} alt="" className="size-full object-cover" /> : <UserRound className="size-5" />}
                </span>
                <span className="min-w-0 flex-1 truncate">Xem trang cá nhân</span>
              </Link>

              <div className="space-y-6">
                {sections.map((section) => (
                  <section key={section.label}>
                    <h2 className="mb-2 px-2 text-[0.7rem] font-bold uppercase tracking-[0.13em] text-muted-foreground">{section.label}</h2>
                    <div className="grid grid-cols-2 gap-2">
                      {section.items.map((item) => {
                        const Icon = item.icon;
                        const active = isNavItemActive(location.pathname, item);
                        return (
                          <Link
                            key={item.path}
                            to={item.path}
                            onClick={onClose}
                            aria-current={active ? 'page' : undefined}
                            className={cn(
                              'flex min-h-14 items-center gap-3 rounded-2xl border px-3 py-3 text-sm font-semibold transition-colors',
                              active
                                ? 'border-primary/25 bg-accent text-accent-foreground'
                                : 'border-transparent bg-surface-subtle text-foreground hover:border-border'
                            )}
                          >
                            <Icon className={cn('size-5 shrink-0', active ? 'text-primary' : 'text-muted-foreground')} />
                            <span className="min-w-0 truncate">{item.shortLabel ?? item.label}</span>
                          </Link>
                        );
                      })}
                    </div>
                  </section>
                ))}
              </div>

              <button
                type="button"
                onClick={() => {
                  onClose();
                  logout();
                }}
                className="mt-6 flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl bg-destructive/10 px-4 text-sm font-semibold text-destructive transition-colors hover:bg-destructive/15"
              >
                <LogOut className="size-4" />
                Đăng xuất
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
