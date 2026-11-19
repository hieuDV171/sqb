/** Owns pending writes so submit/discard can invalidate them synchronously. */
export function createDraftAutosave<T extends object>(key: string, storage = () => localStorage) {
  let timer: ReturnType<typeof setTimeout> | undefined;
  let generation = 0;

  const cancel = () => {
    generation++;
    if (timer !== undefined) clearTimeout(timer);
    timer = undefined;
  };
  const write = (draft: T) => {
    try {
      storage().setItem(key, JSON.stringify({ ...draft, updatedAt: new Date().toISOString() }));
    } catch {
      // Storage may be unavailable; submitting the form must remain possible.
    }
  };

  return {
    cancel,
    schedule(draft: T) {
      cancel();
      const current = generation;
      timer = setTimeout(() => {
        if (current !== generation) return;
        timer = undefined;
        write(draft);
      }, 1000);
    },
    saveNow(draft: T) {
      cancel();
      write(draft);
    },
    clear() {
      cancel();
      try { storage().removeItem(key); } catch { /* Storage may be unavailable. */ }
    },
  };
}
