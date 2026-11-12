import { useEffect } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { GamificationHeader } from '../components/GamificationHeader';
import { DailyCheckInModal } from '../components/DailyCheckInModal';
import { LeaderboardTab } from '../components/LeaderboardTab';
import { ShopInventoryTab } from '../components/ShopInventoryTab';
import { PredictionArenaTab } from '../components/PredictionArenaTab';
import { BadgesTab } from '../components/BadgesTab';
import { useGamificationStore, type GamificationTab } from '../stores/useGamificationStore';
import { useShop, useCheckInStatus } from '../hooks/useGamification';

interface GamificationHubPageProps {
  initialTab?: GamificationTab;
}

const VALID_TABS = new Set<string>(['leaderboard', 'shop', 'predictions', 'badges']);

function tabPath(tab: GamificationTab) {
  return tab === 'shop' ? '/shop' : '/leaderboard';
}

export function GamificationHubPage({ initialTab }: GamificationHubPageProps) {
  const [searchParams] = useSearchParams();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { activeTab, setActiveTab } = useGamificationStore();

  const tabParam = searchParams.get('tab');
  const legacyErrorHunter = tabParam === 'error-hunter';
  const currentTab = legacyErrorHunter ? 'predictions'
    : tabParam && VALID_TABS.has(tabParam) ? tabParam as GamificationTab
      : initialTab ?? 'leaderboard';

  useEffect(() => {
    if (activeTab !== currentTab) {
      setActiveTab(currentTab);
    }
    const targetPath = tabPath(currentTab);
    if (pathname !== targetPath || tabParam !== currentTab) {
      const next = new URLSearchParams(searchParams);
      next.set('tab', currentTab);
      if (legacyErrorHunter) next.set('game', 'game5');
      navigate({ pathname: targetPath, search: next.toString() }, { replace: true });
    }
  }, [currentTab, activeTab, setActiveTab, searchParams, tabParam, pathname, legacyErrorHunter, navigate]);

  const handleTabChange = (tab: GamificationTab) => {
    if (tab === currentTab) return;
    const next = new URLSearchParams(searchParams);
    next.set('tab', tab);
    if (tab !== 'predictions') next.delete('game');
    navigate({ pathname: tabPath(tab), search: next.toString() });
  };

  const { data: shopData } = useShop({ limit: 1 });
  const checkIn = useCheckInStatus();
  const currentStreak = checkIn.data?.currentStreak;
  const hasCheckedInToday = checkIn.data?.hasCheckedInToday;

  return (
    <div className="mx-auto max-w-7xl space-y-4 pb-8 sm:space-y-5">
      {/* Top Banner & Tab Navigation */}
      <GamificationHeader
        userCoins={shopData?.userPoints}
        streakCount={currentStreak}
        hasCheckedInToday={hasCheckedInToday}
        currentTab={currentTab}
        onTabChange={handleTabChange}
        onRetryCheckIn={checkIn.isError ? () => void checkIn.refetch() : undefined}
      />

      {/* Daily Check-in Modal */}
      <DailyCheckInModal
        currentStreak={currentStreak}
        hasCheckedInToday={hasCheckedInToday}
      />

      {/* Main Tab Content */}
      <div className="pt-2">
        {currentTab === 'leaderboard' && <LeaderboardTab />}
        {currentTab === 'shop' && <ShopInventoryTab />}
        {currentTab === 'predictions' && <PredictionArenaTab />}
        {currentTab === 'badges' && <BadgesTab />}
      </div>
    </div>
  );
}
