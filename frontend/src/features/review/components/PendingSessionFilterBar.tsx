import { useId } from 'react';
import { Select } from '@base-ui/react/select';
import { Check, ChevronDown, Search } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { CourseClassResponse } from '@/features/course-class/types/courseClass.types';

interface PendingSessionFilterBarProps {
  search: string;
  onSearchChange: (query: string) => void;
  selectedClassId?: number;
  onClassChange: (id?: number) => void;
  courseClasses: CourseClassResponse[];
  isLoadingClasses: boolean;
}

export function PendingSessionFilterBar({
  search, onSearchChange, selectedClassId, onClassChange, courseClasses, isLoadingClasses,
}: PendingSessionFilterBarProps) {
  const classLabel = useId();
  const searchId = useId();
  const options = [
    { value: '', label: 'Tất cả lớp học phần' },
    ...courseClasses.map((item) => ({
      value: String(item.courseClassId),
      label: [item.classCode, item.subjectCode, item.semesterName].filter(Boolean).join(' · '),
    })),
  ];
  const selectedValue = selectedClassId ? String(selectedClassId) : '';
  if (!options.some((item) => item.value === selectedValue)) {
    options.push({ value: selectedValue, label: `Lớp #${selectedClassId}` });
  }

  return (
    <div className="grid gap-4 rounded-2xl border border-border bg-card p-4 sm:grid-cols-2">
      <div className="min-w-0 space-y-2">
        <Label htmlFor={searchId}>Tìm phiên cần duyệt</Label>
        <div className="relative">
          <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input id={searchId} type="search" className="min-h-11 pl-9" placeholder="Tiêu đề, môn học hoặc mã phiên"
            maxLength={120} value={search} onChange={(event) => onSearchChange(event.target.value)} />
        </div>
      </div>
      <div className="min-w-0 space-y-2">
        <span id={classLabel} className="block text-sm font-medium">Lớp học phần</span>
        <Select.Root items={options} value={selectedValue}
          onValueChange={(value) => onClassChange(value ? Number(value) : undefined)}>
          <Select.Trigger aria-labelledby={classLabel}
            className="flex min-h-11 w-full items-center justify-between gap-2 rounded-xl border border-input bg-background px-3 text-left text-sm hover:bg-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">
            {isLoadingClasses ? <span>Đang tải danh sách lớp…</span> : <Select.Value className="min-w-0 truncate" />}
            <Select.Icon><ChevronDown aria-hidden="true" className="size-4 text-muted-foreground" /></Select.Icon>
          </Select.Trigger>
          <Select.Portal>
            <Select.Positioner sideOffset={6} alignItemWithTrigger={false} className="z-50 w-(--anchor-width) max-w-(--available-width)">
              <Select.Popup className="max-h-(--available-height) overflow-y-auto rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
                <Select.List>
                  {options.map((item) => (
                    <Select.Item key={item.value} value={item.value}
                      className="flex min-h-11 items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm outline-none data-highlighted:bg-accent data-highlighted:text-accent-foreground">
                      <Select.ItemText className="min-w-0 wrap-break-word">{item.label}</Select.ItemText>
                      <Select.ItemIndicator><Check aria-hidden="true" className="size-4 text-academic" /></Select.ItemIndicator>
                    </Select.Item>
                  ))}
                </Select.List>
              </Select.Popup>
            </Select.Positioner>
          </Select.Portal>
        </Select.Root>
      </div>
    </div>
  );
}
