import { MessageSquarePlus } from 'lucide-react';

const FEEDBACK_EMAIL = 'bangiaohuongmuadong@gmail.com';

export function FeedbackButton() {
  return (
    <a
      href={`mailto:${FEEDBACK_EMAIL}`}
      aria-label="Góp ý qua email"
      data-slot="feedback-button"
      className="fixed right-(--app-floating-right) bottom-[max(1rem,env(safe-area-inset-bottom))] z-30 grid size-(--app-feedback-size) touch-manipulation place-items-center rounded-full border border-border bg-card text-foreground shadow-md transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <MessageSquarePlus className="size-5" aria-hidden="true" />
    </a>
  );
}
