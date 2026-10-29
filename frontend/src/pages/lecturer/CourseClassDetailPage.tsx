import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, BookOpen, CalendarDays, Search, School, UserPlus, Users } from 'lucide-react';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import {
  AssignStudentsDialog,
  useClassStudents,
  useCourseClassDetail,
} from '@/features/course-class';

export function CourseClassDetailPage() {
  const { courseClassId: courseClassIdParam } = useParams();
  const courseClassId = Number(courseClassIdParam);
  const [query, setQuery] = useState('');
  const [isAssignOpen, setIsAssignOpen] = useState(false);
  const detailQuery = useCourseClassDetail(courseClassId);
  const studentsQuery = useClassStudents(courseClassId);

  const filteredStudents = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase('vi');
    if (!normalized) return studentsQuery.data ?? [];
    return (studentsQuery.data ?? []).filter((student) =>
      [student.studentCode, student.fullName, student.email, student.className, student.major]
        .some((value) => value?.toLocaleLowerCase('vi').includes(normalized))
    );
  }, [query, studentsQuery.data]);

  if (!Number.isFinite(courseClassId) || courseClassId <= 0) {
    return <ErrorState title="Lớp học phần không hợp lệ" message="Đường dẫn lớp học phần không đúng." />;
  }

  if (detailQuery.isLoading) {
    return (
      <div className="mx-auto max-w-6xl space-y-5">
        <div className="h-8 w-40 animate-pulse rounded-lg bg-slate-200 dark:bg-slate-800" />
        <div className="h-48 animate-pulse rounded-2xl bg-slate-200 dark:bg-slate-800" />
        <div className="h-80 animate-pulse rounded-2xl bg-slate-200 dark:bg-slate-800" />
      </div>
    );
  }

  if (detailQuery.isError || !detailQuery.data) {
    return (
      <div className="mx-auto max-w-4xl">
        <ErrorState
          title="Không thể mở lớp học phần"
          message="Lớp không tồn tại hoặc bạn không có quyền quản lý lớp này."
          onRetry={() => detailQuery.refetch()}
        />
      </div>
    );
  }

  const courseClass = detailQuery.data;

  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-12">
      <Link
        to="/lecturer/course-classes"
        className="inline-flex min-h-10 items-center gap-2 rounded-xl px-2 text-sm font-semibold text-slate-600 transition hover:bg-slate-100 hover:text-indigo-600 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-indigo-400"
      >
        <ArrowLeft className="h-4 w-4" /> Danh sách lớp học phần
      </Link>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xs dark:border-slate-800 dark:bg-slate-900">
        <div className="bg-linear-to-r from-indigo-600 to-violet-600 p-6 text-white sm:p-7">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-indigo-100">Lớp học phần</p>
              <h1 className="mt-2 text-2xl font-black">{courseClass.classCode}</h1>
              <p className="mt-1 text-sm text-indigo-100">
                [{courseClass.subjectCode}] {courseClass.subjectName}
              </p>
            </div>
            <button
              type="button"
              onClick={() => setIsAssignOpen(true)}
              className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-white px-4 text-sm font-bold text-indigo-700 shadow-sm transition hover:bg-indigo-50 active:scale-[0.98]"
            >
              <UserPlus className="h-4 w-4" /> Gán sinh viên
            </button>
          </div>
        </div>
        <div className="grid grid-cols-1 divide-y divide-slate-100 dark:divide-slate-800 sm:grid-cols-3 sm:divide-x sm:divide-y-0">
          <div className="flex items-center gap-3 p-4">
            <CalendarDays className="h-5 w-5 text-indigo-500" />
            <div><p className="text-[11px] text-slate-500">Học kỳ</p><p className="text-sm font-bold text-slate-900 dark:text-white">{courseClass.semesterName}</p></div>
          </div>
          <div className="flex items-center gap-3 p-4">
            <Users className="h-5 w-5 text-indigo-500" />
            <div><p className="text-[11px] text-slate-500">Sinh viên</p><p className="text-sm font-bold text-slate-900 dark:text-white">{courseClass.totalStudents.toLocaleString()}</p></div>
          </div>
          <div className="flex items-center gap-3 p-4">
            <School className="h-5 w-5 text-indigo-500" />
            <div><p className="text-[11px] text-slate-500">Giảng viên</p><p className="text-sm font-bold text-slate-900 dark:text-white">{courseClass.lecturerName || 'Chưa phân công'}</p></div>
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white shadow-xs dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-col gap-3 border-b border-slate-200 p-4 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="font-bold text-slate-900 dark:text-white">Danh sách sinh viên</h2>
            <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">Thông tin ghi danh hiện tại của lớp.</p>
          </div>
          <div className="relative w-full sm:max-w-sm">
            <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm MSSV, họ tên, email..."
              className="min-h-11 w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-3.5 text-sm text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            />
          </div>
        </div>

        {studentsQuery.isLoading ? (
          <div className="space-y-3 p-4">
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={index} className="h-14 animate-pulse rounded-xl bg-slate-100 dark:bg-slate-800" />
            ))}
          </div>
        ) : studentsQuery.isError ? (
          <div className="p-5">
            <ErrorState
              title="Không thể tải danh sách sinh viên"
              message="Vui lòng kiểm tra kết nối và thử lại."
              onRetry={() => studentsQuery.refetch()}
            />
          </div>
        ) : filteredStudents.length === 0 ? (
          <div className="p-5">
            <EmptyState
              icon={query ? BookOpen : Users}
              title={query ? 'Không tìm thấy sinh viên' : 'Lớp chưa có sinh viên'}
              description={query
                ? 'Hãy thử tìm bằng MSSV, họ tên hoặc email khác.'
                : 'Dán danh sách MSSV để bổ sung sinh viên vào lớp học phần.'}
              actionLabel={query ? 'Xóa tìm kiếm' : 'Gán sinh viên'}
              onAction={query ? () => setQuery('') : () => setIsAssignOpen(true)}
            />
          </div>
        ) : (
          <>
            <div className="hidden overflow-x-auto md:block">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                  <tr>
                    <th className="px-5 py-3.5">MSSV</th>
                    <th className="px-5 py-3.5">Sinh viên</th>
                    <th className="px-5 py-3.5">Lớp sinh hoạt</th>
                    <th className="px-5 py-3.5">Chuyên ngành</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {filteredStudents.map((student) => (
                    <tr key={student.userId} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40">
                      <td className="px-5 py-4 font-mono text-xs font-bold text-indigo-600 dark:text-indigo-400">{student.studentCode || '—'}</td>
                      <td className="px-5 py-4">
                        <Link to={`/users/${student.userId}`} className="font-bold text-slate-900 hover:text-indigo-600 dark:text-white dark:hover:text-indigo-400">{student.fullName || 'Chưa cập nhật'}</Link>
                        <p className="mt-0.5 text-xs text-slate-500">{student.email}</p>
                      </td>
                      <td className="px-5 py-4 text-xs text-slate-600 dark:text-slate-300">{student.className || '—'}</td>
                      <td className="px-5 py-4 text-xs text-slate-600 dark:text-slate-300">{student.major || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="divide-y divide-slate-100 p-3 dark:divide-slate-800 md:hidden">
              {filteredStudents.map((student) => (
                <Link key={student.userId} to={`/users/${student.userId}`} className="flex min-h-20 items-center gap-3 rounded-xl p-3 hover:bg-slate-50 dark:hover:bg-slate-800/50">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-indigo-50 font-bold text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400">
                    {student.fullName?.charAt(0) || 'S'}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-bold text-slate-900 dark:text-white">{student.fullName || 'Chưa cập nhật'}</p>
                    <p className="mt-0.5 font-mono text-[11px] text-indigo-600 dark:text-indigo-400">{student.studentCode || '—'}</p>
                    <p className="mt-0.5 truncate text-[11px] text-slate-500">{student.email}</p>
                  </div>
                </Link>
              ))}
            </div>
          </>
        )}
      </section>

      <AssignStudentsDialog
        open={isAssignOpen}
        courseClassId={courseClassId}
        classCode={courseClass.classCode}
        onClose={() => setIsAssignOpen(false)}
      />
    </div>
  );
}
