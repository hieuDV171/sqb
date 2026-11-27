import { useEffect, useRef, type Dispatch, type PointerEvent, type SetStateAction } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export const maxQuestionCount = 100;

interface QuestionCountInputProps {
  id: string;
  value: string;
  onChange: Dispatch<SetStateAction<string>>;
  invalid: boolean;
  describedBy: string;
}

type Step = 1 | -1;

export function QuestionCountInput({ id, value, onChange, invalid, describedBy }: QuestionCountInputProps) {
  const timer = useRef<number | undefined>(undefined);
  const repeated = useRef(false);
  const count = Number(value);

  useEffect(() => () => window.clearTimeout(timer.current), []);

  function stepCount(step: Step) {
    onChange((previous) => {
      const current = Number(previous);
      const whole = step === 1 ? Math.floor(current) : Math.ceil(current);
      return String(Math.min(maxQuestionCount, Math.max(1, whole + step)));
    });
  }

  function stopRepeat() {
    window.clearTimeout(timer.current);
  }

  function startRepeat(event: PointerEvent<HTMLButtonElement>, step: Step) {
    if (!event.isPrimary || event.button !== 0) return;
    stopRepeat();
    repeated.current = false;
    const { currentTarget: button, pointerId } = event;
    button.setPointerCapture(pointerId);

    function repeat() {
      if (!button.hasPointerCapture(pointerId) || button.matches(':disabled')) return;
      repeated.current = true;
      stepCount(step);
      timer.current = window.setTimeout(repeat, 60);
    }

    timer.current = window.setTimeout(repeat, 300);
  }

  return (
    <div className="relative w-28">
      <Input id={id} type="number" inputMode="numeric" min={1} max={maxQuestionCount} step={1} required
        className="h-11 [appearance:textfield] pe-10 md:h-11 [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
        aria-invalid={invalid} aria-describedby={describedBy}
        value={value} onChange={(event) => onChange(event.target.value)} />
      <div className="absolute inset-y-px end-px grid w-8 grid-rows-2 divide-y divide-input overflow-hidden rounded-e-[calc(var(--radius-xl)-1px)] border-s border-input">
        {([1, -1] as const).map((step) => {
          const increase = step === 1;
          const Icon = increase ? ChevronUp : ChevronDown;
          return (
            <Button key={step} type="button" size="icon-xs" variant="ghost"
              className="size-full touch-none rounded-none focus-visible:ring-inset"
              aria-label={increase ? 'Tăng 1 câu' : 'Giảm 1 câu'} aria-controls={id}
              disabled={increase ? count >= maxQuestionCount : count <= 1}
              onPointerDown={(event) => startRepeat(event, step)}
              onPointerUp={stopRepeat} onPointerCancel={stopRepeat} onLostPointerCapture={stopRepeat} onBlur={stopRepeat}
              onClick={(event) => {
                if (event.detail === 0 || !repeated.current) stepCount(step);
                repeated.current = false;
              }}>
              <Icon aria-hidden="true" className="size-3.5" />
            </Button>
          );
        })}
      </div>
    </div>
  );
}
