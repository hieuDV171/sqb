import { useState, useMemo } from 'react';
import { X, Sparkles, Sliders, Send } from 'lucide-react';
import { useGenerateExam, useLecturerClasses } from '../hooks/useExam';

interface GenerateExamModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: (examId: number) => void;
}

export function GenerateExamModal({
  isOpen,
  onClose,
  onSuccess,
}: GenerateExamModalProps) {
  const { data: rawClasses = [], isLoading: isClassesLoading } = useLecturerClasses();
  const generateExamMutation = useGenerateExam();

  const [selectedSubjectId, setSelectedSubjectId] = useState<number | null>(null);
  const [title, setTitle] = useState('');
  const [questionCount, setQuestionCount] = useState<number>(40);
  const [selectedClassIds, setSelectedClassIds] = useState<number[]>([]);
  const [shuffleOptions, setShuffleOptions] = useState(false);
  const [includeAnswerKey, setIncludeAnswerKey] = useState(false);

  // Difficulty Distribution (%)
  const [easyPercent, setEasyPercent] = useState<number>(30);
  const [mediumPercent, setMediumPercent] = useState<number>(50);
  const [hardPercent, setHardPercent] = useState<number>(20);

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

  // Filter classes belonging to the selected subject
  const availableClasses = useMemo(() => {
    if (!selectedSubjectId) return [];
    return rawClasses.filter((cls: any) => cls.subjectId === selectedSubjectId);
  }, [rawClasses, selectedSubjectId]);

  const handleSelectSubject = (sId: number) => {
    setSelectedSubjectId(sId);
    setSelectedClassIds([]); // Reset selected classes when subject changes
  };

  const handleToggleClass = (classId: number) => {
    if (selectedClassIds.includes(classId)) {
      setSelectedClassIds(selectedClassIds.filter((id) => id !== classId));
    } else {
      setSelectedClassIds([...selectedClassIds, classId]);
    }
  };

  const handleSelectAllClasses = () => {
    if (selectedClassIds.length === availableClasses.length) {
      setSelectedClassIds([]);
    } else {
      setSelectedClassIds(availableClasses.map((c: any) => c.classId || c.courseClassId || c.id));
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSubjectId || selectedClassIds.length === 0) return;

    generateExamMutation.mutate(
      {
        subjectId: selectedSubjectId,
        title: title.trim() || undefined,
        questionCount,
        difficultyDistribution: {
          easy: easyPercent / 100,
          medium: mediumPercent / 100,
          hard: hardPercent / 100,
        },
        shuffleOptions,
        includeAnswerKey,
        classIds: selectedClassIds,
      },
      {
        onSuccess: (res) => {
          onClose();
          if (res.data?.examId && onSuccess) {
            onSuccess(res.data.examId);
          }
        },
      }
    );
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl p-6 sm:p-8 space-y-6">
        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header */}
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center shadow-xs">
            <Sparkles className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-black text-slate-900 dark:text-white">
              Tạo Đề Thi Tự Động Từ Ngân Hàng Đề
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Bốc ngẫu nhiên câu hỏi đã duyệt (APPROVED) theo môn học, tỉ lệ ma trận độ khó và gán cho các lớp học phần.
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Step 1: Select Subject */}
          <div className="space-y-1.5">
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
              1. Chọn môn học *
            </label>
            {isClassesLoading ? (
              <div className="h-10 rounded-xl bg-slate-100 dark:bg-slate-800 animate-pulse" />
            ) : uniqueSubjects.length === 0 ? (
              <p className="text-xs text-amber-600 dark:text-amber-400">
                Bạn chưa được phân công lớp học phần nào trong học kỳ hiện tại.
              </p>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {uniqueSubjects.map((s) => {
                  const isSelected = selectedSubjectId === s.id;
                  return (
                    <button
                      key={s.id}
                      type="button"
                      onClick={() => handleSelectSubject(s.id)}
                      className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                        isSelected
                          ? 'bg-indigo-50 dark:bg-indigo-950/60 border-indigo-500 ring-2 ring-indigo-500/20 shadow-xs'
                          : 'bg-white dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 hover:border-indigo-300'
                      }`}
                    >
                      <p className="font-mono font-bold text-xs text-indigo-600 dark:text-indigo-400">
                        {s.code}
                      </p>
                      <p className="font-bold text-xs text-slate-800 dark:text-slate-200 truncate">
                        {s.name}
                      </p>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Step 2: Select Course Classes */}
          {selectedSubjectId && (
            <div className="space-y-2 pt-2 border-t border-slate-100 dark:border-slate-800">
              <div className="flex items-center justify-between">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  2. Lớp học phần áp dụng đề thi * ({selectedClassIds.length}/{availableClasses.length})
                </label>
                <button
                  type="button"
                  onClick={handleSelectAllClasses}
                  className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:underline cursor-pointer"
                >
                  {selectedClassIds.length === availableClasses.length ? 'Bỏ chọn tất cả' : 'Chọn tất cả'}
                </button>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {availableClasses.map((cls: any) => {
                  const cId = cls.classId || cls.courseClassId || cls.id;
                  const isChecked = selectedClassIds.includes(cId);
                  return (
                    <label
                      key={cId}
                      className={`flex items-center gap-2.5 p-2.5 rounded-xl border cursor-pointer transition-all ${
                        isChecked
                          ? 'bg-indigo-50 dark:bg-indigo-950/60 border-indigo-400'
                          : 'bg-slate-50 dark:bg-slate-800/50 border-slate-200 dark:border-slate-700'
                      }`}
                    >
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => handleToggleClass(cId)}
                        className="w-4 h-4 rounded-sm text-indigo-600 focus:ring-indigo-500"
                      />
                      <span className="text-xs font-bold text-slate-800 dark:text-slate-200 font-mono">
                        {cls.classCode || `Lớp ${cId}`}
                      </span>
                    </label>
                  );
                })}
              </div>
            </div>
          )}

          {/* Step 3: Exam Info & Question Count */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-slate-100 dark:border-slate-800">
            <div className="sm:col-span-2">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                Tiêu đề đề thi (Tùy chọn)
              </label>
              <input
                type="text"
                placeholder="Ví dụ: Đề thi Cuối kỳ Hệ điều hành 2024.1"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 text-xs text-slate-800 dark:text-slate-200"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                Số lượng câu hỏi
              </label>
              <select
                value={questionCount}
                onChange={(e) => setQuestionCount(Number(e.target.value))}
                className="w-full px-3.5 py-2 rounded-xl bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 text-xs font-bold text-slate-800 dark:text-slate-200"
              >
                <option value={20}>20 câu</option>
                <option value={30}>30 câu</option>
                <option value={40}>40 câu</option>
                <option value={50}>50 câu</option>
                <option value={60}>60 câu</option>
              </select>
            </div>
          </div>

          {/* Step 4: Difficulty Distribution Matrix */}
          <div className="space-y-2 pt-2 border-t border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-2">
              <Sliders className="w-4 h-4 text-indigo-500" />
              <label className="text-xs font-bold text-slate-700 dark:text-slate-300">
                Phân bổ ma trận độ khó (%) - Tổng: {easyPercent + mediumPercent + hardPercent}%
              </label>
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div className="p-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800/50">
                <span className="text-[11px] font-bold text-emerald-700 dark:text-emerald-300">
                  Dễ ({easyPercent}%)
                </span>
                <input
                  type="number"
                  min="0"
                  max="100"
                  value={easyPercent}
                  onChange={(e) => setEasyPercent(Number(e.target.value))}
                  className="w-full mt-1 px-2.5 py-1 rounded-lg bg-white dark:bg-slate-900 border border-emerald-300 dark:border-emerald-700 text-xs font-bold text-emerald-800 dark:text-emerald-200"
                />
              </div>

              <div className="p-3 rounded-xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/50">
                <span className="text-[11px] font-bold text-amber-700 dark:text-amber-300">
                  Trung bình ({mediumPercent}%)
                </span>
                <input
                  type="number"
                  min="0"
                  max="100"
                  value={mediumPercent}
                  onChange={(e) => setMediumPercent(Number(e.target.value))}
                  className="w-full mt-1 px-2.5 py-1 rounded-lg bg-white dark:bg-slate-900 border border-amber-300 dark:border-amber-700 text-xs font-bold text-amber-800 dark:text-amber-200"
                />
              </div>

              <div className="p-3 rounded-xl bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-800/50">
                <span className="text-[11px] font-bold text-rose-700 dark:text-rose-300">
                  Khó ({hardPercent}%)
                </span>
                <input
                  type="number"
                  min="0"
                  max="100"
                  value={hardPercent}
                  onChange={(e) => setHardPercent(Number(e.target.value))}
                  className="w-full mt-1 px-2.5 py-1 rounded-lg bg-white dark:bg-slate-900 border border-rose-300 dark:border-rose-700 text-xs font-bold text-rose-800 dark:text-rose-200"
                />
              </div>
            </div>
          </div>

          {/* Step 5: Options (Shuffle & Answer Key) */}
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 pt-2 border-t border-slate-100 dark:border-slate-800">
            <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300 cursor-pointer">
              <input
                type="checkbox"
                checked={shuffleOptions}
                onChange={(e) => setShuffleOptions(e.target.checked)}
                className="w-4 h-4 rounded-sm text-indigo-600 focus:ring-indigo-500"
              />
              <span>Đảo ngẫu nhiên các đáp án A, B, C, D</span>
            </label>

            <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300 cursor-pointer">
              <input
                type="checkbox"
                checked={includeAnswerKey}
                onChange={(e) => setIncludeAnswerKey(e.target.checked)}
                className="w-4 h-4 rounded-sm text-indigo-600 focus:ring-indigo-500"
              />
              <span>Kèm bảng đáp án trong dữ liệu đề</span>
            </label>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 rounded-xl text-xs font-bold text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={
                !selectedSubjectId ||
                selectedClassIds.length === 0 ||
                generateExamMutation.isPending
              }
              className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs flex items-center gap-2 shadow-sm hover:scale-105 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
            >
              <Send className="w-3.5 h-3.5" />
              <span>
                {generateExamMutation.isPending ? 'Đang bốc đề thi...' : 'Sinh Đề Thi Ngay'}
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
