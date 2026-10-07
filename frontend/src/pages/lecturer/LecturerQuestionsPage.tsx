import { useMemo } from 'react';
import { CheckCircle, FileText, ArrowRight, Layers, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Badge } from '@/components/ui/badge';
import { LecturerQuestionsExportCard, useLecturerClasses } from '@/features/exam';

export function LecturerQuestionsPage() {
  const { data: rawClasses = [], isLoading } = useLecturerClasses();

  // Unique subjects and classes count
  const subjectsSummary = useMemo(() => {
    const map = new Map<number, { id: number; code: string; name: string; classCount: number }>();
    rawClasses.forEach((cls: any) => {
      const sId = cls.subjectId;
      if (sId) {
        if (!map.has(sId)) {
          map.set(sId, {
            id: sId,
            code: cls.subjectCode || `SUB_${sId}`,
            name: cls.subjectName || `Môn học #${sId}`,
            classCount: 1,
          });
        } else {
          const item = map.get(sId)!;
          item.classCount += 1;
        }
      }
    });
    return Array.from(map.values());
  }, [rawClasses]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
              Ngân Hàng Câu Hỏi Môn Học
            </h1>
            <Badge variant="role">Giảng viên</Badge>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Tra cứu kho câu hỏi chuẩn hóa, xuất ngân hàng câu hỏi ôn tập và quản lý các môn học phụ trách.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link
            to="/lecturer/pending-reviews"
            className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 transition-colors shadow-sm"
          >
            <CheckCircle className="w-3.5 h-3.5 text-emerald-500" />
            Duyệt Câu Hỏi Mới
          </Link>
          <Link
            to="/lecturer/exams"
            className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold rounded-xl bg-blue-600 hover:bg-blue-700 text-white transition-colors shadow-sm"
          >
            <FileText className="w-3.5 h-3.5" />
            Tạo & Xuất Đề Thi
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>

      {/* Export Tool Component */}
      <LecturerQuestionsExportCard />

      {/* Managed Subjects Cards */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Layers className="w-5 h-5 text-indigo-500" />
            <h2 className="text-base font-bold text-slate-900 dark:text-slate-100">
              Danh Sách Môn Học Phụ Trách ({subjectsSummary.length})
            </h2>
          </div>
          <span className="text-xs text-slate-500 dark:text-slate-400">
            Dữ liệu đồng bộ từ phân công giảng dạy
          </span>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                className="h-28 rounded-xl bg-slate-100 dark:bg-slate-800 animate-pulse"
              />
            ))}
          </div>
        ) : subjectsSummary.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500 dark:text-slate-400">
            Bạn chưa được phân công môn học nào trong học kỳ này.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {subjectsSummary.map((sub) => (
              <div
                key={sub.id}
                className="p-4 rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50/60 dark:bg-slate-800/40 hover:border-blue-200 dark:hover:border-blue-800 transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between gap-2 mb-2">
                    <span className="text-xs font-bold px-2 py-0.5 rounded bg-blue-100 dark:bg-blue-900/50 text-blue-700 dark:text-blue-300">
                      {sub.code}
                    </span>
                    <span className="text-xs text-slate-500 dark:text-slate-400">
                      {sub.classCount} lớp học phần
                    </span>
                  </div>
                  <h3 className="font-bold text-slate-900 dark:text-slate-100 text-sm line-clamp-2">
                    {sub.name}
                  </h3>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-200/60 dark:border-slate-700/60 flex items-center justify-between text-xs">
                  <span className="inline-flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-medium">
                    <Sparkles className="w-3.5 h-3.5" />
                    Đã thẩm định
                  </span>
                  <Link
                    to={`/lecturer/exams`}
                    className="text-blue-600 hover:text-blue-700 dark:text-blue-400 font-semibold inline-flex items-center gap-1"
                  >
                    Tạo đề <ArrowRight className="w-3 h-3" />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
