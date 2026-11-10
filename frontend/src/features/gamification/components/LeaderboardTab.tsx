import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Search, Target, Trophy } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { UserAvatar } from '@/components/ui/user-avatar';
import { sessionService } from '@/features/session/services/sessionService';
import { useLeaderboard } from '../hooks/useGamification';
import type { LeaderboardEntryDto, LeaderboardPeriod } from '../types/gamification.types';
import { GamificationSelect } from './GamificationSelect';

const scopes = [
  { value: 'SEMESTER', label: 'Toàn học kỳ' },
  { value: 'SUBJECT', label: 'Theo môn học' },
] as const;

function RankedAvatar({ entry, large = false }: { entry: LeaderboardEntryDto; large?: boolean }) {
  return (
    <div className={`relative shrink-0 ${large ? 'size-14' : 'size-9'}`}>
      <UserAvatar name={entry.fullName} avatarUrl={entry.avatarUrl} decorative className="size-full" />
      {entry.frameUrl && <img src={entry.frameUrl} alt="" className="pointer-events-none absolute inset-0 size-full object-contain" />}
    </div>
  );
}

export function LeaderboardTab() {
  const [period, setPeriod] = useState<LeaderboardPeriod>('SEMESTER');
  const [subjectId, setSubjectId] = useState<number>();
  const [search, setSearch] = useState('');
  const subjects = useQuery({
    queryKey: ['subjects', 'gamification'],
    queryFn: async () => (await sessionService.getAllSubjects()).data ?? [],
    enabled: period === 'SUBJECT',
    staleTime: 5 * 60_000,
  });
  const subject = subjects.data?.find((item) => item.subjectId === subjectId) ?? subjects.data?.[0];
  const leaderboard = useLeaderboard({ period, subjectId: period === 'SUBJECT' ? subject?.subjectId : undefined, limit: 50 });
  const entries = leaderboard.data?.entries ?? [];
  const myRank = leaderboard.data?.myRank;
  const leaders = entries.filter((entry) => entry.rank <= 3).slice(0, 3);
  const keyword = search.trim().toLocaleLowerCase('vi-VN');
  const results = entries.filter((entry) =>
    entry.fullName.toLocaleLowerCase('vi-VN').includes(keyword) || entry.userCode?.toLowerCase().includes(keyword),
  );

  return (
    <section aria-labelledby="leaderboard-title" className="space-y-4">
      <div className="rounded-2xl border border-border bg-card p-4 sm:p-5">
        <h2 id="leaderboard-title" className="text-xl font-bold">Bảng xếp hạng</h2>
        <div className="mt-4 flex flex-wrap items-end gap-3">
          <div role="group" aria-label="Phạm vi xếp hạng" className="flex flex-wrap gap-1 rounded-xl bg-surface-subtle p-1">
            {scopes.map(({ value, label }) => (
              <Button key={value} type="button" aria-pressed={period === value}
                variant={period === value ? 'secondary' : 'ghost'} className="min-h-11" onClick={() => setPeriod(value)}>
                {label}
              </Button>
            ))}
          </div>
          {period === 'SUBJECT' && (
            <div className="w-full sm:w-72">
              <GamificationSelect label="Môn học" value={subject ? String(subject.subjectId) : ''}
                disabled={!subjects.data?.length}
                options={(subjects.data ?? []).map((item) => ({ value: String(item.subjectId), label: `${item.code} · ${item.name}` }))}
                onChange={(value) => setSubjectId(Number(value))} />
            </div>
          )}
        </div>
      </div>

      {period === 'SUBJECT' && subjects.isError ? (
        <ErrorState title="Không thể tải danh mục môn học" onRetry={() => void subjects.refetch()} />
      ) : period === 'SUBJECT' && subjects.isSuccess && !subject ? (
        <EmptyState icon={Trophy} title="Chưa có môn học" />
      ) : leaderboard.isPending ? (
        <Skeleton aria-label="Đang tải bảng xếp hạng" className="h-52 rounded-2xl" />
      ) : leaderboard.isError ? (
        <ErrorState title="Không thể tải bảng xếp hạng" onRetry={() => void leaderboard.refetch()} />
      ) : (
        <>
          {myRank && myRank.rank > 0 && (
            <div className="flex flex-wrap items-center gap-3 rounded-2xl border border-border bg-academic-soft p-4">
              <Target aria-hidden="true" className="size-5 shrink-0 text-academic" />
              <div className="min-w-0 flex-1">
                <p className="font-semibold">Vị trí của bạn · Hạng {myRank.rank}</p>
                <p className="text-sm text-muted-foreground">Top {myRank.topPercent}% trong {myRank.totalParticipants.toLocaleString('vi-VN')} người</p>
              </div>
              <strong className="text-lg tabular-nums">{myRank.totalPoints.toLocaleString('vi-VN')} điểm</strong>
            </div>
          )}

          {leaders.length > 0 && (
            <div className="grid gap-2 sm:grid-cols-3">
              {leaders.map((entry) => (
                <Link key={entry.userId} to={`/users/${entry.userId}`}
                  className="flex min-w-0 items-center gap-3 rounded-2xl border border-border bg-card p-4 hover:bg-surface-subtle focus-visible:outline-2 focus-visible:outline-ring sm:flex-col sm:items-start">
                  <RankedAvatar entry={entry} large />
                  <div className="min-w-0">
                    <p className={`text-sm font-semibold ${entry.rank === 1 ? 'text-reward' : 'text-muted-foreground'}`}>Hạng {entry.rank}</p>
                    <p className="wrap-break-word font-semibold">{entry.fullName}</p>
                    <p className="text-sm tabular-nums text-muted-foreground">{entry.totalPoints.toLocaleString('vi-VN')} điểm</p>
                  </div>
                </Link>
              ))}
            </div>
          )}

          {entries.length === 0 ? (
            <EmptyState icon={Trophy} title="Chưa có thứ hạng" description="Bảng xếp hạng sẽ có dữ liệu khi người học bắt đầu tích điểm." />
          ) : (
            <div className="rounded-2xl border border-border bg-card">
              <div className="border-b border-border p-4">
                <label className="block max-w-md space-y-1.5">
                  <span className="block text-sm font-medium">Tìm trong {entries.length} thứ hạng đang hiển thị</span>
                  <span className="relative block">
                    <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input type="search" value={search} onChange={(event) => setSearch(event.target.value)}
                      placeholder="Nhập tên hoặc mã sinh viên" className="min-h-11 pl-9" />
                  </span>
                </label>
              </div>
              {results.length === 0 ? (
                <p role="status" className="p-5 text-sm text-muted-foreground">Không có kết quả khớp từ khóa.</p>
              ) : (
                <ul className="divide-y divide-border">
                  {results.map((entry) => (
                    <li key={entry.userId} className={entry.isCurrentUser ? 'bg-academic-soft' : undefined}>
                      <Link to={`/users/${entry.userId}`}
                        className="grid min-h-14 grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-2 px-3 py-2 hover:bg-muted focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-ring sm:gap-3 sm:px-4">
                        <span className="min-w-8 text-center text-sm font-semibold tabular-nums text-muted-foreground">#{entry.rank}</span>
                        <div className="flex min-w-0 items-center gap-2">
                          <RankedAvatar entry={entry} />
                          <div className="min-w-0">
                            <p className="truncate font-medium">{entry.fullName}{entry.isCurrentUser && <span className="sr-only"> (Bạn)</span>}</p>
                            <p className="truncate text-xs text-muted-foreground">{entry.userCode}</p>
                          </div>
                        </div>
                        <span className="whitespace-nowrap text-sm font-semibold tabular-nums">{entry.totalPoints.toLocaleString('vi-VN')} điểm</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </>
      )}
    </section>
  );
}
