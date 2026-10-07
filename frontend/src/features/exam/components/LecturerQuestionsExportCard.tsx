import { useState, useMemo } from 'react';
import { Download, FileText, CheckCircle2, AlertCircle, Loader2, Sparkles, BookOpen } from 'lucide-react';
import { useExportQuestions, useLecturerClasses } from '../hooks/useExam';

export function LecturerQuestionsExportCard() {
  const { data: rawClasses = [], isLoading: isClassesLoading } = useLecturerClasses();
  const exportMutation = useExportQuestions();

  const [selectedSubjectId, setSelectedSubjectId] = useState<number | null>(null);
  const [format, setFormat] = useState<'pdf' | 'excel'>('pdf');
  const [includeAnswer, setIncludeAnswer] = useState<boolean>(true);
  const [lastExport, setLastExport] = useState<{
    downloadUrl: string;
    fileName: string;
    fileSizeMb?: number;
  } | null>(null);

  // Extract unique subjects from lecturer's classes
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

  // Auto select first subject when loaded
  useState(() => {
    if (uniqueSubjects.length > 0 && selectedSubjectId === null) {
      setSelectedSubjectId(uniqueSubjects[0].id);
    }
  });

  const handleExport = async () => {
    if (!selectedSubjectId) return;

    try {
      const res = await exportMutation.mutateAsync({
        subjectId: selectedSubjectId,
        format,
        includeAnswer,
      });

      if (res.data?.downloadUrl) {
        const generatedFileName =
          res.data.fileName ||
          `NganHangCauHoi_Mon_${selectedSubjectId}.${format === 'excel' ? 'xlsx' : 'pdf'}`;
        setLastExport({
          downloadUrl: res.data.downloadUrl,
          fileName: generatedFileName,
          fileSizeMb: res.data.fileSizeMb,
        });

        // Trigger download
        const a = document.createElement('a');
        a.href = res.data.downloadUrl;
        a.download = generatedFileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      }
    } catch {
      // Error handled by react-query mutation
    }
  };

  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm">
      <div className="flex items-start gap-4">
        <div className="p-3 rounded-xl bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400">
          <BookOpen className="w-6 h-6" />
        </div>
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
              Xuất Ngân Hàng Câu Hỏi Môn Học
            </h3>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-400">
              <Sparkles className="w-3 h-3" />
              Chính thức
            </span>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Kết xuất toàn bộ câu hỏi đã duyệt của môn học ra định dạng PDF hoặc Excel để làm tài liệu ôn tập, lưu trữ bộ môn hoặc phân phối cho sinh viên.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
            {/* Subject Select */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                Môn học phụ trách *
              </label>
              <select
                value={selectedSubjectId ?? ''}
                onChange={(e) => setSelectedSubjectId(Number(e.target.value))}
                disabled={isClassesLoading || uniqueSubjects.length === 0}
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                {uniqueSubjects.length === 0 ? (
                  <option value="">
                    {isClassesLoading ? 'Đang tải môn học...' : 'Chưa có môn học phụ trách'}
                  </option>
                ) : (
                  uniqueSubjects.map((sub) => (
                    <option key={sub.id} value={sub.id}>
                      [{sub.code}] {sub.name}
                    </option>
                  ))
                )}
              </select>
            </div>

            {/* Format */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                Định dạng xuất file *
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setFormat('pdf')}
                  className={`py-2 px-3 text-xs font-medium rounded-lg border flex items-center justify-center gap-1.5 transition-colors ${
                    format === 'pdf'
                      ? 'border-blue-600 bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-500'
                      : 'border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                  }`}
                >
                  <FileText className="w-3.5 h-3.5 text-rose-500" />
                  PDF (Bản in)
                </button>
                <button
                  type="button"
                  onClick={() => setFormat('excel')}
                  className={`py-2 px-3 text-xs font-medium rounded-lg border flex items-center justify-center gap-1.5 transition-colors ${
                    format === 'excel'
                      ? 'border-emerald-600 bg-emerald-50 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300 dark:border-emerald-500'
                      : 'border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                  }`}
                >
                  <FileText className="w-3.5 h-3.5 text-emerald-600" />
                  Excel (Bảng tính)
                </button>
              </div>
            </div>

            {/* Include Answers Toggle */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                Kèm theo đáp án & giải thích
              </label>
              <label className="flex items-center gap-2 py-2 px-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={includeAnswer}
                  onChange={(e) => setIncludeAnswer(e.target.checked)}
                  className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4"
                />
                <span className="text-xs text-slate-700 dark:text-slate-300 font-medium">
                  {includeAnswer ? 'Bao gồm đáp án đúng & lời giải' : 'Chỉ danh sách đề bài'}
                </span>
              </label>
            </div>
          </div>

          {/* Action Row */}
          <div className="flex items-center justify-between mt-6 pt-4 border-t border-slate-100 dark:border-slate-800">
            <div>
              {lastExport && (
                <div className="flex items-center gap-2 text-xs text-emerald-600 dark:text-emerald-400">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>
                    Đã xuất thành công: <strong>{lastExport.fileName}</strong>
                  </span>
                  <a
                    href={lastExport.downloadUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="underline font-semibold hover:text-emerald-700 ml-1"
                  >
                    Tải lại
                  </a>
                </div>
              )}
              {exportMutation.isError && (
                <div className="flex items-center gap-2 text-xs text-rose-600 dark:text-rose-400">
                  <AlertCircle className="w-4 h-4" />
                  <span>
                    {(exportMutation.error as any)?.response?.data?.message ||
                      'Không thể xuất file câu hỏi. Vui lòng kiểm tra lại số lượng câu hỏi của môn.'}
                  </span>
                </div>
              )}
            </div>

            <button
              type="button"
              onClick={handleExport}
              disabled={!selectedSubjectId || exportMutation.isPending}
              className="px-5 py-2.5 rounded-xl font-semibold text-sm bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white shadow-md shadow-blue-500/20 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 transition-all"
            >
              {exportMutation.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Đang biên dịch tài liệu...
                </>
              ) : (
                <>
                  <Download className="w-4 h-4" />
                  Xuất File Ngay
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
