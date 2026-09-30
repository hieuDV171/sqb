import { ShieldAlert } from "lucide-react";
import { EmptyState } from "@/components/ui/empty-state";
import { Badge } from "@/components/ui/badge";

export function AdminReportsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          Xử Lý Báo Cáo Vi Phạm
        </h1>
        <Badge variant="role">Quản trị viên</Badge>
      </div>
      <EmptyState
        icon={ShieldAlert}
        title="Không có báo cáo vi phạm nào đang chờ xử lý"
        description="Các khiếu nại báo cáo nội dung bài viết, bình luận, hoặc câu hỏi sai quy định cộng đồng sẽ hiển thị tại đây."
      />
    </div>
  );
}
