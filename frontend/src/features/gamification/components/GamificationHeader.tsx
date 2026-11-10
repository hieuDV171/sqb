import { Award, CheckCircle, Flame, ShieldAlert, ShoppingBag, Target, Trophy } from 'lucide-react';
import { SqbCoin } from '@/components/common/SqbCoin';
import { Button } from '@/components/ui/button';
import { useGamificationStore, type GamificationTab } from '../stores/useGamificationStore';

const sections = [
  { id: 'leaderboard', label: 'Bảng xếp hạng', icon: Trophy },
  { id: 'badges', label: 'Huy hiệu', icon: Award },
  { id: 'shop', label: 'Cửa hàng & tủ đồ', icon: ShoppingBag },
  { id: 'predictions', label: 'Dự đoán', icon: Target },
  { id: 'error-hunter', label: 'Thợ săn lỗi', icon: ShieldAlert },
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
      <div className="rounded-3xl border border-border bg-card p-5 sm:p-7">
        <div className="flex flex-col gap-6 xl:flex-row xl:items-center xl:justify-between">
          <div className="min-w-0 space-y-2">
            <span className="inline-flex items-center gap-2 rounded-full bg-reward-soft px-3 py-1 text-sm font-semibold text-reward">
              <Trophy aria-hidden="true" className="size-4" /> Thành tích
            </span>
            <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Trung tâm thành tích</h1>
            <p className="max-w-xl text-sm leading-relaxed text-muted-foreground">
              Theo dõi thứ hạng, sưu tập huy hiệu và dùng xu SQB để cá nhân hóa trang cá nhân.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="flex items-center gap-3 rounded-xl border border-border bg-surface-subtle px-3 py-2">
              <SqbCoin className="size-6" />
              <div>
                <p className="text-xs text-muted-foreground">Số dư xu SQB</p>
                <p className="font-semibold tabular-nums">{userCoins?.toLocaleString('vi-VN') ?? '—'}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-xl border border-border bg-surface-subtle px-3 py-2">
              <Flame aria-hidden="true" className="size-5 text-reward" />
              <div>
                <p className="text-xs text-muted-foreground">Chuỗi điểm danh</p>
                <p className="font-semibold tabular-nums">{streakCount === undefined ? '—' : `${streakCount} ngày`}</p>
              </div>
            </div>
            <Button type="button" variant={hasCheckedInToday ? 'outline' : 'default'} className="min-h-11"
              disabled={hasCheckedInToday === undefined && !onRetryCheckIn}
              onClick={onRetryCheckIn ?? (() => setCheckInModalOpen(true))}>
              {hasCheckedInToday ? <CheckCircle aria-hidden="true" /> : <Flame aria-hidden="true" />}
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
