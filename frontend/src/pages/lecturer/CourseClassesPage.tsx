import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { BookOpen, ChevronRight, History, Plus, Search, School, Users } from 'lucide-react';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { sessionService } from '@/features/session/services/sessionService';
import { useAuthStore } from '@/stores/useAuthStore';
import {
  CreateCourseClassDialog,
  useCourseClasses,
  useLecturers,
} from '@/features/course-class';

export function CourseClassesPage() {
  const role = useAuthStore((state) => state.user?.role);
  const isAdmin = role === 'ADMIN';
  const [includeHistory, setIncludeHistory] = useState(true);
  const [query, setQuery] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  const classesQuery = useCourseClasses(isAdmin, includeHistory);
  const lecturersQuery = useLecturers(isAdmin);
  const subjectsQuery = useQuery({
    queryKey: ['subjects', 'course-class-management'],
    queryFn: async () => (await sessionService.getAllSubjects()).data ?? [],
    staleTime: 5 * 60_000,
  });

  const filteredClasses = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase('vi');
    return (classesQuery.data ?? [])
      .filter((courseClass) => {
        if (!normalized) return true;
        return [
          courseClass.classCode,
          courseClass.subjectCode,
          courseClass.subjectName,
          courseClass.semesterName,
          courseClass.lecturerName,
        ].some((value) => value?.toLocaleLowerCase('vi').includes(normalized));
      })
      .sort((left, right) => {
        if (left.activeSemester !== right.activeSemester) return left.activeSemester ? -1 : 1;
        return right.semesterName.localeCompare(left.semesterName, 'vi');
      });
  }, [classesQuery.data, query]);

  const totalStudents = (classesQuery.data ?? []).reduce(
    (sum, courseClass) => sum + courseClass.totalStudents,
    0
  );
  const activeClasses = (classesQuery.data ?? []).filter((courseClass) => courseClass.activeSemester).length;

  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-12">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="mb-2 flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
            <School className="h-4 w-4" /> Không gian giảng dạy
          </div>
          <h1 className="text-2xl font-bold text-slate-950 dark:text-white">Lớp học phần</h1>
          <p className="mt-1 max-w-2xl text-sm leading-relaxed text-slate-500 dark:text-slate-400">
            Quản lý lớp phụ trách, xem danh sách sinh viên và bổ sung sinh viên bằng MSSV.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setIsCreateOpen(true)}
          className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-700 active:scale-[0.98]"
        >
          <Plus className="h-4 w-4" /> Tạo lớp học phần
        </button>
      </header>

      <section className="grid grid-cols-1 gap-3 sm:grid-cols-3" aria-label="Tổng quan lớp học phần">
        <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs dark:border-slate-800 dark:bg-slate-900">
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400">Tổng số lớp đang hiển thị</p>
          <p className="mt-2 text-2xl font-black text-slate-950 dark:text-white">{classesQuery.data?.length ?? 0}</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs dark:border-slate-800 dark:bg-slate-900">
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400">Lớp học kỳ hiện tại</p>
          <p className="mt-2 text-2xl font-black text-indigo-600 dark:text-indigo-400">{activeClasses}</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs dark:border-slate-800 dark:bg-slate-900">
          <p className="text-xs font-medium text-slate-500 dark:text-slate-400">Lượt ghi danh</p>
          <p className="mt-2 text-2xl font-black text-slate-950 dark:text-white">{totalStudents.toLocaleString()}</p>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white shadow-xs dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-col gap-3 border-b border-slate-200 p-4 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative w-full sm:max-w-md">
            <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm theo mã lớp, môn, học kỳ hoặc giảng viên..."
              className="min-h-11 w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-3.5 text-sm text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            />
          </div>
          {!isAdmin && (
            <label className="inline-flex min-h-11 cursor-pointer items-center gap-2 rounded-xl border border-slate-200 px-3 text-xs font-semibold text-slate-700 dark:border-slate-700 dark:text-slate-300">
              <input
                type="checkbox"
                checked={includeHistory}
                onChange={(event) => setIncludeHistory(event.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
              />
              <History className="h-4 w-4" /> Hiện lớp lịch sử
            </label>
          )}
        </div>

        {classesQuery.isLoading ? (
          <div className="space-y-3 p-4" aria-label="Đang tải lớp học phần">
            {Array.from({ length: 5 }).map((_, index) => (
              <div key={index} className="h-16 animate-pulse rounded-xl bg-slate-100 dark:bg-slate-800" />
            ))}
          </div>
        ) : classesQuery.isError ? (
          <div className="p-5">
            <ErrorState
              title="Không thể tải lớp học phần"
              message="Dữ liệu lớp học phần chưa đồng bộ được từ máy chủ."
              onRetry={() => classesQuery.refetch()}
            />
          </div>
        ) : filteredClasses.length === 0 ? (
          <div className="p-5">
            <EmptyState
              icon={School}
              title={query ? 'Không tìm thấy lớp phù hợp' : 'Chưa có lớp học phần'}
              description={query
                ? 'Hãy thử từ khóa khác hoặc xóa bộ lọc tìm kiếm.'
                : 'Tạo lớp đầu tiên để bắt đầu quản lý sinh viên và nội dung học tập.'}
              actionLabel={query ? 'Xóa tìm kiếm' : 'Tạo lớp học phần'}
              onAction={query ? () => setQuery('') : () => setIsCreateOpen(true)}
            />
          </div>
        ) : (
          <>
            <div className="hidden overflow-x-auto md:block">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                  <tr>
                    <th className="px-5 py-3.5">Lớp / Môn học</th>
                    <th className="px-5 py-3.5">Học kỳ</th>
                    {isAdmin && <th className="px-5 py-3.5">Giảng viên</th>}
                    <th className="px-5 py-3.5 text-right">Sinh viên</th>
                    <th className="w-16 px-5 py-3.5"><span className="sr-only">Mở</span></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {filteredClasses.map((courseClass) => (
                    <tr key={courseClass.courseClassId} className="transition-colors hover:bg-slate-50/80 dark:hover:bg-slate-800/40">
                      <td className="px-5 py-4">
                        <Link to={`/lecturer/course-classes/${courseClass.courseClassId}`} className="group block">
                          <p className="font-bold text-slate-900 group-hover:text-indigo-600 dark:text-white dark:group-hover:text-indigo-400">
                            {courseClass.classCode}
                          </p>
                          <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                            [{courseClass.subjectCode}] {courseClass.subjectName}
                          </p>
                        </Link>
                      </td>
                      <td className="px-5 py-4">
                        <span className={courseClass.activeSemester
                          ? 'rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                          : 'rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-300'}>
                          {courseClass.semesterName}{courseClass.activeSemester ? ' · Hiện tại' : ''}
                        </span>
                      </td>
                      {isAdmin && (
                        <td className="px-5 py-4 text-xs text-slate-600 dark:text-slate-300">
                          {courseClass.lecturerName || 'Chưa phân công'}
                        </td>
                      )}
                      <td className="px-5 py-4 text-right font-bold text-slate-800 dark:text-slate-200">
                        {courseClass.totalStudents.toLocaleString()}
                      </td>
                      <td className="px-5 py-4">
                        <Link
                          to={`/lecturer/course-classes/${courseClass.courseClassId}`}
                          aria-label={`Mở lớp ${courseClass.classCode}`}
                          className="flex h-10 w-10 items-center justify-center rounded-xl text-slate-400 transition hover:bg-indigo-50 hover:text-indigo-600 dark:hover:bg-indigo-950/60 dark:hover:text-indigo-400"
                        >
                          <ChevronRight className="h-5 w-5" />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="divide-y divide-slate-100 p-3 dark:divide-slate-800 md:hidden">
              {filteredClasses.map((courseClass) => (
                <Link
                  key={courseClass.courseClassId}
                  to={`/lecturer/course-classes/${courseClass.courseClassId}`}
                  className="flex min-h-24 items-center gap-3 rounded-xl p-3 transition hover:bg-slate-50 dark:hover:bg-slate-800/50"
                >
                  <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400">
                    <BookOpen className="h-5 w-5" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <p className="font-bold text-slate-900 dark:text-white">{courseClass.classCode}</p>
                      {courseClass.activeSemester && (
                        <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300">Hiện tại</span>
                      )}
                    </div>
                    <p className="mt-1 truncate text-xs text-slate-500 dark:text-slate-400">
                      [{courseClass.subjectCode}] {courseClass.subjectName}
                    </p>
                    <p className="mt-1 flex items-center gap-3 text-[11px] text-slate-500 dark:text-slate-400">
                      <span>{courseClass.semesterName}</span>
                      <span className="flex items-center gap-1"><Users className="h-3 w-3" /> {courseClass.totalStudents}</span>
                    </p>
                  </div>
                  <ChevronRight className="h-5 w-5 shrink-0 text-slate-400" />
                </Link>
              ))}
            </div>
          </>
        )}
      </section>

      <CreateCourseClassDialog
        open={isCreateOpen}
        isAdmin={isAdmin}
        subjects={subjectsQuery.data ?? []}
        lecturers={lecturersQuery.data ?? []}
        onClose={() => setIsCreateOpen(false)}
      />
    </div>
  );
}
