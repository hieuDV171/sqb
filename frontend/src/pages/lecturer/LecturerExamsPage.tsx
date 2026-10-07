import { useState, useMemo } from 'react';
import {
  FileText,
  Plus,
  Eye,
  Download,
  Calendar,
  Filter,
  RefreshCw,
  SlidersHorizontal,
  GraduationCap,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { EmptyState } from '@/components/ui/empty-state';
import {
  useMyExams,
  useLecturerClasses,
  GenerateExamModal,
  ExportExamModal,
  ExamDetailModal,
  type ExamItem,
} from '@/features/exam';

export function LecturerExamsPage() {
  const { data: rawClasses = [] } = useLecturerClasses();
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | undefined>(undefined);

  // Pagination cursor state
  const [cursor, setCursor] = useState<number | undefined>(undefined);

  // Fetch exams query
  const {
    data: examListData,
    isLoading: isExamsLoading,
    isFetching,
    error,
    refetch,
  } = useMyExams({
    subjectId: selectedSubjectId,
    after: cursor,
    limit: 10,
  });

  // Unique subjects for filter
  const uniqueSubjects = useMemo(() => {
    const map = new Map<number, { id: number; code: string; name: string }>();
    rawClasses.forEach((cls: any) => {
      const sId = cls.subjectId;
      if (sId && !map.has(sId)) {
        map.set(sId, {
          id: sId,
          code: cls.subjectCode || `SUB_${sId}`,
          name: cls.subjectName || `Môn học #${sId}`,
        });
      }
    });
    return Array.from(map.values());
  }, [rawClasses]);

  // Modals state
  const [isGenerateModalOpen, setIsGenerateModalOpen] = useState(false);
  const [previewExamId, setPreviewExamId] = useState<number | null>(null);
  const [exportExam, setExportExam] = useState<{ id: number; title: string } | null>(null);

  // Exams list from current query
  const exams: ExamItem[] = useMemo(() => {
    return examListData?.items || [];
  }, [examListData]);

  const handleSubjectFilterChange = (subjectId?: number) => {
    setSelectedSubjectId(subjectId);
    setCursor(undefined);
  };

  const handleExamCreated = (newExamId: number) => {
    setIsGenerateModalOpen(false);
    refetch();
    // Auto preview newly generated exam
    setPreviewExamId(newExamId);
  };

  return (
    <div className="space-y-6">
      {/* Top Banner & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
              Quản Lý & Xuất Đề Thi
            </h1>
            <Badge variant="role">Giảng viên</Badge>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Tạo đề thi trắc nghiệm ngẫu nhiên từ ngân hàng câu hỏi đã duyệt, soát đề và xuất bản in PDF/Excel cho các lớp học phần.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setIsGenerateModalOpen(true)}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl font-semibold text-sm bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white shadow-md shadow-blue-500/20 transition-all cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          Tạo Đề Thi Mới
        </button>
      </div>

      {/* Filter and Stats Toolbar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            Lọc theo môn:
          </span>
          <select
            value={selectedSubjectId ?? ''}
            onChange={(e) =>
              handleSubjectFilterChange(e.target.value ? Number(e.target.value) : undefined)
            }
            className="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Tất cả môn học ({uniqueSubjects.length})</option>
            {uniqueSubjects.map((sub) => (
              <option key={sub.id} value={sub.id}>
                [{sub.code}] {sub.name}
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-center gap-3">
          <span className="text-xs text-slate-500 dark:text-slate-400">
            Hiển thị: <strong>{exams.length}</strong> đề thi
          </span>
          <button
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
            className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
            title="Làm mới danh sách"
          >
            <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Main Content Area */}
      {isExamsLoading ? (
        // Loading Skeleton
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div
              key={i}
              className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 animate-pulse space-y-4"
            >
              <div className="h-5 bg-slate-200 dark:bg-slate-800 rounded w-3/4" />
              <div className="h-4 bg-slate-100 dark:bg-slate-800/60 rounded w-1/2" />
              <div className="h-10 bg-slate-100 dark:bg-slate-800/40 rounded" />
              <div className="flex justify-between pt-2">
                <div className="h-8 bg-slate-200 dark:bg-slate-800 rounded w-24" />
                <div className="h-8 bg-slate-200 dark:bg-slate-800 rounded w-24" />
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        // Error State
        <div className="p-8 text-center bg-white dark:bg-slate-900 rounded-2xl border border-rose-200 dark:border-rose-900/30">
          <p className="text-rose-600 dark:text-rose-400 font-semibold mb-2">
            Không thể tải danh sách đề thi.
          </p>
          <button
            type="button"
            onClick={() => refetch()}
            className="px-4 py-2 text-xs font-semibold rounded-lg bg-rose-50 text-rose-700 hover:bg-rose-100 dark:bg-rose-950/40 dark:text-rose-300 cursor-pointer"
          >
            Thử lại
          </button>
        </div>
      ) : exams.length === 0 ? (
        // Empty State
        <EmptyState
          icon={FileText}
          title={selectedSubjectId ? 'Không có đề thi nào cho môn học này' : 'Chưa có đề thi nào'}
          description="Bấm 'Tạo Đề Thi Mới' để hệ thống tự động bốc ngẫu nhiên câu hỏi theo tỉ lệ độ khó mong muốn và kết xuất đề thi."
          actionLabel="Tạo đề thi đầu tiên"
          onAction={() => setIsGenerateModalOpen(true)}
        />
      ) : (
        // Grid of Exams
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {exams.map((exam) => (
            <div
              key={exam.examId}
              className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 hover:border-blue-300 dark:hover:border-blue-700/60 p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between"
            >
              <div>
                {/* Header */}
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-blue-50 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
                        #{exam.examId}
                      </span>
                      {exam.subjectCode && (
                        <span className="text-xs font-semibold px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                          {exam.subjectCode}
                        </span>
                      )}
                    </div>
                    <h3 className="font-bold text-slate-900 dark:text-slate-100 text-base line-clamp-1">
                      {exam.title || `Đề thi môn ${exam.subjectName || exam.subjectCode || ''}`}
                    </h3>
                  </div>

                  <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-indigo-50 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300 whitespace-nowrap">
                    {exam.questionCount} câu hỏi
                  </span>
                </div>

                {/* Subject Name & Date */}
                <div className="flex items-center gap-4 text-xs text-slate-500 dark:text-slate-400 mt-2">
                  <span className="flex items-center gap-1">
                    <GraduationCap className="w-3.5 h-3.5" />
                    {exam.subjectName || 'Chưa rõ môn'}
                  </span>
                  {exam.createdAt && (
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3.5 h-3.5" />
                      {new Date(exam.createdAt).toLocaleDateString('vi-VN')}
                    </span>
                  )}
                </div>

                {/* Difficulty Distribution Bar */}
                {exam.statistic && (
                  <div className="mt-4 p-3 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800">
                    <div className="flex items-center justify-between text-[11px] font-medium text-slate-600 dark:text-slate-300 mb-1.5">
                      <span className="flex items-center gap-1">
                        <SlidersHorizontal className="w-3 h-3 text-slate-400" />
                        Phân bố độ khó
                      </span>
                      <div className="flex items-center gap-2">
                        <span className="text-emerald-600 dark:text-emerald-400">
                          Dễ: {exam.statistic.easyCount || 0}
                        </span>
                        <span className="text-amber-600 dark:text-amber-400">
                          TB: {exam.statistic.mediumCount || 0}
                        </span>
                        <span className="text-rose-600 dark:text-rose-400">
                          Khó: {exam.statistic.hardCount || 0}
                        </span>
                      </div>
                    </div>
                    {/* Visual Progress Bar */}
                    <div className="h-1.5 w-full bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden flex">
                      <div
                        className="bg-emerald-500"
                        style={{
                          width: `${
                            ((exam.statistic.easyCount || 0) /
                              (exam.questionCount || 1)) *
                            100
                          }%`,
                        }}
                      />
                      <div
                        className="bg-amber-500"
                        style={{
                          width: `${
                            ((exam.statistic.mediumCount || 0) /
                              (exam.questionCount || 1)) *
                            100
                          }%`,
                        }}
                      />
                      <div
                        className="bg-rose-500"
                        style={{
                          width: `${
                            ((exam.statistic.hardCount || 0) /
                              (exam.questionCount || 1)) *
                            100
                          }%`,
                        }}
                      />
                    </div>
                  </div>
                )}
              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-between gap-2 mt-5 pt-3 border-t border-slate-100 dark:border-slate-800">
                <button
                  type="button"
                  onClick={() => setPreviewExamId(exam.examId)}
                  className="flex-1 py-2 px-3 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-200 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700/80 flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                >
                  <Eye className="w-3.5 h-3.5 text-blue-500" />
                  Soát Đề & Đáp Án
                </button>

                <button
                  type="button"
                  onClick={() =>
                    setExportExam({
                      id: exam.examId,
                      title: exam.title || `Đề thi #${exam.examId}`,
                    })
                  }
                  className="py-2 px-4 rounded-xl text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 dark:bg-blue-600 dark:hover:bg-blue-500 shadow-sm flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" />
                  Xuất File
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination Footer */}
      {examListData?.pagination?.hasNext && (
        <div className="flex justify-center pt-4">
          <button
            type="button"
            onClick={() => {
              const lastItem = exams[exams.length - 1];
              if (lastItem) setCursor(lastItem.examId);
            }}
            disabled={isFetching}
            className="px-6 py-2.5 rounded-xl font-semibold text-xs border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 transition-colors flex items-center gap-2 cursor-pointer"
          >
            {isFetching ? (
              <>
                <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                Đang tải thêm...
              </>
            ) : (
              'Xem thêm các đề thi cũ hơn'
            )}
          </button>
        </div>
      )}

      {/* Generate Exam Modal */}
      <GenerateExamModal
        isOpen={isGenerateModalOpen}
        onClose={() => setIsGenerateModalOpen(false)}
        onSuccess={handleExamCreated}
      />

      {/* Preview Exam Modal */}
      <ExamDetailModal
        examId={previewExamId}
        isOpen={previewExamId !== null}
        onClose={() => setPreviewExamId(null)}
      />

      {/* Export Exam Modal */}
      {exportExam && (
        <ExportExamModal
          examId={exportExam.id}
          examTitle={exportExam.title}
          isOpen={true}
          onClose={() => setExportExam(null)}
        />
      )}
    </div>
  );
}
