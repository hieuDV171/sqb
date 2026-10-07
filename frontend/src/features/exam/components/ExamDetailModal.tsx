import { useState } from 'react';
import { X, FileDown, CheckCircle, HelpCircle } from 'lucide-react';
import { useExamDetail } from '../hooks/useExam';
import { ExportExamModal } from './ExportExamModal';

interface ExamDetailModalProps {
  examId: number | null;
  isOpen: boolean;
  onClose: () => void;
}

export function ExamDetailModal({
  examId,
  isOpen,
  onClose,
}: ExamDetailModalProps) {
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const { data: exam, isLoading } = useExamDetail(examId || undefined);

  if (!isOpen || !examId) return null;

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
        <div className="relative w-full max-w-4xl h-[90vh] flex flex-col rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl overflow-hidden">
          {/* Header */}
          <div className="p-5 sm:p-6 border-b border-slate-200 dark:border-slate-800 flex items-start justify-between gap-4 shrink-0 bg-slate-50/50 dark:bg-slate-900/50">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400">
                  {exam?.subjectName || 'Môn học'}
                </span>
                <span className="text-xs text-slate-400">
                  • {exam?.questionCount ?? 0} câu hỏi
                </span>
              </div>
              <h2 className="text-lg sm:text-xl font-black text-slate-900 dark:text-white line-clamp-1">
                {exam?.title || `Đề thi #${examId}`}
              </h2>
              {exam?.statistic && (
                <div className="flex items-center gap-3 pt-1 text-xs">
                  <span className="text-emerald-600 dark:text-emerald-400 font-semibold">
                    Dễ: {exam.statistic.easyCount ?? 0}
                  </span>
                  <span className="text-amber-600 dark:text-amber-400 font-semibold">
                    Trung bình: {exam.statistic.mediumCount ?? 0}
                  </span>
                  <span className="text-rose-600 dark:text-rose-400 font-semibold">
                    Khó: {exam.statistic.hardCount ?? 0}
                  </span>
                </div>
              )}
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                onClick={() => setIsExportModalOpen(true)}
                className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center gap-1.5 shadow-sm hover:scale-105 active:scale-95 transition-all cursor-pointer"
              >
                <FileDown className="w-4 h-4" />
                <span>Xuất File</span>
              </button>

              <button
                type="button"
                onClick={onClose}
                className="p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Question List (Scrollable) */}
          <div className="flex-1 overflow-y-auto p-5 sm:p-8 space-y-6">
            {isLoading ? (
              <div className="space-y-4">
                {Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="h-32 rounded-2xl bg-slate-100 dark:bg-slate-800 animate-pulse" />
                ))}
              </div>
            ) : !exam || exam.questions.length === 0 ? (
              <div className="py-16 text-center text-slate-400">
                <HelpCircle className="w-12 h-12 mx-auto mb-2 opacity-30" />
                <p className="font-semibold text-sm">Không có dữ liệu câu hỏi trong đề thi này.</p>
              </div>
            ) : (
              exam.questions.map((q, idx) => {
                const diffColor =
                  q.difficulty === 'EASY'
                    ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                    : q.difficulty === 'MEDIUM'
                    ? 'bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300'
                    : q.difficulty === 'HARD'
                    ? 'bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300'
                    : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400';

                return (
                  <div
                    key={q.questionId || idx}
                    className="p-5 rounded-2xl bg-slate-50/70 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-700/80 space-y-3.5"
                  >
                    {/* Question Top Meta */}
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <span className="w-7 h-7 rounded-lg bg-indigo-600 text-white font-black text-xs flex items-center justify-center">
                          {q.order || idx + 1}
                        </span>
                        <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider ${diffColor}`}>
                          {q.difficulty || 'Chưa phân loại'}
                        </span>
                        {q.topic && (
                          <span className="text-[11px] text-slate-400 hidden sm:inline">
                            • {q.topic}
                          </span>
                        )}
                      </div>
                      <span className="text-[11px] font-mono text-slate-400">
                        ID: #{q.questionId}
                      </span>
                    </div>

                    {/* Content */}
                    <p className="text-sm font-semibold text-slate-900 dark:text-white leading-relaxed">
                      {q.content}
                    </p>

                    {/* Question Images */}
                    {q.imageUrls && q.imageUrls.length > 0 && (
                      <div className="flex flex-wrap gap-2 pt-1">
                        {q.imageUrls.map((img, i) => (
                          <img
                            key={i}
                            src={img}
                            alt="Minh họa"
                            className="max-h-48 rounded-xl object-contain border border-slate-200 dark:border-slate-700"
                          />
                        ))}
                      </div>
                    )}

                    {/* Options Grid */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                      {q.options.map((opt) => (
                        <div
                          key={opt.key}
                          className={`p-3 rounded-xl border text-xs flex items-start gap-2.5 transition-all ${
                            opt.isCorrect
                              ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-300 dark:border-emerald-700/80 font-semibold text-emerald-900 dark:text-emerald-200 ring-1 ring-emerald-400/30'
                              : 'bg-white dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300'
                          }`}
                        >
                          <span
                            className={`w-5 h-5 rounded-md font-bold text-xs flex items-center justify-center shrink-0 ${
                              opt.isCorrect
                                ? 'bg-emerald-600 text-white'
                                : 'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300'
                            }`}
                          >
                            {opt.key}
                          </span>
                          <span className="flex-1">{opt.text}</span>
                          {opt.isCorrect && (
                            <CheckCircle className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Export Modal */}
      <ExportExamModal
        examId={examId}
        examTitle={exam?.title}
        isOpen={isExportModalOpen}
        onClose={() => setIsExportModalOpen(false)}
      />
    </>
  );
}
