import { useState } from 'react';
import {
  ShoppingBag,
  Package,
  Check,
  Shirt,
} from 'lucide-react';
import {
  useShop,
  useMyInventory,
  useBuyCosmetic,
  useEquipCosmetic,
  useUnequipCosmetic,
} from '../hooks/useGamification';
import { useGamificationStore } from '../stores/useGamificationStore';
import { SqbCoin } from '@/components/common/SqbCoin';
import { FittingRoomPreview } from './FittingRoomPreview';
import type {
  CosmeticType,
  CosmeticRarity,
  CosmeticSortBy,
} from '../types/gamification.types';
import { getRarityColor } from '../types/gamification.types';

export function ShopInventoryTab() {
  const [subTab, setSubTab] = useState<'shop' | 'inventory'>('shop');
  const [selectedType, setSelectedType] = useState<CosmeticType | undefined>(undefined);
  const [selectedRarity, setSelectedRarity] = useState<CosmeticRarity | undefined>(undefined);
  const [sortBy, setSortBy] = useState<CosmeticSortBy>('NEWEST');

  const { data: shopData, isLoading: isShopLoading } = useShop({
    type: selectedType,
    sortBy,
    limit: 30,
  });

  const { data: inventoryData, isLoading: isInventoryLoading } = useMyInventory({
    type: selectedType,
    rarity: selectedRarity,
    limit: 30,
  });

  const buyMutation = useBuyCosmetic();
  const equipMutation = useEquipCosmetic();
  const unequipMutation = useUnequipCosmetic();

  const { setPreviewCosmetic, previewCosmeticId } = useGamificationStore();

  const userCoins = shopData?.userPoints ?? 0;
  const currentlyEquipped = inventoryData?.currentlyEquipped;

  // Selected item being previewed
  const shopItems = shopData?.items || [];
  const inventoryItems = inventoryData?.inventory || [];

  const previewedShopItem = shopItems.find((i) => i.cosmeticId === previewCosmeticId);
  const previewedInventoryItem = inventoryItems.find((i) => i.cosmeticId === previewCosmeticId);
  const isPreviewedOwned = previewedShopItem?.isOwned || !!previewedInventoryItem;
  const previewedPrice = previewedShopItem?.price;

  const handleBuy = (cosmeticId: number) => {
    buyMutation.mutate(cosmeticId);
  };

  const handleEquip = (cosmeticId: number) => {
    equipMutation.mutate({ cosmeticId });
  };

  const handleUnequip = (cosmeticId: number) => {
    unequipMutation.mutate({ cosmeticId });
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Fitting Room Live Preview Box */}
      <FittingRoomPreview
        currentlyEquipped={currentlyEquipped}
        onBuy={handleBuy}
        onEquip={handleEquip}
        isItemOwned={isPreviewedOwned}
        itemPrice={previewedPrice}
        userCoins={userCoins}
      />

      {/* Sub-tab Navigation (Shop vs My Inventory) */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 p-3 rounded-2xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs">
        <div className="flex items-center gap-1.5 p-1 bg-slate-100 dark:bg-slate-900 rounded-xl">
          <button
            type="button"
            onClick={() => setSubTab('shop')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              subTab === 'shop'
                ? 'bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <ShoppingBag className="w-4 h-4" />
            <span>Cửa Hàng Vật Phẩm</span>
          </button>
          <button
            type="button"
            onClick={() => setSubTab('inventory')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              subTab === 'inventory'
                ? 'bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <Package className="w-4 h-4" />
            <span>Tủ Đồ Cá Nhân</span>
            {inventoryItems.length > 0 && (
              <span className="px-1.5 py-0.2 rounded-full text-[10px] font-black bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400">
                {inventoryItems.length}
              </span>
            )}
          </button>
        </div>

        {/* Filters & Sorting */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Filter by Type */}
          <select
            value={selectedType || ''}
            onChange={(e) => setSelectedType((e.target.value as CosmeticType) || undefined)}
            className="px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs font-semibold text-slate-700 dark:text-slate-300 focus:outline-hidden"
          >
            <option value="">Tất cả phân loại</option>
            <option value="AVATAR_FRAME">Khung Avatar</option>
            <option value="PROFILE_PIN">Huy hiệu cài áo</option>
            <option value="CHAT_BUBBLE">Bong bóng chat</option>
          </select>

          {/* Filter by Rarity (Inventory only) */}
          {subTab === 'inventory' && (
            <select
              value={selectedRarity || ''}
              onChange={(e) => setSelectedRarity((e.target.value as CosmeticRarity) || undefined)}
              className="px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs font-semibold text-slate-700 dark:text-slate-300 focus:outline-hidden"
            >
              <option value="">Tất cả độ hiếm</option>
              <option value="COMMON">Phổ thông</option>
              <option value="RARE">Hiếm</option>
              <option value="EPIC">Sử thi</option>
              <option value="LEGENDARY">Thần thoại</option>
            </select>
          )}

          {/* Sort By (Shop only) */}
          {subTab === 'shop' && (
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as CosmeticSortBy)}
              className="px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs font-semibold text-slate-700 dark:text-slate-300 focus:outline-hidden"
            >
              <option value="NEWEST">Mới nhất</option>
              <option value="PRICE_ASC">Giá: Thấp đến cao</option>
              <option value="PRICE_DESC">Giá: Cao đến thấp</option>
              <option value="RARITY">Độ hiếm</option>
            </select>
          )}
        </div>
      </div>

      {/* Main Content Grid */}
      {subTab === 'shop' ? (
        /* SHOP ITEMS GRID */
        <div>
          {isShopLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <div
                  key={i}
                  className="p-5 rounded-3xl bg-white dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 animate-pulse space-y-4"
                >
                  <div className="w-24 h-24 rounded-2xl bg-slate-200 dark:bg-slate-700 mx-auto" />
                  <div className="h-4 bg-slate-200 dark:bg-slate-700 rounded-sm w-3/4 mx-auto" />
                  <div className="h-8 bg-slate-200 dark:bg-slate-700 rounded-xl" />
                </div>
              ))}
            </div>
          ) : shopItems.length === 0 ? (
            <div className="py-16 text-center text-slate-400 bg-white dark:bg-slate-800/50 rounded-3xl border border-slate-200 dark:border-slate-800">
              <ShoppingBag className="w-12 h-12 mx-auto mb-3 opacity-30" />
              <p className="font-bold text-base text-slate-700 dark:text-slate-300">
                Không tìm thấy vật phẩm nào
              </p>
              <p className="text-xs text-slate-500 mt-1">
                Các sự kiện mở bán vật phẩm mới sẽ sớm được cập nhật!
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-5">
              {shopItems.map((item) => {
                const rarityStyle = getRarityColor(item.rarity);
                const isAffordable = userCoins >= item.price;
                const isCurrentlyPreviewed = previewCosmeticId === item.cosmeticId;

                return (
                  <div
                    key={item.cosmeticId}
                    className={`group relative flex flex-col justify-between p-5 rounded-3xl bg-white dark:bg-slate-800/90 border transition-all duration-300 hover:shadow-lg ${rarityStyle.border} ${rarityStyle.glow} ${
                      isCurrentlyPreviewed ? 'ring-2 ring-indigo-500 ring-offset-2' : ''
                    }`}
                  >
                    {/* Item Top: Rarity Badge & Type */}
                    <div className="flex items-center justify-between gap-2 mb-3">
                      <span
                        className={`px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider ${rarityStyle.badge}`}
                      >
                        {item.rarity}
                      </span>
                      <span className="text-[11px] font-medium text-slate-400">
                        {item.type === 'AVATAR_FRAME'
                          ? 'Khung Avatar'
                          : item.type === 'PROFILE_PIN'
                          ? 'Huy hiệu cài'
                          : 'Bong bóng chat'}
                      </span>
                    </div>

                    {/* Thumbnail Display */}
                    <div className="relative w-28 h-28 mx-auto my-3 flex items-center justify-center">
                      {item.assetUrl ? (
                        <img
                          src={item.assetUrl}
                          alt={item.name}
                          className="w-full h-full object-contain drop-shadow-md group-hover:scale-110 transition-transform duration-300"
                        />
                      ) : (
                        <div className="w-20 h-20 rounded-2xl bg-indigo-50 dark:bg-indigo-950 flex items-center justify-center text-3xl">
                          🎁
                        </div>
                      )}
                    </div>

                    {/* Name & Description */}
                    <div className="text-center space-y-1 mb-4">
                      <h4 className="font-bold text-sm text-slate-900 dark:text-white line-clamp-1">
                        {item.name}
                      </h4>
                      <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-2">
                        {item.description}
                      </p>
                    </div>

                    {/* Price & Action Buttons */}
                    <div className="space-y-2 pt-2 border-t border-slate-100 dark:border-slate-700/60">
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-slate-400">Giá bán:</span>
                        <span className="font-mono font-black text-amber-500 text-sm flex items-center gap-1.5">
                          <SqbCoin className="w-4 h-4" />
                          <span>{item.price.toLocaleString()}</span>
                        </span>
                      </div>

                      <div className="grid grid-cols-2 gap-2">
                        {/* Preview button */}
                        <button
                          type="button"
                          onClick={() =>
                            setPreviewCosmetic(item.cosmeticId, item.assetUrl || null, item.name)
                          }
                          className={`py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition-all cursor-pointer ${
                            isCurrentlyPreviewed
                              ? 'bg-indigo-600 text-white'
                              : 'bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200'
                          }`}
                        >
                          <Shirt className="w-3.5 h-3.5" />
                          <span>{isCurrentlyPreviewed ? 'Đang thử' : 'Ướm thử'}</span>
                        </button>

                        {/* Buy or Owned button */}
                        {item.isOwned ? (
                          <div className="py-2 px-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 font-bold text-xs flex items-center justify-center gap-1">
                            <Check className="w-3.5 h-3.5" />
                            <span>Đã có</span>
                          </div>
                        ) : (
                          <button
                            type="button"
                            disabled={!isAffordable || buyMutation.isPending}
                            onClick={() => handleBuy(item.cosmeticId)}
                            className="py-2 px-3 rounded-xl bg-linear-to-r from-amber-400 to-yellow-500 hover:from-amber-300 hover:to-yellow-400 text-indigo-950 font-black text-xs flex items-center justify-center gap-1 shadow-xs hover:scale-105 active:scale-95 transition-all cursor-pointer disabled:opacity-40"
                          >
                            <ShoppingBag className="w-3.5 h-3.5" />
                            <span>Mua ngay</span>
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ) : (
        /* MY INVENTORY GRID */
        <div>
          {isInventoryLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
              {Array.from({ length: 6 }).map((_, i) => (
                <div
                  key={i}
                  className="p-5 rounded-3xl bg-white dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 animate-pulse space-y-4"
                >
                  <div className="w-24 h-24 rounded-2xl bg-slate-200 dark:bg-slate-700 mx-auto" />
                  <div className="h-4 bg-slate-200 dark:bg-slate-700 rounded-sm w-3/4 mx-auto" />
                </div>
              ))}
            </div>
          ) : inventoryItems.length === 0 ? (
            <div className="py-16 text-center text-slate-400 bg-white dark:bg-slate-800/50 rounded-3xl border border-slate-200 dark:border-slate-800">
              <Package className="w-12 h-12 mx-auto mb-3 opacity-30" />
              <p className="font-bold text-base text-slate-700 dark:text-slate-300">
                Tủ đồ của bạn đang trống
              </p>
              <p className="text-xs text-slate-500 mt-1">
                Hãy ghé qua Cửa Hàng Vật Phẩm để chọn cho mình những khung avatar thật ngầu nhé!
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-5">
              {inventoryItems.map((item) => {
                const rarityStyle = getRarityColor(item.rarity);
                const isEquipped =
                  currentlyEquipped?.[item.type]?.cosmeticId === item.cosmeticId;
                const isCurrentlyPreviewed = previewCosmeticId === item.cosmeticId;

                return (
                  <div
                    key={item.cosmeticId}
                    className={`group relative flex flex-col justify-between p-5 rounded-3xl bg-white dark:bg-slate-800/90 border transition-all duration-300 hover:shadow-lg ${rarityStyle.border} ${rarityStyle.glow} ${
                      isEquipped ? 'ring-2 ring-emerald-500 ring-offset-2' : ''
                    }`}
                  >
                    {/* Top Status */}
                    <div className="flex items-center justify-between gap-2 mb-3">
                      <span
                        className={`px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider ${rarityStyle.badge}`}
                      >
                        {item.rarity}
                      </span>
                      {isEquipped && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400 flex items-center gap-1">
                          <Check className="w-3 h-3" />
                          Đang dùng
                        </span>
                      )}
                    </div>

                    {/* Thumbnail */}
                    <div className="relative w-28 h-28 mx-auto my-3 flex items-center justify-center">
                      {item.assetUrl ? (
                        <img
                          src={item.assetUrl}
                          alt={item.name}
                          className="w-full h-full object-contain drop-shadow-md group-hover:scale-110 transition-transform duration-300"
                        />
                      ) : (
                        <div className="w-20 h-20 rounded-2xl bg-indigo-50 dark:bg-indigo-950 flex items-center justify-center text-3xl">
                          🎁
                        </div>
                      )}
                    </div>

                    {/* Info */}
                    <div className="text-center space-y-1 mb-4">
                      <h4 className="font-bold text-sm text-slate-900 dark:text-white line-clamp-1">
                        {item.name}
                      </h4>
                      <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-2">
                        {item.description}
                      </p>
                    </div>

                    {/* Action buttons */}
                    <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-100 dark:border-slate-700/60">
                      <button
                        type="button"
                        onClick={() =>
                          setPreviewCosmetic(item.cosmeticId, item.assetUrl || null, item.name)
                        }
                        className={`py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1 transition-all cursor-pointer ${
                          isCurrentlyPreviewed
                            ? 'bg-indigo-600 text-white'
                            : 'bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200'
                        }`}
                      >
                        <Shirt className="w-3.5 h-3.5" />
                        <span>Thử đồ</span>
                      </button>

                      {isEquipped ? (
                        <button
                          type="button"
                          disabled={unequipMutation.isPending}
                          onClick={() => handleUnequip(item.cosmeticId)}
                          className="py-2 px-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 hover:bg-rose-100 text-rose-600 dark:text-rose-400 font-bold text-xs flex items-center justify-center gap-1 transition-colors cursor-pointer"
                        >
                          <span>Tháo gỡ</span>
                        </button>
                      ) : (
                        <button
                          type="button"
                          disabled={equipMutation.isPending}
                          onClick={() => handleEquip(item.cosmeticId)}
                          className="py-2 px-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center justify-center gap-1 shadow-sm hover:scale-105 active:scale-95 transition-all cursor-pointer"
                        >
                          <Check className="w-3.5 h-3.5" />
                          <span>Trang bị</span>
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
