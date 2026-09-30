import React, { useState } from 'react';
import type { DuplicateWarning, DuplicateDetectionTier } from '../types/review.types';
import { AlertTriangle, Sparkles, FileText, Image as ImageIcon, ChevronRight, X } from 'lucide-react';
import { cn } from '@/lib/utils';

interface DuplicateWarningBadgeProps {
  warnings: DuplicateWarning[];
}

const getTierConfig = (tier: DuplicateDetectionTier) => {
  switch (tier) {
    case 'RULE_BASED':
    case 'TRIGRAM':
      return {
        label: 'Trùng lặp từ ngữ (Text Trigram)',
        color: 'bg-amber-100 text-amber-800 border-amber-300 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800',
        badgeColor: 'bg-amber-500 text-white',
        icon: FileText,
      };
    case 'TEXT_EMBEDDING':
      return {
        label: 'Trùng ngữ nghĩa Vector (AI Embedding)',
        color: 'bg-purple-100 text-purple-800 border-purple-300 dark:bg-purple-950/60 dark:text-purple-300 dark:border-purple-800',
        badgeColor: 'bg-purple-600 text-white',
        icon: Sparkles,
      };
    case 'P_HASH':
    case 'IMAGE_EMBEDDING':
      return {
        label: 'Trùng lặp hình ảnh (P-Hash / AI Vision)',
        color: 'bg-rose-100 text-rose-800 border-rose-300 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-800',
        badgeColor: 'bg-rose-600 text-white',
        icon: ImageIcon,
      };
    default:
      return {
        label: 'Cảnh báo trùng lặp',
        color: 'bg-slate-100 text-slate-800 border-slate-300 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700',
        badgeColor: 'bg-slate-600 text-white',
        icon: AlertTriangle,
      };
  }
};

export const DuplicateWarningBadge: React.FC<DuplicateWarningBadgeProps> = ({ warnings }) => {
  const [selectedWarning, setSelectedWarning] = useState<DuplicateWarning | null>(null);

  if (!warnings || warnings.length === 0) return null;

  const highestScore = Math.max(...warnings.map((w) => w.similarityScore || 0));
  const scorePercent = (highestScore * 100).toFixed(0);

  return (
    <>
      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => setSelectedWarning(warnings[0])}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-rose-50 hover:bg-rose-100 dark:bg-rose-950/50 dark:hover:bg-rose-900/60 text-rose-700 dark:text-rose-300 border border-rose-200 dark:border-rose-800 transition-colors cursor-pointer shadow-2xs"
        >
          <AlertTriangle className="w-3.5 h-3.5 text-rose-500 animate-pulse shrink-0" />
          <span>
            Phát hiện trùng lặp ({warnings.length}): <strong>{scorePercent}%</strong>
          </span>
          <ChevronRight className="w-3 h-3 text-rose-400" />
        </button>
      </div>

      {/* Detail Popover / Modal */}
      {selectedWarning && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl max-w-lg w-full p-5 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-rose-500" />
                <h3 className="font-bold text-slate-800 dark:text-slate-100 text-sm md:text-base">
                  Chi tiết Cảnh báo Trùng lặp
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setSelectedWarning(null)}
                className="p-1 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* List of Warnings */}
            <div className="space-y-3 max-h-80 overflow-y-auto pr-1">
              {warnings.map((w, idx) => {
                const config = getTierConfig(w.tier);
                const Icon = config.icon;
                const percent = (w.similarityScore * 100).toFixed(1);

                return (
                  <div
                    key={idx}
                    className={cn(
                      'p-3.5 rounded-2xl border text-xs space-y-2',
                      config.color
                    )}
                  >
                    <div className="flex items-center justify-between font-bold">
                      <div className="flex items-center gap-1.5">
                        <Icon className="w-4 h-4" />
                        <span>{config.label}</span>
                      </div>
                      <span className={cn('px-2 py-0.5 rounded-full text-[10px] font-bold', config.badgeColor)}>
                        {percent}% tương đồng
                      </span>
                    </div>

                    <div className="text-[11px] opacity-90">
                      <strong>Câu hỏi trùng khớp trong ngân hàng:</strong> #{w.similarQuestionId}
                    </div>

                    {w.matchedSnipet && (
                      <div className="p-2 rounded-xl bg-white/70 dark:bg-black/30 border border-current/20 text-[11px] font-mono leading-relaxed break-words">
                        "{w.matchedSnipet}"
                      </div>
                    )}

                    {w.mediaUrl && (
                      <div className="mt-2">
                        <span className="text-[10px] font-semibold block mb-1">Ảnh trùng khớp:</span>
                        <img
                          src={w.mediaUrl}
                          alt="matched visual"
                          className="h-24 w-auto rounded-xl object-contain border border-current/20 bg-white"
                        />
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="button"
                onClick={() => setSelectedWarning(null)}
                className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold transition-colors cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
