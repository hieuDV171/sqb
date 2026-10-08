import { useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { GamificationHeader } from '../components/GamificationHeader';
import { DailyCheckInModal } from '../components/DailyCheckInModal';
import { LeaderboardTab } from '../components/LeaderboardTab';
import { ShopInventoryTab } from '../components/ShopInventoryTab';
import { PredictionArenaTab } from '../components/PredictionArenaTab';
import { BadgesTab } from '../components/BadgesTab';
import { ErrorHunterTab } from '../components/ErrorHunterTab';
import { useGamificationStore, type GamificationTab } from '../stores/useGamificationStore';
import { useShop, useLeaderboard, useCheckInStatus } from '../hooks/useGamification';

interface GamificationHubPageProps {
  initialTab?: GamificationTab;
}

const VALID_TABS: GamificationTab[] = ['leaderboard', 'shop', 'predictions', 'badges', 'error-hunter'];

export function GamificationHubPage({ initialTab }: GamificationHubPageProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const { activeTab, setActiveTab } = useGamificationStore();

  // Xác định tab hiện tại: Ưu tiên query param trên URL -> initialTab -> fallback 'leaderboard'
  const tabParam = searchParams.get('tab') as GamificationTab | null;
  const currentTab: GamificationTab = useMemo(() => {
    if (tabParam && VALID_TABS.includes(tabParam)) {
      return tabParam;
    }
    return initialTab && VALID_TABS.includes(initialTab) ? initialTab : 'leaderboard';
  }, [tabParam, initialTab]);

  // Đồng bộ trạng thái tab vào store và đảm bảo query param ?tab= luôn hiện diện
  useEffect(() => {
    if (activeTab !== currentTab) {
      setActiveTab(currentTab);
    }
    if (searchParams.get('tab') !== currentTab) {
      setSearchParams({ tab: currentTab }, { replace: true });
    }
  }, [currentTab, activeTab, setActiveTab, searchParams, setSearchParams]);

  const handleTabChange = (tab: GamificationTab) => {
    setActiveTab(tab);
    setSearchParams({ tab }, { replace: true });
  };

  // Fetch quick user gamification data (coins from shop endpoint, myRank from leaderboard, check-in status from DB)
  const { data: shopData } = useShop({ limit: 1 });
  const { data: leaderboardData } = useLeaderboard({ limit: 1 });
  const { data: checkInStatus } = useCheckInStatus();

  const userCoins = shopData?.userPoints ?? leaderboardData?.myRank?.totalPoints ?? 0;
  const currentStreak = checkInStatus?.currentStreak ?? 0;
  const hasCheckedInToday = checkInStatus?.hasCheckedInToday ?? false;

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-16">
      {/* Top Banner & Tab Navigation */}
      <GamificationHeader
        userCoins={userCoins}
        streakCount={currentStreak}
        hasCheckedInToday={hasCheckedInToday}
        currentTab={currentTab}
        onTabChange={handleTabChange}
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
        {currentTab === 'error-hunter' && <ErrorHunterTab />}
      </div>
    </div>
  );
}
