import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';

export function AdminPageHeader({ title, description, icon: Icon, action }: {
  title: string;
  description: string;
  icon: LucideIcon;
  action?: ReactNode;
}) {
  return (
    <header className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-border bg-card p-5 sm:p-6">
      <div className="flex min-w-0 flex-1 items-start gap-3">
        <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-academic-soft text-academic">
          <Icon aria-hidden="true" className="size-5" />
        </span>
        <div className="min-w-0 space-y-1">
          <p className="text-xs font-semibold text-academic">Không gian quản trị</p>
          <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
          <p className="max-w-2xl text-sm leading-relaxed text-muted-foreground">{description}</p>
        </div>
      </div>
      {action}
    </header>
  );
}
