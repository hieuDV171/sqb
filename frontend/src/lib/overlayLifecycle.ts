// Owners may close in any order; only the last release restores page scrolling.
const scrollLocks = new WeakMap<HTMLElement, { owners: Set<symbol>; previousOverflow: string }>();
const layers: symbol[] = [];

export const OVERLAY_FOCUSABLE_SELECTOR = 'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])';

export function trapOverlayTab(container: HTMLElement | null, event: KeyboardEvent) {
  if (!container || event.key !== 'Tab') return;
  const elements = Array.from(container.querySelectorAll<HTMLElement>(OVERLAY_FOCUSABLE_SELECTOR))
    .filter(element => element.getClientRects().length > 0 && !element.closest('[hidden], [inert]'));
  const first = elements[0];
  const last = elements.at(-1);
  const active = container.ownerDocument.activeElement;
  if (!first) {
    event.preventDefault();
    if (!container.hasAttribute('tabindex')) container.setAttribute('tabindex', '-1');
    container.focus();
  } else if (event.shiftKey && (active === first || !container.contains(active))) {
    event.preventDefault();
    last?.focus();
  } else if (!event.shiftKey && (active === last || !container.contains(active))) {
    event.preventDefault();
    first.focus();
  }
}

export function acquireBodyScrollLock(body = document.body) {
  let lock = scrollLocks.get(body);
  if (!lock) {
    lock = { owners: new Set(), previousOverflow: body.style.overflow };
    scrollLocks.set(body, lock);
  }
  const owner = Symbol('scroll-lock');
  lock.owners.add(owner);
  body.style.overflow = 'hidden';
  return () => {
    if (!lock.owners.delete(owner) || lock.owners.size !== 0) return;
    body.style.overflow = lock.previousOverflow;
    scrollLocks.delete(body);
  };
}

export function registerOverlayLayer() {
  const owner = Symbol('overlay');
  layers.push(owner);
  return {
    isTopmost: () => layers.at(-1) === owner,
    release() {
      const index = layers.indexOf(owner);
      if (index !== -1) layers.splice(index, 1);
    },
  };
}

export function hasActiveOverlay() {
  return layers.length > 0;
}
