import { cn } from '@/lib/utils';

export function ActionError({ message, id, className }: {
  message: string | null | undefined;
  id?: string;
  className?: string;
}) {
  if (!message) return null;

  return (
    <p id={id} role="alert" className={cn('text-sm wrap-break-word text-destructive', className)}>
      {message}
    </p>
  );
}
