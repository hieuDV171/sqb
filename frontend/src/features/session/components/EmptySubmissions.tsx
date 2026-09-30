import React from 'react';
import { BookOpen, Plus, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface EmptySubmissionsProps {
  onProposeNew?: () => void;
  filtered?: boolean;
}

export const EmptySubmissions: React.FC<EmptySubmissionsProps> = ({
  onProposeNew,
  filtered = false,
}) => {
  return (
    <div className="flex flex-col items-center justify-center p-8 sm:p-12 text-center bg-white/50 dark:bg-slate-900/50 border border-slate-200/80 dark:border-slate-800/80 rounded-3xl backdrop-blur-xs">
      <div className="w-16 h-16 rounded-2xl bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 flex items-center justify-center mb-4 shadow-inner ring-8 ring-indigo-50/50 dark:ring-indigo-950/20">
        <BookOpen className="w-8 h-8" />
      </div>

      <h3 className="text-lg font-bold text-slate-800 dark:text-slate-100">
        {filtered
          ? 'Không tìm thấy phiên nộp phù hợp'
          : 'Bạn chưa có phiên đề xuất câu hỏi nào'}
      </h3>

      <p className="text-sm text-slate-500 dark:text-slate-400 max-w-md mt-1.5 mb-6">
        {filtered
          ? 'Hãy thử thay đổi bộ lọc trạng thái hoặc môn học để tìm kiếm phiên đề xuất của bạn.'
          : 'Hãy đóng góp các câu hỏi trắc nghiệm chất lượng vào ngân hàng câu hỏi môn học của trường để nhận điểm thưởng và huy hiệu!'}
      </p>

      {onProposeNew && (
        <Button
          onClick={onProposeNew}
          className="rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-medium shadow-md shadow-indigo-200 dark:shadow-none transition-all flex items-center gap-2 px-5 py-2.5"
        >
          <Plus className="w-4 h-4" />
          <span>Tạo phiên đề xuất mới</span>
          <Sparkles className="w-3.5 h-3.5 text-amber-300" />
        </Button>
      )}
    </div>
  );
};
