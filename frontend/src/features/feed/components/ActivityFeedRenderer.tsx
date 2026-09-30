import React from 'react';
import type { ActivityFeedItemDto } from '../types/feed.types';
import { PostFeedCard } from './PostFeedCard';
import { ApprovedSessionCard } from './cards/ApprovedSessionCard';
import { EarnedBadgeCard } from './cards/EarnedBadgeCard';
import { MilestoneCard } from './cards/MilestoneCard';
import { LectureVideoCard } from './cards/LectureVideoCard';

interface ActivityFeedRendererProps {
  item: ActivityFeedItemDto;
}

export const ActivityFeedRenderer: React.FC<ActivityFeedRendererProps> = ({ item }) => {
  switch (item.actionType) {
    case 'SESSION_RESOLVED_APPROVED':
      return <ApprovedSessionCard item={item} />;
    case 'EARNED_BADGE':
      return <EarnedBadgeCard item={item} />;
    case 'REACHED_MILESTONE':
      return <MilestoneCard item={item} />;
    case 'PUBLISHED_LECTURE_VIDEO':
      return <LectureVideoCard item={item} />;
    case 'CREATED_POST':
    default:
      return <PostFeedCard item={item} />;
  }
};
