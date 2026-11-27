import React from 'react';
import { X, BarChart3, TrendingUp, Star } from 'lucide-react';
import { useQuestionStatistics } from '../hooks/useQuestionStatistics';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from '@/components/ui/error-state';

interface QuestionStatsModalProps {
  questionId: number | null;
  questionCode?: string;
  isOpen: boolean;
  onClose: () => void;
}

export const QuestionStatsModal: React.FC<QuestionStatsModalProps> = ({
  questionId,
  questionCode,
  isOpen,
  onClose,
}) => {
  const { data, isLoading, isError, error, refetch } = useQuestionStatistics(
    questionId,
    isOpen
  );

  if (!isOpen || !questionId) return null;

  const totalAnswer = data?.totalAnswer || 0;
  const correctPercent = data ? Math.round(data.correctRate * 100) : 0;
  const distribution = data?.optionDistribution || {};

  // Find max count for relative bar width
  const maxOptionCount = Math.max(...Object.values(distribution), 1);

  const avgRating = data?.ratingSummary?.avgRating
    ? (data.ratingSummary.avgRating + 1).toFixed(1) // Map 0..4 to 1..5 scale
    : '0.0';
  const totalRatings = data?.ratingSummary?.totalRatings || 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-md rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl overflow-hidden p-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-4">
          <div className="flex items-center gap-2.5">
            <span className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
              <BarChart3 className="w-5 h-5" />
            </span>
            <div>
              <h3 className="font-bold text-slate-900 dark:text-white text-base">
                Thống kê thực tế câu hỏi
              </h3>
              {questionCode && (
                <p className="text-xs text-slate-400 font-mono">Mã: {questionCode}</p>
              )}
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        {isLoading ? (
          <div className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-3">
              <Skeleton className="h-20 rounded-2xl" />
              <Skeleton className="h-20 rounded-2xl" />
            </div>
            <Skeleton className="h-32 rounded-2xl" />
          </div>
        ) : isError ? (
          <ErrorState
            title="Không thể tải thống kê"
            message={(error as Error)?.message || 'Vui lòng thử lại sau.'}
            onRetry={() => refetch()}
          />
        ) : (
          <div className="space-y-5">
            {/* Quick KPI Cards */}
            <div className="grid grid-cols-2 gap-3">
              {/* Total Attempts & Correct Rate */}
              <div className="p-3.5 rounded-2xl bg-indigo-50/60 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/40">
                <div className="flex items-center gap-1.5 text-xs text-indigo-600 dark:text-indigo-400 font-bold mb-1">
                  <TrendingUp className="w-4 h-4" />
                  <span>Tỷ lệ làm đúng</span>
                </div>
                <div className="text-2xl font-black text-indigo-950 dark:text-indigo-200">
                  {correctPercent}%
                </div>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                  Trên tổng {totalAnswer} lượt làm
                </p>
              </div>

              {/* Community Rating */}
              <div className="p-3.5 rounded-2xl bg-amber-50/60 dark:bg-amber-950/30 border border-amber-100 dark:border-amber-900/40">
                <div className="flex items-center gap-1.5 text-xs text-amber-600 dark:text-amber-400 font-bold mb-1">
                  <Star className="w-4 h-4 fill-amber-500 text-amber-500" />
                  <span>Đánh giá chất lượng</span>
                </div>
                <div className="text-2xl font-black text-amber-950 dark:text-amber-200">
                  {avgRating} <span className="text-xs font-normal text-slate-400">/ 5</span>
                </div>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                  {totalRatings} lượt đánh giá
                </p>
              </div>
            </div>

            {/* Option Distribution Bars */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold text-slate-700 dark:text-slate-300">
                Phân bố lựa chọn của cộng đồng (Nhận biết bẫy đề thi):
              </h4>

              {Object.keys(distribution).length === 0 ? (
                <p className="text-xs text-slate-400 dark:text-slate-500 italic py-2 text-center">
                  Chưa có dữ liệu lựa chọn cho câu hỏi này.
                </p>
              ) : (
                <div className="space-y-2">
                  {Object.entries(distribution)
                    .sort(([a], [b]) => a.localeCompare(b))
                    .map(([optKey, count]) => {
                      const percent = totalAnswer > 0 ? Math.round((count / totalAnswer) * 100) : 0;
                      const relativeWidth = Math.round((count / maxOptionCount) * 100);

                      return (
                        <div key={optKey} className="space-y-1">
                          <div className="flex justify-between items-center text-xs">
                            <span className="font-bold text-slate-800 dark:text-slate-200">
                              Lựa chọn {optKey}
                            </span>
                            <span className="text-slate-500 dark:text-slate-400 font-mono text-[11px]">
                              {count} lượt ({percent}%)
                            </span>
                          </div>
                          <div className="w-full h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-indigo-500 dark:bg-indigo-400 rounded-full transition-all duration-300"
                              style={{ width: `${Math.max(relativeWidth, 4)}%` }}
                            />
                          </div>
                        </div>
                      );
                    })}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="flex justify-end pt-2 border-t border-slate-100 dark:border-slate-800">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-xs sm:text-sm font-semibold text-slate-700 dark:text-slate-200 transition-colors cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
};
