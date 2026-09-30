import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Clock,
  Eye,
  CheckCircle2,
  Check,
  Copy,
  User,
  Bot,
  Lightbulb,
  Edit3,
  Trash2,
  BookOpen,
  Calendar,
  MessageSquare,
  Heart,
  ShieldCheck,
  AlertCircle,
  RefreshCw,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  useSubmissionDetail,
  useSessionMutations,
  EditSubmissionModal,
  DeleteSubmissionDialog,
  type SessionStatus,
} from '@/features/session';

export function SubmissionDetailPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const parsedId = sessionId ? Number(sessionId) : null;

  const [copied, setCopied] = useState(false);
  const [zoomedImage, setZoomedImage] = useState<string | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

  const {
    data: detail,
    isLoading,
    isError,
    error,
    refetch,
  } = useSubmissionDetail(parsedId);

  const { updateMutation, deleteMutation } = useSessionMutations();

  const handleCopyCode = () => {
    if (!detail?.sessionCode) return;
    navigator.clipboard.writeText(detail.sessionCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const effectiveStatus: SessionStatus =
    detail?.status || (detail?.reviewedAt ? 'RESOLVED' : 'PENDING');

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

  const statusBadge = getStatusBadge(effectiveStatus);
  const StatusIcon = statusBadge.icon;
  const isEditable = effectiveStatus === 'PENDING';

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 flex flex-col items-center justify-center space-y-3">
        <div className="w-9 h-9 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-xs text-slate-500">Đang tải chi tiết phiên đề xuất...</p>
      </div>
    );
  }

  if (isError || !detail) {
    return (
      <div className="max-w-xl mx-auto px-4 py-16 text-center space-y-4">
        <div className="w-14 h-14 rounded-2xl bg-rose-100 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 flex items-center justify-center mx-auto">
          <AlertCircle className="w-7 h-7" />
        </div>
        <h3 className="text-lg font-bold text-slate-900 dark:text-white">
          Không tìm thấy thông tin phiên
        </h3>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          {error?.message || 'Phiên đề xuất này không tồn tại hoặc bạn không có quyền truy cập.'}
        </p>
        <div className="flex justify-center gap-3 pt-2">
          <button
            type="button"
            onClick={() => navigate('/sessions')}
            className="px-4 py-2 rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-semibold"
          >
            Về danh sách
          </button>
          <button
            type="button"
            onClick={() => refetch()}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-semibold shadow-xs"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Thử lại</span>
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-3 sm:px-6 py-6 sm:py-8 space-y-6">
      {/* Top Navigation */}
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => navigate('/sessions')}
          className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-xs sm:text-sm font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Về danh sách phiên</span>
        </button>

        <div className="flex items-center gap-2">
          {isEditable && (
            <>
              <button
                type="button"
                onClick={() => setIsEditModalOpen(true)}
                className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 font-semibold text-xs sm:text-sm hover:bg-indigo-100 transition-colors cursor-pointer"
              >
                <Edit3 className="w-4 h-4" />
                <span>Chỉnh sửa</span>
              </button>

              <button
                type="button"
                onClick={() => setIsDeleteDialogOpen(true)}
                className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-rose-50 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400 font-semibold text-xs sm:text-sm hover:bg-rose-100 transition-colors cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />
                <span>Xóa</span>
              </button>
            </>
          )}
        </div>
      </div>

      {/* Main Header Card */}
      <div className="bg-white/90 dark:bg-slate-900/90 border border-slate-200/80 dark:border-slate-800/80 rounded-3xl p-5 sm:p-7 shadow-xs backdrop-blur-md space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="px-2.5 py-1 rounded-xl text-xs font-bold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200/60 dark:border-indigo-800/60">
              {detail.subjectCode}
            </span>

            <button
              type="button"
              onClick={handleCopyCode}
              className="flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-mono font-medium bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer"
            >
              <span>{detail.sessionCode}</span>
              {copied ? (
                <Check className="w-3 h-3 text-emerald-500" />
              ) : (
                <Copy className="w-3 h-3 text-slate-400" />
              )}
            </button>
          </div>

          <span
            className={cn(
              'flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border',
              statusBadge.className
            )}
          >
            <StatusIcon className="w-3.5 h-3.5" />
            <span>{statusBadge.label}</span>
          </span>
        </div>

        <div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white tracking-tight">
            {detail.title}
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Môn học: <strong className="text-slate-700 dark:text-slate-300">{detail.subjectName}</strong>
          </p>
        </div>

        {detail.content && (
          <div className="p-4 bg-slate-50 dark:bg-slate-800/40 rounded-2xl border border-slate-100 dark:border-slate-800 text-xs sm:text-sm text-slate-700 dark:text-slate-300 leading-relaxed">
            <span className="font-semibold block text-slate-900 dark:text-white mb-1">
              Ghi chú đề xuất:
            </span>
            {detail.content}
          </div>
        )}

        {/* Lecturer Review Banner */}
        {detail.reviewedByLecturer && (
          <div className="bg-emerald-50/70 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-800/60 p-4 rounded-2xl flex items-start gap-3">
            <div className="w-9 h-9 rounded-xl bg-emerald-100 dark:bg-emerald-900/40 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div className="space-y-0.5 text-xs">
              <span className="font-bold text-emerald-900 dark:text-emerald-300">
                Phiên đã được Giảng viên xét duyệt
              </span>
              <p className="text-emerald-800 dark:text-emerald-400">
                Giảng viên phụ trách: <strong>{detail.reviewedByLecturer}</strong>
              </p>
              {detail.reviewedAt && (
                <p className="text-slate-500 dark:text-slate-400 text-[11px]">
                  Thời gian duyệt: {new Date(detail.reviewedAt).toLocaleString('vi-VN')}
                </p>
              )}
            </div>
          </div>
        )}

        <div className="pt-3 border-t border-slate-100 dark:border-slate-800/80 flex items-center justify-between text-xs text-slate-500">
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1">
              <Calendar className="w-3.5 h-3.5" />
              <span>{new Date(detail.createdAt).toLocaleDateString('vi-VN')}</span>
            </span>
            <span className="flex items-center gap-1">
              <Heart className="w-3.5 h-3.5" />
              <span>{detail.reactCount || 0}</span>
            </span>
            <span className="flex items-center gap-1">
              <MessageSquare className="w-3.5 h-3.5" />
              <span>{detail.commentCount || 0}</span>
            </span>
          </div>

          <span className="font-semibold text-indigo-600 dark:text-indigo-400">
            {detail.questions?.length || 0} câu hỏi
          </span>
        </div>
      </div>

      {/* Questions List */}
      <div className="space-y-4">
        <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <BookOpen className="w-5 h-5 text-indigo-600" />
          <span>Danh sách câu hỏi trắc nghiệm ({detail.questions?.length || 0})</span>
        </h3>

        <div className="space-y-4">
          {detail.questions?.map((q, idx) => (
            <div
              key={q.questionId || idx}
              className="bg-white/90 dark:bg-slate-900/90 border border-slate-200/80 dark:border-slate-800/80 rounded-3xl p-5 sm:p-6 shadow-xs backdrop-blur-xs space-y-4"
            >
              <div className="flex items-center justify-between gap-2 flex-wrap border-b border-slate-100 dark:border-slate-800 pb-3">
                <span className="text-xs font-bold px-3 py-1 rounded-xl bg-indigo-100/70 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300">
                  Câu hỏi #{idx + 1}
                </span>

                <div className="flex items-center gap-2 text-xs">
                  {q.source === 'LLM' ? (
                    <span className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-purple-50 dark:bg-purple-950/40 text-purple-600 dark:text-purple-300 font-medium">
                      <Bot className="w-3.5 h-3.5" />
                      <span>AI Generated</span>
                    </span>
                  ) : (
                    <span className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-medium">
                      <User className="w-3.5 h-3.5" />
                      <span>Tự soạn thảo</span>
                    </span>
                  )}

                  {q.confidenceScore != null && (
                    <span className="text-xs text-slate-400 font-mono">
                      Độ tin cậy: {q.confidenceScore.toFixed(1)}/4.0
                    </span>
                  )}
                </div>
              </div>

              <p className="text-sm font-medium text-slate-900 dark:text-slate-100 leading-relaxed whitespace-pre-wrap">
                {q.content}
              </p>

              {q.imageUrls && q.imageUrls.length > 0 && (
                <div className="flex gap-2 flex-wrap pt-1">
                  {q.imageUrls.map((img, i) => (
                    <img
                      key={i}
                      src={img}
                      alt={`Minh họa câu hỏi ${idx + 1}`}
                      onClick={() => setZoomedImage(img)}
                      className="w-24 h-24 object-cover rounded-xl border border-slate-200 dark:border-slate-700 cursor-zoom-in hover:opacity-90 transition-opacity"
                    />
                  ))}
                </div>
              )}

              {/* Options */}
              <div className="space-y-2 pt-2">
                {q.options?.map((opt) => (
                  <div
                    key={opt.key}
                    className={cn(
                      'p-3 rounded-2xl border text-xs sm:text-sm flex items-start gap-3 transition-colors',
                      opt.isCorrect
                        ? 'bg-emerald-50/70 dark:bg-emerald-950/30 border-emerald-300 dark:border-emerald-800/80 text-emerald-900 dark:text-emerald-200 font-medium'
                        : 'bg-slate-50/70 dark:bg-slate-800/30 border-slate-200 dark:border-slate-700/60 text-slate-700 dark:text-slate-300'
                    )}
                  >
                    <span
                      className={cn(
                        'w-7 h-7 rounded-xl flex items-center justify-center font-bold text-xs shrink-0',
                        opt.isCorrect
                          ? 'bg-emerald-600 text-white shadow-xs'
                          : 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300'
                      )}
                    >
                      {opt.key}
                    </span>

                    <div className="flex-1 space-y-1.5 min-w-0">
                      <p className="leading-snug">{opt.text}</p>
                      {opt.mediaUrl && (
                        <img
                          src={opt.mediaUrl}
                          alt={`Đáp án ${opt.key}`}
                          onClick={() => setZoomedImage(opt.mediaUrl!)}
                          className="w-16 h-16 object-cover rounded-lg border border-slate-200 dark:border-slate-700 cursor-zoom-in mt-1"
                        />
                      )}
                    </div>

                    {opt.isCorrect && (
                      <span className="flex items-center gap-1 text-xs font-bold text-emerald-600 dark:text-emerald-400 shrink-0">
                        <Check className="w-4 h-4" />
                        <span>Đáp án đúng</span>
                      </span>
                    )}
                  </div>
                ))}
              </div>

              {q.explanation && (
                <div className="p-3.5 bg-amber-50/60 dark:bg-amber-950/20 border border-amber-200/60 dark:border-amber-900/40 rounded-2xl text-xs text-amber-900 dark:text-amber-200 flex items-start gap-2.5">
                  <Lightbulb className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold">Giải thích: </span>
                    <span className="leading-relaxed">{q.explanation}</span>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Edit Modal */}
      <EditSubmissionModal
        detail={detail}
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        onSave={async (sessionId, data) => {
          await updateMutation.mutateAsync({ sessionId, data });
          setIsEditModalOpen(false);
          refetch();
        }}
        isSaving={updateMutation.isPending}
      />

      {/* Delete Dialog */}
      <DeleteSubmissionDialog
        isOpen={isDeleteDialogOpen}
        sessionId={parsedId}
        sessionCode={detail.sessionCode}
        onClose={() => setIsDeleteDialogOpen(false)}
        onConfirm={async (sessionId) => {
          await deleteMutation.mutateAsync(sessionId);
          setIsDeleteDialogOpen(false);
          navigate('/sessions');
        }}
        isDeleting={deleteMutation.isPending}
      />

      {/* Lightbox zoomed image */}
      {zoomedImage && (
        <div
          onClick={() => setZoomedImage(null)}
          className="fixed inset-0 z-60 bg-black/80 flex items-center justify-center p-4 cursor-zoom-out animate-in fade-in"
        >
          <img
            src={zoomedImage}
            alt="Zoomed"
            className="max-w-full max-h-full object-contain rounded-2xl shadow-2xl"
          />
        </div>
      )}
    </div>
  );
}

export default SubmissionDetailPage;
