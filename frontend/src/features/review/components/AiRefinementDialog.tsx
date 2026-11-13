import { useState, type FormEvent } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { toast } from '@/stores/useToastStore';
import { reviewService } from '../services/reviewService';
import type { SessionQuestionReviewDto } from '../types/review.types';

export function AiRefinementDialog({ question, sessionId, onClose }: {
  question: SessionQuestionReviewDto; sessionId: number; onClose: () => void;
}) {
  const client = useQueryClient();
  const [prompt, setPrompt] = useState('');
  const [acknowledged, setAcknowledged] = useState(false);
  const refine = useMutation({ mutationFn: () => reviewService.refineQuestion(question.questionId, prompt.trim()) });
  const result = refine.data?.data;
  const apply = useMutation({
    mutationFn: ({ editLogId, approve }: { editLogId: number; approve: boolean }) =>
      reviewService.applyRefinement(question.questionId, editLogId, approve, acknowledged),
    onSuccess: async (_, { approve }) => {
      await client.invalidateQueries({ queryKey: ['reviewSessionDetail', sessionId] });
      toast.success(approve ? 'Đã áp dụng bản AI.' : 'Đã loại bỏ bản AI.');
      onClose();
    },
  });
  const busy = refine.isPending || apply.isPending;
  const suggestion = result?.suggestedQuestion;
  const audit = result?.hallucinationAudit;
  const violations = audit?.violations ?? [];
  const unverified = !audit || violations.some(({ type }) => type === 'UNVERIFIED');
  const needsAcknowledgement = unverified || audit.isHallucinated || violations.length > 0;
  const invalid = !suggestion?.content?.trim() || suggestion.refusalReason || (suggestion.options?.length ?? 0) < 2
    || !suggestion.options?.some((option) => option.isCorrect);
  const error = apply.error ?? refine.error;

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!prompt.trim() || busy) return;
    setAcknowledged(false);
    apply.reset();
    refine.mutate();
  }

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !busy) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-2xl -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
          <Dialog.Title className="text-lg font-bold">AI tinh chỉnh câu hỏi</Dialog.Title>
          <Dialog.Description className="text-sm text-muted-foreground">Mô tả điểm cần sửa rồi xem bản đề xuất cùng kết quả BTProp.</Dialog.Description>
          {!result && <form onSubmit={submit} className="space-y-2">
            <Label htmlFor="ai-refine-prompt">Yêu cầu tinh chỉnh</Label>
            <Textarea id="ai-refine-prompt" value={prompt} onChange={(event) => setPrompt(event.target.value)} maxLength={1000} required disabled={busy} />
            <Button type="submit" disabled={busy || !prompt.trim()}>{refine.isPending ? 'Đang tinh chỉnh và kiểm chứng…' : 'Tạo bản đề xuất'}</Button>
          </form>}
          {result && (invalid ? (
            <p role="alert" className="rounded-xl bg-destructive/10 p-3 text-sm text-destructive">{suggestion?.refusalReason || 'AI trả về câu hỏi hoặc đáp án không hợp lệ.'}</p>
          ) : (
            <>
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="min-w-0 space-y-2 rounded-xl border border-border p-3">
                  <h3 className="font-semibold">Câu hiện tại</h3>
                  <p className="wrap-break-word whitespace-pre-wrap text-sm">{question.content}</p>
                  <ul className="space-y-1 text-sm">{question.options.map((option) => <li key={option.key}>{option.key}. {option.text}{option.isCorrect && ' (Đúng)'}</li>)}</ul>
                  {question.explanation && <p className="wrap-break-word whitespace-pre-wrap text-sm text-muted-foreground">{question.explanation}</p>}
                </div>
                <div className="min-w-0 space-y-2 rounded-xl border border-primary/30 p-3">
                  <h3 className="font-semibold">Bản AI đề xuất</h3>
                  <p className="wrap-break-word whitespace-pre-wrap text-sm">{suggestion?.content}</p>
                  <ul className="space-y-1 text-sm">{suggestion?.options?.map((option, index) => <li key={index}>{option.key}. {option.text}{option.isCorrect && ' (Đúng)'}</li>)}</ul>
                  {suggestion?.explanation && <p className="wrap-break-word whitespace-pre-wrap text-sm text-muted-foreground">{suggestion.explanation}</p>}
                </div>
              </div>
              <div className="space-y-2 rounded-xl border border-border p-3 text-sm">
                <h3 className="font-semibold">Kiểm chứng BTProp</h3>
                <p>{unverified ? 'Chưa kiểm chứng được.' : needsAcknowledgement ? 'Có cảnh báo cần xem lại.' : 'Không phát hiện vấn đề.'}
                  {!unverified && ` Độ tin cậy ${Math.round(audit.confidenceScore * 100)}%.`}</p>
                {violations.length > 0 && <ul className="list-disc space-y-1 pl-5">{violations.map((item, index) => <li key={index}>{item.nodeStatement}. {item.detail}</li>)}</ul>}
                {needsAcknowledgement && <label className="flex min-h-11 cursor-pointer items-center gap-2 font-medium">
                  <input type="checkbox" checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked)} disabled={busy} /> Tôi đã tự rà soát bản đề xuất.
                </label>}
              </div>
            </>
          ))}
          {error && <p role="alert" className="text-sm text-destructive">{error.message || 'Không thể thực hiện thao tác. Hãy thử lại.'}</p>}
          <div className="flex flex-wrap justify-end gap-2">
            <Button variant="ghost" onClick={onClose} disabled={busy}>Đóng</Button>
            {result && <>
              <Button variant="outline" disabled={busy} onClick={() => apply.mutate({ editLogId: result.editLogId, approve: false })}>Loại bỏ bản AI</Button>
              <Button disabled={busy || Boolean(invalid) || (needsAcknowledgement && !acknowledged)} onClick={() => apply.mutate({ editLogId: result.editLogId, approve: true })}>
                {apply.isPending ? 'Đang lưu…' : 'Áp dụng bản AI'}
              </Button>
            </>}
          </div>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
