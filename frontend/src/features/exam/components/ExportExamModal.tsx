import { useState } from 'react';
import { X, FileDown, FileText, Table, Download } from 'lucide-react';
import { useExportExam } from '../hooks/useExam';

interface ExportExamModalProps {
  examId: number | null;
  examTitle?: string;
  isOpen: boolean;
  onClose: () => void;
}

export function ExportExamModal({
  examId,
  examTitle,
  isOpen,
  onClose,
}: ExportExamModalProps) {
  const [format, setFormat] = useState<'pdf' | 'excel'>('pdf');
  const [paperSize, setPaperSize] = useState<'A4' | 'Letter'>('A4');
  const [includeAnswerKey, setIncludeAnswerKey] = useState(true);

  const exportMutation = useExportExam();

  const handleExport = () => {
    if (!examId) return;
    exportMutation.mutate(
      {
        examId,
        params: {
          format,
          paperSize,
          includeAnswerKey,
        },
      },
      {
        onSuccess: () => {
          onClose();
        },
      }
    );
  };

  if (!isOpen || !examId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-md rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl p-6 sm:p-7 space-y-5">
        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center shadow-xs">
            <FileDown className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-lg font-black text-slate-900 dark:text-white">
              Xuất Bản In Đề Thi
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 truncate max-w-[260px]">
              {examTitle || `Đề thi #${examId}`}
            </p>
          </div>
        </div>

        {/* Format Selector */}
        <div className="space-y-2">
          <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
            Định dạng tệp xuất
          </label>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setFormat('pdf')}
              className={`p-3.5 rounded-2xl border flex items-center gap-3 transition-all cursor-pointer ${
                format === 'pdf'
                  ? 'bg-rose-50 dark:bg-rose-950/40 border-rose-400 ring-2 ring-rose-400/20 text-rose-700 dark:text-rose-300'
                  : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:border-slate-300'
              }`}
            >
              <div className="p-2 rounded-xl bg-rose-500 text-white shrink-0 shadow-xs">
                <FileText className="w-4 h-4" />
              </div>
              <div className="text-left">
                <p className="font-bold text-xs">Tài liệu PDF</p>
                <p className="text-[10px] text-slate-400">Chuẩn in ấn A4</p>
              </div>
            </button>

            <button
              type="button"
              onClick={() => setFormat('excel')}
              className={`p-3.5 rounded-2xl border flex items-center gap-3 transition-all cursor-pointer ${
                format === 'excel'
                  ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-400 ring-2 ring-emerald-400/20 text-emerald-700 dark:text-emerald-300'
                  : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:border-slate-300'
              }`}
            >
              <div className="p-2 rounded-xl bg-emerald-600 text-white shrink-0 shadow-xs">
                <Table className="w-4 h-4" />
              </div>
              <div className="text-left">
                <p className="font-bold text-xs">Bảng tính Excel</p>
                <p className="text-[10px] text-slate-400">Dữ liệu XLSX</p>
              </div>
            </button>
          </div>
        </div>

        {/* Paper Size (PDF only) */}
        {format === 'pdf' && (
          <div className="space-y-1.5">
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
              Khổ giấy in
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setPaperSize('A4')}
                className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all cursor-pointer ${
                  paperSize === 'A4'
                    ? 'bg-indigo-50 dark:bg-indigo-950/60 border-indigo-400 text-indigo-600 dark:text-indigo-400'
                    : 'bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400'
                }`}
              >
                Khổ tiêu chuẩn A4
              </button>
              <button
                type="button"
                onClick={() => setPaperSize('Letter')}
                className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all cursor-pointer ${
                  paperSize === 'Letter'
                    ? 'bg-indigo-50 dark:bg-indigo-950/60 border-indigo-400 text-indigo-600 dark:text-indigo-400'
                    : 'bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400'
                }`}
              >
                Khổ Letter
              </button>
            </div>
          </div>
        )}

        {/* Answer Key Option */}
        <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700">
          <label className="flex items-center gap-2.5 text-xs font-semibold text-slate-700 dark:text-slate-300 cursor-pointer">
            <input
              type="checkbox"
              checked={includeAnswerKey}
              onChange={(e) => setIncludeAnswerKey(e.target.checked)}
              className="w-4 h-4 rounded-sm text-indigo-600 focus:ring-indigo-500"
            />
            <span>Kèm bảng đáp án đúng và lời giải chi tiết ở cuối tài liệu</span>
          </label>
        </div>

        {/* Actions */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            Đóng
          </button>
          <button
            type="button"
            disabled={exportMutation.isPending}
            onClick={handleExport}
            className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs flex items-center gap-2 shadow-sm hover:scale-105 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
          >
            <Download className="w-3.5 h-3.5" />
            <span>
              {exportMutation.isPending ? 'Đang xuất tệp...' : 'Tải File Về Máy'}
            </span>
          </button>
        </div>
      </div>
    </div>
  );
}
