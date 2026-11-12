import { useState, type FormEvent } from 'react';
import { BookOpen, Flag, Send, ShieldAlert, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useAuthStore } from '@/stores/useAuthStore';
import { useReviewErrorHunter } from '../hooks/useGamification';

// Point.GAME5_REPORT_AWARD in the backend.
const reportReward = new Intl.NumberFormat('vi-VN').format(0.5);
const steps = [
  { icon: BookOpen, title: 'Luyện tập câu hỏi', text: 'Mở một bộ câu hỏi và chọn câu bạn muốn kiểm tra.' },
  { icon: Flag, title: 'Gửi báo lỗi', text: 'Trong mục phản hồi của câu hỏi, chọn Báo lỗi và mô tả rõ điểm sai.' },
  { icon: ShieldCheck, title: 'Chờ thẩm định', text: 'Giảng viên kiểm tra báo cáo. Báo cáo được xác nhận trên câu đã duyệt sẽ được tính điểm.' },
];

export function ErrorHunterTab() {
  const role = useAuthStore((state) => state.user?.role);
  const canReview = role === 'LECTURER' || role === 'ADMIN';
  const [questionId, setQuestionId] = useState('');
  const [ratingUserId, setRatingUserId] = useState('');
  const [confirmed, setConfirmed] = useState(true);
  const [notes, setNotes] = useState('');
  const reviewError = useReviewErrorHunter();

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!questionId || !ratingUserId || reviewError.isPending) return;
    reviewError.mutate({
      questionId: Number(questionId),
      ratingUserId: Number(ratingUserId),
      data: { isComfirmed: confirmed, notes: notes.trim() || undefined },
    });
  }

  return (
    <div className="space-y-5">
      <section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-border bg-card p-4 sm:p-6">
        <div className="flex min-w-0 items-start gap-3">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-academic-soft text-academic">
            <ShieldAlert aria-hidden="true" className="size-5" />
          </span>
          <div className="space-y-1">
            <h2 className="text-xl font-bold tracking-tight sm:text-2xl">Thợ săn lỗi câu hỏi</h2>
            <p className="max-w-2xl text-sm leading-relaxed text-muted-foreground">
              Báo lỗi kiến thức, dữ kiện hoặc đáp án trong lúc luyện tập để giảng viên thẩm định.
            </p>
          </div>
        </div>
        <div className="rounded-xl border border-success/25 bg-success-soft px-4 py-3">
          <p className="text-lg font-bold tabular-nums text-success">+{reportReward} điểm bí mật</p>
          <p className="text-xs text-muted-foreground">Mỗi báo cáo hợp lệ được xác nhận</p>
        </div>
      </section>

      <ol className="grid gap-3 sm:grid-cols-3">
        {steps.map(({ icon: Icon, title, text }) => (
          <li key={title} className="space-y-2 rounded-2xl border border-border bg-card p-4">
            <Icon aria-hidden="true" className="size-5 text-academic" />
            <h3 className="font-semibold">{title}</h3>
            <p className="text-sm leading-relaxed text-muted-foreground">{text}</p>
          </li>
        ))}
      </ol>
      <Button render={<Link to="/questions/practice" />} nativeButton={false} variant="outline">
        <BookOpen aria-hidden="true" /> Mở bộ câu hỏi luyện tập
      </Button>

      {canReview && (
        <section className="space-y-4 rounded-2xl border border-border bg-card p-4 sm:p-6">
          <h3 className="text-lg font-bold">Thẩm định báo lỗi</h3>
          <form onSubmit={submit} className="max-w-2xl space-y-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="error-hunter-question">ID câu hỏi</Label>
                <Input id="error-hunter-question" type="number" min={1} required value={questionId}
                  onChange={(event) => setQuestionId(event.target.value)} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="error-hunter-reporter">ID người báo lỗi</Label>
                <Input id="error-hunter-reporter" type="number" min={1} required value={ratingUserId}
                  onChange={(event) => setRatingUserId(event.target.value)} />
              </div>
            </div>
            <label className="flex min-h-11 cursor-pointer items-center gap-2 rounded-xl border border-border bg-surface-subtle px-3 text-sm">
              <input type="checkbox" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} className="size-4 shrink-0 accent-academic" />
              Xác nhận câu hỏi có lỗi
            </label>
            <div className="space-y-1.5">
              <Label htmlFor="error-hunter-notes">Ghi chú thẩm định</Label>
              <textarea id="error-hunter-notes" rows={3} value={notes} onChange={(event) => setNotes(event.target.value)}
                className="block w-full rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50" />
            </div>
            <Button type="submit" disabled={reviewError.isPending}>
              <Send aria-hidden="true" /> {reviewError.isPending ? 'Đang gửi…' : 'Gửi kết quả thẩm định'}
            </Button>
          </form>
        </section>
      )}
    </div>
  );
}
