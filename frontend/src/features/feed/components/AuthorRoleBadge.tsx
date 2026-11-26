import type { AuthorDto } from '@/types/post.types';

const roles = {
  STUDENT: { short: 'SV', name: 'Sinh viên', color: 'bg-blue-700 text-white' },
  LECTURER: { short: 'GV', name: 'Giảng viên', color: 'bg-amber-700 text-white' },
  ADMIN: { short: 'AD', name: 'Quản trị viên', color: 'bg-rose-700 text-white' },
  SYSTEM: { short: 'HT', name: 'Hệ thống', color: 'bg-emerald-700 text-white dark:bg-emerald-400 dark:text-emerald-950' },
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
