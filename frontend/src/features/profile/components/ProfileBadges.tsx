import { useState } from 'react';
import { Award } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { BadgeDetailsDialog, BadgeIcon } from '@/features/gamification/components/BadgesTab';
import { useUserBadges } from '@/features/gamification/hooks/useGamification';
import type { UserBadgeResponseDto } from '@/features/gamification/types/gamification.types';

export function ProfileBadges({ userId }: { userId: number }) {
  const badges = useUserBadges(userId);
  const [selected, setSelected] = useState<UserBadgeResponseDto | null>(null);

  return (
    <Card>
      <CardHeader><CardTitle className="flex items-center gap-2"><Award className="size-5 text-primary" aria-hidden="true" /> Huy hiệu đã đạt</CardTitle></CardHeader>
      <CardContent>
        {badges.isPending ? <Skeleton className="h-20" aria-label="Đang tải huy hiệu" /> : badges.isError ? (
          <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground" role="alert">
            Không thể tải huy hiệu. <Button variant="outline" size="sm" onClick={() => void badges.refetch()}>Thử lại</Button>
          </div>
        ) : badges.data.length === 0 ? (
          <p className="text-sm text-muted-foreground">Chưa có huy hiệu.</p>
        ) : (
          <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {badges.data.map((badge) => <li key={badge.badgeId}>
              <button type="button" onClick={() => setSelected(badge)} className="flex w-full items-center gap-3 rounded-xl border border-border p-2 text-left hover:bg-muted focus-visible:outline-2 focus-visible:outline-ring">
                <BadgeIcon badge={badge} /><span className="min-w-0 wrap-break-word font-semibold">{badge.name}
                  {badge.active === false && <span className="block text-xs font-medium text-muted-foreground">Ngừng trao</span>}
                </span>
              </button>
            </li>)}
          </ul>
        )}
      </CardContent>
      {selected && <BadgeDetailsDialog badge={selected} earnedAt={selected.earnedAt} onClose={() => setSelected(null)} />}
    </Card>
  );
}
