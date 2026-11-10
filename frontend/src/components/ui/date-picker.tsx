import { useId, useRef, useState, type KeyboardEvent } from 'react';
import { Popover } from '@base-ui/react/popover';
import { CalendarDays, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

const weekdays = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
const fullDate = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'full', timeZone: 'UTC' });
const monthTitle = new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric', timeZone: 'UTC' });

function calendarDate(year: number, month: number, day: number) {
  const date = new Date(0);
  date.setUTCFullYear(year, month, day);
  return date;
}

function isoDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

function addMonths(date: Date, offset: number) {
  const year = date.getUTCFullYear();
  const month = date.getUTCMonth() + offset;
  const lastDay = calendarDate(year, month + 1, 0).getUTCDate();
  return calendarDate(year, month, Math.min(date.getUTCDate(), lastDay));
}

function mondayIndex(date: Date) {
  return (date.getUTCDay() + 6) % 7;
}

type DatePickerProps = {
  id: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  allowClear?: boolean;
};

export function DatePicker({ id, value, onChange, disabled, allowClear }: DatePickerProps) {
  const titleId = useId();
  const activeDay = useRef<HTMLButtonElement>(null);
  const localNow = new Date();
  const today = calendarDate(localNow.getFullYear(), localNow.getMonth(), localNow.getDate());
  const todayIso = isoDate(today);
  const [open, setOpen] = useState(false);
  const [cursor, setCursor] = useState(today);
  const year = cursor.getUTCFullYear();
  const month = cursor.getUTCMonth();
  const monthStart = calendarDate(year, month, 1);
  const firstDay = mondayIndex(monthStart);
  const dayCount = calendarDate(year, month + 1, 0).getUTCDate();

  function choose(next: string) {
    onChange(next);
    setOpen(false);
  }

  function move(next: Date, focusDay = false) {
    if (next.getUTCFullYear() < 1 || next.getUTCFullYear() > 9999) return;
    setCursor(next);
    if (focusDay) requestAnimationFrame(() => activeDay.current?.focus());
  }

  function onDayKeyDown(event: KeyboardEvent<HTMLButtonElement>, date: Date) {
    let offset: number;
    switch (event.key) {
      case 'ArrowLeft': offset = -1; break;
      case 'ArrowRight': offset = 1; break;
      case 'ArrowUp': offset = -7; break;
      case 'ArrowDown': offset = 7; break;
      case 'Home': offset = -mondayIndex(date); break;
      case 'End': offset = 6 - mondayIndex(date); break;
      case 'PageUp':
      case 'PageDown':
        event.preventDefault();
        move(addMonths(date, (event.key === 'PageUp' ? -1 : 1) * (event.shiftKey ? 12 : 1)), true);
        return;
      default: return;
    }
    event.preventDefault();
    move(calendarDate(year, month, date.getUTCDate() + offset), true);
  }

  return (
    <Popover.Root open={open} onOpenChange={(next) => {
      if (next) setCursor(value ? new Date(value + 'T00:00:00Z') : today);
      setOpen(next);
    }}>
      <Popover.Trigger render={<Button id={id} type="button" variant="outline" disabled={disabled}
        className={cn('w-full justify-between border-input bg-card', !value && 'text-muted-foreground')} />}>
        {value ? value.split('-').reverse().join('/') : 'Chọn ngày'}
        <CalendarDays aria-hidden="true" className="text-academic" />
      </Popover.Trigger>
      <Popover.Portal>
        <Popover.Positioner align="start" sideOffset={8} className="z-[60]">
          <Popover.Popup initialFocus={activeDay} aria-labelledby={titleId}
            className="max-h-[var(--available-height)] w-80 max-w-[calc(100vw-1rem)] overflow-y-auto rounded-2xl border border-border bg-card p-2 text-card-foreground shadow-xl">
            <div className="grid grid-cols-[auto_auto_1fr_auto_auto] items-center">
              <Button type="button" size="icon" variant="ghost" aria-label="Năm trước" disabled={year === 1} onClick={() => move(addMonths(cursor, -12))}><ChevronsLeft aria-hidden="true" /></Button>
              <Button type="button" size="icon" variant="ghost" aria-label="Tháng trước" disabled={year === 1 && month === 0} onClick={() => move(addMonths(cursor, -1))}><ChevronLeft aria-hidden="true" /></Button>
              <p id={titleId} aria-live="polite" className="text-center text-sm font-semibold capitalize">{monthTitle.format(monthStart)}</p>
              <Button type="button" size="icon" variant="ghost" aria-label="Tháng sau" disabled={year === 9999 && month === 11} onClick={() => move(addMonths(cursor, 1))}><ChevronRight aria-hidden="true" /></Button>
              <Button type="button" size="icon" variant="ghost" aria-label="Năm sau" disabled={year === 9999} onClick={() => move(addMonths(cursor, 12))}><ChevronsRight aria-hidden="true" /></Button>
            </div>
            <div className="mt-2 grid grid-cols-7">
              {weekdays.map((day) => <span key={day} aria-hidden="true" className="py-2 text-center text-xs text-muted-foreground">{day}</span>)}
              {Array.from({ length: firstDay }, (_, index) => <span key={index} aria-hidden="true" />)}
              {Array.from({ length: dayCount }, (_, index) => {
                const date = calendarDate(year, month, index + 1);
                const iso = isoDate(date);
                const focused = index + 1 === cursor.getUTCDate();
                const isToday = iso === todayIso;
                return (
                  <Button key={iso} ref={focused ? activeDay : undefined} type="button" size="icon" variant={iso === value ? 'default' : 'ghost'}
                    aria-label={fullDate.format(date)} aria-pressed={iso === value} aria-current={isToday ? 'date' : undefined}
                    tabIndex={focused ? 0 : -1} className={cn('h-11 w-full rounded-lg', isToday && iso !== value && 'bg-academic-soft text-academic')}
                    onKeyDown={(event) => onDayKeyDown(event, date)} onClick={() => choose(iso)}>
                    {index + 1}
                  </Button>
                );
              })}
            </div>
            <div className="mt-2 flex justify-end gap-2 border-t border-border pt-2">
              {allowClear && value && <Button type="button" variant="ghost" onClick={() => choose('')}>Xóa ngày</Button>}
              <Button type="button" variant="ghost" onClick={() => choose(todayIso)}>Hôm nay</Button>
            </div>
          </Popover.Popup>
        </Popover.Positioner>
      </Popover.Portal>
    </Popover.Root>
  );
}
