import { X, Check, ShoppingBag, Shirt } from 'lucide-react';
import { useGamificationStore } from '../stores/useGamificationStore';
import { useAuthStore } from '@/stores/useAuthStore';
import { SqbCoin } from '@/components/common/SqbCoin';
import type { EquippedItemDto } from '../types/gamification.types';

interface FittingRoomPreviewProps {
  currentlyEquipped?: Record<string, EquippedItemDto>;
  onBuy?: (cosmeticId: number) => void;
  onEquip?: (cosmeticId: number) => void;
  isItemOwned?: boolean;
  itemPrice?: number;
  userCoins?: number;
}

export function FittingRoomPreview({
  currentlyEquipped,
  onBuy,
  onEquip,
  isItemOwned,
  itemPrice,
  userCoins = 0,
}: FittingRoomPreviewProps) {
  const { previewCosmeticId, previewAssetUrl, previewCosmeticName, clearPreview } =
    useGamificationStore();
  const { user } = useAuthStore();

  const currentFrameUrl =
    previewAssetUrl || currentlyEquipped?.['AVATAR_FRAME']?.assetUrl;

  const isPreviewing = !!previewCosmeticId;

  return (
    <div className="relative overflow-hidden rounded-3xl bg-linear-to-b from-indigo-50/60 to-purple-50/40 dark:from-slate-800/90 dark:to-indigo-950/40 border border-indigo-200/80 dark:border-indigo-900/60 p-5 sm:p-6 shadow-sm">
      <div className="flex flex-col sm:flex-row items-center gap-6">
        {/* Live Avatar Preview Area */}
        <div className="relative shrink-0">
          <div className="relative w-28 h-28 sm:w-32 sm:h-32 flex items-center justify-center">
            {/* User Avatar */}
            {user?.avatarUrl ? (
              <img
                src={user.avatarUrl}
                alt={user.username || 'User Avatar'}
                className="w-20 h-20 sm:w-24 sm:h-24 rounded-full object-cover shadow-md"
              />
            ) : (
              <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-full bg-linear-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-white text-2xl font-black shadow-md">
                {user?.username?.charAt(0) || 'U'}
              </div>
            )}

            {/* Avatar Frame (Equipped or Previewed) */}
            {currentFrameUrl && (
              <img
                src={currentFrameUrl}
                alt="Avatar Frame"
                className="absolute inset-0 w-full h-full object-contain pointer-events-none drop-shadow-md transition-all duration-300 scale-110"
              />
            )}
          </div>

          {/* Indicator Badge */}
          {isPreviewing && (
            <span className="absolute -bottom-2 left-1/2 -translate-x-1/2 px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-linear-to-r from-amber-500 to-yellow-500 text-white shadow-xs whitespace-nowrap animate-pulse">
              Đang thử đồ
            </span>
          )}
        </div>

        {/* Fitting Details & Action Buttons */}
        <div className="flex-1 text-center sm:text-left space-y-2">
          <div className="flex items-center justify-center sm:justify-start gap-2">
            <span className="p-1.5 rounded-lg bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400">
              <Shirt className="w-4 h-4" />
            </span>
            <h3 className="font-bold text-sm text-slate-800 dark:text-white">
              Phòng Thử Đồ Trực Quan (Live Preview)
            </h3>
          </div>

          {isPreviewing ? (
            <div className="space-y-1">
              <p className="text-base font-black text-indigo-950 dark:text-indigo-200">
                {previewCosmeticName}
              </p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Khung trang trí đang được ướm thử trực tiếp lên ảnh đại diện của bạn.
              </p>
            </div>
          ) : (
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Bấm vào nút <span className="font-bold text-indigo-600 dark:text-indigo-400">"Thử đồ"</span> ở bất kỳ khung đại diện nào bên dưới để ướm thử trước khi mua hoặc trang bị.
            </p>
          )}

          {/* Buttons if previewing */}
          {isPreviewing && (
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2.5 pt-2">
              {isItemOwned ? (
                <button
                  type="button"
                  onClick={() => onEquip && previewCosmeticId && onEquip(previewCosmeticId)}
                  className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold flex items-center gap-1.5 shadow-sm hover:scale-105 active:scale-95 transition-all cursor-pointer"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>Trang Bị Ngay</span>
                </button>
              ) : (
                <button
                  type="button"
                  disabled={typeof itemPrice === 'number' && userCoins < itemPrice}
                  onClick={() => onBuy && previewCosmeticId && onBuy(previewCosmeticId)}
                  className="px-4 py-2 rounded-xl bg-linear-to-r from-amber-500 to-yellow-500 hover:from-amber-400 hover:to-yellow-400 text-indigo-950 text-xs font-black flex items-center gap-1.5 shadow-sm hover:scale-105 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
                >
                  <ShoppingBag className="w-3.5 h-3.5" />
                  <span className="flex items-center gap-1">
                    <span>Mua Ngay</span>
                    {typeof itemPrice === 'number' && (
                      <span className="flex items-center gap-0.5">
                        ({itemPrice.toLocaleString()} <SqbCoin className="w-3.5 h-3.5" />)
                      </span>
                    )}
                  </span>
                </button>
              )}

              <button
                type="button"
                onClick={clearPreview}
                className="px-3 py-2 rounded-xl bg-slate-200/80 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 text-xs font-bold flex items-center gap-1 transition-all cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
                <span>Hủy Thử</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
