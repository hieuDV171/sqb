import { InfiniteListFooter } from '@/components/ui/infinite-list-footer';
import { useRef, useState } from 'react';
import { AlertDialog } from '@base-ui/react/alert-dialog';
import { Check, Eye, Loader2, Package, ShoppingBag } from 'lucide-react';
import { SqbCoin } from '@/components/common/SqbCoin';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { useBuyCosmetic, useEquipCosmetic, useMyInventory, useShopPages, useUnequipCosmetic } from '../hooks/useGamification';
import type { CosmeticType, CosmeticRarity, CosmeticSortBy, InventoryItemDto, ShopItemDto } from '../types/gamification.types';
import { getRarityColor } from '../types/gamification.types';
import { FittingRoomPreview } from './FittingRoomPreview';
import { FilterSelect } from '@/components/ui/filter-select';

const typeLabels: Record<CosmeticType, string> = {
  AVATAR_FRAME: 'Khung ảnh đại diện',
  CHAT_BUBBLE: 'Bong bóng chat',
  PROFILE_PIN: 'Huy hiệu cài',
  BADGE_EFFECT: 'Hiệu ứng huy hiệu',
};
const rarityLabels: Record<CosmeticRarity, string> = {
  COMMON: 'Thường', RARE: 'Hiếm', EPIC: 'Sử thi', LEGENDARY: 'Huyền thoại',
};
const types = [
  { value: '', label: 'Tất cả loại' },
  { value: 'AVATAR_FRAME', label: typeLabels.AVATAR_FRAME },
  { value: 'CHAT_BUBBLE', label: typeLabels.CHAT_BUBBLE },
];
const rarities = [
  { value: '', label: 'Tất cả độ hiếm' },
  ...Object.entries(rarityLabels).map(([value, label]) => ({ value, label })),
];
const sorts = [
  { value: 'NEWEST', label: 'Mới nhất' },
  { value: 'PRICE_ASC', label: 'Giá thấp đến cao' },
  { value: 'PRICE_DESC', label: 'Giá cao đến thấp' },
  { value: 'RARITY', label: 'Độ hiếm' },
];

type CosmeticItem = ShopItemDto | InventoryItemDto;

export function ShopInventoryTab() {
  const [view, setView] = useState<'shop' | 'inventory'>('shop');
  const [type, setType] = useState<CosmeticType>();
  const [rarity, setRarity] = useState<CosmeticRarity>();
  const [sort, setSort] = useState<CosmeticSortBy>('NEWEST');
  const [previewId, setPreviewId] = useState<number>();
  const [purchase, setPurchase] = useState<ShopItemDto | null>(null);
  const previewRef = useRef<HTMLDivElement>(null);
  const isShop = view === 'shop';
  const shop = useShopPages({ type, sortBy: sort, limit: 30 }, isShop);
  const inventory = useMyInventory({ type, rarity, limit: 30 }, true);
  const buy = useBuyCosmetic();
  const equip = useEquipCosmetic();
  const unequip = useUnequipCosmetic();

  const query = isShop ? shop : inventory;
  const items = (isShop ? shop.data?.pages.flatMap((page) => page.items) : inventory.data?.pages.flatMap((page) => page.inventory)) ?? [];
  const equipped = inventory.data?.pages[0]?.currentlyEquipped;
  const coins = shop.data?.pages[0]?.userPoints;
  const preview = items.find((item) => item.cosmeticId === previewId);
  const busy = buy.isPending || equip.isPending || unequip.isPending;
  const canConfirm = purchase !== null && !shop.isLoadingError && coins !== undefined && coins >= purchase.price;

  function itemAction(item: CosmeticItem) {
    if ('price' in item) {
      if (item.isOwned) return <span className="inline-flex min-h-11 items-center gap-1.5 rounded-xl bg-success-soft px-3 text-sm font-medium text-success">
        <Check aria-hidden="true" className="size-4" /> Đã sở hữu
      </span>;
      const affordable = coins !== undefined && coins >= item.price;
      return <Button type="button" className="min-h-11" disabled={busy || !affordable} onClick={() => { buy.reset(); setPurchase(item); }}>
        {affordable ? 'Mua' : 'Chưa đủ xu'}
      </Button>;
    }
    const isEquipped = equipped?.[item.type]?.cosmeticId === item.cosmeticId;
    const changing = (equip.isPending && equip.variables?.cosmeticId === item.cosmeticId)
      || (unequip.isPending && unequip.variables?.cosmeticId === item.cosmeticId);
    return <Button type="button" variant={isEquipped ? 'outline' : 'default'} className="min-h-11" disabled={busy}
      onClick={() => {
        if (isEquipped) unequip.mutate({ cosmeticId: item.cosmeticId });
        else equip.mutate({ cosmeticId: item.cosmeticId });
      }}>
      {changing && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
      {isEquipped ? 'Tháo' : 'Trang bị'}
    </Button>;
  }

  return (
    <section aria-labelledby="cosmetic-title" className="space-y-4">
      <div className="rounded-2xl border border-border bg-card p-4 sm:p-5">
        <h2 id="cosmetic-title" className="text-xl font-bold">Cửa hàng & tủ đồ</h2>
        <p className="mt-1 text-sm text-muted-foreground">Xem trước, mua bằng xu SQB và trang bị vật phẩm của bạn.</p>
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <div role="group" aria-label="Danh sách vật phẩm" className="flex flex-wrap gap-1 rounded-xl bg-surface-subtle p-1">
            <Button type="button" aria-pressed={isShop} variant={isShop ? 'secondary' : 'ghost'} className="min-h-11"
              onClick={() => { setView('shop'); setPreviewId(undefined); }}>
              <ShoppingBag aria-hidden="true" /> Cửa hàng
            </Button>
            <Button type="button" aria-pressed={!isShop} variant={isShop ? 'ghost' : 'secondary'} className="min-h-11"
              onClick={() => { setView('inventory'); setPreviewId(undefined); }}>
              <Package aria-hidden="true" /> Tủ đồ
            </Button>
          </div>
          <div className="flex w-full flex-wrap gap-3 sm:w-auto">
            <FilterSelect label="Loại vật phẩm" inlineLabel value={type ?? ''} options={types}
              onChange={(value) => { setType(value ? value as CosmeticType : undefined); setPreviewId(undefined); }} />
            {isShop
              ? <FilterSelect label="Sắp xếp" inlineLabel value={sort} options={sorts}
                  onChange={(value) => { setSort(value as CosmeticSortBy); setPreviewId(undefined); }} />
              : <FilterSelect label="Độ hiếm" inlineLabel value={rarity ?? ''} options={rarities}
                  onChange={(value) => { setRarity(value ? value as CosmeticRarity : undefined); setPreviewId(undefined); }} />}
          </div>
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_17rem]">
        <div ref={previewRef} tabIndex={-1} className="min-w-0 self-start rounded-2xl focus-visible:outline-2 focus-visible:outline-ring xl:order-2">
          <FittingRoomPreview equippedFrame={equipped?.AVATAR_FRAME} item={preview}
            onClear={() => setPreviewId(undefined)} action={preview && itemAction(preview)} />
        </div>
        <div className="min-w-0 xl:order-1">
          {query.isPending ? (
            <Skeleton aria-label="Đang tải vật phẩm" className="h-52 rounded-2xl" />
          ) : query.isLoadingError ? (
            <ErrorState title={isShop ? 'Không thể tải cửa hàng' : 'Không thể tải tủ đồ'} onRetry={() => void query.refetch()} />
          ) : items.length === 0 ? (
            <EmptyState icon={Package} title={isShop ? 'Không có vật phẩm phù hợp' : 'Không có vật phẩm trong tủ đồ này'}
              description={isShop ? 'Hãy thử chọn loại vật phẩm khác.' : 'Bạn có thể đổi bộ lọc hoặc xem vật phẩm tại cửa hàng.'}
              actionLabel={isShop ? undefined : 'Đến cửa hàng'} onAction={isShop ? undefined : () => { setView('shop'); setType(undefined); }} />
          ) : (
            <ul className="grid gap-3 sm:grid-cols-2 2xl:grid-cols-3">
              {items.map((item) => {
                const isEquipped = equipped?.[item.type]?.cosmeticId === item.cosmeticId;
                const rarityStyle = item.rarity === 'LEGENDARY' ? 'bg-reward-soft text-reward' : getRarityColor(item.rarity).badge;
                return (
                  <li key={item.cosmeticId} className="flex min-w-0 flex-col gap-3 rounded-2xl border border-border bg-card p-4">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${rarityStyle}`}>{rarityLabels[item.rarity]}</span>
                      {isEquipped && <span className="text-xs font-semibold text-success">Đang trang bị</span>}
                    </div>
                    <div className="grid h-32 place-items-center rounded-xl bg-surface-subtle">
                      {item.assetUrl ? <img src={item.assetUrl} alt="" loading="lazy" className="h-24 max-w-[85%] object-contain" />
                        : <Package aria-hidden="true" className="size-9 text-muted-foreground" />}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-xs text-muted-foreground">{typeLabels[item.type]}</p>
                      <h3 className="wrap-break-word font-semibold">{item.name}</h3>
                      <p className="mt-1 wrap-break-word text-sm text-muted-foreground">{item.description}</p>
                    </div>
                    {'price' in item && <p className="flex items-center gap-1.5 text-sm font-semibold text-reward">
                      <SqbCoin className="size-4" /> {item.price.toLocaleString('vi-VN')} xu
                    </p>}
                    <div className="flex flex-wrap gap-2 border-t border-border pt-3">
                      <Button type="button" variant="outline" aria-pressed={previewId === item.cosmeticId} className="min-h-11"
                        onClick={() => {
                          setPreviewId(item.cosmeticId);
                          previewRef.current?.focus({ preventScroll: true });
                          previewRef.current?.scrollIntoView({ block: 'nearest' });
                        }}>
                        <Eye aria-hidden="true" /> Xem trước
                      </Button>
                      {itemAction(item)}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
          <InfiniteListFooter query={query} label="Tải thêm vật phẩm" />
        </div>
      </div>

      <AlertDialog.Root open={purchase !== null} onOpenChange={(open) => { if (!open && !buy.isPending) setPurchase(null); }}>
        {purchase && (
          <AlertDialog.Portal>
            <AlertDialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
            <AlertDialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-md -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
              <AlertDialog.Title className="text-xl font-semibold">Xác nhận mua vật phẩm</AlertDialog.Title>
              <AlertDialog.Description className="text-sm leading-relaxed text-muted-foreground">
                Bạn sẽ dùng {purchase.price.toLocaleString('vi-VN')} xu SQB để mua <strong className="text-foreground">{purchase.name}</strong>.
                {canConfirm && coins !== undefined && ` Số dư sau khi mua là ${(coins - purchase.price).toLocaleString('vi-VN')} xu.`}
              </AlertDialog.Description>
              {buy.isError && <p role="alert" className="text-sm text-destructive">Không thể mua vật phẩm. Vui lòng thử lại.</p>}
              <div className="flex flex-wrap justify-end gap-2">
                <AlertDialog.Close render={<Button type="button" variant="outline" className="min-h-11" disabled={buy.isPending} />}>Hủy</AlertDialog.Close>
                <Button type="button" className="min-h-11" disabled={buy.isPending || !canConfirm}
                  onClick={() => buy.mutate(purchase.cosmeticId, { onSuccess: () => setPurchase(null) })}>
                  {buy.isPending && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
                  {buy.isPending ? 'Đang mua…' : 'Xác nhận mua'}
                </Button>
              </div>
            </AlertDialog.Popup>
          </AlertDialog.Portal>
        )}
      </AlertDialog.Root>
    </section>
  );
}
