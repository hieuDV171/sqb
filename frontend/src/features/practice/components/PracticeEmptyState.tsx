import React from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, ArrowLeft } from 'lucide-react';

interface PracticeEmptyStateProps {
  title?: string;
  message?: string;
  actionText?: string;
  onAction?: () => void;
}

export const PracticeEmptyState: React.FC<PracticeEmptyStateProps> = ({
  title = 'Không tìm thấy câu hỏi luyện tập',
  message = 'Phiên câu hỏi này hiện tại chưa có câu hỏi nào hoặc chưa được phê duyệt để luyện tập.',
  actionText = 'Quay lại danh sách câu hỏi',
  onAction,
}) => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center justify-center p-8 sm:p-12 text-center rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm max-w-lg mx-auto my-12">
      <div className="w-16 h-16 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center mb-4 shadow-xs">
        <BookOpen className="w-8 h-8" />
      </div>
      <h3 className="text-lg font-bold text-slate-800 dark:text-slate-100 mb-1.5">{title}</h3>
      <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 max-w-sm mb-6 leading-relaxed">
        {message}
      </p>
      <button
        type="button"
        onClick={onAction || (() => navigate('/questions'))}
        className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-semibold transition-all shadow-md shadow-indigo-600/20 active:scale-95 cursor-pointer"
      >
        <ArrowLeft className="w-4 h-4" />
        {actionText}
      </button>
    </div>
  );
};
