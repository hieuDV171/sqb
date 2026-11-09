import { useId, useState, type FormEvent } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { Loader2, Plus, Trash2, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { QuestionOption } from '@/features/session/types/session.types';
import type { EditQuestionRequest, SessionQuestionReviewDto } from '../types/review.types';

interface EditQuestionModalProps {
  question: SessionQuestionReviewDto;
  questionNumber: number;
  isSaving: boolean;
  onClose: () => void;
  onSave: (data: EditQuestionRequest) => Promise<void>;
}

const letters = [...'ABCDEFGHIJKLMNOPQRSTUVWXYZ'];
const textareaClass = 'w-full rounded-xl border border-input bg-background p-3 text-sm focus-visible:outline-2 focus-visible:outline-ring';

export function EditQuestionModal({ question, questionNumber, isSaving, onClose, onSave }: EditQuestionModalProps) {
  const id = useId();
  const [content, setContent] = useState(question.content ?? '');
  const [explanation, setExplanation] = useState(question.explanation ?? '');
  const [options, setOptions] = useState<QuestionOption[]>(() => question.options?.length
    ? question.options.map((option) => ({ ...option }))
    : letters.slice(0, 4).map((key, index) => ({ key, text: '', isCorrect: index === 0 })));
  const [autoApprove, setAutoApprove] = useState(true);
  const [error, setError] = useState('');
  const nextKey = letters.find((key) => !options.some((option) => option.key === key));

  function updateOption(key: string, patch: Partial<QuestionOption>) {
    setOptions((previous) => previous.map((option) => option.key === key ? { ...option, ...patch } : option));
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSaving) return;
    if (!content.trim() || options.length < 2 || options.some((option) => !option.text?.trim() && !option.mediaUrl)
      || !options.some((option) => option.isCorrect)) {
      setError('Nhập nội dung câu hỏi, ít nhất hai phương án và đánh dấu ít nhất một đáp án đúng.');
      return;
    }
    setError('');
    try {
      await onSave({ content: content.trim(), explanation: explanation.trim(), options, autoApprove });
    } catch {
      setError('Không thể lưu thay đổi. Bạn có thể thử lại.');
    }
  }

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !isSaving) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto flex max-h-[calc(100dvh-2rem)] max-w-2xl -translate-y-1/2 flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-xl">
          <header className="flex items-start justify-between gap-2 border-b border-border p-4">
            <div className="space-y-1">
              <Dialog.Title className="text-xl font-semibold">Biên tập câu {questionNumber}</Dialog.Title>
              <Dialog.Description className="text-sm text-muted-foreground">Chỉnh nội dung, phương án và lời giải.</Dialog.Description>
            </div>
            <Dialog.Close render={<Button variant="ghost" size="icon" aria-label="Đóng biên tập" disabled={isSaving} />}>
              <X aria-hidden="true" />
            </Dialog.Close>
          </header>
          <form onSubmit={submit} className="min-h-0 overflow-y-auto">
            <fieldset disabled={isSaving} className="space-y-4 p-4">
              <div className="space-y-2">
                <Label htmlFor={`${id}-content`}>Nội dung câu hỏi</Label>
                <textarea id={`${id}-content`} rows={3} required value={content} className={textareaClass}
                  onChange={(event) => setContent(event.target.value)} />
              </div>
              <fieldset className="space-y-2">
                <legend className="text-sm font-semibold">Phương án trả lời</legend>
                {options.map((option) => (
                  <div key={option.key} className="space-y-2 rounded-xl border border-border bg-surface-subtle p-3">
                    <div className="flex items-center justify-between gap-2">
                      <Label htmlFor={`${id}-option-${option.key}`}>Phương án {option.key}</Label>
                      {options.length > 2 && (
                        <Button variant="ghost" size="icon" type="button" aria-label={`Xóa phương án ${option.key}`}
                          onClick={() => setOptions((previous) => previous.filter((item) => item.key !== option.key))}>
                          <Trash2 aria-hidden="true" />
                        </Button>
                      )}
                    </div>
                    <Input id={`${id}-option-${option.key}`} required={!option.mediaUrl} value={option.text ?? ''}
                      className="min-h-11" onChange={(event) => updateOption(option.key, { text: event.target.value })} />
                    {option.mediaUrl && <img src={option.mediaUrl} alt={`Ảnh phương án ${option.key}`} className="max-h-20 max-w-full rounded-lg object-contain" />}
                    <label className="flex min-h-11 cursor-pointer items-center gap-2 text-sm">
                      <input type="checkbox" checked={option.isCorrect} className="size-4 accent-academic"
                        onChange={(event) => updateOption(option.key, { isCorrect: event.target.checked })} />
                      Đáp án đúng
                    </label>
                  </div>
                ))}
                {nextKey && (
                  <Button type="button" variant="outline" className="min-h-11" onClick={() => setOptions([
                    ...options, { key: nextKey, text: '', isCorrect: false },
                  ])}><Plus aria-hidden="true" /> Thêm phương án</Button>
                )}
              </fieldset>
              <div className="space-y-2">
                <Label htmlFor={`${id}-explanation`}>Lời giải</Label>
                <textarea id={`${id}-explanation`} rows={3} value={explanation} className={textareaClass}
                  onChange={(event) => setExplanation(event.target.value)} />
              </div>
              <label className="flex min-h-11 cursor-pointer items-center gap-2 text-sm font-medium">
                <input type="checkbox" checked={autoApprove} onChange={(event) => setAutoApprove(event.target.checked)}
                  className="size-4 accent-academic" /> Phê duyệt sau khi lưu
              </label>
            </fieldset>
            {error && <p role="alert" className="px-4 pb-3 text-sm text-destructive">{error}</p>}
            <footer className="sticky bottom-0 flex flex-wrap justify-end gap-2 border-t border-border bg-card p-4">
              <Dialog.Close render={<Button variant="outline" className="min-h-11" disabled={isSaving} />}>Hủy</Dialog.Close>
              <Button type="submit" className="min-h-11" disabled={isSaving}>
                {isSaving && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
                {isSaving ? 'Đang lưu…' : autoApprove ? 'Lưu và phê duyệt' : 'Lưu thay đổi'}
              </Button>
            </footer>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
