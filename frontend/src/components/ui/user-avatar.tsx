import { useState } from 'react';
import { cn } from '@/lib/utils';

interface UserAvatarProps {
  name?: string | null;
  avatarUrl?: string | null;
  className?: string;
  decorative?: boolean;
}

export function UserAvatar({ name, avatarUrl, className, decorative }: UserAvatarProps) {
  const [failedUrl, setFailedUrl] = useState<string>();
  const displayName = name?.trim() || 'Người dùng';
  const [initial] = displayName.normalize();
  const src = avatarUrl?.trim();

  return (
    <span
      className={cn('grid size-10 shrink-0 place-items-center overflow-hidden rounded-full bg-primary text-sm font-bold text-primary-foreground', className)}
      role={decorative ? undefined : 'img'}
      aria-label={decorative ? undefined : displayName}
      aria-hidden={decorative || undefined}
    >
      {src && src !== failedUrl ? (
        <img src={src} alt="" className="size-full object-cover" loading="lazy" onError={() => setFailedUrl(src)} />
      ) : (
        initial.toLocaleUpperCase('vi-VN')
      )}
    </span>
  );
}
