import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';

export function AuthorProfileLink({ userId, name, className, children }: {
  userId?: number | null;
  name?: string | null;
  className?: string;
  children: ReactNode;
}) {
  // System and anonymized authors do not have a public profile link.
  if (userId == null) return <span className={className}>{children}</span>;

  return (
    <Link to={`/users/${userId}`} aria-label={`Xem trang cá nhân của ${name || 'người dùng SQB'}`}
      className={cn('rounded-md transition-colors hover:text-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring', className)}>
      {children}
    </Link>
  );
}
