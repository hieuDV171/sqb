import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { GamificationHeader } from '../components/GamificationHeader';
import { DailyCheckInModal } from '../components/DailyCheckInModal';
import { LeaderboardTab } from '../components/LeaderboardTab';
import { ShopInventoryTab } from '../components/ShopInventoryTab';
import { PredictionArenaTab } from '../components/PredictionArenaTab';
import { BadgesTab } from '../components/BadgesTab';
import { ErrorHunterTab } from '../components/ErrorHunterTab';
import { useGamificationStore, type GamificationTab } from '../stores/useGamificationStore';
import { useShop, useLeaderboard } from '../hooks/useGamification';

interface GamificationHubPageProps {
  initialTab?: GamificationTab;
}

export function GamificationHubPage({ initialTab }: GamificationHubPageProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const { activeTab, setActiveTab, currentStreak, hasCheckedInToday } = useGamificationStore();

  // Sync tab with URL search parameter
  useEffect(() => {
    const tabParam = searchParams.get('tab') as GamificationTab | null;
    if (tabParam && ['leaderboard', 'shop', 'predictions', 'badges', 'error-hunter'].includes(tabParam)) {
      setActiveTab(tabParam);
    } else if (initialTab) {
      setActiveTab(initialTab);
    }
  }, [searchParams, initialTab, setActiveTab]);

  // Update URL when activeTab changes
  useEffect(() => {
    if (searchParams.get('tab') !== activeTab) {
      setSearchParams({ tab: activeTab }, { replace: true });
    }
  }, [activeTab, searchParams, setSearchParams]);

  // Fetch quick user gamification data (coins from shop endpoint, myRank from leaderboard)
  const { data: shopData } = useShop({ limit: 1 });
  const { data: leaderboardData } = useLeaderboard({ limit: 1 });

  const userCoins = shopData?.userPoints ?? leaderboardData?.myRank?.totalPoints ?? 0;

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-16">
      {/* Top Banner & Tab Navigation */}
      <GamificationHeader
        userCoins={userCoins}
        streakCount={currentStreak}
        hasCheckedInToday={hasCheckedInToday}
      />

      {/* Daily Check-in Modal */}
      <DailyCheckInModal
        currentStreak={currentStreak}
        hasCheckedInToday={hasCheckedInToday}
      />

      {/* Main Tab Content */}
      <div className="pt-2">
        {activeTab === 'leaderboard' && <LeaderboardTab />}
        {activeTab === 'shop' && <ShopInventoryTab />}
        {activeTab === 'predictions' && <PredictionArenaTab />}
        {activeTab === 'badges' && <BadgesTab />}
        {activeTab === 'error-hunter' && <ErrorHunterTab />}
      </div>
    </div>
  );
}
