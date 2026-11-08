import type { ReactNode } from 'react';
import { BookOpen } from 'lucide-react';

interface StudyHeaderProps {
  section: string;
  title: string;
  description?: string;
  action?: ReactNode;
}

export function StudyHeader({ section, title, description, action }: StudyHeaderProps) {
  return (
    <header className="flex flex-col gap-5 border-b border-border pb-5 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0 space-y-2">
        <p className="flex items-center gap-2 text-xs font-semibold tracking-wide text-academic">
          <BookOpen aria-hidden="true" className="size-4" />
          {section}
        </p>
        <h1 className="break-words text-2xl font-bold tracking-tight sm:text-3xl">{title}</h1>
        {description && <p className="max-w-prose text-sm leading-relaxed text-muted-foreground">{description}</p>}
      </div>
      {action && <div className="shrink-0 self-start sm:self-auto">{action}</div>}
    </header>
  );
}
