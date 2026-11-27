import { useId } from 'react';
import { Select } from '@base-ui/react/select';
import { Check, ChevronDown } from 'lucide-react';
import type { SubjectResponse } from '../types/session.types';

interface StudySubjectSelectProps {
  subjects: SubjectResponse[];
  value?: number;
  onChange: (subjectId: number | undefined) => void;
  label?: string;
  emptyLabel?: string;
  description?: string;
  required?: boolean;
}

export function StudySubjectSelect({ subjects, value, onChange, label = 'Môn học', emptyLabel = 'Tất cả môn học', description, required }: StudySubjectSelectProps) {
  const labelId = useId();
  const descriptionId = useId();
  const items = [
    { value: '', label: emptyLabel },
    ...subjects.map((subject) => ({ value: String(subject.subjectId), label: `${subject.code} · ${subject.name}` })),
  ];

  return (
    <div className="min-w-0 space-y-1.5">
      <span id={labelId} className="block text-sm font-medium">
        {label}{required && <span aria-hidden="true" className="ml-1 text-destructive">*</span>}
      </span>
      <Select.Root items={items} value={value === undefined ? '' : String(value)} onValueChange={(next) => onChange(next ? Number(next) : undefined)}>
        <Select.Trigger
          aria-labelledby={labelId}
          aria-describedby={description ? descriptionId : undefined}
          aria-required={required}
          className="flex min-h-11 w-full min-w-0 cursor-pointer items-center justify-between gap-2 rounded-xl border border-input bg-background px-3 text-left text-sm text-foreground hover:bg-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
        >
          <Select.Value className="min-w-0 truncate" />
          <Select.Icon className="shrink-0"><ChevronDown aria-hidden="true" className="size-4 text-muted-foreground" /></Select.Icon>
        </Select.Trigger>
        <Select.Portal>
          <Select.Positioner sideOffset={6} alignItemWithTrigger={false} className="z-50 w-(--anchor-width) max-w-(--available-width)">
            <Select.Popup className="max-h-(--available-height) overflow-y-auto rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
              <Select.List>
                {items.map((item) => (
                  <Select.Item
                    key={item.value}
                    value={item.value}
                    className="flex min-h-11 cursor-pointer items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm outline-none data-highlighted:bg-accent data-highlighted:text-accent-foreground data-selected:font-semibold"
                  >
                    <Select.ItemText className="min-w-0 wrap-break-word">{item.label}</Select.ItemText>
                    <Select.ItemIndicator className="shrink-0 text-academic"><Check aria-hidden="true" className="size-4" /></Select.ItemIndicator>
                  </Select.Item>
                ))}
              </Select.List>
            </Select.Popup>
          </Select.Positioner>
        </Select.Portal>
      </Select.Root>
      {description && <p id={descriptionId} className="text-xs leading-relaxed text-muted-foreground">{description}</p>}
    </div>
  );
}
