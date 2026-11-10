import { ShieldAlert } from 'lucide-react';
import { EmptyState } from '@/components/ui/empty-state';
import { AdminPageHeader } from './AdminPageHeader';

export function AdminReportsPage() {
  return (
    <div className="mx-auto max-w-6xl space-y-4 pb-8">
      <AdminPageHeader icon={ShieldAlert} title="Báo cáo vi phạm"
        description="Khu vực quản trị dành cho các báo cáo nội dung do người dùng gửi." />
      <EmptyState icon={ShieldAlert} title="Danh sách báo cáo chưa khả dụng"
        description="Người dùng vẫn có thể gửi báo cáo. Chức năng xem và xử lý báo cáo dành cho quản trị viên đang được chuẩn bị."
        className="border-border bg-card dark:border-border dark:bg-card" />
    </div>
  );
}
