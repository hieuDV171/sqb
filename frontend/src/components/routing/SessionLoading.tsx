import { BookOpenCheck } from 'lucide-react';

export function SessionLoading() {
  return (
    <main className="session-loading relative isolate grid min-h-dvh place-items-center overflow-hidden bg-background px-6 py-12">
      <div aria-hidden="true" className="session-loading-glow pointer-events-none absolute inset-0 -z-10" />
      <div className="w-full max-w-sm text-center">
        <div aria-hidden="true" className="session-loading-mark relative mx-auto mb-8 grid size-32 place-items-center">
          <div className="absolute inset-0 rounded-full border border-primary/10" />
          <div className="session-loading-orbit absolute inset-0 rounded-full border border-transparent border-t-primary/65">
            <span className="absolute left-1/2 top-0 size-2 -translate-x-1/2 -translate-y-1/2 rounded-full bg-academic" />
          </div>
          <div className="session-loading-book relative grid size-20 place-items-center rounded-3xl border border-primary/20 bg-primary/10 text-primary shadow-[0_12px_48px_color-mix(in_srgb,var(--primary)_12%,transparent)]">
            <BookOpenCheck className="size-10" strokeWidth={1.6} />
            <span className="absolute -right-1 -top-1 size-4 rounded-full border-4 border-background bg-academic" />
          </div>
        </div>
        <p className="text-3xl font-bold tracking-tight text-foreground">SQB</p>
        <p className="mt-2 text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">Campus Intelligence</p>
        <div role="status" aria-live="polite" className="session-loading-status mt-9">
          <p className="text-sm font-medium text-foreground">Đang khôi phục phiên đăng nhập…</p>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">Không gian học tập của bạn sắp sẵn sàng.</p>
        </div>
        <div aria-hidden="true" className="session-loading-progress mx-auto mt-5 flex w-fit items-center gap-1.5">
          <span className="session-loading-dot size-1.5 rounded-full bg-primary" />
          <span className="session-loading-dot size-1.5 rounded-full bg-primary [animation-delay:160ms]" />
          <span className="session-loading-dot size-1.5 rounded-full bg-primary [animation-delay:320ms]" />
        </div>
      </div>
    </main>
  );
}
