import { Link } from 'react-router-dom';
import { ArrowRight, BookOpen, Trophy } from 'lucide-react';
import type { ProfileResponse } from '@/types/user.types';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

export function ProfileGamificationStats({ profile, isOwnProfile }: { profile: ProfileResponse; isOwnProfile: boolean }) {
  const stats = [
    { label: 'Điểm đóng góp', value: profile.gamificationPoints },
    { label: 'Câu hỏi đóng góp', value: profile.totalProposedQuestions },
    { label: 'Huy hiệu', value: profile.badgesCount },
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h2 className="flex items-center gap-2 text-lg font-semibold">
            <span className="grid size-9 place-items-center rounded-xl bg-gamification-soft text-gamification">
              <Trophy className="size-5" aria-hidden="true" />
            </span>
            Thành tích học tập
          </h2>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <dl className="grid gap-2 sm:grid-cols-3">
          {stats.map(({ label, value }) => (
            <div key={label} className="flex flex-col gap-1 rounded-xl border border-border bg-surface-subtle p-3">
              <dt className="text-sm text-muted-foreground">{label}</dt>
              <dd className="text-2xl font-semibold tabular-nums">{(value ?? 0).toLocaleString('vi-VN')}</dd>
            </div>
          ))}
        </dl>
        <div className="flex flex-wrap gap-2">
          {isOwnProfile && (
            <Button nativeButton={false} variant="outline" render={<Link to="/questions" />}>
              <BookOpen aria-hidden="true" /> Phiên nộp của tôi
            </Button>
          )}
          <Button nativeButton={false} variant="ghost" render={<Link to="/leaderboard" />}>
            Bảng xếp hạng <ArrowRight aria-hidden="true" />
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
