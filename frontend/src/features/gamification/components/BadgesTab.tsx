import { useState } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { Award, Lock, Users, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { useAllBadges, useMyBadges } from '../hooks/useGamification';
import { getTierColor, type BadgeResponseDto, type BadgeTier, type UserBadgeResponseDto } from '../types/gamification.types';
import { GamificationSelect } from './GamificationSelect';

const tierLabels: Record<BadgeTier, string> = {
  BRONZE: 'Đồng', SILVER: 'Bạc', GOLD: 'Vàng', PLATINUM: 'Bạch kim', DIAMOND: 'Kim cương',
};
const tiers = [
  { value: '', label: 'Tất cả cấp' },
  ...Object.entries(tierLabels).map(([value, label]) => ({ value, label })),
];

type BadgeItem = BadgeResponseDto | UserBadgeResponseDto;

function BadgeIcon({ badge }: { badge: BadgeItem }) {
  const { badgeBg, badgeBorder, textColor } = getTierColor(badge.badgeTier);
  return (
    <span className={`grid size-14 shrink-0 place-items-center rounded-2xl border p-2 ${badgeBg} ${badgeBorder} ${textColor}`}>
      {badge.iconUrl ? <img src={badge.iconUrl} alt="" loading="lazy" className="size-10 object-contain" />
        : <Award aria-hidden="true" className="size-7" />}
    </span>
  );
}

export function BadgesTab() {
  const [mode, setMode] = useState<'my' | 'all'>('my');
  const [tier, setTier] = useState('');
  const [selectedBadge, setSelectedBadge] = useState<BadgeItem | null>(null);
  const all = useAllBadges();
  const mine = useMyBadges();
  const owned = new Map((mine.data ?? []).map((badge) => [badge.badgeId, badge]));
  const catalog = new Map((all.data ?? []).map((badge) => [badge.badgeId, badge]));
  const items = ((mode === 'my' ? mine.data : all.data) ?? []).filter((badge) => !tier || badge.badgeTier === tier);
  const loading = mine.isPending || (mode === 'all' && all.isPending);
  const error = mine.isError || (mode === 'all' && all.isError);
  const earned = selectedBadge && owned.get(selectedBadge.badgeId);
  const totalEarned = selectedBadge ? catalog.get(selectedBadge.badgeId)?.totalEarnedUsers : undefined;

  return (
    <section aria-labelledby="badges-title" className="space-y-4">
      <div className="rounded-2xl border border-border bg-card p-4 sm:p-5">
        <h2 id="badges-title" className="text-xl font-bold">Bộ sưu tập huy hiệu</h2>
        <p className="mt-1 text-sm text-muted-foreground">Xem thành tích đã đạt và khám phá các huy hiệu còn lại.</p>
        <div className="mt-4 flex flex-wrap items-end gap-3">
          <div role="group" aria-label="Danh sách huy hiệu" className="flex flex-wrap gap-1 rounded-xl bg-surface-subtle p-1">
            <Button type="button" aria-pressed={mode === 'my'} variant={mode === 'my' ? 'secondary' : 'ghost'} className="min-h-11"
              onClick={() => setMode('my')}>Của tôi{mine.data && ` (${mine.data.length})`}</Button>
            <Button type="button" aria-pressed={mode === 'all'} variant={mode === 'all' ? 'secondary' : 'ghost'} className="min-h-11"
              onClick={() => setMode('all')}>Danh mục{all.data && ` (${all.data.length})`}</Button>
          </div>
          <div className="w-full sm:w-44"><GamificationSelect label="Cấp huy hiệu" value={tier} options={tiers} onChange={setTier} /></div>
        </div>
      </div>

      {error ? (
        <ErrorState title="Không thể tải huy hiệu" onRetry={() => {
          if (mine.isError) void mine.refetch();
          if (mode === 'all' && all.isError) void all.refetch();
        }} />
      ) : loading ? (
        <Skeleton aria-label="Đang tải huy hiệu" className="h-40 rounded-2xl" />
      ) : items.length === 0 ? (
        <EmptyState icon={Award} title={tier ? 'Không có huy hiệu ở cấp này' : mode === 'my' ? 'Bạn chưa có huy hiệu' : 'Chưa có huy hiệu trong danh mục'}
          actionLabel={tier ? 'Bỏ bộ lọc' : mode === 'my' ? 'Xem danh mục' : undefined}
          onAction={tier ? () => setTier('') : mode === 'my' ? () => setMode('all') : undefined} />
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {items.map((badge) => {
            const userBadge = owned.get(badge.badgeId);
            return (
              <li key={badge.badgeId}>
                <button type="button" onClick={() => setSelectedBadge(badge)}
                  className="flex h-full w-full cursor-pointer items-start gap-3 rounded-2xl border border-border bg-card p-4 text-left transition-colors hover:bg-surface-subtle focus-visible:outline-2 focus-visible:outline-ring">
                  <BadgeIcon badge={badge} />
                  <span className="min-w-0 space-y-1">
                    <span className="block text-xs font-medium text-muted-foreground">{tierLabels[badge.badgeTier]}</span>
                    <span className="block wrap-break-word font-semibold">{badge.name}</span>
                    <span className="line-clamp-2 text-sm text-muted-foreground">{badge.description}</span>
                    <span className={`flex items-center gap-1.5 text-xs font-medium ${userBadge ? 'text-success' : 'text-muted-foreground'}`}>
                      {userBadge ? <Award aria-hidden="true" className="size-4" /> : <Lock aria-hidden="true" className="size-4" />}
                      {userBadge ? `Đã đạt ${new Date(userBadge.earnedAt).toLocaleDateString('vi-VN')}` : 'Chưa đạt'}
                    </span>
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}

      <Dialog.Root open={selectedBadge !== null} onOpenChange={(open) => { if (!open) setSelectedBadge(null); }}>
        {selectedBadge && (
          <Dialog.Portal>
            <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
            <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-md -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
              <div className="flex items-start gap-3">
                <BadgeIcon badge={selectedBadge} />
                <div className="min-w-0 flex-1">
                  <p className="text-sm text-muted-foreground">{tierLabels[selectedBadge.badgeTier]}</p>
                  <Dialog.Title className="wrap-break-word text-xl font-bold">{selectedBadge.name}</Dialog.Title>
                </div>
                <Dialog.Close render={<Button variant="ghost" size="icon" aria-label="Đóng chi tiết huy hiệu" />}>
                  <X aria-hidden="true" />
                </Dialog.Close>
              </div>
              <Dialog.Description className="text-sm leading-relaxed text-muted-foreground">{selectedBadge.description}</Dialog.Description>
              <div className="flex flex-wrap items-center gap-3 border-t border-border pt-3 text-sm">
                <span className={earned ? 'text-success' : 'text-muted-foreground'}>
                  {earned ? `Đã đạt ${new Date(earned.earnedAt).toLocaleDateString('vi-VN')}` : 'Chưa đạt'}
                </span>
                {totalEarned != null && <span className="inline-flex items-center gap-1.5 text-muted-foreground">
                  <Users aria-hidden="true" className="size-4" /> {totalEarned.toLocaleString('vi-VN')} người đã đạt
                </span>}
              </div>
            </Dialog.Popup>
          </Dialog.Portal>
        )}
      </Dialog.Root>
    </section>
  );
}
