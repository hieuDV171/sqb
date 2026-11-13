import { useRef, useState } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { Loader2, School, X } from 'lucide-react';
import { FilterSelect } from '@/components/ui/filter-select';
import type { SubjectResponse } from '@/features/session';
import type { LecturerSummaryResponse } from '@/features/course-class';
import { useCreateCourseClass } from '@/features/course-class';

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
  const classCodeRef = useRef<HTMLInputElement>(null);
  const createMutation = useCreateCourseClass();

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
    <Dialog.Root open={open} disablePointerDismissal onOpenChange={(next) => {
      if (!next && !createMutation.isPending) onClose();
    }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-sm" />
        <Dialog.Popup initialFocus={classCodeRef}
          className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-lg -translate-y-1/2 overflow-y-auto rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-start justify-between border-b border-slate-200 p-5 dark:border-slate-800">
            <div className="flex gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400">
                <School className="h-5 w-5" />
              </span>
              <div>
                <Dialog.Title className="font-bold text-slate-900 dark:text-white">
                  Tạo lớp học phần
                </Dialog.Title>
                <Dialog.Description className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                  Lớp mới được tạo trong học kỳ đang kích hoạt.
                </Dialog.Description>
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
                ref={classCodeRef}
                id="class-code"
                value={classCode}
                onChange={(event) => setClassCode(event.target.value)}
                maxLength={50}
                required
                placeholder="Ví dụ: 161234"
                className="min-h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 text-sm text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
              />
            </div>

            <FilterSelect
              label="Môn học"
              value={subjectId}
              onChange={setSubjectId}
              required
              options={[
                { value: '', label: 'Chọn môn học' },
                ...subjects.map((subject) => ({
                  value: String(subject.subjectId),
                  label: `[${subject.code}] ${subject.name}`,
                })),
              ]}
            />

            {isAdmin && (
              <FilterSelect
                label="Giảng viên phụ trách"
                value={lecturerId}
                onChange={setLecturerId}
                required
                options={[
                  { value: '', label: 'Chọn giảng viên' },
                  ...lecturers.map((lecturer) => ({
                    value: String(lecturer.id),
                    label: `${lecturer.fullName} — ${lecturer.email}`,
                  })),
                ]}
              />
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
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
