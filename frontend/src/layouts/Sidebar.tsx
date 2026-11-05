import { Link, useLocation } from 'react-router-dom';
import { ChevronLeft, ChevronRight, PanelLeftClose, UserRoundCheck } from 'lucide-react';
import { useAuthStore, type UserRole } from '@/stores/useAuthStore';
import { cn } from '@/lib/utils';
import { ROLE_NAV_SECTIONS, isNavItemActive } from './navigation';

interface SidebarProps {
  isCollapsed: boolean;
  onToggleCollapse: () => void;
}

const ROLE_LABELS: Record<UserRole, string> = {
  STUDENT: 'Không gian sinh viên',
  LECTURER: 'Không gian giảng viên',
  ADMIN: 'Trung tâm vận hành',
};

export function Sidebar({ isCollapsed, onToggleCollapse }: SidebarProps) {
  const { user } = useAuthStore();
  const location = useLocation();
  const currentRole: UserRole = user?.role || 'STUDENT';
  const sections = ROLE_NAV_SECTIONS[currentRole] ?? ROLE_NAV_SECTIONS.STUDENT;

  return (
    <aside
      className={cn(
        'sticky top-[var(--app-header-height)] z-30 hidden h-[calc(100dvh-var(--app-header-height))] shrink-0 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground transition-[width] duration-200 md:flex',
        isCollapsed ? 'w-[var(--app-sidebar-collapsed-width)]' : 'w-[var(--app-sidebar-width)]'
      )}
      aria-label="Điều hướng chính"
    >
      <div className={cn('flex min-h-20 items-center border-b border-sidebar-border px-4', isCollapsed && 'justify-center px-2')}>
        {isCollapsed ? (
          <span className="grid size-10 place-items-center rounded-xl bg-sidebar-accent text-sidebar-accent-foreground" title={ROLE_LABELS[currentRole]}>
            <UserRoundCheck className="size-5" aria-hidden="true" />
          </span>
        ) : (
          <div className="min-w-0">
            <p className="text-[0.68rem] font-bold uppercase tracking-[0.14em] text-muted-foreground">Workspace</p>
            <p className="mt-1 truncate text-sm font-semibold text-foreground">{ROLE_LABELS[currentRole]}</p>
          </div>
        )}
      </div>

      <nav className="min-h-0 flex-1 overflow-y-auto px-3 py-4" aria-label={ROLE_LABELS[currentRole]}>
        <div className="space-y-6">
          {sections.map((section) => (
            <section key={section.label} aria-labelledby={isCollapsed ? undefined : `nav-${section.label}`}>
              {!isCollapsed && (
                <h2 id={`nav-${section.label}`} className="mb-2 px-3 text-[0.68rem] font-bold uppercase tracking-[0.13em] text-muted-foreground">
                  {section.label}
                </h2>
              )}
              <div className="space-y-1">
                {section.items.map((item) => {
                  const Icon = item.icon;
                  const active = isNavItemActive(location.pathname, item);

                  return (
                    <Link
                      key={item.path}
                      to={item.path}
                      title={isCollapsed ? item.label : undefined}
                      aria-current={active ? 'page' : undefined}
                      className={cn(
                        'group relative flex min-h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring',
                        active
                          ? 'bg-sidebar-accent font-semibold text-sidebar-accent-foreground'
                          : 'text-sidebar-foreground hover:bg-secondary hover:text-foreground',
                        isCollapsed && 'justify-center px-0'
                      )}
                    >
                      <Icon className={cn('size-5 shrink-0', active ? 'text-primary' : 'text-muted-foreground group-hover:text-foreground')} aria-hidden="true" />
                      {!isCollapsed && <span className="min-w-0 flex-1 truncate">{item.label}</span>}
                      {active && <span className="absolute inset-y-2 left-0 w-0.5 rounded-r-full bg-primary" aria-hidden="true" />}
                    </Link>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      </nav>

      <div className="border-t border-sidebar-border p-3">
        <button
          type="button"
          onClick={onToggleCollapse}
          className={cn(
            'flex min-h-11 w-full items-center gap-3 rounded-xl px-3 text-sm font-medium text-muted-foreground transition-colors duration-200 hover:bg-secondary hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring',
            isCollapsed && 'justify-center px-0'
          )}
          aria-label={isCollapsed ? 'Mở rộng thanh điều hướng' : 'Thu gọn thanh điều hướng'}
        >
          {isCollapsed ? <ChevronRight className="size-5" /> : <PanelLeftClose className="size-5" />}
          {!isCollapsed && (
            <>
              <span className="flex-1 text-left">Thu gọn</span>
              <ChevronLeft className="size-4" aria-hidden="true" />
            </>
          )}
        </button>
      </div>
    </aside>
  );
}
