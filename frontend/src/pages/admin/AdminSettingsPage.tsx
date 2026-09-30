import { Settings } from "lucide-react";
import { EmptyState } from "@/components/ui/empty-state";
import { Badge } from "@/components/ui/badge";

export function AdminSettingsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          Cấu Hình Hệ Thống SQB
        </h1>
        <Badge variant="role">Quản trị viên</Badge>
      </div>
      <EmptyState
        icon={Settings}
        title="Thiết lập tham số máy chủ & AI sidecar"
        description="Quản lý ngưỡng tương đồng vector trùng lặp câu hỏi, thời hạn phiên JWT, cấu hình SMTP email và MinIO storage."
      />
    </div>
  );
}
