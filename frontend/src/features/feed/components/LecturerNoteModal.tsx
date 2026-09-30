import React, { useState } from 'react';
import { usePostMutations } from '../hooks/usePostMutations';
import { Award, X, Loader2, Sparkles } from 'lucide-react';

interface LecturerNoteModalProps {
  postId: number;
  initialNote?: string;
  isOpen: boolean;
  onClose: () => void;
}

export const LecturerNoteModal: React.FC<LecturerNoteModalProps> = ({
  postId,
  initialNote = '',
  isOpen,
  onClose,
}) => {
  const [note, setNote] = useState(initialNote);
  const { updateLecturerNote, isUpdatingNote } = usePostMutations();

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await updateLecturerNote({
      id: postId,
      data: { content: note.trim() },
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl max-w-lg w-full overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-100 dark:border-slate-800 bg-amber-50/50 dark:bg-amber-950/20">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-amber-100 dark:bg-amber-900/50 text-amber-600 dark:text-amber-400">
              <Award className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-slate-900 dark:text-white text-base">
                Nhận xét chuyên môn của Giảng viên
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Ghi chú chính thức giúp định hướng kiến thức cho sinh viên
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5">
          <div className="mb-4">
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-2">
              Nội dung nhận xét / Hướng dẫn giải chi tiết:
            </label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={4}
              placeholder="Ví dụ: Định lý Lagrange áp dụng cho bài toán này cần chú ý điều kiện liên tục trên đoạn [a,b]..."
              className="w-full bg-slate-50 dark:bg-slate-800/80 rounded-2xl p-3.5 text-sm text-slate-800 dark:text-slate-100 placeholder:text-slate-400 border border-slate-200 dark:border-slate-700 focus:outline-hidden focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500 transition-all resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-sm font-medium text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isUpdatingNote}
              className="flex items-center gap-2 px-5 py-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-600 hover:to-amber-700 text-white font-medium text-sm shadow-md shadow-amber-500/20 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
            >
              {isUpdatingNote ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang lưu...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Lưu nhận xét</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
