import { MessageSquare } from "lucide-react";
import { EmptyState } from "@/components/ui/empty-state";

export function MessagesPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          Hộp Thư & Trò Chuyện Trực Tuyến
        </h1>
      </div>
      <EmptyState
        icon={MessageSquare}
        title="Chưa có hội thoại nào được chọn"
        description="Bắt đầu nhắn tin 1-1 hoặc trò chuyện nhóm theo thời gian thực (Realtime WebSocket STOMP)."
      />
    </div>
  );
}
