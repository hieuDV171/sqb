import React, { useState } from 'react';
import {
  X,
  Star,
  AlertTriangle,
  Send,
  ShieldAlert,
  Loader2,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useQuestionRatingMutation } from '../hooks/useQuestionRatingMutation';
import { useQuestionRatings } from '../hooks/useQuestionRatings';

interface QuestionRateModalProps {
  questionId: number | null;
  questionCode?: string;
  sessionId?: number;
  isOpen: boolean;
  onClose: () => void;
}

export const QuestionRateModal: React.FC<QuestionRateModalProps> = ({
  questionId,
  questionCode,
  sessionId,
  isOpen,
  onClose,
}) => {
  const [selectedStar, setSelectedStar] = useState<number>(5); // 1 to 5
  const [isErrorMode, setIsErrorMode] = useState<boolean>(false);
  const [comment, setComment] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'rate' | 'reviews'>('rate');

  const { mutateAsync: rateQuestion, isPending: isSubmitting } =
    useQuestionRatingMutation();
  const {
    data: ratingsData,
    isLoading: isLoadingRatings,
    hasNextPage,
    fetchNextPage,
    isFetchingNextPage,
  } = useQuestionRatings(questionId, isOpen && activeTab === 'reviews');

  if (!isOpen || !questionId) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await rateQuestion({
        questionId,
        sessionId,
        request: {
          rating: isErrorMode ? 0 : Math.max(0, Math.min(4, selectedStar - 1)),
          isError: isErrorMode,
          comment: comment.trim() || undefined,
        },
      });
      setComment('');
      onClose();
    } catch (err) {
      console.error('Failed to submit rating:', err);
    }
  };

  const allReviews = ratingsData?.pages.flatMap((page) => page.items) || [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl overflow-hidden p-6 space-y-5">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-4">
          <div className="flex items-center gap-2.5">
            <span
              className={cn(
                'p-2 rounded-xl text-white shadow-xs',
                isErrorMode ? 'bg-rose-500' : 'bg-amber-500'
              )}
            >
              {isErrorMode ? (
                <ShieldAlert className="w-5 h-5" />
              ) : (
                <Star className="w-5 h-5 fill-white" />
              )}
            </span>
            <div>
              <h3 className="font-bold text-slate-900 dark:text-white text-base">
                {isErrorMode ? 'Báo lỗi câu hỏi (Error Hunter)' : 'Đánh giá câu hỏi'}
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

        {/* Tab Toggle: Form Đánh giá vs Lịch sử đánh giá */}
        <div className="flex p-1 bg-slate-100 dark:bg-slate-800 rounded-xl">
          <button
            type="button"
            onClick={() => setActiveTab('rate')}
            className={cn(
              'flex-1 py-1.5 text-xs font-bold rounded-lg transition-all cursor-pointer text-center',
              activeTab === 'rate'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-800'
            )}
          >
            Đánh giá của bạn
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('reviews')}
            className={cn(
              'flex-1 py-1.5 text-xs font-bold rounded-lg transition-all cursor-pointer text-center',
              activeTab === 'reviews'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs'
                : 'text-slate-500 dark:text-slate-400 hover:text-slate-800'
            )}
          >
            Đánh giá từ cộng đồng
          </button>
        </div>

        {activeTab === 'rate' ? (
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Mode Switcher Toggle: Normal Rating vs Error Hunter */}
            <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200/80 dark:border-slate-700/80 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <AlertTriangle
                  className={cn(
                    'w-4 h-4',
                    isErrorMode ? 'text-rose-500' : 'text-slate-400'
                  )}
                />
                <div>
                  <span className="text-xs font-bold text-slate-800 dark:text-slate-200 block">
                    Báo lỗi câu hỏi này?
                  </span>
                  <span className="text-[11px] text-slate-400">
                    Kích hoạt Thợ Săn Lỗi nhận thưởng khi GV xác nhận
                  </span>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setIsErrorMode(!isErrorMode)}
                className={cn(
                  'w-11 h-6 rounded-full transition-colors relative cursor-pointer',
                  isErrorMode ? 'bg-rose-500' : 'bg-slate-300 dark:bg-slate-700'
                )}
              >
                <div
                  className={cn(
                    'w-4 h-4 rounded-full bg-white transition-transform absolute top-1',
                    isErrorMode ? 'right-1' : 'left-1'
                  )}
                />
              </button>
            </div>

            {/* Star Rating Section (Only in Normal Mode) */}
            {!isErrorMode && (
              <div className="space-y-2 text-center py-2">
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300 block">
                  Chấm điểm chất lượng câu hỏi:
                </label>
                <div className="flex items-center justify-center gap-2">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setSelectedStar(star)}
                      className="p-1.5 transition-transform hover:scale-115 active:scale-95 cursor-pointer"
                    >
                      <Star
                        className={cn(
                          'w-8 h-8 transition-colors',
                          star <= selectedStar
                            ? 'text-amber-500 fill-amber-500'
                            : 'text-slate-300 dark:text-slate-700'
                        )}
                      />
                    </button>
                  ))}
                </div>
                <p className="text-xs text-slate-400 font-medium">
                  {selectedStar === 5 && '🌟 Rất xuất sắc & thực tế'}
                  {selectedStar === 4 && '👍 Hay & bám sát kiến thức'}
                  {selectedStar === 3 && '👌 Tạm ổn, ở mức cơ bản'}
                  {selectedStar === 2 && '👎 Chưa thực sự rõ ràng'}
                  {selectedStar === 1 && '⚠️ Kém chất lượng hoặc khó hiểu'}
                </p>
              </div>
            )}

            {/* Comment Box */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700 dark:text-slate-300">
                {isErrorMode ? 'Mô tả chi tiết lỗi phát hiện:' : 'Nhận xét thêm (tùy chọn):'}
              </label>
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                rows={3}
                required={isErrorMode}
                placeholder={
                  isErrorMode
                    ? 'Ví dụ: Đề bài câu hỏi bị thiếu dữ kiện, hoặc đáp án đúng ghi A nhưng tính ra phải là C...'
                    : 'Chia sẻ cảm nhận về mức độ phân hóa, bẫy trắc nghiệm của câu hỏi này...'
                }
                className="w-full bg-slate-50 dark:bg-slate-800/80 rounded-2xl p-3 text-xs md:text-sm text-slate-800 dark:text-slate-100 border border-slate-200 dark:border-slate-700 focus:outline-hidden focus:border-indigo-500 resize-none transition-all placeholder:text-slate-400"
              />
            </div>

            {/* Submit Action */}
            <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
              <button
                type="button"
                onClick={onClose}
                disabled={isSubmitting}
                className="px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={isSubmitting || (isErrorMode && !comment.trim())}
                className={cn(
                  'flex items-center gap-2 px-5 py-2 rounded-xl text-xs sm:text-sm font-semibold text-white transition-all shadow-md active:scale-95 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed',
                  isErrorMode
                    ? 'bg-rose-600 hover:bg-rose-700 shadow-rose-600/20'
                    : 'bg-amber-600 hover:bg-amber-700 shadow-amber-600/20'
                )}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang gửi...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    <span>{isErrorMode ? 'Gửi báo lỗi' : 'Gửi đánh giá'}</span>
                  </>
                )}
              </button>
            </div>
          </form>
        ) : (
          /* Reviews Tab */
          <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
            {isLoadingRatings ? (
              <div className="space-y-2 py-4 text-center">
                <Loader2 className="w-6 h-6 animate-spin mx-auto text-indigo-500" />
                <p className="text-xs text-slate-400">Đang tải đánh giá...</p>
              </div>
            ) : allReviews.length === 0 ? (
              <p className="text-xs text-slate-400 dark:text-slate-500 italic py-8 text-center">
                Chưa có đánh giá nào từ cộng đồng cho câu hỏi này.
              </p>
            ) : (
              <div className="space-y-2.5">
                {allReviews.map((r, i) => (
                  <div
                    key={i}
                    className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200/60 dark:border-slate-700/60 text-xs space-y-1.5"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-1.5">
                        {r.isError ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-rose-100 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400 font-bold text-[11px]">
                            <ShieldAlert className="w-3 h-3" /> Báo lỗi
                          </span>
                        ) : (
                          <div className="flex items-center gap-0.5 text-amber-500">
                            {Array.from({ length: Math.round(r.rating) + 1 }).map((_, si) => (
                              <Star key={si} className="w-3.5 h-3.5 fill-amber-500" />
                            ))}
                          </div>
                        )}
                      </div>
                      <span className="text-[11px] text-slate-400">
                        {new Date(r.createdAt).toLocaleDateString('vi-VN')}
                      </span>
                    </div>

                    {r.comment && (
                      <p className="text-slate-700 dark:text-slate-300 leading-relaxed">
                        {r.comment}
                      </p>
                    )}
                  </div>
                ))}

                {hasNextPage && (
                  <button
                    type="button"
                    onClick={() => fetchNextPage()}
                    disabled={isFetchingNextPage}
                    className="w-full py-2 text-xs font-bold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 rounded-xl transition-all cursor-pointer"
                  >
                    {isFetchingNextPage ? 'Đang tải thêm...' : 'Xem thêm đánh giá'}
                  </button>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
