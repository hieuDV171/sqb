import { Link } from 'react-router-dom';
import type { ProfileResponse } from '@/types/user.types';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import {
  Trophy,
  Award,
  HelpCircle,
  ShoppingBag,
  ArrowRight,
  TrendingUp,
  BookOpen,
} from 'lucide-react';

interface ProfileGamificationStatsProps {
  profile: ProfileResponse;
}

export function ProfileGamificationStats({ profile }: ProfileGamificationStatsProps) {
  const stats = [
    {
      title: 'Điểm cống hiến (XP)',
      value: profile.gamificationPoints ?? 0,
      icon: Trophy,
      color: 'from-amber-500 to-orange-500',
      textColor: 'text-amber-600 dark:text-amber-400',
      bgColor: 'bg-amber-50 dark:bg-amber-950/40',
      borderColor: 'border-amber-200 dark:border-amber-900/50',
      sub: 'Đứng hạng trên bảng xếp hạng',
    },
    {
      title: 'Câu hỏi đã đóng góp',
      value: profile.totalProposedQuestions ?? 0,
      icon: HelpCircle,
      color: 'from-indigo-500 to-blue-500',
      textColor: 'text-indigo-600 dark:text-indigo-400',
      bgColor: 'bg-indigo-50 dark:bg-indigo-950/40',
      borderColor: 'border-indigo-200 dark:border-indigo-900/50',
      sub: 'Được giảng viên phê duyệt',
    },
    {
      title: 'Huy hiệu thành tích',
      value: profile.badgesCount ?? 0,
      icon: Award,
      color: 'from-emerald-500 to-teal-500',
      textColor: 'text-emerald-600 dark:text-emerald-400',
      bgColor: 'bg-emerald-50 dark:bg-emerald-950/40',
      borderColor: 'border-emerald-200 dark:border-emerald-900/50',
      sub: 'Mở khóa qua hoạt động học tập',
    },
  ];

  return (
    <Card className="rounded-3xl border-slate-200/80 dark:border-slate-800 shadow-xs">
      <CardHeader className="pb-3 flex flex-row items-center justify-between">
        <CardTitle className="text-base font-bold flex items-center gap-2 text-slate-900 dark:text-white">
          <div className="w-8 h-8 rounded-xl bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 flex items-center justify-center">
            <TrendingUp className="w-4 h-4" />
          </div>
          <span>Thành tích & Gamification</span>
        </CardTitle>

        <Link
          to="/leaderboard"
          className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1"
        >
          <span>Xem BXH</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {stats.map((stat, idx) => {
            const Icon = stat.icon;
            return (
              <div
                key={idx}
                className={`p-4 rounded-2xl border ${stat.bgColor} ${stat.borderColor} relative overflow-hidden transition-all hover:scale-[1.02]`}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-semibold text-slate-600 dark:text-slate-300">
                    {stat.title}
                  </span>
                  <div
                    className={`w-7 h-7 rounded-xl bg-linear-to-tr ${stat.color} text-white flex items-center justify-center shadow-xs`}
                  >
                    <Icon className="w-3.5 h-3.5" />
                  </div>
                </div>
                <div className="text-2xl font-black font-mono tracking-tight text-slate-900 dark:text-white">
                  {stat.value.toLocaleString()}
                </div>
                <p className="text-[10px] text-slate-500 dark:text-slate-400 mt-1 truncate">
                  {stat.sub}
                </p>
              </div>
            );
          })}
        </div>

        {/* Shop cosmetic banner call-to-action */}
        <div className="p-3.5 rounded-2xl bg-linear-to-r from-indigo-500/10 via-purple-500/10 to-pink-500/10 border border-indigo-200/50 dark:border-indigo-900/50 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-indigo-600 text-white shadow-xs">
              <ShoppingBag className="w-4 h-4" />
            </div>
            <div>
              <p className="text-xs font-bold text-slate-800 dark:text-slate-100">
                Đổi khung đại diện & danh hiệu
              </p>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                Sử dụng điểm XP đã tích lũy trong cửa hàng vật phẩm
              </p>
            </div>
          </div>
          <Link
            to="/shop"
            className="px-3 py-1.5 rounded-xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs font-bold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-slate-700/80 transition-colors shadow-2xs shrink-0"
          >
            Vào cửa hàng
          </Link>
        </div>

        {/* User Proposed Sessions Link */}
        <div className="p-3.5 rounded-2xl bg-indigo-50/70 dark:bg-indigo-950/40 border border-indigo-200/60 dark:border-indigo-900/60 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-indigo-600 text-white shadow-xs">
              <BookOpen className="w-4 h-4" />
            </div>
            <div>
              <p className="text-xs font-bold text-slate-800 dark:text-slate-100">
                Bộ đề câu hỏi trắc nghiệm
              </p>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                Xem và luyện tập các câu hỏi do thành viên này đóng góp
              </p>
            </div>
          </div>
          <Link
            to={`/users/${profile.userId}/sessions`}
            className="px-3 py-1.5 rounded-xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs font-bold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-slate-700/80 transition-colors shadow-2xs shrink-0"
          >
            Luyện tập
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}
