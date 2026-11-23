import { useEffect, useRef, useState, type SubmitEvent } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowUp, ChevronDown, CircleAlert, FileText, RotateCcw, Sparkles, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { toast } from '@/stores/useToastStore';
import { reviewService } from '../services/reviewService';
import type { AiRefinement, SessionQuestionReviewDto } from '../types/review.types';

const starterPrompts = ['Viết lại câu hỏi cho rõ hơn', 'Cải thiện phương án nhiễu', 'Giải thích đáp án đúng'];

type Draft = AiRefinement['suggestedQuestion'];
type RefineRequest = { prompt: string; previousEditLogId?: number };
type Turn = RefineRequest & { id: number; result?: AiRefinement; error?: string; discarded?: boolean };
type Decision = { editLogId: number; approve: boolean; acknowledged: boolean };

function isValidDraft(draft: Draft) {
  return Boolean(draft.content?.trim() && !draft.refusalReason
    && (draft.options?.length ?? 0) >= 2 && draft.options?.some((option) => option.isCorrect));
}

function requestError(error: unknown) {
  if (error && typeof error === 'object' && 'code' in error
      && (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT')) {
    return 'Đã hết thời gian chờ phản hồi. Bạn có thể gửi lại yêu cầu hoặc biên tập trực tiếp.';
  }
  const message = error && typeof error === 'object' && 'message' in error ? error.message : undefined;
  if (message === 'Network Error') {
    return 'Không kết nối được với máy chủ. Kiểm tra kết nối rồi thử lại.';
  }
  return typeof message === 'string' && message ? message : 'Không nhận được phản hồi. Hãy thử lại.';
}

function QuestionPreview({ content, options, explanation, imageUrls }: Draft & { imageUrls?: string[] }) {
  return (
    <div className="space-y-3 wrap-break-word text-sm leading-relaxed">
      <p className="whitespace-pre-wrap">{content}</p>
      {imageUrls && imageUrls.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {imageUrls.map((url, index) => (
            <img key={index} src={url} alt={`Ảnh đề bài ${index + 1}`} loading="lazy"
              className="max-h-36 max-w-full rounded-lg object-contain" />
          ))}
        </div>
      )}
      <ul className="space-y-1.5" aria-label="Phương án trả lời">
        {options?.map((option, index) => (
          <li key={index} className="flex items-start gap-2">
            <span className="shrink-0 font-semibold">{option.key}.</span>
            <div className="min-w-0">
              <span className="whitespace-pre-wrap">{option.text}</span>
              {option.mediaUrl && <img src={option.mediaUrl} alt={`Ảnh phương án ${option.key}`} loading="lazy" className="my-1 max-h-28 max-w-full rounded-lg object-contain" />}
              {option.isCorrect && <span className="ml-2 font-medium text-success">Đúng</span>}
            </div>
          </li>
        ))}
      </ul>
      {explanation && (
        <p className="whitespace-pre-wrap text-muted-foreground">
          <span className="font-semibold text-foreground">Lời giải</span>{' · '}{explanation}
        </p>
      )}
    </div>
  );
}

function Proposal({ result, discarded, busy, saving, onDecision }: {
  result: AiRefinement;
  discarded?: boolean;
  busy: boolean;
  saving: boolean;
  onDecision: (decision: Decision) => void;
}) {
  const [acknowledged, setAcknowledged] = useState(false);
  const suggestion = result.suggestedQuestion;
  const audit = result.hallucinationAudit;
  const violations = audit?.violations ?? [];
  const unverified = !audit || violations.some(({ type }) => type === 'UNVERIFIED');
  const needsAcknowledgement = unverified || audit.isHallucinated || violations.length > 0;
  const valid = isValidDraft(suggestion);

  return (
    <div className="space-y-3">
      {valid ? (
        <>
          <div className="space-y-2 rounded-xl border border-border bg-card p-3 sm:p-4">
            <h3 className="text-sm font-semibold">Bản đề xuất</h3>
            <QuestionPreview {...suggestion} />
          </div>
          {!discarded && (
            <div className="space-y-2 text-sm">
              <p className={needsAcknowledgement ? 'font-medium text-warning' : 'text-muted-foreground'}>
                {unverified ? 'BTProp chưa kiểm chứng được'
                  : needsAcknowledgement ? 'BTProp có cảnh báo cần xem lại'
                  : 'BTProp không phát hiện vấn đề'}
                {!unverified && ` · Độ tin cậy ${Math.round(audit.confidenceScore * 100)}%`}
              </p>
              {violations.length > 0 && (
                <ul className="list-disc space-y-1 wrap-break-word pl-5 text-muted-foreground">
                  {violations.map((item, index) => <li key={index}>{item.detail || item.nodeStatement}</li>)}
                </ul>
              )}
              {needsAcknowledgement && (
                <label className="flex min-h-11 w-fit cursor-pointer items-center gap-2 font-medium">
                  <input type="checkbox" className="size-4 shrink-0 accent-primary" checked={acknowledged}
                    disabled={busy} onChange={(event) => setAcknowledged(event.target.checked)} />
                  Tôi đã tự rà soát bản đề xuất.
                </label>
              )}
            </div>
          )}
        </>
      ) : (
        <p role="alert" className="text-sm wrap-break-word text-destructive">
          {suggestion.refusalReason || 'AI chưa tạo được bản đề xuất hợp lệ. Bạn có thể sửa yêu cầu và gửi lại.'}
        </p>
      )}

      {discarded ? <p role="status" className="text-sm text-muted-foreground">Đã loại bỏ bản đề xuất.</p> : (
        <div className="flex flex-wrap gap-2">
          {valid && (
            <Button size="sm" className="min-h-11" disabled={busy || (needsAcknowledgement && !acknowledged)}
              onClick={() => onDecision({ editLogId: result.editLogId, approve: true, acknowledged })}>
              {saving ? 'Đang lưu…' : 'Áp dụng đề xuất'}
            </Button>
          )}
          <Button size="sm" variant="outline" className="min-h-11" disabled={busy}
            onClick={() => onDecision({ editLogId: result.editLogId, approve: false, acknowledged: false })}>
            Loại bỏ
          </Button>
        </div>
      )}
    </div>
  );
}

export function AiRefinementDialog({ question, sessionId, onClose }: {
  question: SessionQuestionReviewDto; sessionId: number; onClose: () => void;
}) {
  const client = useQueryClient();
  const composer = useRef<HTMLTextAreaElement>(null);
  const history = useRef<HTMLDivElement>(null);
  const latestReply = useRef<HTMLDivElement>(null);
  const [prompt, setPrompt] = useState('');
  const [turns, setTurns] = useState<Turn[]>([]);
  const [useOriginal, setUseOriginal] = useState(false);
  const previousDraft = turns.findLast((turn) => turn.result && !turn.discarded
    && isValidDraft(turn.result.suggestedQuestion));
  const previousEditLogId = useOriginal ? undefined : previousDraft?.result?.editLogId;
  const lastTurn = turns.at(-1);

  const refine = useMutation({
    mutationFn: async ({ prompt, previousEditLogId }: Turn) => {
      const response = await reviewService.refineQuestion(question.questionId, prompt, previousEditLogId);
      if (!response.data) throw new Error('Không nhận được bản đề xuất. Hãy thử lại.');
      return response.data;
    },
    onSuccess: (result, { id }) => {
      setTurns((items) => items.map((turn) => turn.id === id ? { ...turn, result } : turn));
      if (isValidDraft(result.suggestedQuestion)) setUseOriginal(false);
    },
    onError: (error, { id }) => {
      setTurns((items) => items.map((turn) => turn.id === id ? { ...turn, error: requestError(error) } : turn));
    },
  });

  const apply = useMutation({
    mutationFn: ({ editLogId, approve, acknowledged }: Decision) =>
      reviewService.applyRefinement(question.questionId, editLogId, approve, acknowledged),
    onSuccess: async (_, { approve, editLogId }) => {
      await client.invalidateQueries({ queryKey: ['reviewSessionDetail', sessionId] });
      if (approve) {
        toast.success('Đã áp dụng bản AI.');
        onClose();
        return;
      }
      setTurns((items) => items.map((turn) => turn.result?.editLogId === editLogId ? { ...turn, discarded: true } : turn));
    },
  });

  const busy = refine.isPending || apply.isPending;

  useEffect(() => {
    if (!lastTurn || !history.current || !latestReply.current) return;
    const viewport = history.current;
    viewport.scrollTop += latestReply.current.getBoundingClientRect().top - viewport.getBoundingClientRect().top - 16;
  }, [lastTurn]);

  function send(request: RefineRequest) {
    if (!request.prompt || busy) return;
    const turn = { ...request, id: turns.length };
    apply.reset();
    setTurns((items) => [...items, turn]);
    setPrompt('');
    refine.mutate(turn);
  }

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    send({ prompt: prompt.trim(), previousEditLogId });
  }

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !busy) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup initialFocus={composer}
          className="fixed inset-x-2 top-1/2 z-50 mx-auto flex h-[min(42rem,calc(100dvh-1rem))] max-w-2xl -translate-y-1/2 flex-col overflow-hidden rounded-2xl border border-border bg-card sm:inset-x-4 sm:h-[min(42rem,calc(100dvh-2rem))]">
          <header className="flex shrink-0 items-center gap-3 border-b border-border px-4 py-3 sm:px-5">
            <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-academic-soft text-academic">
              <Sparkles aria-hidden="true" className="size-5" />
            </span>
            <div className="min-w-0 flex-1">
              <Dialog.Title className="font-bold leading-tight">AI tinh chỉnh câu hỏi</Dialog.Title>
              <Dialog.Description className="mt-0.5 text-xs text-muted-foreground">Câu hỏi #{question.questionId}</Dialog.Description>
            </div>
            <Dialog.Close render={<Button size="icon" variant="ghost" aria-label="Đóng cuộc hội thoại" disabled={busy} />}>
              <X aria-hidden="true" />
            </Dialog.Close>
          </header>

          <div ref={history}
            className="min-h-0 flex-1 overflow-y-auto overscroll-contain bg-surface-subtle px-4 py-4 [scrollbar-color:var(--muted-foreground)_transparent] [scrollbar-width:thin] sm:px-5">
            <div className="space-y-5">
              <details className="group rounded-xl border border-border bg-card">
                <summary className="flex min-h-11 cursor-pointer list-none items-center gap-2 rounded-xl px-3 text-sm font-medium focus-visible:outline-2 focus-visible:outline-ring [&::-webkit-details-marker]:hidden">
                  <FileText aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
                  <span className="flex-1">Xem câu hỏi gốc</span>
                  <ChevronDown aria-hidden="true" className="size-4 shrink-0 text-muted-foreground group-open:rotate-180" />
                </summary>
                <div className="border-t border-border px-3 py-3"><QuestionPreview {...question} /></div>
              </details>

              <div className="space-y-1">
                <p className="text-xs font-semibold text-academic">Trợ lý SQB</p>
                <p className="max-w-xl text-sm leading-relaxed">Bạn muốn chỉnh phần nào của câu hỏi? Hãy mô tả thay đổi ở đề bài, phương án trả lời hoặc lời giải.</p>
              </div>

              {turns.length === 0 && (
                <div className="flex flex-wrap gap-2">
                  {starterPrompts.map((starter) => (
                    <Button key={starter} size="sm" variant="outline" className="min-h-11 whitespace-normal text-left"
                      onClick={() => { setPrompt(starter); composer.current?.focus(); }}>
                      {starter}
                    </Button>
                  ))}
                </div>
              )}

              {turns.map((turn) => (
                <div key={turn.id} className="space-y-4">
                  <div className="ml-auto w-fit max-w-[90%]">
                    <p className="mb-1 text-right text-xs font-semibold text-muted-foreground">Bạn</p>
                    <p className="rounded-2xl rounded-tr-sm bg-academic-soft px-3 py-2 text-sm leading-relaxed wrap-break-word whitespace-pre-wrap text-foreground">
                      {turn.prompt}
                    </p>
                  </div>
                  <div ref={turn === lastTurn ? latestReply : undefined} className="space-y-2">
                    <p className="text-xs font-semibold text-academic">Trợ lý SQB</p>
                    {turn.result ? (
                      <Proposal result={turn.result} discarded={turn.discarded} busy={busy}
                        saving={apply.isPending && apply.variables?.editLogId === turn.result.editLogId && apply.variables.approve}
                        onDecision={apply.mutate} />
                    ) : turn.error ? (
                      <div className="space-y-2">
                        <p role="alert" className="flex items-start gap-2 text-sm leading-relaxed text-destructive">
                          <CircleAlert aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
                          <span className="min-w-0 wrap-break-word">{turn.error}</span>
                        </p>
                        {turn === lastTurn && (
                          <Button size="sm" variant="outline" className="min-h-11" disabled={busy}
                            onClick={() => send({ prompt: turn.prompt, previousEditLogId })}>
                            <RotateCcw aria-hidden="true" /> Gửi lại yêu cầu
                          </Button>
                        )}
                      </div>
                    ) : (
                      <p className="flex items-center gap-2 text-sm text-muted-foreground">
                        <span aria-hidden="true" className="size-2 rounded-full bg-primary motion-safe:animate-pulse" />
                        Đang tạo đề xuất và kiểm chứng…
                      </p>
                    )}
                    {apply.error && apply.variables?.editLogId === turn.result?.editLogId && (
                      <p role="alert" className="text-sm wrap-break-word text-destructive">{requestError(apply.error)}</p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div role="status" className="sr-only">
            {refine.isPending ? 'Đang tạo đề xuất và kiểm chứng…'
              : lastTurn?.result && isValidDraft(lastTurn.result.suggestedQuestion) ? 'Đã có bản đề xuất mới.' : ''}
          </div>
          <form onSubmit={submit} className="shrink-0 space-y-2 border-t border-border px-4 py-3 sm:px-5">
            {previousDraft && (
              <div className="flex flex-wrap items-center justify-between gap-x-2">
                <p className="text-xs text-muted-foreground">{useOriginal ? 'Đang dùng câu hỏi gốc' : 'Đang chỉnh tiếp bản đề xuất gần nhất'}</p>
                <Button size="sm" variant={useOriginal ? 'secondary' : 'ghost'} aria-pressed={useOriginal}
                  className="min-h-11 px-2 text-academic" disabled={busy}
                  onClick={() => { setUseOriginal(!useOriginal); composer.current?.focus(); }}>
                  Dùng câu hỏi gốc
                </Button>
              </div>
            )}
            <div className="flex items-end gap-2 rounded-xl border border-input bg-background p-1.5 focus-within:border-ring focus-within:ring-2 focus-within:ring-ring/40">
              <Textarea ref={composer} aria-label="Yêu cầu tinh chỉnh" rows={2} value={prompt} maxLength={1000} enterKeyHint="send"
                readOnly={busy} placeholder={previousEditLogId ? 'Bạn muốn chỉnh thêm gì?' : 'Nhập yêu cầu tinh chỉnh…'}
                onChange={(event) => setPrompt(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing && event.nativeEvent.keyCode !== 229) {
                    event.preventDefault();
                    event.currentTarget.form?.requestSubmit();
                  }
                }}
                className="min-h-11 resize-none border-0 bg-transparent px-2 py-1.5 focus-visible:ring-0" />
              <Button render={<button type="submit" />} size="icon" aria-label="Gửi yêu cầu" title="Gửi yêu cầu"
                disabled={busy || !prompt.trim()}>
                <ArrowUp aria-hidden="true" />
              </Button>
            </div>
            <p className="hidden text-xs text-muted-foreground sm:block">Enter để gửi · Shift + Enter để xuống dòng</p>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
