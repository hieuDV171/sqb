import type { AuthorDto } from '@/types/post.types';

const roles = {
  STUDENT: { short: 'SV', name: 'Sinh viên', color: 'bg-blue-500 text-white dark:bg-blue-400 dark:text-blue-950' },
  LECTURER: { short: 'GV', name: 'Giảng viên', color: 'bg-amber-500 text-white dark:bg-amber-400 dark:text-amber-950' },
  ADMIN: { short: 'AD', name: 'Quản trị viên', color: 'bg-rose-500 text-white dark:bg-rose-400 dark:text-rose-950' },
  SYSTEM: { short: 'HT', name: 'Hệ thống', color: 'bg-emerald-500 text-white dark:bg-emerald-400 dark:text-emerald-950' },
} satisfies Record<NonNullable<AuthorDto['role']>, { short: string; name: string; color: string }>;

export function AuthorRoleBadge({ role }: { role?: AuthorDto['role'] }) {
  const badge = role && roles[role];
  if (!badge) return null;

  return (
    <span className={`pointer-events-none absolute -bottom-1 -right-1 rounded-full border border-card px-1.5 py-0.5 text-[9px] leading-none font-bold ${badge.color}`}>
      <span aria-hidden="true">{badge.short}</span>
      <span className="sr-only">Vai trò {badge.name}</span>
    </span>
  );
}
