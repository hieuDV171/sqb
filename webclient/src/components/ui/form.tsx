import { cloneElement, createContext, useContext, useId, useLayoutEffect, useRef, useState,
  type ComponentProps, type ReactElement, type SubmitEvent } from 'react';
import { ActionError } from './action-error';

type Control = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;
type InvalidField = { control: Control; fieldId?: string; message: string; label?: string };

const ValidationContext = createContext<InvalidField[]>([]);
const numberLimitFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 20 });

function isControl(element: Element): element is Control {
  return element instanceof HTMLInputElement || element instanceof HTMLSelectElement || element instanceof HTMLTextAreaElement;
}

function constraintMessage(control: Control) {
  const { validity } = control;
  if (!control.willValidate || validity.valid) return undefined;

  if (validity.badInput) return 'Nhập một giá trị hợp lệ.';
  if (validity.valueMissing) {
    return control instanceof HTMLSelectElement || (control instanceof HTMLInputElement && ['checkbox', 'radio', 'file'].includes(control.type))
      ? 'Vui lòng chọn một giá trị.'
      : 'Vui lòng điền trường này.';
  }
  if (validity.typeMismatch) {
    return (control as HTMLInputElement).type === 'email' ? 'Nhập địa chỉ email hợp lệ.' : 'Nhập địa chỉ URL hợp lệ.';
  }
  if (validity.patternMismatch) return control.dataset.patternError ?? 'Nhập đúng định dạng yêu cầu.';
  if (validity.tooShort) return `Nhập ít nhất ${(control as HTMLInputElement).minLength} ký tự.`;
  if (validity.tooLong) return `Nhập tối đa ${(control as HTMLInputElement).maxLength} ký tự.`;
  if (control instanceof HTMLInputElement) {
    const { min, max, step, type } = control;
    if (validity.rangeUnderflow || validity.rangeOverflow) {
      const minimum = type === 'number' && min ? numberLimitFormat.format(Number(min)) : min;
      const maximum = type === 'number' && max ? numberLimitFormat.format(Number(max)) : max;
      if (min && max) return `Nhập giá trị từ ${minimum} đến ${maximum}.`;
      return validity.rangeUnderflow ? `Nhập giá trị từ ${minimum} trở lên.` : `Nhập giá trị tối đa là ${maximum}.`;
    }
    if (validity.stepMismatch && type === 'number') {
      return Number(step || '1') === 1 ? 'Nhập một số nguyên.' : `Nhập số theo bước ${step.replace('.', ',')}.`;
    }
  }
  return 'Kiểm tra lại giá trị đã nhập.';
}

function readErrors(controls: Control[]): InvalidField[] {
  return controls.flatMap((control) => {
    const message = constraintMessage(control);
    if (!message) return [];
    const fieldId = control.dataset.validationField;
    const label = fieldId ? undefined : control.getAttribute('aria-label') || control.labels?.[0]?.textContent?.trim();
    return [{ control, fieldId, message, label }];
  });
}

type FormProps = Omit<ComponentProps<'form'>, 'action' | 'noValidate' | 'onSubmit'> & {
  onSubmit: (event: SubmitEvent<HTMLFormElement>) => void;
  validation?: 'constraints' | 'manual';
};

export function Form({ children, onSubmit, onChange, onBlur, validation = 'constraints', ...props }: FormProps) {
  const [errors, setErrors] = useState<InvalidField[]>([]);
  const focusOnCommit = useRef(false);
  const summary = useRef<HTMLDivElement>(null);

  useLayoutEffect(() => {
    if (!focusOnCommit.current) return;
    focusOnCommit.current = false;
    const firstField = errors.find(({ control }) => control.getClientRects().length > 0 && control.getAttribute('aria-hidden') !== 'true');
    if (firstField) firstField.control.focus();
    else summary.current?.focus();
  }, [errors]);

  function submit(event: SubmitEvent<HTMLFormElement>) {
    if (event.target !== event.currentTarget) return;
    event.preventDefault();
    if (validation === 'constraints') {
      const nextErrors = readErrors(Array.from(event.currentTarget.elements).filter(isControl));
      focusOnCommit.current = nextErrors.length > 0;
      if (nextErrors.length > 0 || errors.length > 0) setErrors(nextErrors);
      if (nextErrors.length > 0) return;
    }
    onSubmit(event);
  }

  function refreshErrors(form: HTMLFormElement) {
    if (errors.length === 0) return;
    const nextErrors = readErrors(errors.map(({ control }) => control).filter((control) => control.form === form));
    if (nextErrors.length !== errors.length || nextErrors.some(({ message }, index) => message !== errors[index]?.message)) {
      setErrors(nextErrors);
    }
  }

  const unboundErrors = errors.filter(({ fieldId }) => !fieldId);

  return (
    <ValidationContext.Provider value={errors}>
      <form {...props} noValidate onSubmit={submit}
        onChange={(event) => { onChange?.(event); refreshErrors(event.currentTarget); }}
        onBlur={(event) => { onBlur?.(event); refreshErrors(event.currentTarget); }}>
        {children}
        {unboundErrors.length > 0 && (
          <div ref={summary} tabIndex={-1} className="col-span-full basis-full space-y-1">
            {unboundErrors.map(({ label, message }, index) => (
              <ActionError key={index} message={label ? `${label}. ${message}` : message} />
            ))}
          </div>
        )}
      </form>
    </ValidationContext.Provider>
  );
}

type ValidatedControlProps = Pick<ComponentProps<'input'>, 'aria-invalid' | 'aria-describedby'> & {
  'data-validation-field'?: string;
  'data-pattern-error'?: string;
};

export function ValidatedField({ children, patternError }: {
  children: ReactElement<ValidatedControlProps>;
  patternError?: string;
}) {
  const id = useId();
  const errors = useContext(ValidationContext);
  const message = errors.find(({ fieldId }) => fieldId === id)?.message;
  const errorId = `${id}-error`;
  const describedBy = [children.props['aria-describedby'], message && errorId].filter(Boolean).join(' ');

  return (
    <>
      {cloneElement(children, {
        'data-validation-field': id,
        'data-pattern-error': patternError,
        'aria-invalid': message ? true : children.props['aria-invalid'],
        'aria-describedby': describedBy || undefined,
      })}
      <ActionError as="span" id={errorId} className="mt-1 block" message={message} />
    </>
  );
}
