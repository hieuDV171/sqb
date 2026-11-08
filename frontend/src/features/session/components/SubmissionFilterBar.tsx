import { Button } from '@/components/ui/button';
import type { SubjectResponse, SessionStatus } from '../types/session.types';
import { StudySubjectSelect } from './StudySubjectSelect';

interface SubmissionFilterBarProps {
  subjects: SubjectResponse[];
  selectedSubjectId?: number;
  onSelectSubject: (subjectId: number | undefined) => void;
  selectedStatus?: SessionStatus;
  onSelectStatus: (status: SessionStatus | undefined) => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
}

const statuses: { value?: SessionStatus; label: string }[] = [
  { label: 'Tất cả' },
  { value: 'PENDING', label: 'Chờ duyệt' },
  { value: 'REVIEWING', label: 'Đang xem xét' },
  { value: 'RESOLVED', label: 'Đã xét duyệt' },
];

const fieldClass = 'min-h-11 w-full min-w-0 rounded-xl border border-input bg-background px-3 text-sm text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring';

export function SubmissionFilterBar({
  subjects, selectedSubjectId, onSelectSubject, selectedStatus, onSelectStatus, searchQuery, onSearchChange,
}: SubmissionFilterBarProps) {
  return (
    <section aria-label="Lọc phiên nộp" className="space-y-4 rounded-2xl border border-border bg-card p-4 sm:p-5">
      <div className="flex flex-wrap gap-2" role="group" aria-label="Trạng thái duyệt">
        {statuses.map(({ value, label }) => (
          <Button
            key={label}
            variant={selectedStatus === value ? 'secondary' : 'ghost'}
            aria-pressed={selectedStatus === value}
            onClick={() => onSelectStatus(value)}
            className="min-h-11 aria-pressed:bg-academic-soft aria-pressed:text-academic"
          >
            {label}
          </Button>
        ))}
      </div>
      <div className="grid gap-3 sm:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
        <StudySubjectSelect subjects={subjects} value={selectedSubjectId} onChange={onSelectSubject} />
        <label className="min-w-0 space-y-1.5 text-sm font-medium">
          <span className="block">Tìm trong các phiên đã tải</span>
          <input
            className={fieldClass}
            type="search"
            value={searchQuery}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Tiêu đề, mã phiên hoặc môn học"
          />
        </label>
      </div>
    </section>
  );
}
