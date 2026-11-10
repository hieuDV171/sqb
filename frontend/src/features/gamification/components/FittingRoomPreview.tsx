import type { ReactNode } from 'react';
import { Package, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { AvatarWithFrame } from '@/features/profile/components/AvatarWithFrame';
import { useCurrentUserAvatarName } from '@/features/profile/hooks/useCurrentUserAvatarName';
import { useAuthStore } from '@/stores/useAuthStore';
import type { EquippedItemDto, InventoryItemDto } from '../types/gamification.types';

interface FittingRoomPreviewProps {
  equippedFrame?: EquippedItemDto;
  item?: Pick<InventoryItemDto, 'name' | 'type' | 'assetUrl'>;
  onClear: () => void;
  action?: ReactNode;
}

export function FittingRoomPreview({ equippedFrame, item, onClear, action }: FittingRoomPreviewProps) {
  const user = useAuthStore((state) => state.user);
  const name = useCurrentUserAvatarName(user);
  const previewFrame = !item || item.type === 'AVATAR_FRAME';
  const frameUrl = item ? item.assetUrl : equippedFrame?.assetUrl;

  return (
    <aside aria-labelledby="fitting-room-title" className="space-y-3 rounded-2xl border border-border bg-card p-4">
      <div className="flex items-center justify-between gap-2">
        <h3 id="fitting-room-title" className="font-semibold">Xem trước vật phẩm</h3>
        {item && <Button type="button" size="icon" variant="ghost" aria-label="Đóng xem trước" onClick={onClear}><X aria-hidden="true" /></Button>}
      </div>
      <div className="grid h-36 place-items-center rounded-xl bg-surface-subtle">
        {previewFrame ? (
          <div aria-hidden="true">
            <AvatarWithFrame name={name} avatarUrl={user?.avatarUrl} frameUrl={frameUrl} size="xl" />
          </div>
        ) : item.assetUrl ? (
          <img src={item.assetUrl} alt="" className="h-28 max-w-[85%] object-contain" />
        ) : (
          <Package aria-hidden="true" className="size-9 text-muted-foreground" />
        )}
      </div>
      <p aria-live="polite" className="wrap-break-word text-sm text-muted-foreground">
        {item ? item.name : 'Chọn Xem trước trên một vật phẩm để thử.'}
      </p>
      {action}
    </aside>
  );
}
