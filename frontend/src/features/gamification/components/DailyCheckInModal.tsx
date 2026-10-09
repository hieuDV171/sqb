import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X, Flame, Check, Gift, Sparkles } from 'lucide-react';
import { useGamificationStore } from '../stores/useGamificationStore';
import { useDailyCheckIn } from '../hooks/useGamification';
import { SqbCoin } from '@/components/common/SqbCoin';

interface DailyCheckInModalProps {
  currentStreak?: number;
  hasCheckedInToday?: boolean;
}

export function DailyCheckInModal({
  currentStreak = 0,
  hasCheckedInToday = false,
}: DailyCheckInModalProps) {
  const { isCheckInModalOpen, setCheckInModalOpen } = useGamificationStore();
  const checkInMutation = useDailyCheckIn();
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Particle Confetti Launcher
  const triggerConfetti = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    const particles: Array<{
      x: number;
      y: number;
      vx: number;
      vy: number;
      size: number;
      color: string;
      alpha: number;
    }> = [];

    const colors = ['#f59e0b', '#ec4899', '#6366f1', '#10b981', '#3b82f6', '#fbbf24'];

    for (let i = 0; i < 90; i++) {
      particles.push({
        x: canvas.width / 2,
        y: canvas.height / 2 - 50,
        vx: (Math.random() - 0.5) * 12,
        vy: (Math.random() - 0.8) * 14,
        size: Math.random() * 6 + 4,
        color: colors[Math.floor(Math.random() * colors.length)],
        alpha: 1,
      });
    }

    let animationId: number;
    const render = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      let alive = false;

      particles.forEach((p) => {
        p.x += p.vx;
        p.y += p.vy;
        p.vy += 0.25; // gravity
        p.alpha -= 0.012; // fade

        if (p.alpha > 0) {
          alive = true;
          ctx.save();
          ctx.globalAlpha = p.alpha;
          ctx.fillStyle = p.color;
          ctx.beginPath();
          ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore();
        }
      });

      if (alive) {
        animationId = requestAnimationFrame(render);
      }
    };

    render();
    return () => cancelAnimationFrame(animationId);
  };

  const handleCheckIn = () => {
    checkInMutation.mutate(undefined, {
      onSuccess: () => {
        triggerConfetti();
      },
    });
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isCheckInModalOpen) {
        setCheckInModalOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isCheckInModalOpen, setCheckInModalOpen]);

  if (!isCheckInModalOpen) return null;

  const modalContent = (
    <div
      className="fixed inset-0 z-[999] flex items-center justify-center p-4 bg-black/65 backdrop-blur-sm animate-in fade-in duration-200"
      onClick={() => setCheckInModalOpen(false)}
    >
      <canvas
        ref={canvasRef}
        className="fixed inset-0 pointer-events-none z-[1000] w-full h-full"
      />

      <div
        className="relative w-full max-w-lg overflow-hidden rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl p-6 sm:p-8 space-y-6 animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button */}
        <button
          type="button"
          onClick={() => setCheckInModalOpen(false)}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-500 mb-1">
            <Flame className="w-8 h-8 fill-amber-500 animate-pulse" />
          </div>
          <h2 className="text-2xl font-black tracking-tight text-slate-900 dark:text-white">
            Điểm Danh Chuyên Cần 7 Ngày
          </h2>
          <p className="text-sm text-slate-500 dark:text-slate-400 max-w-sm mx-auto flex items-center justify-center flex-wrap gap-1">
            <span>Duy trì chuỗi điểm danh hằng ngày để tích lũy xu SQB</span>
            <SqbCoin className="w-4 h-4" />
            <span>và mở khóa Huy hiệu Chiến Binh Điểm Danh!</span>
          </p>
        </div>

        {/* 7-Day Calendar Grid */}
        <div className="grid grid-cols-4 sm:grid-cols-7 gap-2.5">
          {Array.from({ length: 7 }, (_, i) => {
            const dayNum = i + 1;
            // Số ngày đã tích lũy hoàn tất trong chu kỳ 7 ngày hiện tại
            const completedDaysInCycle = hasCheckedInToday
              ? (currentStreak % 7 === 0 && currentStreak > 0 ? 7 : currentStreak % 7)
              : (currentStreak % 7);

            const isDayPast = dayNum <= completedDaysInCycle;
            const isDayToday = hasCheckedInToday
              ? dayNum === completedDaysInCycle
              : dayNum === completedDaysInCycle + 1;
            const isGrandPrize = dayNum === 7;

            return (
              <div
                key={dayNum}
                className={`relative flex flex-col items-center justify-between p-3 rounded-2xl border text-center transition-all ${
                  isGrandPrize ? 'col-span-2 sm:col-span-1' : ''
                } ${
                  isDayPast
                    ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-300 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300'
                    : isDayToday
                    ? 'bg-amber-50 dark:bg-amber-950/50 border-amber-400 dark:border-amber-600 ring-2 ring-amber-400/40 text-amber-800 dark:text-amber-200 scale-105 shadow-md'
                    : 'bg-slate-50 dark:bg-slate-800/50 border-slate-200 dark:border-slate-800 text-slate-400 dark:text-slate-500'
                }`}
              >
                <span className="text-[11px] font-bold uppercase tracking-wider mb-1">
                  Ngày {dayNum}
                </span>

                <div className="my-1.5 flex items-center justify-center">
                  {isDayPast ? (
                    <div className="w-8 h-8 rounded-full bg-emerald-500 text-white flex items-center justify-center shadow-xs">
                      <Check className="w-4 h-4 stroke-[3]" />
                    </div>
                  ) : isGrandPrize ? (
                    <div className="w-9 h-9 rounded-2xl bg-linear-to-tr from-amber-400 to-yellow-300 text-amber-950 flex items-center justify-center shadow-md animate-bounce">
                      <Gift className="w-5 h-5" />
                    </div>
                  ) : (
                    <SqbCoin className="w-6 h-6" />
                  )}
                </div>

                <div
                  className={`text-xs font-black font-mono mt-1 flex items-center justify-center gap-0.5 ${
                    isDayPast
                      ? 'text-emerald-600 dark:text-emerald-400'
                      : isGrandPrize
                      ? 'text-amber-600 dark:text-amber-300 font-bold'
                      : 'text-slate-600 dark:text-slate-300'
                  }`}
                >
                  {isDayPast ? (
                    <span className="text-[11px] font-bold text-emerald-600 dark:text-emerald-400">
                      Đã nhận
                    </span>
                  ) : (
                    <>
                      <span>{isGrandPrize ? '+5' : '+1'}</span>
                      <SqbCoin className="w-3.5 h-3.5" />
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Streak Info Callout */}
        <div className="p-4 rounded-2xl bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-900/60 flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-indigo-600 text-white shrink-0 shadow-xs">
            <Sparkles className="w-5 h-5" />
          </div>
          <div className="text-xs space-y-0.5">
            <p className="font-bold text-slate-800 dark:text-slate-200">
              Chuỗi hiện tại: <span className="text-indigo-600 dark:text-indigo-400 font-black">{currentStreak} ngày liên tiếp</span>
            </p>
            <p className="text-slate-500 dark:text-slate-400">
              Đạt chuỗi 7 ngày để mở khóa Rương Vàng và nhận ngay huy hiệu Ong Chăm Chỉ!
            </p>
          </div>
        </div>

        {/* Action Button */}
        <div className="pt-2">
          {hasCheckedInToday ? (
            <div className="w-full py-3.5 px-4 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 font-bold text-sm text-center flex items-center justify-center gap-2">
              <Check className="w-4 h-4 text-emerald-500 stroke-[3]" />
              <span>Hôm nay bạn đã hoàn thành điểm danh! Hẹn gặp lại vào ngày mai.</span>
            </div>
          ) : (
            <button
              type="button"
              disabled={checkInMutation.isPending}
              onClick={handleCheckIn}
              className="w-full py-4 px-6 rounded-2xl bg-linear-to-r from-emerald-500 via-emerald-600 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-black text-base shadow-xl shadow-emerald-600/30 transition-all duration-200 hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 cursor-pointer flex items-center justify-center gap-2.5"
            >
              <Flame className="w-5 h-5 fill-amber-300 text-amber-300 animate-pulse" />
              <span className="flex items-center gap-1.5">
                {checkInMutation.isPending ? (
                  'Đang xác nhận...'
                ) : (
                  <>
                    <span>Điểm Danh Ngay (+{(currentStreak + 1) % 7 === 0 ? '5' : '1'} xu SQB</span>
                    <SqbCoin className="w-5 h-5 drop-shadow-md" />
                    <span>)</span>
                  </>
                )}
              </span>
            </button>
          )}
        </div>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
}
