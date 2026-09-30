import React from 'react';
import { CheckCircle2 } from 'lucide-react';

export const PendingReviewsEmpty: React.FC = () => {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 p-8 md:p-12 text-center shadow-xs space-y-3">
      <div className="w-16 h-16 rounded-3xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto shadow-inner">
        <CheckCircle2 className="w-8 h-8" />
      </div>
      <h3 className="font-bold text-lg text-slate-800 dark:text-slate-100">
        Không có phiên nào đang chờ duyệt
      </h3>
      <p className="text-xs md:text-sm text-slate-500 dark:text-slate-400 max-w-md mx-auto leading-relaxed">
        Tuyệt vời! Bạn đã hoàn tất đánh giá toàn bộ các câu hỏi đề xuất từ sinh viên. Khi có phiên nộp mới từ các môn học bạn giảng dạy, danh sách sẽ hiển thị tại đây.
      </p>
    </div>
  );
};
