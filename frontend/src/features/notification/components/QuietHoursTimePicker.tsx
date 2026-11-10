import { useId } from 'react';
import { Select } from '@base-ui/react/select';
import { Check, ChevronDown } from 'lucide-react';

const toOptions = (count: number) => Array.from({ length: count }, (_, number) => {
  const value = String(number).padStart(2, '0');
  return { value, label: value };
});
const hours = toOptions(24);
const minutes = toOptions(60);

function TimePartSelect({ label, value, options, onChange }: {
  label: string;
  value: string;
  options: ReturnType<typeof toOptions>;
  onChange: (value: string) => void;
}) {
  const labelId = useId();

  return (
    <Select.Root
      value={value}
      items={options}
      onValueChange={(next) => { if (next !== null) onChange(next); }}
    >
      <Select.Trigger
        aria-labelledby={labelId}
        className="flex min-h-11 min-w-0 flex-1 cursor-pointer items-center justify-center gap-2 rounded-lg px-2 text-sm tabular-nums hover:bg-muted focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-ring"
      >
        <span id={labelId} className="sr-only">{label}</span>
        <Select.Value />
        <Select.Icon><ChevronDown aria-hidden="true" className="size-3.5 text-muted-foreground" /></Select.Icon>
      </Select.Trigger>
      <Select.Portal>
        <Select.Positioner sideOffset={6} alignItemWithTrigger={false} className="z-[72] min-w-(--anchor-width)">
          <Select.Popup className="max-h-[min(16rem,var(--available-height))] overflow-y-auto overscroll-contain rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
            <Select.List>
              {options.map((option) => (
                <Select.Item
                  key={option.value}
                  value={option.value}
                  className="flex min-h-10 cursor-pointer items-center justify-between gap-1 rounded-lg px-2 text-sm tabular-nums outline-none data-highlighted:bg-accent data-highlighted:text-accent-foreground data-selected:font-semibold"
                >
                  <Select.ItemText>{option.label}</Select.ItemText>
                  <Select.ItemIndicator><Check aria-hidden="true" className="size-3.5 text-academic" /></Select.ItemIndicator>
                </Select.Item>
              ))}
            </Select.List>
          </Select.Popup>
        </Select.Positioner>
      </Select.Portal>
    </Select.Root>
  );
}

export function QuietHoursTimePicker({ label, value, onChange }: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  const labelId = useId();
  const [hour, minute] = value.split(':');

  return (
    <div role="group" aria-labelledby={labelId} className="space-y-1 text-sm">
      <span id={labelId} className="block">{label}</span>
      <div className="flex items-center rounded-xl border border-input bg-background">
        <TimePartSelect label={`Giờ (${label})`} value={hour} options={hours} onChange={(next) => onChange(`${next}:${minute}`)} />
        <span aria-hidden="true" className="text-muted-foreground">:</span>
        <TimePartSelect label={`Phút (${label})`} value={minute} options={minutes} onChange={(next) => onChange(`${hour}:${next}`)} />
      </div>
    </div>
  );
}
