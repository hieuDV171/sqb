import { ShieldAlert, ArrowLeft, Home } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/useAuthStore";

export function ForbiddenPage() {
  const navigate = useNavigate();
  const { user } = useAuthStore();

  const getHomeRoute = () => {
    if (user?.role === "LECTURER") return "/lecturer/sessions";
    if (user?.role === "ADMIN") return "/admin/users";
    return "/feed";
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100">
      <div className="max-w-md w-full text-center space-y-6 p-8 rounded-3xl bg-white dark:bg-slate-900 shadow-xl border border-slate-200 dark:border-slate-800">
        <div className="mx-auto w-20 h-20 rounded-3xl bg-rose-500/10 flex items-center justify-center text-rose-500 ring-12 ring-rose-500/5">
          <ShieldAlert className="w-10 h-10" />
        </div>

        <div className="space-y-2">
          <span className="text-xs font-bold tracking-widest text-rose-500 uppercase">
            Error 403 • Forbidden
          </span>
          <h1 className="text-2xl font-bold tracking-tight">
            Truy Cập Bị Từ Chối
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Vai trò hiện tại của bạn (
            <span className="font-semibold text-slate-800 dark:text-slate-200">
              {user?.role || "GUEST"}
            </span>
            ) không đủ thẩm quyền để truy cập trang này. Vui lòng liên hệ quản trị viên hoặc quay lại.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-3 justify-center pt-2">
          <Button
            variant="outline"
            onClick={() => navigate(-1)}
            className="gap-2"
          >
            <ArrowLeft className="w-4 h-4" />
            Quay lại
          </Button>
          <Link to={getHomeRoute()}>
            <Button className="w-full sm:w-auto gap-2">
              <Home className="w-4 h-4" />
              Về trang chủ
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
