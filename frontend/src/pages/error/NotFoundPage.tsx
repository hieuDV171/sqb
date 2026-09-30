import { HelpCircle, ArrowLeft, Home } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100">
      <div className="max-w-md w-full text-center space-y-6 p-8 rounded-3xl bg-white dark:bg-slate-900 shadow-xl border border-slate-200 dark:border-slate-800">
        <div className="mx-auto w-20 h-20 rounded-3xl bg-indigo-500/10 flex items-center justify-center text-indigo-500 ring-12 ring-indigo-500/5">
          <HelpCircle className="w-10 h-10" />
        </div>

        <div className="space-y-2">
          <span className="text-xs font-bold tracking-widest text-indigo-500 uppercase">
            Error 404 • Not Found
          </span>
          <h1 className="text-2xl font-bold tracking-tight">
            Không Tìm Thấy Trang
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Đường dẫn bạn vừa truy cập không tồn tại hoặc đã được di chuyển sang một liên kết khác trên hệ thống SQB.
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
          <Link to="/">
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
