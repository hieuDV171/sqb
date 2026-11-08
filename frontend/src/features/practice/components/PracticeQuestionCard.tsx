import { useState } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { ArrowLeft, ArrowRight, BarChart3, Check, Heart, Lightbulb, Loader2, Maximize2, MessageSquare, Star, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { reactService } from '@/features/feed/services/reactService';
import type { PracticeQuestionDto } from '../types/practice.types';

interface PracticeQuestionCardProps {
  question: PracticeQuestionDto;
  selectedOptions: string[];
  onToggleOption: (key: string) => void;
  onSubmitAnswer: () => void;
  isSubmittingAnswer: boolean;
  answerError?: string;
  onPrevQuestion: () => void;
  onNextQuestion: () => void;
  hasPrev: boolean;
  hasNext: boolean;
  onOpenStats: (questionId: number, code?: string) => void;
  onOpenRate: (questionId: number, code?: string) => void;
  onOpenDiscussion: (questionId: number, code?: string) => void;
  localAnswerResult?: { isCorrect: boolean; selected: string[] };
  reaction?: { liked: boolean; count: number };
  onReactionChange: (value: { liked: boolean; count: number }) => void;
}

export function PracticeQuestionCard({
  question, selectedOptions, onToggleOption, onSubmitAnswer, isSubmittingAnswer, answerError,
  onPrevQuestion, onNextQuestion, hasPrev, hasNext, onOpenStats, onOpenRate, onOpenDiscussion, localAnswerResult,
  reaction, onReactionChange,
}: PracticeQuestionCardProps) {
  const [preview, setPreview] = useState<{ url: string; label: string } | null>(null);
  const [reacting, setReacting] = useState(false);
  const { questionId, questionCode, hiddenFields } = question;
  const answered = question.myInteraction.answered || Boolean(localAnswerResult);
  const OptionBody = answered ? 'div' : 'label';
  const correct = localAnswerResult?.isCorrect;
  const correctKeys = hiddenFields?.correctAnswer?.split(',').map((key) => key.trim()) ?? [];

  async function toggleLike() {
    setReacting(true);
    try {
      const { data } = await reactService.toggleReact({ targetType: 'QUESTION', targetId: questionId, reactionType: 'LIKE' });
      if (data) onReactionChange({ liked: data.myReaction === 'LIKE', count: data.totalCount });
    } catch {
      // Axios displays the request error.
    } finally {
      setReacting(false);
    }
  }

  function imagePreview(url: string, label: string) {
    return (
      <Button variant="outline" className="relative h-auto max-w-full p-1" aria-label={label} onClick={() => setPreview({ url, label })}>
        <img src={url} alt="" className="max-h-40 max-w-full rounded-lg object-contain" loading="lazy" />
        <Maximize2 aria-hidden="true" className="absolute bottom-2 right-2 rounded bg-background p-1 size-6" />
      </Button>
    );
  }

  return (
    <article className="space-y-6 rounded-2xl border border-border bg-card p-4 sm:p-6">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-border pb-3">
        <p className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <span className="rounded-full bg-academic-soft px-3 py-1 font-medium text-academic">
            {question.source === 'LLM' ? 'AI hỗ trợ' : 'Tự biên soạn'}
          </span>
          {questionCode && <span className="font-mono">#{questionCode}</span>}
        </p>
        <div role="group" aria-label="Tương tác với câu hỏi" className="flex flex-wrap gap-2">
          <Button variant="ghost" className="min-h-11 min-w-11 px-2" aria-label="Thích câu hỏi" aria-pressed={reaction?.liked ?? false} disabled={reacting} onClick={() => void toggleLike()}>
            <Heart aria-hidden="true" className={reaction?.liked ? 'fill-current text-destructive' : undefined} />
            {reaction?.count ?? question.reactCount ?? 0}
          </Button>
          <Button variant="ghost" size="icon" className="size-11" aria-label="Xem thống kê câu hỏi" onClick={() => onOpenStats(questionId, questionCode)}>
            <BarChart3 aria-hidden="true" />
          </Button>
          <Button variant="ghost" className="min-h-11 min-w-11 px-2" aria-label={question.myInteraction.rated ? 'Xem hoặc sửa đánh giá' : 'Đánh giá hoặc báo lỗi'} onClick={() => onOpenRate(questionId, questionCode)}>
            <Star aria-hidden="true" className={question.myInteraction.rated ? 'fill-current text-warning' : undefined} />
            {question.ratingCount ?? 0}
          </Button>
          <Button variant="ghost" className="min-h-11 min-w-11 px-2" aria-label="Thảo luận về câu hỏi" onClick={() => onOpenDiscussion(questionId, questionCode)}>
            <MessageSquare aria-hidden="true" /> {question.commentCount ?? 0}
          </Button>
        </div>
      </header>

      <p className="whitespace-pre-wrap break-words text-base font-medium leading-relaxed sm:text-lg">{question.content}</p>
      {!!question.imageUrls?.length && (
        <div className="flex flex-wrap gap-3">
          {question.imageUrls.map((url, index) => (
            <div key={url + index} className="min-w-0">{imagePreview(url, `Phóng to hình minh họa ${index + 1}`)}</div>
          ))}
        </div>
      )}

      <fieldset className="min-w-0 space-y-3">
        <legend className="mb-3 text-sm font-medium text-muted-foreground">
          {answered ? 'Các phương án' : 'Chọn một hoặc nhiều phương án'}
        </legend>
        {question.options.map(({ key, text, mediaUrl }) => {
          const selected = (localAnswerResult?.selected ?? selectedOptions).includes(key);
          const correctOption = answered && correctKeys.includes(key);
          const wrongOption = answered && selected && !correctOption && correct !== undefined;
          return (
            <div key={key} className={cn(
              'rounded-xl border border-border bg-surface-subtle',
              selected && !answered && 'border-academic bg-academic-soft',
              correctOption && 'border-success bg-success-soft',
              wrongOption && 'border-destructive bg-destructive/10',
            )}>
              <OptionBody className={cn('flex min-h-12 items-start gap-3 rounded-xl p-3 text-sm leading-relaxed', !answered && 'cursor-pointer hover:bg-muted/40')}>
                {!answered && (
                  <input type="checkbox" checked={selected} onChange={() => onToggleOption(key)}
                    disabled={isSubmittingAnswer} className="mt-1 size-4 shrink-0 accent-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring" />
                )}
                <span className="shrink-0 font-semibold">{key}.</span>
                <span className="min-w-0 flex-1 whitespace-pre-wrap break-words">{text}</span>
                {correctOption && <span className="inline-flex shrink-0 items-center gap-1 font-medium text-success"><Check aria-hidden="true" className="size-4" /> Đúng</span>}
                {wrongOption && <span className="inline-flex shrink-0 items-center gap-1 font-medium text-destructive"><X aria-hidden="true" className="size-4" /> Đã chọn</span>}
              </OptionBody>
              {mediaUrl && <div className="px-3 pb-3">{imagePreview(mediaUrl, `Phóng to hình của phương án ${key}`)}</div>}
            </div>
          );
        })}
      </fieldset>

      {answered ? (
        <div className="space-y-3" role="status">
          <div className={cn(
            'rounded-xl border border-border bg-muted px-4 py-3 text-sm',
            correct === true && 'border-success bg-success-soft',
            correct === false && 'border-destructive bg-destructive/10',
          )}>
            <p className="font-semibold">{correct === undefined ? 'Đã trả lời trước đó' : correct ? 'Trả lời đúng' : 'Trả lời chưa đúng'}</p>
            {hiddenFields?.correctAnswer && <p className="mt-1">Đáp án đúng <strong>{hiddenFields.correctAnswer}</strong></p>}
          </div>
          {hiddenFields?.explanation && (
            <div className="space-y-2 rounded-xl border border-border bg-surface-subtle p-4">
              <h3 className="flex items-center gap-2 text-sm font-semibold"><Lightbulb aria-hidden="true" className="size-4 text-academic" /> Giải thích</h3>
              <p className="whitespace-pre-wrap break-words text-sm leading-relaxed">{hiddenFields.explanation}</p>
            </div>
          )}
        </div>
      ) : (
        <div className="space-y-2">
          {answerError && <p role="alert" className="text-sm text-destructive">{answerError}</p>}
          <Button className="min-h-11" disabled={!selectedOptions.length || isSubmittingAnswer} onClick={onSubmitAnswer}>
            {isSubmittingAnswer && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
            {isSubmittingAnswer ? 'Đang kiểm tra…' : 'Kiểm tra đáp án'}
          </Button>
        </div>
      )}

      <footer className="flex flex-wrap justify-between gap-2 border-t border-border pt-4">
        <Button variant="outline" className="min-h-11" disabled={!hasPrev} onClick={onPrevQuestion}><ArrowLeft aria-hidden="true" /> Câu trước</Button>
        <Button variant="outline" className="min-h-11" disabled={!hasNext} onClick={onNextQuestion}>Câu tiếp <ArrowRight aria-hidden="true" /></Button>
      </footer>

      <Dialog.Root open={Boolean(preview)} onOpenChange={(open) => { if (!open) setPreview(null); }}>
        <Dialog.Portal>
          <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/70" />
          <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-w-4xl -translate-y-1/2 rounded-2xl border border-border bg-card p-4 shadow-xl">
            <div className="mb-3 flex items-center justify-between gap-2">
              <Dialog.Title className="font-semibold">{preview?.label}</Dialog.Title>
              <Dialog.Close render={<Button variant="ghost" size="icon" className="size-11" aria-label="Đóng hình" />}><X aria-hidden="true" /></Dialog.Close>
            </div>
            {preview && <img src={preview.url} alt={preview.label} className="mx-auto max-h-[72dvh] max-w-full object-contain" />}
          </Dialog.Popup>
        </Dialog.Portal>
      </Dialog.Root>
    </article>
  );
}
