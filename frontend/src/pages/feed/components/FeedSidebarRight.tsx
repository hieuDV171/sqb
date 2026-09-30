import type { SubjectItem } from '@/types/post.types';
import { Link } from 'react-router-dom';
import { BookOpen, Trophy, Calendar, Zap, Flame, ExternalLink } from 'lucide-react';

interface FeedSidebarRightProps {
  subjects: SubjectItem[];
  selectedSubjectId?: number;
  onSelectSubject: (subjectId?: number) => void;
}

export function FeedSidebarRight({
  subjects,
  selectedSubjectId,
  onSelectSubject,
}: FeedSidebarRightProps) {
  return (
    <aside className="space-y-4 w-full">
      
      {/* Widget 1: Trending Subjects Filter */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 p-4 shadow-xs space-y-3 transition-colors">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-1.5">
            <Flame className="w-4 h-4 text-orange-500 fill-orange-400" />
            Môn Học Sôi Nổi
          </h3>
          {selectedSubjectId && (
            <button
              type="button"
              onClick={() => onSelectSubject(undefined)}
              className="text-[11px] font-semibold text-indigo-600 dark:text-indigo-400 hover:text-indigo-800 dark:hover:text-indigo-300 cursor-pointer"
            >
              Xóa bộ lọc
            </button>
          )}
        </div>

        <div className="space-y-1">
          {subjects.map((sub) => {
            const isSelected = selectedSubjectId === sub.id;

            return (
              <button
                key={sub.id}
                type="button"
                onClick={() => onSelectSubject(isSelected ? undefined : sub.id)}
                className={`w-full flex items-center justify-between p-2 rounded-xl text-left text-xs transition-colors cursor-pointer ${
                  isSelected
                    ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-400 font-bold border border-indigo-200 dark:border-indigo-800'
                    : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800'
                }`}
              >
                <div className="flex items-center gap-2 min-w-0">
                  <BookOpen className={`w-3.5 h-3.5 shrink-0 ${isSelected ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400 dark:text-slate-500'}`} />
                  <span className="truncate">{sub.name}</span>
                </div>
                <span className="text-[10px] font-medium text-slate-400 dark:text-slate-500 shrink-0 ml-2">
                  {sub.postCount || 12} bài
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Widget 2: Mini Leaderboard Preview */}
      <div className="bg-linear-to-br from-indigo-950 via-slate-900 to-slate-950 text-white rounded-2xl p-4 shadow-sm space-y-3 border border-indigo-800/60 dark:border-slate-800">
        <div className="flex items-center justify-between">
          <h3 className="text-xs font-bold text-amber-300 flex items-center gap-1.5 uppercase tracking-wider">
            <Trophy className="w-3.5 h-3.5 text-amber-400 fill-amber-400" />
            Bảng Vàng Học Kỳ
          </h3>
          <Link
            to="/leaderboard"
            className="text-[11px] font-semibold text-indigo-300 hover:text-white flex items-center gap-0.5"
          >
            Chi tiết <ExternalLink className="w-3 h-3" />
          </Link>
        </div>

        <div className="space-y-2">
          {/* Mini Top 1 */}
          <div className="flex items-center justify-between p-2 bg-white/10 rounded-xl text-xs backdrop-blur-xs">
            <div className="flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-amber-400 text-slate-900 font-black text-[10px] flex items-center justify-center">
                1
              </span>
              <span className="font-bold text-slate-100">Nguyễn Văn An</span>
            </div>
            <span className="font-bold text-amber-300 flex items-center gap-0.5 text-[11px]">
              <Zap className="w-3 h-3 fill-amber-400 text-amber-400" /> 2,450
            </span>
          </div>

          {/* Mini Top 2 */}
          <div className="flex items-center justify-between p-2 bg-white/5 rounded-xl text-xs">
            <div className="flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-slate-300 text-slate-900 font-black text-[10px] flex items-center justify-center">
                2
              </span>
              <span className="font-medium text-slate-200">Trần Thị Bình</span>
            </div>
            <span className="font-bold text-slate-300 text-[11px]">2,180 pts</span>
          </div>

          {/* Mini Top 3 */}
          <div className="flex items-center justify-between p-2 bg-white/5 rounded-xl text-xs">
            <div className="flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-orange-400 text-slate-900 font-black text-[10px] flex items-center justify-center">
                3
              </span>
              <span className="font-medium text-slate-200">Lê Quang Cường</span>
            </div>
            <span className="font-bold text-orange-300 text-[11px]">1,920 pts</span>
          </div>
        </div>

        <p className="text-[10px] text-indigo-200/80 leading-relaxed text-center pt-1 border-t border-white/10">
          Cuộc đua Top 3 nhận <strong>+300 / +180 / +90 xu</strong> cuối kỳ!
        </p>
      </div>

      {/* Widget 3: Academic Calendar Reminder */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 p-4 shadow-xs space-y-2 transition-colors">
        <div className="flex items-center gap-1.5 text-xs font-bold text-slate-900 dark:text-slate-100">
          <Calendar className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
          <span>Sự Kiện Học Thuật</span>
        </div>
        <div className="p-2.5 bg-slate-50 dark:bg-slate-800/60 rounded-xl text-xs space-y-1">
          <span className="text-[10px] font-bold text-rose-600 dark:text-rose-400 uppercase">Hạn chót 15/09</span>
          <p className="font-semibold text-slate-800 dark:text-slate-200 text-xs">Đóng đề xuất câu hỏi đợt 1</p>
          <p className="text-[11px] text-slate-500 dark:text-slate-400">Giảng viên bắt đầu hội đồng duyệt đề thi giữa kỳ.</p>
        </div>
      </div>

    </aside>
  );
}
