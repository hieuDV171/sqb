import React, { useState, useEffect } from 'react';
import type { SessionQuestionReviewDto, EditQuestionRequest } from '../types/review.types';
import type { QuestionOption } from '@/features/session/types/session.types';
import { X, Plus, Trash2, CheckCircle2, Loader2, Save } from 'lucide-react';
import { cn } from '@/lib/utils';
import { toast } from '@/stores/useToastStore';

interface EditQuestionModalProps {
  isOpen: boolean;
  question: SessionQuestionReviewDto | null;
  questionNumber: number;
  isSaving: boolean;
  onClose: () => void;
  onSave: (data: EditQuestionRequest) => Promise<void>;
}

export const EditQuestionModal: React.FC<EditQuestionModalProps> = ({
  isOpen,
  question,
  questionNumber,
  isSaving,
  onClose,
  onSave,
}) => {
  const [content, setContent] = useState('');
  const [explanation, setExplanation] = useState('');
  const [options, setOptions] = useState<QuestionOption[]>([]);
  const [autoApprove, setAutoApprove] = useState(true);

  useEffect(() => {
    if (question) {
      setContent(question.content || '');
      setExplanation(question.explanation || '');
      setOptions(
        question.options && question.options.length > 0
          ? JSON.parse(JSON.stringify(question.options))
          : [
              { key: 'A', text: '', isCorrect: true },
              { key: 'B', text: '', isCorrect: false },
              { key: 'C', text: '', isCorrect: false },
              { key: 'D', text: '', isCorrect: false },
            ]
      );
      setAutoApprove(true);
    }
  }, [question]);

  if (!isOpen || !question) return null;

  const handleOptionChange = (idx: number, text: string) => {
    setOptions((prev) =>
      prev.map((opt, i) => (i === idx ? { ...opt, text } : opt))
    );
  };

  const handleToggleCorrect = (idx: number) => {
    setOptions((prev) =>
      prev.map((opt, i) => (i === idx ? { ...opt, isCorrect: !opt.isCorrect } : opt))
    );
  };

  const handleAddOption = () => {
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const nextKey = letters[options.length] || `Option ${options.length + 1}`;
    setOptions((prev) => [...prev, { key: nextKey, text: '', isCorrect: false }]);
  };

  const handleRemoveOption = (idx: number) => {
    if (options.length <= 2) {
      toast.error('Câu hỏi phải có tối thiểu 2 phương án lựa chọn');
      return;
    }
    const filtered = options.filter((_, i) => i !== idx);
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const reindexed = filtered.map((opt, i) => ({
      ...opt,
      key: letters[i] || `${i + 1}`,
    }));
    setOptions(reindexed);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!content.trim()) {
      toast.error('Nội dung câu hỏi không được để trống');
      return;
    }

    const hasEmptyOption = options.some((opt) => !opt.text.trim());
    if (hasEmptyOption) {
      toast.error('Vui lòng nhập nội dung cho tất cả các phương án lựa chọn');
      return;
    }

    const hasCorrectOption = options.some((opt) => opt.isCorrect);
    if (!hasCorrectOption) {
      toast.error('Vui lòng chọn ít nhất một đáp án đúng');
      return;
    }

    await onSave({
      content: content.trim(),
      explanation: explanation.trim(),
      options,
      autoApprove,
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl max-w-2xl w-full max-h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h3 className="font-bold text-slate-800 dark:text-slate-100 text-base">
              Biên tập Câu hỏi {questionNumber}
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Giảng viên trực tiếp chỉnh sửa nội dung, lựa chọn và lời giải đáp án
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSaving}
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-5 space-y-4">
          {/* Question Content */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700 dark:text-slate-300">
              Nội dung câu hỏi đề xuất <span className="text-rose-500">*</span>:
            </label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={3}
              placeholder="Nhập nội dung đề bài..."
              className="w-full bg-slate-50 dark:bg-slate-800/60 rounded-2xl p-3 text-xs md:text-sm text-slate-800 dark:text-slate-100 border border-slate-200 dark:border-slate-700 focus:outline-hidden focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all resize-none"
            />
          </div>

          {/* Options List */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-700 dark:text-slate-300">
                Các phương án lựa chọn <span className="text-rose-500">*</span>:
              </label>
              <button
                type="button"
                onClick={handleAddOption}
                className="flex items-center gap-1 text-xs text-blue-600 dark:text-blue-400 font-semibold hover:underline cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Thêm phương án</span>
              </button>
            </div>

            <div className="space-y-2.5">
              {options.map((opt, idx) => (
                <div key={idx} className="flex items-center gap-2">
                  {/* Correct Toggle Pill */}
                  <button
                    type="button"
                    onClick={() => handleToggleCorrect(idx)}
                    title={opt.isCorrect ? 'Đáp án đúng (Bấm để hủy)' : 'Bấm để đặt làm đáp án đúng'}
                    className={cn(
                      'w-8 h-8 rounded-xl flex items-center justify-center font-bold text-xs shrink-0 transition-all cursor-pointer border',
                      opt.isCorrect
                        ? 'bg-emerald-500 text-white border-emerald-600 shadow-xs'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border-slate-200 dark:border-slate-700 hover:border-emerald-400'
                    )}
                  >
                    {opt.key}
                  </button>

                  {/* Option Text Input */}
                  <input
                    type="text"
                    value={opt.text}
                    onChange={(e) => handleOptionChange(idx, e.target.value)}
                    placeholder={`Nội dung lựa chọn ${opt.key}...`}
                    className={cn(
                      'flex-1 bg-slate-50 dark:bg-slate-800/60 rounded-xl px-3 py-2 text-xs md:text-sm text-slate-800 dark:text-slate-100 border transition-all focus:outline-hidden',
                      opt.isCorrect
                        ? 'border-emerald-300 dark:border-emerald-800/60 bg-emerald-50/20'
                        : 'border-slate-200 dark:border-slate-700 focus:border-blue-500'
                    )}
                  />

                  {/* Remove Button */}
                  {options.length > 2 && (
                    <button
                      type="button"
                      onClick={() => handleRemoveOption(idx)}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors cursor-pointer"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>
            <p className="text-[11px] text-slate-400 dark:text-slate-500">
              * Bấm vào chữ cái đầu (A, B, C...) để đánh dấu/hủy đáp án đúng (hỗ trợ nhiều đáp án đúng).
            </p>
          </div>

          {/* Explanation */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700 dark:text-slate-300">
              Lời giải thích chi tiết:
            </label>
            <textarea
              value={explanation}
              onChange={(e) => setExplanation(e.target.value)}
              rows={2}
              placeholder="Giải thích tại sao đáp án trên là chính xác..."
              className="w-full bg-slate-50 dark:bg-slate-800/60 rounded-2xl p-3 text-xs md:text-sm text-slate-800 dark:text-slate-100 border border-slate-200 dark:border-slate-700 focus:outline-hidden focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all resize-none"
            />
          </div>

          {/* Auto Approve Checkbox */}
          <div className="pt-2 border-t border-slate-100 dark:border-slate-800">
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={autoApprove}
                onChange={(e) => setAutoApprove(e.target.checked)}
                className="w-4 h-4 rounded text-blue-600 focus:ring-blue-500 cursor-pointer"
              />
              <span className="text-xs font-semibold text-slate-700 dark:text-slate-200 flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                Tự động Phê duyệt câu hỏi này vào ngân hàng sau khi lưu
              </span>
            </label>
          </div>
        </form>

        {/* Footer */}
        <div className="flex items-center justify-end gap-2 p-4 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/30">
          <button
            type="button"
            onClick={onClose}
            disabled={isSaving}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={isSaving}
            className="flex items-center gap-1.5 px-5 py-2 rounded-xl text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 shadow-md shadow-blue-500/20 transition-all cursor-pointer disabled:opacity-50"
          >
            {isSaving ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Đang lưu...</span>
              </>
            ) : (
              <>
                <Save className="w-3.5 h-3.5" />
                <span>Lưu thay đổi</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
