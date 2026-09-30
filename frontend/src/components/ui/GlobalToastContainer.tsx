import { useToastStore, type ToastItem } from '@/stores/useToastStore';
import {
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  Info,
  X,
} from 'lucide-react';

export function GlobalToastContainer() {
  const toasts = useToastStore((state) => state.toasts);
  const removeToast = useToastStore((state) => state.removeToast);

  if (toasts.length === 0) return null;

  return (
    <div
      role="region"
      aria-label="Thông báo hệ thống"
      className="fixed top-5 right-5 z-50 flex flex-col gap-2.5 max-w-sm w-full pointer-events-none px-4 sm:px-0"
    >
      {toasts.map((item) => (
        <ToastCard key={item.id} item={item} onDismiss={() => removeToast(item.id)} />
      ))}
    </div>
  );
}

function ToastCard({
  item,
  onDismiss,
}: {
  item: ToastItem;
  onDismiss: () => void;
}) {
  const config = getToastConfig(item.type);
  const Icon = config.icon;

  return (
    <div
      className={`pointer-events-auto flex items-start gap-3 p-4 rounded-2xl border shadow-xl backdrop-blur-md transition-all duration-200 animate-in fade-in slide-in-from-top-2 ${config.containerClass}`}
    >
      <div className={`p-1 rounded-xl shrink-0 ${config.iconWrapperClass}`}>
        <Icon className="w-5 h-5" />
      </div>

      <div className="flex-1 min-w-0 pt-0.5">
        {item.title && (
          <h5 className="font-bold text-sm text-slate-900 dark:text-slate-100 mb-0.5 leading-snug">
            {item.title}
          </h5>
        )}
        <p className="text-xs text-slate-700 dark:text-slate-300 leading-relaxed break-words">
          {item.message}
        </p>
      </div>

      <button
        type="button"
        onClick={onDismiss}
        className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg hover:bg-black/5 dark:hover:bg-white/10 transition-colors cursor-pointer shrink-0"
        title="Đóng thông báo"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}

function getToastConfig(type: ToastItem['type']) {
  switch (type) {
    case 'success':
      return {
        icon: CheckCircle2,
        containerClass:
          'bg-emerald-50/95 dark:bg-slate-900/95 border-emerald-300/80 dark:border-emerald-800/60 shadow-emerald-500/10',
        iconWrapperClass: 'bg-emerald-100 dark:bg-emerald-950/80 text-emerald-600 dark:text-emerald-400',
      };
    case 'error':
      return {
        icon: AlertCircle,
        containerClass:
          'bg-red-50/95 dark:bg-slate-900/95 border-red-300/80 dark:border-red-800/60 shadow-red-500/10',
        iconWrapperClass: 'bg-red-100 dark:bg-red-950/80 text-red-600 dark:text-red-400',
      };
    case 'warning':
      return {
        icon: AlertTriangle,
        containerClass:
          'bg-amber-50/95 dark:bg-slate-900/95 border-amber-300/80 dark:border-amber-800/60 shadow-amber-500/10',
        iconWrapperClass: 'bg-amber-100 dark:bg-amber-950/80 text-amber-600 dark:text-amber-400',
      };
    case 'info':
    default:
      return {
        icon: Info,
        containerClass:
          'bg-indigo-50/95 dark:bg-slate-900/95 border-indigo-300/80 dark:border-indigo-800/60 shadow-indigo-500/10',
        iconWrapperClass: 'bg-indigo-100 dark:bg-indigo-950/80 text-indigo-600 dark:text-indigo-400',
      };
  }
}
