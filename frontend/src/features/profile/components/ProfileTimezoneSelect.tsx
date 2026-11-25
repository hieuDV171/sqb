import { Combobox } from '@base-ui/react/combobox';
import { Check, ChevronDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export const DEFAULT_PROFILE_TIMEZONE = 'Asia/Ho_Chi_Minh';
const timezones = [...new Set([
  DEFAULT_PROFILE_TIMEZONE,
  'UTC',
  ...(Intl.supportedValuesOf?.('timeZone') ?? []).filter((zone) => zone !== 'Asia/Saigon'),
])];

function timezoneLabel(zone: string) {
  return zone === DEFAULT_PROFILE_TIMEZONE || zone === 'Asia/Saigon'
    ? 'Việt Nam (UTC+07:00)'
    : zone.replaceAll('_', ' ');
}

export function ProfileTimezoneSelect({ value, onChange, disabled }: {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}) {
  const { contains } = Combobox.useFilter({ locale: 'vi' });
  const options = timezones.includes(value) ? timezones : [value, ...timezones];

  return (
    <div className="min-w-0 space-y-1.5">
      <Label id="profile-timezone-label" htmlFor="profile-timezone" className="block leading-5">Múi giờ hoạt động</Label>
      <Combobox.Root items={options} value={value} disabled={disabled}
        filter={(zone, query) => contains(`${timezoneLabel(zone)} ${zone} ${zone.replaceAll('_', ' ')}`, query)}
        onValueChange={(zone) => { if (zone !== null) onChange(zone); }}>
        <Combobox.Trigger render={<Button id="profile-timezone" type="button" variant="outline"
          aria-labelledby="profile-timezone-label profile-timezone-value"
          className="min-h-11 w-full min-w-0 justify-between border-input px-3 font-normal" />}>
          <span id="profile-timezone-value" className="truncate">{timezoneLabel(value)}</span>
          <ChevronDown aria-hidden="true" className="text-muted-foreground" />
        </Combobox.Trigger>
        <Combobox.Portal>
          <Combobox.Positioner sideOffset={6} className="z-[60] w-(--anchor-width) max-w-(--available-width)">
            <Combobox.Popup aria-label="Chọn múi giờ" className="flex max-h-[min(20rem,var(--available-height))] flex-col rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
              <div className="p-1">
                <Combobox.Input aria-label="Tìm múi giờ" placeholder="Tìm múi giờ" render={<Input />} />
              </div>
              <Combobox.Empty className="px-3 py-2 text-sm text-muted-foreground empty:p-0">Không tìm thấy múi giờ.</Combobox.Empty>
              <Combobox.List className="min-h-0 overflow-y-auto">
                {(zone: string) => (
                  <Combobox.Item key={zone} value={zone}
                    className="flex min-h-11 cursor-pointer items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm outline-none data-highlighted:bg-accent">
                    <span className="min-w-0 wrap-break-word">{timezoneLabel(zone)}</span>
                    <Combobox.ItemIndicator className="shrink-0 text-academic"><Check aria-hidden="true" className="size-4" /></Combobox.ItemIndicator>
                  </Combobox.Item>
                )}
              </Combobox.List>
            </Combobox.Popup>
          </Combobox.Positioner>
        </Combobox.Portal>
      </Combobox.Root>
    </div>
  );
}
