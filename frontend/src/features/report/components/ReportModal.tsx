import { useEffect, useId, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { X, ShieldAlert, AlertTriangle, Loader2, Send, ImagePlus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { mediaService } from '@/services/mediaService';
import { useCreateReport } from '../hooks/useReport';
import type { ViolationType, ReportTargetType } from '../types/report.types';

interface ReportModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetType: ReportTargetType;
  targetId: number;
  targetTitle?: string;
}

const evidenceImageTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const maxEvidenceBytes = 5 * 1024 * 1024;
const pendingMediaLifetimeMs = 24 * 60 * 60 * 1000;

interface EvidenceImage {
  file: File;
  previewUrl: string;
  upload?: { publicUrl: string; uploadedAt: number };
}

export function ReportModal({
  isOpen,
  onClose,
  targetType,
  targetId,
  targetTitle,
}: ReportModalProps) {
  const createReportMutation = useCreateReport();

  const [violationType, setViolationType] = useState<ViolationType>('SPAM');
  const [description, setDescription] = useState('');
  const [evidence, setEvidence] = useState<EvidenceImage | null>(null);
  const [evidenceError, setEvidenceError] = useState('');
  const [submitPhase, setSubmitPhase] = useState<'idle' | 'uploading' | 'sending'>('idle');
  const evidenceInput = useRef<HTMLInputElement>(null);
  const evidenceHelpId = useId();
  const previewUrl = evidence?.previewUrl;
  const isSubmitting = submitPhase !== 'idle';

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  if (!isOpen) return null;

  const violationOptions: { key: ViolationType; label: string; desc: string }[] = [
    {
      key: 'SPAM',
      label: 'Nội dung rác hoặc quảng cáo',
      desc: 'Quảng cáo sản phẩm, phát tán liên kết độc hại hoặc spam bài viết liên tục.',
    },
    {
      key: 'HARASSMENT',
      label: 'Quấy rối hoặc xúc phạm',
      desc: 'Công kích cá nhân, đe dọa hoặc dùng từ ngữ xúc phạm danh dự người khác.',
    },
    {
      key: 'HATE_SPEECH',
      label: 'Ngôn từ thù địch',
      desc: 'Phân biệt đối xử, kích động thù hằn hoặc vi phạm quy tắc ứng xử học đường.',
    },
    {
      key: 'INAPPROPRIATE_CONTENT',
      label: 'Nội dung không phù hợp',
      desc: 'Hình ảnh, ngôn từ nhạy cảm hoặc vi phạm văn hóa sinh viên Bách Khoa.',
    },
    {
      key: 'COPYRIGHT',
      label: 'Vi phạm bản quyền tài liệu',
      desc: 'Đăng tải tài liệu, đề thi bảo mật hoặc sao chép trái phép không ghi nguồn.',
    },
    {
      key: 'OTHER',
      label: 'Lý do khác',
      desc: 'Hành vi vi phạm khác chưa được phân loại cụ thể ở trên.',
    },
  ];

  const clearEvidence = () => {
    setEvidence(null);
    setEvidenceError('');
  };

  const handleClose = () => {
    if (isSubmitting) return;
    clearEvidence();
    onClose();
  };

  const handleEvidenceChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.currentTarget.files?.[0];
    event.currentTarget.value = '';
    if (!file) return;

    if (!evidenceImageTypes.includes(file.type) || file.size === 0 || file.size > maxEvidenceBytes) {
      setEvidence(null);
      setEvidenceError('Hãy chọn ảnh JPG, PNG, WebP hoặc GIF không rỗng và tối đa 5 MB.');
      return;
    }

    setEvidenceError('');
    setEvidence({ file, previewUrl: URL.createObjectURL(file) });
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (isSubmitting) return;

    const uploaded = evidence?.upload;
    let evidenceUrl = uploaded && Date.now() - uploaded.uploadedAt < pendingMediaLifetimeMs ? uploaded.publicUrl : undefined;
    try {
      if (evidence && !evidenceUrl) {
        setSubmitPhase('uploading');
        setEvidenceError('');
        try {
          const uploadedAt = Date.now();
          const { publicUrl } = await mediaService.uploadViaPresign(evidence.file, 'POST', { preserveOriginal: true });
          evidenceUrl = publicUrl;
          setEvidence({ ...evidence, upload: { publicUrl, uploadedAt } });
        } catch {
          setEvidenceError('Không thể tải ảnh lên. Hãy bấm Gửi báo cáo để thử lại.');
          return;
        }
      }

      setSubmitPhase('sending');
      await createReportMutation.mutateAsync({
        targetType,
        targetId,
        violationType,
        description: description.trim() || undefined,
        evidenceUrls: evidenceUrl ? [evidenceUrl] : undefined,
      });
      onClose();
      setDescription('');
      clearEvidence();
    } catch {
      // Handled by mutation onError toast
    } finally {
      setSubmitPhase('idle');
    }
  };

  const getTargetTypeText = (t: ReportTargetType) => {
    switch (t) {
      case 'USER':
        return 'người dùng';
      case 'POST':
        return 'bài viết';
      case 'COMMENT':
        return 'bình luận';
      default:
        return 'nội dung';
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-slate-900 dark:text-slate-100 text-base">
                Báo Cáo Vi Phạm
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Báo cáo {getTargetTypeText(targetType)} {targetTitle ? `"${targetTitle}"` : ''}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={handleClose}
            disabled={isSubmitting}
            aria-label="Đóng báo cáo"
            className="p-1.5 rounded-xl text-slate-400 enabled:hover:text-slate-600 dark:enabled:hover:text-slate-200 enabled:hover:bg-slate-100 dark:enabled:hover:bg-slate-800 transition-colors enabled:cursor-pointer disabled:opacity-50"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto space-y-5 flex-1">
          {/* Violation Type Options */}
          <div>
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2.5">
              Lý do báo cáo vi phạm *
            </label>
            <div className="space-y-2">
              {violationOptions.map((opt) => (
                <label
                  key={opt.key}
                  className={`flex items-start gap-3 p-3 rounded-2xl border transition-all cursor-pointer ${
                    violationType === opt.key
                      ? 'border-rose-400 dark:border-rose-700/80 bg-rose-50/30 dark:bg-rose-950/20'
                      : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/40'
                  }`}
                >
                  <input
                    type="radio"
                    name="violationType"
                    value={opt.key}
                    checked={violationType === opt.key}
                    onChange={() => setViolationType(opt.key)}
                    className="mt-1 text-rose-600 focus:ring-rose-500"
                  />
                  <div className="flex-1">
                    <p className="text-xs sm:text-sm font-bold text-slate-900 dark:text-slate-100">
                      {opt.label}
                    </p>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5 leading-relaxed">
                      {opt.desc}
                    </p>
                  </div>
                </label>
              ))}
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
              Mô tả chi tiết bổ sung
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Vui lòng cung cấp thêm thông tin về hành vi vi phạm để ban quản trị đối soát nhanh chóng..."
              maxLength={500}
              className="w-full px-3.5 py-2.5 text-xs sm:text-sm rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:ring-2 focus:ring-rose-500 focus:outline-none resize-none"
            />
            <div className="text-right text-[10px] text-slate-400 mt-1">
              {description.length}/500 ký tự
            </div>
          </div>

          <fieldset className="min-w-0 space-y-2">
            <legend className="text-xs font-bold uppercase tracking-wider text-foreground">Ảnh bằng chứng (tùy chọn)</legend>
            <p id={evidenceHelpId} className="text-xs text-muted-foreground">JPG, PNG, WebP hoặc GIF. Tối đa 5 MB.</p>
            <input
              ref={evidenceInput}
              type="file"
              accept={evidenceImageTypes.join(',')}
              onChange={handleEvidenceChange}
              disabled={isSubmitting}
              hidden
            />
            {evidence && (
              <div className="flex items-center gap-3 rounded-xl border border-border bg-muted/40 p-2">
                <img src={evidence.previewUrl} alt="Xem trước ảnh bằng chứng" className="size-16 shrink-0 rounded-lg bg-background object-contain" />
                <span className="min-w-0 flex-1 truncate text-sm" title={evidence.file.name}>{evidence.file.name}</span>
                <Button type="button" variant="ghost" size="icon" className="size-11" disabled={isSubmitting} aria-label="Gỡ ảnh bằng chứng" onClick={clearEvidence}>
                  <X aria-hidden="true" />
                </Button>
              </div>
            )}
            <Button
              type="button"
              variant="outline"
              className="min-h-11"
              aria-describedby={evidenceHelpId}
              disabled={isSubmitting}
              onClick={() => evidenceInput.current?.click()}
            >
              <ImagePlus aria-hidden="true" />
              {evidence ? 'Đổi ảnh' : 'Chọn ảnh'}
            </Button>
            {evidenceError && <p role="alert" className="text-xs text-destructive">{evidenceError}</p>}
          </fieldset>

          <div className="p-3 rounded-2xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/60 flex items-start gap-2.5">
            <AlertTriangle className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
            <p className="text-[11px] text-amber-800 dark:text-amber-300 leading-relaxed">
              Báo cáo vi phạm sai sự thật hoặc lạm dụng tính năng báo cáo có thể dẫn đến việc tài khoản của bạn bị hạn chế quyền tương tác.
            </p>
          </div>

          {/* Footer Buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              onClick={handleClose}
              disabled={isSubmitting}
              className="px-4 py-2 text-xs font-semibold rounded-xl text-slate-600 dark:text-slate-300 enabled:hover:bg-slate-100 dark:enabled:hover:bg-slate-800 transition-colors enabled:cursor-pointer disabled:opacity-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2.5 text-xs font-bold rounded-xl text-white bg-rose-600 hover:bg-rose-700 disabled:opacity-50 transition-colors shadow-sm shadow-rose-500/20 flex items-center gap-1.5 cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  {submitPhase === 'uploading' ? 'Đang tải ảnh…' : 'Đang gửi…'}
                </>
              ) : (
                <>
                  <Send className="w-3.5 h-3.5" />
                  Gửi Báo Cáo
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
