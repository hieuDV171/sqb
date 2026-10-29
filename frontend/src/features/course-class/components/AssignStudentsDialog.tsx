import { useMemo, useState } from 'react';
import { CheckCircle2, Loader2, UserPlus, X } from 'lucide-react';
import { useAssignStudents } from '@/features/course-class';
import type { AssignStudentsResponse } from '@/features/course-class';
import { useModalFocusTrap } from '../hooks/useModalFocusTrap';

interface AssignStudentsDialogProps {
  open: boolean;
  courseClassId: number;
  classCode: string;
  onClose: () => void;
}

export function AssignStudentsDialog({ open, courseClassId, classCode, onClose }: AssignStudentsDialogProps) {
  const [rawCodes, setRawCodes] = useState('');
  const [result, setResult] = useState<AssignStudentsResponse | null>(null);
  const assignMutation = useAssignStudents(courseClassId);
  const dialogRef = useModalFocusTrap(open, onClose, assignMutation.isPending);
  const studentCodes = useMemo(
    () => Array.from(new Set(rawCodes.split(/[\s,;]+/).map((code) => code.trim()).filter(Boolean))),
    [rawCodes]
  );

  if (!open) return null;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (studentCodes.length === 0) return;
    const response = await assignMutation.mutateAsync({ studentCodes });
    setResult(response.data);
  };

  const handleClose = () => {
    if (assignMutation.isPending) return;
    setRawCodes('');
    setResult(null);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-sm">
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="assign-students-title"
        className="w-full max-w-xl rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-800 dark:bg-slate-900"
      >
        <div className="flex items-start justify-between border-b border-slate-200 p-5 dark:border-slate-800">
          <div className="flex gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400">
              <UserPlus className="h-5 w-5" />
            </span>
            <div>
              <h2 id="assign-students-title" className="font-bold text-slate-900 dark:text-white">
                Gán sinh viên vào lớp {classCode}
              </h2>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                Dán danh sách MSSV, ngăn cách bằng xuống dòng, dấu phẩy hoặc khoảng trắng.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleClose}
            disabled={assignMutation.isPending}
            aria-label="Đóng hộp thoại"
            className="flex h-10 w-10 items-center justify-center rounded-xl text-slate-500 transition-colors hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500/30 disabled:opacity-50 dark:hover:bg-slate-800"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 p-5">
          <div className="space-y-1.5">
            <label htmlFor="student-codes" className="text-sm font-semibold text-slate-700 dark:text-slate-300">
              Danh sách MSSV *
            </label>
            <textarea
              data-autofocus
              id="student-codes"
              rows={7}
              value={rawCodes}
              onChange={(event) => {
                setRawCodes(event.target.value);
                setResult(null);
              }}
              placeholder={'20230001\n20230002\n20230003'}
              className="w-full resize-y rounded-xl border border-slate-200 bg-slate-50 p-3.5 font-mono text-sm text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            />
            <p className="text-[11px] text-slate-500 dark:text-slate-400">
              Đã nhận diện {studentCodes.length} mã sinh viên duy nhất.
            </p>
          </div>

          {result && (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-xs dark:border-emerald-900 dark:bg-emerald-950/30">
              <div className="flex items-center gap-2 font-bold text-emerald-800 dark:text-emerald-300">
                <CheckCircle2 className="h-4 w-4" /> Kết quả gán sinh viên
              </div>
              <div className="mt-3 grid grid-cols-3 gap-2 text-center">
                <div><strong className="block text-base">{result.totalEnrolled}</strong>Đã thêm</div>
                <div><strong className="block text-base">{result.alreadyEnrolledCount}</strong>Đã có</div>
                <div><strong className="block text-base">{result.notFoundCount}</strong>Không hợp lệ</div>
              </div>
              {result.notFoundCodes.length > 0 && (
                <p className="mt-3 break-words text-rose-700 dark:text-rose-300">
                  Không tìm thấy: {result.notFoundCodes.join(', ')}
                </p>
              )}
            </div>
          )}

          <div className="flex justify-end gap-3 pt-1">
            <button
              type="button"
              onClick={handleClose}
              disabled={assignMutation.isPending}
              className="min-h-11 rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
            >
              {result ? 'Đóng' : 'Hủy'}
            </button>
            {!result && (
              <button
                type="submit"
                disabled={assignMutation.isPending || studentCodes.length === 0}
                className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-indigo-600 px-4 text-sm font-semibold text-white transition hover:bg-indigo-700 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
              >
                {assignMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                Gán {studentCodes.length} sinh viên
              </button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
