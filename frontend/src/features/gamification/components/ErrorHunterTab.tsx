import { useState } from 'react';
import { ShieldAlert, Send, UserCheck } from 'lucide-react';
import { useReviewErrorHunter } from '../hooks/useGamification';
import { useAuthStore } from '@/stores/useAuthStore';

export function ErrorHunterTab() {
  const { user } = useAuthStore();
  const isLecturerOrAdmin = user?.role === 'LECTURER' || user?.role === 'ADMIN';

  // Review Error Form state (for Lecturers/Admins)
  const [questionId, setQuestionId] = useState<string>('');
  const [ratingUserId, setRatingUserId] = useState<string>('');
  const [isComfirmed, setIsComfirmed] = useState<boolean>(true);
  const [notes, setNotes] = useState<string>('');

  const reviewErrorMutation = useReviewErrorHunter();

  const handleReview = (e: React.FormEvent) => {
    e.preventDefault();
    if (!questionId || !ratingUserId) return;
    reviewErrorMutation.mutate({
      questionId: Number(questionId),
      ratingUserId: Number(ratingUserId),
      data: {
        isComfirmed,
        notes: notes.trim() || undefined,
      },
    });
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Intro Banner */}
      <div className="p-6 sm:p-8 rounded-3xl bg-linear-to-r from-rose-500/10 via-amber-500/10 to-orange-500/10 border border-rose-200/80 dark:border-rose-900/50 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="space-y-2 max-w-xl">
          <div className="flex items-center gap-2">
            <span className="p-1.5 rounded-xl bg-rose-500 text-white shadow-xs">
              <ShieldAlert className="w-4 h-4" />
            </span>
            <span className="text-xs font-black uppercase tracking-wider text-rose-600 dark:text-rose-400">
              Cơ Chế Báo Lỗi & Thẩm Định
            </span>
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white">
            Thợ Săn Lỗi Câu Hỏi (Error Hunter)
          </h2>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-300">
            Khi phát hiện câu hỏi trong ngân hàng đề có lỗi sai về kiến thức, thiếu dữ kiện hoặc đáp án không chính xác, hãy bấm báo lỗi trong khi làm bài luyện tập. Khi được Giảng viên xác nhận, bạn sẽ nhận ngay <strong className="text-rose-600 dark:text-rose-400">+50 điểm cống hiến</strong> và mở khóa Huy hiệu Thợ Săn Lỗi!
          </p>
        </div>

        <div className="flex flex-col gap-2 shrink-0">
          <div className="p-4 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 shadow-sm text-center">
            <p className="text-2xl font-black text-rose-600 dark:text-rose-400 font-mono">+50 pts</p>
            <p className="text-[11px] font-bold text-slate-500 uppercase">Thưởng mỗi lỗi chuẩn</p>
          </div>
        </div>
      </div>

      {/* Rules / Steps */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-5 rounded-2xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs space-y-2">
          <span className="w-8 h-8 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 font-black text-xs flex items-center justify-center">
            1
          </span>
          <h4 className="font-bold text-sm text-slate-900 dark:text-white">Luyện tập câu hỏi</h4>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Làm bài trắc nghiệm tại mục Luyện tập hoặc trong các bộ đề thi của cộng đồng sinh viên.
          </p>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs space-y-2">
          <span className="w-8 h-8 rounded-xl bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 font-black text-xs flex items-center justify-center">
            2
          </span>
          <h4 className="font-bold text-sm text-slate-900 dark:text-white">Báo cáo lỗi & Góp ý</h4>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Nhấn vào biểu tượng Báo lỗi trên câu hỏi, ghi rõ lý do nghi ngờ sai đề bài hoặc lời giải.
          </p>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs space-y-2">
          <span className="w-8 h-8 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 font-black text-xs flex items-center justify-center">
            3
          </span>
          <h4 className="font-bold text-sm text-slate-900 dark:text-white">Nhận thưởng vinh danh</h4>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Giảng viên thẩm định tính chính xác của báo cáo. Nếu được duyệt, bạn nhận thưởng và huy hiệu.
          </p>
        </div>
      </div>

      {/* Lecturer / Admin Review Workspace */}
      {isLecturerOrAdmin && (
        <div className="p-6 sm:p-8 rounded-3xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs space-y-5">
          <div className="flex items-center gap-2.5">
            <span className="p-2 rounded-xl bg-indigo-600 text-white shadow-xs">
              <UserCheck className="w-4 h-4" />
            </span>
            <div>
              <h3 className="font-black text-base text-slate-900 dark:text-white">
                Bàn Thẩm Định Báo Lỗi (Dành Cho Giảng Viên & Quản Trị Viên)
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Xác nhận các báo lỗi từ sinh viên để cộng điểm thưởng hoặc từ chối báo lỗi sai.
              </p>
            </div>
          </div>

          <form onSubmit={handleReview} className="space-y-4 max-w-lg">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                  Mã câu hỏi (Question ID):
                </label>
                <input
                  type="number"
                  required
                  placeholder="Ví dụ: 105"
                  value={questionId}
                  onChange={(e) => setQuestionId(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs font-bold text-slate-800 dark:text-slate-100"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                  ID Người báo lỗi (Rating User ID):
                </label>
                <input
                  type="number"
                  required
                  placeholder="Ví dụ: 42"
                  value={ratingUserId}
                  onChange={(e) => setRatingUserId(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs font-bold text-slate-800 dark:text-slate-100"
                />
              </div>
            </div>

            <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700">
              <input
                type="checkbox"
                id="isComfirmed"
                checked={isComfirmed}
                onChange={(e) => setIsComfirmed(e.target.checked)}
                className="w-4 h-4 rounded-sm text-indigo-600 focus:ring-indigo-500"
              />
              <label htmlFor="isComfirmed" className="text-xs font-bold text-slate-800 dark:text-slate-200 cursor-pointer">
                Xác nhận câu hỏi thực sự có lỗi (Đồng ý thưởng điểm cho người báo)
              </label>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                Ghi chú giải thích thẩm định:
              </label>
              <textarea
                rows={3}
                placeholder="Ghi rõ lý do chấp nhận hoặc từ chối báo lỗi..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs text-slate-800 dark:text-slate-100"
              />
            </div>

            <button
              type="submit"
              disabled={reviewErrorMutation.isPending}
              className="py-3 px-6 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-sm hover:scale-105 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
            >
              <Send className="w-3.5 h-3.5" />
              <span>{reviewErrorMutation.isPending ? 'Đang gửi...' : 'Xác Nhận Thẩm Định'}</span>
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
