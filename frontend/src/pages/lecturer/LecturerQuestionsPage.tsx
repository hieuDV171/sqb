import { BookOpen } from "lucide-react";
import { EmptyState } from "@/components/ui/empty-state";
import { Badge } from "@/components/ui/badge";

export function LecturerQuestionsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          Ngân Hàng Câu Hỏi Môn Học
        </h1>
        <Badge variant="role">Giảng viên</Badge>
      </div>
      <EmptyState
        icon={BookOpen}
        title="Kho ngân hàng câu hỏi chính thức"
        description="Tra cứu, phân loại độ khó (Bloom), và quản lý kho câu hỏi đã qua thẩm định của bộ môn."
      />
    </div>
  );
}
