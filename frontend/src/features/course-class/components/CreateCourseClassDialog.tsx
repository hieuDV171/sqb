import { useState } from 'react';
import { Loader2, School, X } from 'lucide-react';
import type { SubjectResponse } from '@/features/session';
import type { LecturerSummaryResponse } from '@/features/course-class';
import { useCreateCourseClass } from '@/features/course-class';
import { useModalFocusTrap } from '../hooks/useModalFocusTrap';

interface CreateCourseClassDialogProps {
  open: boolean;
  isAdmin: boolean;
  subjects: SubjectResponse[];
  lecturers: LecturerSummaryResponse[];
  onClose: () => void;
}

export function CreateCourseClassDialog({
  open,
  isAdmin,
  subjects,
  lecturers,
  onClose,
}: CreateCourseClassDialogProps) {
  const [classCode, setClassCode] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [lecturerId, setLecturerId] = useState('');
  const createMutation = useCreateCourseClass();
  const dialogRef = useModalFocusTrap(open, onClose, createMutation.isPending);

  if (!open) return null;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!classCode.trim() || !subjectId || (isAdmin && !lecturerId)) return;
    await createMutation.mutateAsync({
      classCode: classCode.trim(),
      subjectId: Number(subjectId),
      lecturerId: isAdmin ? Number(lecturerId) : undefined,
    });
    setClassCode('');
    setSubjectId('');
    setLecturerId('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-sm">
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-class-title"
        className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-800 dark:bg-slate-900"
      >
        <div className="flex items-start justify-between border-b border-slate-200 p-5 dark:border-slate-800">
          <div className="flex gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400">
              <School className="h-5 w-5" />
            </span>
            <div>
              <h2 id="create-class-title" className="font-bold text-slate-900 dark:text-white">
                Tạo lớp học phần
              </h2>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                Lớp mới được tạo trong học kỳ đang kích hoạt.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={createMutation.isPending}
            aria-label="Đóng hộp thoại"
            className="flex h-10 w-10 items-center justify-center rounded-xl text-slate-500 transition-colors hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500/30 disabled:opacity-50 dark:hover:bg-slate-800"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 p-5">
          <div className="space-y-1.5">
            <label htmlFor="class-code" className="text-sm font-semibold text-slate-700 dark:text-slate-300">
              Mã lớp học phần *
            </label>
            <input
              data-autofocus
              id="class-code"
              value={classCode}
              onChange={(event) => setClassCode(event.target.value)}
              maxLength={50}
              required
              placeholder="Ví dụ: 161234"
              className="min-h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 text-sm text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            />
          </div>

          <div className="space-y-1.5">
            <label htmlFor="class-subject" className="text-sm font-semibold text-slate-700 dark:text-slate-300">
              Môn học *
            </label>
            <select
              id="class-subject"
              value={subjectId}
              onChange={(event) => setSubjectId(event.target.value)}
              required
              className="min-h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 text-sm text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            >
              <option value="">Chọn môn học</option>
              {subjects.map((subject) => (
                <option key={subject.subjectId} value={subject.subjectId}>
                  [{subject.code}] {subject.name}
                </option>
              ))}
            </select>
          </div>

          {isAdmin && (
            <div className="space-y-1.5">
              <label htmlFor="class-lecturer" className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Giảng viên phụ trách *
              </label>
              <select
                id="class-lecturer"
                value={lecturerId}
                onChange={(event) => setLecturerId(event.target.value)}
                required
                className="min-h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 text-sm text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
              >
                <option value="">Chọn giảng viên</option>
                {lecturers.map((lecturer) => (
                  <option key={lecturer.id} value={lecturer.id}>
                    {lecturer.fullName} — {lecturer.email}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={createMutation.isPending}
              className="min-h-11 rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending || !classCode.trim() || !subjectId || (isAdmin && !lecturerId)}
              className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-indigo-600 px-4 text-sm font-semibold text-white transition hover:bg-indigo-700 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
            >
              {createMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Tạo lớp
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
