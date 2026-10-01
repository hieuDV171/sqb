import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Clock,
  Eye,
  CheckCircle2,
  Copy,
  Check,
  MessageSquare,
  Heart,
  ChevronRight,
  Edit3,
  Trash2,
  HelpCircle,
  PlayCircle,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import type { SubmissionSessionSummaryDto, SessionStatus } from '../types/session.types';

interface SubmissionCardProps {
  submission: SubmissionSessionSummaryDto;
  currentStatusFilter?: SessionStatus;
  onViewDetail: (sessionId: number) => void;
  onEdit?: (submission: SubmissionSessionSummaryDto) => void;
  onDelete?: (sessionId: number, sessionCode: string) => void;
}

export const SubmissionCard: React.FC<SubmissionCardProps> = ({
  submission,
  currentStatusFilter,
  onViewDetail,
  onEdit,
  onDelete,
}) => {
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);

  // Status can be from submission object or the currently selected filter
  const effectiveStatus: SessionStatus =
    submission.status || currentStatusFilter || 'PENDING';

  const handleCopyCode = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText(submission.sessionCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const formattedDate = new Date(submission.createdAt).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  const getStatusBadge = (status: SessionStatus) => {
    switch (status) {
      case 'PENDING':
        return {
          label: 'Chờ duyệt',
          icon: Clock,
          className:
            'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
        };
      case 'REVIEWING':
        return {
          label: 'Đang xem xét',
          icon: Eye,
          className:
            'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20',
        };
      case 'RESOLVED':
        return {
          label: 'Đã duyệt',
          icon: CheckCircle2,
          className:
            'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
        };
      default:
        return {
          label: 'Chờ duyệt',
          icon: Clock,
          className:
            'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
        };
    }
  };

  const statusInfo = getStatusBadge(effectiveStatus);
  const StatusIcon = statusInfo.icon;
  const isEditable = effectiveStatus === 'PENDING';

  return (
    <div
      onClick={() => onViewDetail(submission.sessionId)}
      className="group relative bg-white/90 dark:bg-slate-900/90 border border-slate-200/80 dark:border-slate-800/80 hover:border-indigo-400/60 dark:hover:border-indigo-500/50 rounded-3xl p-5 sm:p-6 shadow-xs hover:shadow-md transition-all duration-200 cursor-pointer backdrop-blur-xs flex flex-col justify-between"
    >
      <div>
        {/* Header row: Subject Tag, Session Code & Status Badge */}
        <div className="flex flex-wrap items-center justify-between gap-2.5 mb-3">
          <div className="flex items-center gap-2 flex-wrap">
            {/* Subject Code Badge */}
            <span className="px-2.5 py-1 rounded-xl text-xs font-bold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200/60 dark:border-indigo-800/60">
              {submission.subjectCode}
            </span>

            {/* Session Code with Copy */}
            <button
              type="button"
              onClick={handleCopyCode}
              title="Nhấn để sao chép mã phiên"
              className="flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-mono font-medium bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer"
            >
              <span>{submission.sessionCode}</span>
              {copied ? (
                <Check className="w-3 h-3 text-emerald-500" />
              ) : (
                <Copy className="w-3 h-3 text-slate-400" />
              )}
            </button>
          </div>

          {/* Status Badge */}
          <span
            className={cn(
              'flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border',
              statusInfo.className
            )}
          >
            <StatusIcon className="w-3.5 h-3.5" />
            <span>{statusInfo.label}</span>
          </span>
        </div>

        {/* Title */}
        <h4 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors line-clamp-2">
          {submission.title || 'Phiên đề xuất câu hỏi'}
        </h4>

        {/* Content Snippet */}
        {submission.content && (
          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400 mt-1.5 line-clamp-2 leading-relaxed">
            {submission.content}
          </p>
        )}

        {/* Subject Full Name */}
        <p className="text-xs text-slate-500 dark:text-slate-500 mt-2 font-medium">
          Môn học: <span className="text-slate-700 dark:text-slate-300">{submission.subjectName}</span>
        </p>
      </div>

      {/* Footer Info & Actions */}
      <div className="pt-4 mt-4 border-t border-slate-100 dark:border-slate-800/80 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500 dark:text-slate-400">
        {/* Meta Stats */}
        <div className="flex items-center gap-4 flex-wrap">
          <div className="flex items-center gap-1 font-semibold text-indigo-600 dark:text-indigo-400">
            <HelpCircle className="w-4 h-4" />
            <span>{submission.questionCounts || 0} câu hỏi</span>
          </div>

          <div className="flex items-center gap-1">
            <Heart className="w-3.5 h-3.5" />
            <span>{submission.reactCount || 0}</span>
          </div>

          <div className="flex items-center gap-1">
            <MessageSquare className="w-3.5 h-3.5" />
            <span>{submission.commentCount || 0}</span>
          </div>

          <span className="text-slate-400 dark:text-slate-500">
            {formattedDate}
          </span>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-1.5" onClick={(e) => e.stopPropagation()}>
          {isEditable && onEdit && (
            <button
              type="button"
              onClick={() => onEdit(submission)}
              title="Chỉnh sửa phiên đề xuất"
              className="p-2 rounded-xl text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 transition-colors cursor-pointer"
            >
              <Edit3 className="w-4 h-4" />
            </button>
          )}

          {isEditable && onDelete && (
            <button
              type="button"
              onClick={() => onDelete(submission.sessionId, submission.sessionCode)}
              title="Xóa phiên nộp nháp"
              className="p-2 rounded-xl text-slate-600 dark:text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors cursor-pointer"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          )}

          {effectiveStatus === 'RESOLVED' && (
            <button
              type="button"
              onClick={() => navigate(`/sessions/${submission.sessionId}/practice`)}
              className="flex items-center gap-1 px-3 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold transition-all shadow-xs active:scale-95 cursor-pointer"
            >
              <PlayCircle className="w-3.5 h-3.5" />
              <span>Luyện tập</span>
            </button>
          )}

          <button
            type="button"
            onClick={() => onViewDetail(submission.sessionId)}
            className="flex items-center gap-1 px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-medium transition-colors cursor-pointer"
          >
            <span>Chi tiết</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
};
