import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Crown, Medal, Award, Search, Target, Trophy } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { UserAvatar } from '@/components/ui/user-avatar';
import { sessionService } from '@/features/session/services/sessionService';
import { useLeaderboard } from '@/features/gamification';
import type { LeaderboardEntryDto, LeaderboardPeriod } from '@/features/gamification';
import { GamificationSelect } from './GamificationSelect';

const scopes = [
  { value: 'SEMESTER', label: 'Toàn học kỳ' },
  { value: 'SUBJECT', label: 'Theo môn học' },
] as const;

const podiumPlaces = [
  {
    rank: 1, title: 'Quán quân', icon: Crown, position: 'sm:col-start-2',
    height: 'sm:h-40', avatarRing: 'ring-amber-400',
    crown: 'size-7 fill-amber-300 text-amber-600 motion-safe:animate-bounce dark:text-amber-300',
    finish: 'border-amber-400/70 from-amber-200 to-yellow-50 text-amber-950 dark:border-amber-400/50 dark:from-amber-800/75 dark:to-amber-950 dark:text-amber-100',
  },
  {
    rank: 2, title: 'Á quân', icon: Medal, position: 'sm:col-start-1',
    height: 'sm:h-32', avatarRing: 'ring-slate-400',
    crown: 'size-6 fill-slate-300 text-slate-600 dark:text-slate-300',
    finish: 'border-slate-300 from-slate-200 to-slate-50 text-slate-900 dark:border-slate-500 dark:from-slate-700 dark:to-slate-900 dark:text-slate-100',
  },
  {
    rank: 3, title: 'Quý quân', icon: Award, position: 'sm:col-start-3',
    height: 'sm:h-28', avatarRing: 'ring-orange-500/70',
    crown: 'size-6 fill-orange-300 text-orange-700 dark:text-orange-300',
    finish: 'border-orange-300 from-orange-200 to-orange-50 text-orange-950 dark:border-orange-500/60 dark:from-orange-800/75 dark:to-orange-950 dark:text-orange-100',
  },
] as const;

function RankedAvatar({ entry, large = false }: { entry: LeaderboardEntryDto; large?: boolean }) {
  return (
    <div className={`relative shrink-0 ${large ? 'size-22' : 'size-9'}`}>
      <UserAvatar
        name={entry.fullName}
        avatarUrl={entry.avatarUrl}
        decorative
        className={large ? 'size-full text-3xl leading-none' : 'size-full'}
      />
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
  const leaders = podiumPlaces.flatMap((place) => {
    const entry = entries.find((candidate) => candidate.rank === place.rank);
    return entry ? [{ ...place, entry }] : [];
  });
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
            <div className="flex flex-wrap items-center gap-3 rounded-2xl border border-indigo-200 bg-linear-to-r from-indigo-100 via-violet-50 to-card p-4 dark:border-indigo-500/35 dark:from-indigo-950 dark:via-violet-950/65">
              <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-indigo-700 text-white dark:bg-indigo-400 dark:text-indigo-950">
                <Target aria-hidden="true" className="size-5" />
              </span>
              <div className="min-w-0 flex-1">
                <p className="font-semibold">Vị trí của bạn · Hạng {myRank.rank}</p>
                <p className="text-sm text-muted-foreground">Top {myRank.topPercent}% trong {myRank.totalParticipants.toLocaleString('vi-VN')} người</p>
              </div>
              <strong className="text-lg text-indigo-800 tabular-nums dark:text-indigo-200">{myRank.totalPoints.toLocaleString('vi-VN')} điểm</strong>
            </div>
          )}

          {leaders.length > 0 && (
            <section aria-labelledby="podium-title"
              className="space-y-4 rounded-2xl border border-border bg-linear-to-b from-indigo-50 via-card to-card p-4 dark:from-indigo-950/55 sm:p-6">
              <h3 id="podium-title" className="flex items-center justify-center gap-2 text-lg font-bold">
                <Trophy aria-hidden="true" className="size-5 text-reward" /> Bục vinh danh
              </h3>
              <ol className="mx-auto grid max-w-3xl gap-3 sm:grid-cols-3 sm:items-end sm:gap-4">
                {leaders.map(({ entry, rank, title, icon: Icon, position, height, avatarRing, crown, finish }) => (
                  <li key={entry.userId} className={`min-w-0 ${position} sm:row-start-1`}>
                    <Link to={`/users/${entry.userId}`}
                      className="group flex min-w-0 flex-col items-stretch gap-2 rounded-2xl border border-border bg-card p-3 transition-shadow hover:shadow-md focus-visible:outline-2 focus-visible:outline-ring sm:border-transparent sm:bg-transparent sm:p-0">
                      <div className="flex min-w-0 items-center gap-3 sm:flex-col sm:gap-2 sm:px-2 sm:text-center">
                        <div className="flex shrink-0 flex-col items-center gap-1.5">
                          <span className="grid h-7 place-items-center">
                            <Crown aria-hidden="true" className={crown} />
                          </span>
                          <div className={`shrink-0 rounded-full ring-2 ring-offset-2 ring-offset-card ${avatarRing}`}>
                            <RankedAvatar entry={entry} large />
                          </div>
                        </div>
                        <div className="min-w-0">
                          <p className="wrap-break-word font-semibold group-hover:underline">{entry.fullName}</p>
                          <p className="text-sm font-medium text-muted-foreground tabular-nums">{entry.totalPoints.toLocaleString('vi-VN')} điểm</p>
                        </div>
                      </div>
                      <div className={`flex items-center justify-center gap-2 rounded-xl border bg-linear-to-b px-2 py-2 text-center sm:flex-col sm:gap-1 sm:rounded-t-2xl sm:rounded-b-lg sm:border-t-2 ${height} ${finish}`}>
                        <Icon aria-hidden="true" className="size-4 sm:size-5" />
                        <span className="font-bold sm:text-2xl">Hạng {rank}</span>
                        <span className="text-sm font-medium">{title}</span>
                      </div>
                    </Link>
                  </li>
                ))}
              </ol>
            </section>
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
