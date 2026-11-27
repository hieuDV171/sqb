import { AlertCircle, RefreshCw } from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"

export interface ErrorStateProps {
  title?: string
  message?: string
  onRetry?: () => void
  className?: string
}

export function ErrorState({
  title = "Đã xảy ra lỗi tải dữ liệu",
  message = "Không thể kết nối đến máy chủ hoặc dữ liệu không tồn tại. Vui lòng thử lại sau.",
  onRetry,
  className,
}: ErrorStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center p-8 sm:p-12 text-center rounded-2xl border border-rose-200/80 dark:border-rose-900/40 bg-rose-50/40 dark:bg-rose-950/20 backdrop-blur-xs",
        className
      )}
    >
      <div className="w-14 h-14 sm:w-16 sm:h-16 rounded-2xl bg-rose-100 dark:bg-rose-900/50 flex items-center justify-center text-rose-600 dark:text-rose-400 mb-4 shadow-inner ring-8 ring-rose-50/60 dark:ring-rose-950/20">
        <AlertCircle className="w-7 h-7 sm:w-8 sm:h-8" />
      </div>

      <h3 className="text-base sm:text-lg font-semibold text-slate-900 dark:text-slate-100">
        {title}
      </h3>

      <p className="mt-1.5 text-xs sm:text-sm text-slate-600 dark:text-slate-400 max-w-sm leading-relaxed">
        {message}
      </p>

      {onRetry && (
        <div className="mt-5">
          <Button variant="outline" onClick={onRetry} className="gap-2 border-rose-300 dark:border-rose-800 text-rose-700 dark:text-rose-300 hover:bg-rose-100/50 dark:hover:bg-rose-950/50">
            <RefreshCw className="w-4 h-4" />
            Thử lại
          </Button>
        </div>
      )}
    </div>
  )
}
