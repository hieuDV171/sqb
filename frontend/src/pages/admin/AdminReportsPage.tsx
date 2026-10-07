import { useState } from 'react';
import {
  ShieldAlert,
  AlertTriangle,
  UserX,
  ShieldCheck,
  Clock,
  ExternalLink,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { EmptyState } from '@/components/ui/empty-state';
import { Link } from 'react-router-dom';

export function AdminReportsPage() {
  const [filterStatus, setFilterStatus] = useState<'PENDING' | 'RESOLVED'>('PENDING');

  // Static violation categories summary
  const summaryCards = [
    {
      title: 'Báo cáo chờ xử lý',
      count: 0,
      icon: Clock,
      color: 'text-amber-600 bg-amber-50 dark:bg-amber-950/40 border-amber-200 dark:border-amber-800/60',
    },
    {
      title: 'Vi phạm Spam / Rác',
      count: 0,
      icon: AlertTriangle,
      color: 'text-blue-600 bg-blue-50 dark:bg-blue-950/40 border-blue-200 dark:border-blue-800/60',
    },
    {
      title: 'Xúc phạm & Quấy rối',
      count: 0,
      icon: UserX,
      color: 'text-rose-600 bg-rose-50 dark:bg-rose-950/40 border-rose-200 dark:border-rose-800/60',
    },
    {
      title: 'Đã giải quyết',
      count: 14,
      icon: ShieldCheck,
      color: 'text-emerald-600 bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800/60',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
              Xử Lý Báo Cáo Vi Phạm
            </h1>
            <Badge variant="role">Quản trị viên</Badge>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Tiếp nhận và xử lý các đơn khiếu nại báo cáo hành vi người dùng, bài viết rác hoặc câu hỏi sai quy định.
          </p>
        </div>

        <Link
          to="/admin/users"
          className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 transition-colors shadow-xs"
        >
          <span>Quản lý tài khoản</span>
          <ExternalLink className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* Overview Stat Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {summaryCards.map((card, idx) => {
          const Icon = card.icon;
          return (
            <div
              key={idx}
              className={`p-4 rounded-2xl border ${card.color} transition-all flex items-center justify-between`}
            >
              <div>
                <p className="text-xs font-medium text-slate-500 dark:text-slate-400">
                  {card.title}
                </p>
                <p className="text-2xl font-extrabold mt-1 text-slate-900 dark:text-slate-100">
                  {card.count}
                </p>
              </div>
              <div className="p-2.5 rounded-xl bg-white/60 dark:bg-slate-900/60 shadow-xs">
                <Icon className="w-5 h-5" />
              </div>
            </div>
          );
        })}
      </div>

      {/* Policy Standards Notice */}
      <div className="p-4 sm:p-5 rounded-2xl bg-indigo-50/50 dark:bg-indigo-950/20 border border-indigo-100 dark:border-indigo-900/40">
        <h3 className="text-xs font-bold uppercase tracking-wider text-indigo-700 dark:text-indigo-400 mb-1.5">
          Quy trình xử lý vi phạm tiêu chuẩn cộng đồng
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs text-slate-600 dark:text-slate-300 mt-2">
          <div className="p-3 rounded-xl bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800">
            <span className="font-bold text-amber-600 block mb-1">Mức 1: Nhắc nhở & Ẩn</span>
            Áp dụng cho bài viết đăng nhầm chuyên mục hoặc câu hỏi chưa chuẩn hóa. Nội dung sẽ được ẩn khỏi bảng tin công khai.
          </div>
          <div className="p-3 rounded-xl bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800">
            <span className="font-bold text-rose-500 block mb-1">Mức 2: Trừ điểm & Khóa đăng</span>
            Áp dụng cho hành vi spam liên tục hoặc gửi câu hỏi rác phá hoại ngân hàng đề. Khóa quyền đăng bài 7 ngày và trừ 50 XP.
          </div>
          <div className="p-3 rounded-xl bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800">
            <span className="font-bold text-rose-700 block mb-1">Mức 3: Khóa tài khoản vĩnh viễn</span>
            Áp dụng cho xúc phạm danh dự nghiêm trọng, ngôn từ thù địch hoặc phát tán tài liệu thi bảo mật trái phép.
          </div>
        </div>
      </div>

      {/* Filter Tabs & Content */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm space-y-4">
        <div className="flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 pb-3">
          <button
            type="button"
            onClick={() => setFilterStatus('PENDING')}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition-all cursor-pointer ${
              filterStatus === 'PENDING'
                ? 'bg-rose-600 text-white shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            Đang chờ xử lý (0)
          </button>
          <button
            type="button"
            onClick={() => setFilterStatus('RESOLVED')}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition-all cursor-pointer ${
              filterStatus === 'RESOLVED'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            Đã xử lý xong (14)
          </button>
        </div>

        {filterStatus === 'PENDING' ? (
          <EmptyState
            icon={ShieldAlert}
            title="Không có báo cáo vi phạm nào đang chờ xử lý"
            description="Môi trường học thuật hiện đang lành mạnh. Khi sinh viên hoặc giảng viên gửi đơn khiếu nại qua nút 'Báo cáo vi phạm', đơn sẽ xuất hiện tại đây để xem xét."
          />
        ) : (
          <div className="py-8 text-center text-xs text-slate-500 dark:text-slate-400">
            14 báo cáo trước đó đã được xử lý và lưu trữ vào nhật ký kiểm toán quản trị.
          </div>
        )}
      </div>
    </div>
  );
}
