import { useId } from 'react';
import { Select } from '@base-ui/react/select';
import { Check, ChevronDown } from 'lucide-react';

interface GamificationSelectProps {
  label: string;
  value: string;
  options: { value: string; label: string }[];
  onChange: (value: string) => void;
  disabled?: boolean;
  inlineLabel?: boolean;
}

export function GamificationSelect({ label, value, options, onChange, disabled, inlineLabel }: GamificationSelectProps) {
  const labelId = useId();
  const placeholder = options.find((option) => option.value === '')?.label ?? 'Chọn một mục';

  return (
    <div className={inlineLabel
      ? 'grid w-full min-w-0 grid-cols-[auto_minmax(0,1fr)] items-center gap-2 sm:w-auto sm:grid-cols-[auto_minmax(9rem,11rem)]'
      : 'min-w-0 space-y-1.5'}>
      <span id={labelId} className="block text-sm font-medium">{label}</span>
      <Select.Root items={options} value={value} disabled={disabled}
        onValueChange={(next) => { if (next !== null) onChange(next); }}>
        <Select.Trigger aria-labelledby={labelId}
          className="flex min-h-11 w-full min-w-0 items-center justify-between gap-2 rounded-xl border border-input bg-background px-3 text-left text-sm hover:bg-muted focus-visible:outline-2 focus-visible:outline-ring disabled:opacity-60">
          <Select.Value className="min-w-0 truncate" placeholder={placeholder} />
          <Select.Icon className="shrink-0"><ChevronDown aria-hidden="true" className="size-4 text-muted-foreground" /></Select.Icon>
        </Select.Trigger>
        <Select.Portal>
          <Select.Positioner sideOffset={6} alignItemWithTrigger={false} className="z-50 w-(--anchor-width) max-w-(--available-width)">
            <Select.Popup className="max-h-(--available-height) overflow-y-auto rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
              <Select.List>
                {options.map((option) => (
                  <Select.Item key={option.value} value={option.value}
                    className="flex min-h-11 cursor-pointer items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm outline-none data-highlighted:bg-accent data-selected:font-semibold">
                    <Select.ItemText className="min-w-0 wrap-break-word">{option.label}</Select.ItemText>
                    <Select.ItemIndicator><Check aria-hidden="true" className="size-4 text-academic" /></Select.ItemIndicator>
                  </Select.Item>
                ))}
              </Select.List>
            </Select.Popup>
          </Select.Positioner>
        </Select.Portal>
      </Select.Root>
    </div>
  );
}
