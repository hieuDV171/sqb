import { useState, type FormEvent } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ShieldAlert, Star, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
import { cn } from '@/lib/utils';
import { useQuestionRatingMutation } from '../hooks/useQuestionRatingMutation';
import { useQuestionRatings } from '../hooks/useQuestionRatings';
import { updatePracticeQuestion } from '../hooks/updatePracticeQuestion';
import { practiceService } from '../services/practiceService';
import type { MyInteractionDto } from '../types/practice.types';

interface QuestionRateModalProps {
  questionId: number;
  questionCode?: string;
  sessionId: number;
  interaction: MyInteractionDto;
  onClose: () => void;
}

const tabs = [
  { value: 'rate', label: 'Đánh giá của bạn' },
  { value: 'report', label: 'Báo lỗi' },
  { value: 'reviews', label: 'Đánh giá từ cộng đồng' },
] as const;

export function QuestionRateModal({ questionId, questionCode, sessionId, interaction, onClose }: QuestionRateModalProps) {
  const [tab, setTab] = useState<(typeof tabs)[number]['value']>('rate');
  const [stars, setStars] = useState<number | null>(interaction.myRating == null ? null : interaction.myRating + 1);
  const [ratingComment, setRatingComment] = useState(interaction.ratingComment ?? '');
  const [reportDescription, setReportDescription] = useState('');
  const rating = useQuestionRatingMutation();
  const ratings = useQuestionRatings(questionId, tab === 'reviews');
  const queryClient = useQueryClient();
  const report = useMutation({
    mutationFn: (description: string) => practiceService.reportQuestion(questionId, description),
    onSuccess: () => {
      updatePracticeQuestion(queryClient, sessionId, questionId, (question) => ({
        ...question,
        myInteraction: { ...question.myInteraction, reported: true },
      }));
      setReportDescription('');
    },
  });
  const isSubmitting = rating.isPending || report.isPending;

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSubmitting) return;
    if (tab === 'report' && reportDescription.trim()) report.mutate(reportDescription.trim());
    if (tab === 'rate' && stars != null) {
      rating.mutate({ questionId, sessionId, request: { rating: stars - 1, comment: ratingComment.trim() || undefined } });
    }
  }

  const reviews = ratings.data?.pages.flatMap((page) => page.items) ?? [];
  const error = tab === 'report' ? report.error : rating.error;
  const saved = tab === 'report' ? report.isSuccess : rating.isSuccess;

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !isSubmitting) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-xl -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl">
          <header className="flex items-start justify-between gap-2">
            <div>
              <Dialog.Title className="text-lg font-semibold">Phản hồi về câu hỏi</Dialog.Title>
              {questionCode && <Dialog.Description className="text-sm text-muted-foreground">{questionCode}</Dialog.Description>}
            </div>
            <Dialog.Close render={<Button variant="ghost" size="icon" className="size-11" disabled={isSubmitting} aria-label="Đóng phản hồi" />}><X aria-hidden="true" /></Dialog.Close>
          </header>
          <div role="group" aria-label="Loại phản hồi" className="flex flex-wrap gap-2">
            {tabs.map(({ value, label }) => (
              <Button key={value} variant={tab === value ? 'secondary' : 'ghost'} aria-pressed={tab === value}
                className="min-h-11" onClick={() => setTab(value)} disabled={isSubmitting}>{label}</Button>
            ))}
          </div>
          {tab === 'reviews' ? (
            <div className="space-y-3">
              {ratings.isLoading && <p role="status" className="text-sm text-muted-foreground">Đang tải đánh giá…</p>}
              {ratings.isLoadingError && <ErrorState title="Không tải được đánh giá" onRetry={() => void ratings.refetch()} />}
              {ratings.isSuccess && reviews.length === 0 && <p className="text-sm text-muted-foreground">Chưa có đánh giá sao.</p>}
              {reviews.map((review) => (
                <article key={review.userId} className="space-y-1 rounded-xl border border-border bg-surface-subtle p-3 text-sm">
                  <p className="font-medium">{review.rating + 1}/5 sao</p>
                  {review.comment && <p className="whitespace-pre-wrap break-words text-muted-foreground">{review.comment}</p>}
                </article>
              ))}
              {ratings.hasNextPage && <Button variant="outline" className="min-h-11" disabled={ratings.isFetching} onClick={() => void ratings.fetchNextPage()}>
                {ratings.isFetchNextPageError ? 'Thử tải lại' : 'Xem thêm đánh giá'}
              </Button>}
            </div>
          ) : (
            <form className="space-y-4" onSubmit={submit}>
              {tab === 'rate' && (
                <fieldset className="space-y-2">
                  <legend className="text-sm font-medium">Chấm chất lượng câu hỏi</legend>
                  <div className="flex flex-wrap gap-2">
                    {[1, 2, 3, 4, 5].map((value) => (
                      <Button key={value} type="button" variant="outline" size="icon" className="size-11"
                        aria-label={`${value} sao`} aria-pressed={stars === value} onClick={() => setStars(value)}>
                        <Star aria-hidden="true" className={cn('size-5', stars != null && value <= stars && 'fill-current text-warning')} />
                      </Button>
                    ))}
                  </div>
                </fieldset>
              )}
              {tab === 'report' && (
                <p className="flex gap-2 text-sm text-muted-foreground"><ShieldAlert aria-hidden="true" className="size-4 shrink-0" />
                  {interaction.reported ? 'Bạn đã gửi báo lỗi. Gửi lại để cập nhật nội dung.' : 'Báo lỗi được lưu riêng với đánh giá sao.'}
                </p>
              )}
              <label className="block space-y-1.5 text-sm font-medium">
                <span>{tab === 'report' ? 'Mô tả lỗi' : 'Nhận xét (tùy chọn)'}</span>
                <textarea rows={3} maxLength={500} required={tab === 'report'}
                  className="w-full rounded-xl border border-input bg-background p-3 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
                  value={tab === 'report' ? reportDescription : ratingComment}
                  onChange={(event) => tab === 'report' ? setReportDescription(event.target.value) : setRatingComment(event.target.value)} />
              </label>
              {error && <p role="alert" className="text-sm text-destructive">{error.message || 'Không gửi được phản hồi.'}</p>}
              {saved && <p role="status" className="text-sm text-success">{tab === 'report' ? 'Đã lưu báo lỗi.' : 'Đã lưu đánh giá.'}</p>}
              <Button type="submit" className="min-h-11" disabled={isSubmitting || (tab === 'report' ? !reportDescription.trim() : stars == null)}>
                {isSubmitting ? 'Đang gửi…' : tab === 'report' ? 'Gửi báo lỗi' : 'Lưu đánh giá'}
              </Button>
            </form>
          )}
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
