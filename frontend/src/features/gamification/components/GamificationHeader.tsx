import { Award, CheckCircle, Flame, ShoppingBag, Target, Trophy } from 'lucide-react';
import { SqbCoin } from '@/components/common/SqbCoin';
import { Button } from '@/components/ui/button';
import { useGamificationStore, type GamificationTab } from '@/features/gamification';

const sections = [
  { id: 'leaderboard', label: 'Bảng xếp hạng', icon: Trophy },
  { id: 'badges', label: 'Huy hiệu', icon: Award },
  { id: 'shop', label: 'Cửa hàng & tủ đồ', icon: ShoppingBag },
  { id: 'predictions', label: 'Dự đoán', icon: Target },
] as const;

interface GamificationHeaderProps {
  userCoins?: number;
  streakCount?: number;
  hasCheckedInToday?: boolean;
  currentTab?: GamificationTab;
  onTabChange?: (tab: GamificationTab) => void;
  onRetryCheckIn?: () => void;
}

export function GamificationHeader({ userCoins, streakCount, hasCheckedInToday, currentTab, onTabChange, onRetryCheckIn }: GamificationHeaderProps) {
  const { activeTab, setActiveTab, setCheckInModalOpen } = useGamificationStore();
  const selectedTab = currentTab ?? activeTab;

  return (
    <header className="space-y-4">
      <div className="relative isolate overflow-hidden rounded-3xl border border-indigo-400/30 bg-indigo-950 bg-linear-to-r from-indigo-950 via-indigo-800 to-purple-950 p-5 text-white shadow-lg shadow-indigo-950/20 sm:p-7">
        <div aria-hidden="true" className="pointer-events-none absolute -top-28 -right-20 size-80 rounded-full bg-fuchsia-400/20 blur-3xl" />
        <div aria-hidden="true" className="pointer-events-none absolute -bottom-48 -left-16 size-96 rounded-full bg-indigo-400/25 blur-3xl" />
        <div className="relative flex flex-col gap-6 xl:flex-row xl:items-center xl:justify-between">
          <div className="min-w-0 space-y-2">
            <span className="inline-flex items-center gap-2 rounded-full border-2 border-amber-500 bg-amber-300/15 px-3 py-1 text-sm font-semibold text-amber-300">
              <Trophy aria-hidden="true" className="size-4" /> Thành tích
            </span>
            <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Trung tâm thành tích</h1>
            <p className="max-w-xl text-sm leading-relaxed text-indigo-100">
              Theo dõi thứ hạng, sưu tập huy hiệu và dùng xu SQB để cá nhân hóa trang cá nhân.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="flex items-center gap-3 rounded-2xl border-2 border-amber-500 bg-white/10 px-3 py-2.5">
              <span aria-hidden="true" className="grid size-9 shrink-0 place-items-center rounded-xl bg-amber-300/15">
                <SqbCoin className="size-6" />
              </span>
              <div>
                <p className="text-xs text-indigo-100">Số dư xu SQB</p>
                <p className="text-lg font-bold text-amber-300 tabular-nums">{userCoins?.toLocaleString('vi-VN') ?? '—'}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-2xl border-2 border-orange-500 bg-white/10 px-3 py-2.5">
              <span aria-hidden="true" className="grid size-9 shrink-0 place-items-center rounded-xl bg-orange-300/15">
                <Flame className="size-5 text-orange-400" />
              </span>
              <div>
                <p className="text-xs text-indigo-100">Chuỗi điểm danh</p>
                <p className="text-lg font-bold text-orange-300 tabular-nums">{streakCount === undefined ? '—' : `${streakCount} ngày`}</p>
              </div>
            </div>
            <Button type="button" variant="ghost"
              className={`min-h-11 rounded-full border-3 focus-visible:border-white focus-visible:ring-white/60 ${hasCheckedInToday
                ? 'border-emerald-600 bg-emerald-200/10 text-emerald-300 hover:bg-emerald-200/20 hover:text-emerald-50'
                : 'border-emerald-300/80 bg-linear-to-r from-emerald-600 via-emerald-700 to-teal-700 font-bold text-white shadow-lg shadow-emerald-500/35 hover:border-emerald-200 hover:text-white hover:shadow-emerald-400/50'}`}
              disabled={hasCheckedInToday === undefined && !onRetryCheckIn}
              onClick={onRetryCheckIn ?? (() => setCheckInModalOpen(true))}>
              {hasCheckedInToday ? <CheckCircle aria-hidden="true" /> : <Flame aria-hidden="true" className="fill-amber-300 text-amber-300" />}
              {onRetryCheckIn ? 'Thử tải điểm danh' : hasCheckedInToday ? 'Xem điểm danh' : 'Điểm danh'}
            </Button>
          </div>
        </div>
      </div>

      <nav aria-label="Các mục thành tích" className="flex flex-wrap gap-1.5 rounded-2xl border border-border bg-card p-2">
        {sections.map(({ id, label, icon: Icon }) => (
          <Button key={id} type="button" aria-current={selectedTab === id ? 'page' : undefined}
            variant={selectedTab === id ? 'secondary' : 'ghost'}
            className={`min-h-11 ${selectedTab === id ? 'bg-academic-soft text-academic' : 'text-muted-foreground'}`}
            onClick={() => { setActiveTab(id); onTabChange?.(id); }}>
            <Icon aria-hidden="true" /> {label}
          </Button>
        ))}
      </nav>
    </header>
  );
}
