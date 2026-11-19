import { useEffect, useRef } from 'react';
import { acquireBodyScrollLock, OVERLAY_FOCUSABLE_SELECTOR, registerOverlayLayer, trapOverlayTab } from '@/lib/overlayLifecycle';

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
    const releaseScroll = acquireBodyScrollLock();
    const layer = registerOverlayLayer();

    const animationFrame = window.requestAnimationFrame(() => {
      if (!layer.isTopmost()) return;
      const preferred = dialogRef.current?.querySelector<HTMLElement>('[data-autofocus]');
      const first = dialogRef.current?.querySelector<HTMLElement>(OVERLAY_FOCUSABLE_SELECTOR);
      (preferred ?? first)?.focus();
    });

    const handleKeyDown = (event: KeyboardEvent) => {
      if (!layer.isTopmost() || event.defaultPrevented) return;
      if (event.key === 'Escape') {
        event.preventDefault();
        event.stopImmediatePropagation();
        if (!lockedRef.current) closeRef.current();
        return;
      }
      trapOverlayTab(dialogRef.current, event);
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      const wasTopmost = layer.isTopmost();
      layer.release();
      releaseScroll();
      window.cancelAnimationFrame(animationFrame);
      window.removeEventListener('keydown', handleKeyDown);
      if (wasTopmost && previousActiveElement?.isConnected) previousActiveElement.focus();
    };
  }, [open]);

  return dialogRef;
}
