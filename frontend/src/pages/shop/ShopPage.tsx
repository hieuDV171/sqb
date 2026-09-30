import { ShoppingBag } from "lucide-react";
import { EmptyState } from "@/components/ui/empty-state";

export function ShopPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          Cửa Hàng Vật Phẩm & Khung Avatar
        </h1>
      </div>
      <EmptyState
        icon={ShoppingBag}
        title="Cửa hàng phần thưởng Gamification"
        description="Sử dụng xu tích lũy từ hoạt động đóng góp và luyện tập câu hỏi để mua khung đại diện (Avatar Frames) và huy hiệu vinh danh (Profile Pins)."
      />
    </div>
  );
}
