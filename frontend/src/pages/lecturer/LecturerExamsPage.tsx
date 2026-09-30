import { FileText } from "lucide-react";
import { EmptyState } from "@/components/ui/empty-state";
import { Badge } from "@/components/ui/badge";

export function LecturerExamsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          Quản Lý & Xuất Đề Thi
        </h1>
        <Badge variant="role">Giảng viên</Badge>
      </div>
      <EmptyState
        icon={FileText}
        title="Tính năng tạo đề thi ngẫu nhiên & xuất file PDF/Word"
        description="Sinh đề thi tự động theo ma trận tỷ lệ nhận biết, thông hiểu, vận dụng và xuất bản bản in đề thi chính thức."
      />
    </div>
  );
}
