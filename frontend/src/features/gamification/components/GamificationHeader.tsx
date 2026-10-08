import { Trophy, ShoppingBag, Target, Award, Flame, CheckCircle, Sparkles, ShieldAlert } from 'lucide-react';
import { useGamificationStore, type GamificationTab } from '../stores/useGamificationStore';
import { SqbCoin } from '@/components/common/SqbCoin';

interface GamificationHeaderProps {
  userCoins?: number;
  streakCount?: number;
  hasCheckedInToday?: boolean;
  currentTab?: GamificationTab;
  onTabChange?: (tab: GamificationTab) => void;
}

export function GamificationHeader({
  userCoins = 0,
  streakCount = 0,
  hasCheckedInToday = false,
  currentTab,
  onTabChange,
}: GamificationHeaderProps) {
  const { activeTab, setActiveTab, setCheckInModalOpen } = useGamificationStore();
  const selectedTab = currentTab ?? activeTab;

  const navTabs: { id: GamificationTab; label: string; icon: typeof Trophy; badge?: string }[] = [
    { id: 'leaderboard', label: 'Bảng Xếp Hạng', icon: Trophy },
    { id: 'shop', label: 'Cửa Hàng & Tủ Đồ', icon: ShoppingBag },
    { id: 'predictions', label: 'Đấu Trường Dự Đoán', icon: Target, badge: 'HOT' },
    { id: 'badges', label: 'Bộ Sưu Tập Huy Hiệu', icon: Award },
    { id: 'error-hunter', label: 'Thợ Săn Lỗi', icon: ShieldAlert },
  ];

  return (
    <div className="space-y-6">
      {/* Top Banner with XP, Level & Daily Streak */}
      <div className="relative overflow-hidden rounded-3xl bg-linear-to-r from-indigo-900 via-indigo-800 to-purple-900 p-6 sm:p-8 text-white shadow-xl">
        {/* Background glow effects */}
        <div className="absolute -top-24 -right-24 w-80 h-80 rounded-full bg-purple-500/20 blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -left-24 w-80 h-80 rounded-full bg-indigo-500/20 blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          {/* Main Title & Description */}
          <div className="space-y-2">
            <div className="flex items-center gap-3">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black tracking-wider uppercase bg-amber-400/20 border border-amber-300/40 text-amber-300">
                <Sparkles className="w-3.5 h-3.5" />
                Hệ Thống Gamification
              </span>
              {hasCheckedInToday && (
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 border border-emerald-400/30 text-emerald-300">
                  <CheckCircle className="w-3.5 h-3.5" />
                  Đã điểm danh hôm nay
                </span>
              )}
            </div>

            <h1 className="text-2xl sm:text-3xl font-black tracking-tight">
              Trung Tâm Vinh Danh & Gamification
            </h1>
            <p className="text-sm text-indigo-200/90 max-w-xl">
              Tích lũy xu SQB qua hoạt động học tập, tranh tài trên bảng vinh danh và đổi các trang bị cá nhân hóa độc quyền.
            </p>
          </div>

          {/* Quick Stats & Check-in CTA Button */}
          <div className="flex flex-wrap items-center gap-3 sm:gap-4 shrink-0">
            {/* Coins Balance Box */}
            <div className="px-5 py-3.5 rounded-2xl bg-white/10 backdrop-blur-md border border-white/15 shadow-inner flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-amber-400/20 border border-amber-400/30 flex items-center justify-center text-xl shadow-xs">
                <SqbCoin className="w-6 h-6" />
              </div>
              <div>
                <p className="text-[11px] font-medium text-indigo-200 uppercase tracking-wider">Số dư xu SQB</p>
                <p className="text-xl sm:text-2xl font-black font-mono tracking-tight text-amber-300">
                  {userCoins.toLocaleString()}
                </p>
              </div>
            </div>

            {/* Daily Streak Box */}
            <div className="px-5 py-3.5 rounded-2xl bg-white/10 backdrop-blur-md border border-white/15 shadow-inner flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-orange-500/20 border border-orange-400/30 flex items-center justify-center text-orange-400 shadow-xs">
                <Flame className="w-5 h-5 fill-orange-400" />
              </div>
              <div>
                <p className="text-[11px] font-medium text-indigo-200 uppercase tracking-wider">Chuỗi liên tiếp</p>
                <p className="text-xl sm:text-2xl font-black font-mono tracking-tight text-orange-300">
                  {streakCount} <span className="text-xs font-sans text-indigo-200 font-semibold">ngày</span>
                </p>
              </div>
            </div>

            {/* Daily Check-in Button */}
            <button
              type="button"
              onClick={() => setCheckInModalOpen(true)}
              className={`px-5 py-3.5 rounded-2xl font-bold text-sm flex items-center gap-2 shadow-lg transition-all duration-200 ${
                hasCheckedInToday
                  ? 'bg-emerald-600/80 hover:bg-emerald-600 text-white cursor-pointer'
                  : 'bg-linear-to-r from-amber-400 to-yellow-500 hover:from-amber-300 hover:to-yellow-400 text-indigo-950 hover:scale-105 shadow-amber-500/25 cursor-pointer active:scale-95'
              }`}
            >
              {hasCheckedInToday ? (
                <>
                  <CheckCircle className="w-4 h-4" />
                  <span>Xem Lịch Điểm Danh</span>
                </>
              ) : (
                <>
                  <Flame className="w-4 h-4 fill-indigo-950" />
                  <span className="flex items-center gap-1.5">
                    <span>Điểm Danh Ngay (+{(streakCount + 1) % 7 === 0 ? '5' : '1'} xu SQB</span>
                    <SqbCoin className="w-4 h-4" />
                    <span>)</span>
                  </span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Tabs Navigation Bar */}
      <div className="flex items-center gap-2 p-1.5 bg-slate-100 dark:bg-slate-800/80 rounded-2xl border border-slate-200/80 dark:border-slate-700/80 overflow-x-auto scrollbar-none">
        {navTabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = selectedTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => {
                setActiveTab(tab.id);
                onTabChange?.(tab.id);
              }}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl font-bold text-sm whitespace-nowrap transition-all duration-200 ${
                isActive
                  ? 'bg-white dark:bg-slate-700 text-indigo-600 dark:text-indigo-400 shadow-xs'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-white/50 dark:hover:bg-slate-700/40'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-indigo-600 dark:text-indigo-400' : ''}`} />
              <span>{tab.label}</span>
              {tab.badge && (
                <span className="px-1.5 py-0.2 rounded-md text-[10px] font-black uppercase bg-rose-500 text-white shadow-2xs">
                  {tab.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
