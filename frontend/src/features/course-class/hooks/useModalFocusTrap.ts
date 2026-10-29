import { useEffect, useRef } from 'react';

const FOCUSABLE_SELECTOR = [
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  'a[href]',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

export function useModalFocusTrap(open: boolean, onClose: () => void, locked = false) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const closeRef = useRef(onClose);
  const lockedRef = useRef(locked);

  useEffect(() => {
    closeRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    lockedRef.current = locked;
  }, [locked]);

  useEffect(() => {
    if (!open) return;
    const previousActiveElement = document.activeElement as HTMLElement | null;
    const animationFrame = window.requestAnimationFrame(() => {
      const preferred = dialogRef.current?.querySelector<HTMLElement>('[data-autofocus]');
      const first = dialogRef.current?.querySelector<HTMLElement>(FOCUSABLE_SELECTOR);
      (preferred ?? first)?.focus();
    });

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        if (!lockedRef.current) closeRef.current();
        return;
      }
      if (event.key !== 'Tab') return;

      const focusable = Array.from(
        dialogRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR) ?? []
      );
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.cancelAnimationFrame(animationFrame);
      window.removeEventListener('keydown', handleKeyDown);
      previousActiveElement?.focus();
    };
  }, [open]);

  return dialogRef;
}
