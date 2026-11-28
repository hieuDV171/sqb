import { cn } from '@/lib/utils';

export function ActionError({ message, id, className, as: Tag = 'p' }: {
  message: string | null | undefined;
  id?: string;
  className?: string;
  as?: 'p' | 'span';
}) {
  if (!message) return null;

  return (
    <Tag id={id} role="alert" className={cn('text-sm wrap-break-word text-destructive', className)}>
      {message}
    </Tag>
  );
}
