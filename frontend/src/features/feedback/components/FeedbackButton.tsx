import { Dialog } from '@base-ui/react/dialog';
import { Loader2, MessageSquarePlus } from 'lucide-react';
import { useId, useRef, useState, type FormEvent } from 'react';
import { useLocation } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { toast } from '@/stores/useToastStore';
import { useSendFeedback } from '../hooks/useSendFeedback';

const MAX_LENGTH = 2000;

export function FeedbackButton() {
  const { pathname } = useLocation();
  const messageId = useId();
  const messageInput = useRef<HTMLTextAreaElement>(null);
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [messageInvalid, setMessageInvalid] = useState(false);
  const send = useSendFeedback();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (send.isPending) return;

    const content = message.trim();
    if (!content) {
      setMessageInvalid(true);
      messageInput.current?.focus();
      return;
    }

    send.mutate({ message: content, pagePath: pathname.length <= 512 ? pathname : undefined }, {
      onSuccess: () => {
        setMessage('');
        setOpen(false);
        toast.success('Cảm ơn bạn đã góp ý cho SQB.');
      },
    });
  }

  const rateLimited = typeof send.error === 'object' && send.error !== null
    && 'code' in send.error && send.error.code === '1019';
  const errorMessage = rateLimited
    ? 'Mỗi tài khoản có thể gửi một góp ý mỗi phút. Hãy thử lại sau.'
    : 'Chưa gửi được góp ý. Nội dung vẫn ở đây để bạn thử lại.';

  return (
    <Dialog.Root open={open} onOpenChange={(nextOpen) => {
      if (send.isPending) return;
      setOpen(nextOpen);
      if (nextOpen) {
        setMessageInvalid(false);
        send.reset();
      }
    }}>
      <Dialog.Trigger
        render={<Button type="button" variant="ghost" className="h-11 min-w-11 px-2 text-muted-foreground sm:px-3" />}
        aria-label="Góp ý"
        title="Góp ý"
      >
        <MessageSquarePlus className="size-5" aria-hidden="true" />
        <span className="hidden sm:inline">Góp ý</span>
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-foreground/45 dark:bg-black/65" />
        <Dialog.Popup initialFocus={messageInput} className="fixed left-1/2 top-1/2 z-50 max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border border-border bg-card p-5 text-card-foreground shadow-xl sm:p-6">
          <Dialog.Title className="text-xl font-semibold">Góp ý cho SQB</Dialog.Title>
          <Dialog.Description className="mt-1 text-sm text-muted-foreground">
            Bạn gặp lỗi hoặc có ý tưởng cải thiện? Hãy kể ngắn gọn điều bạn đang làm.
          </Dialog.Description>

          <form onSubmit={submit} aria-busy={send.isPending} noValidate className="mt-5 space-y-4">
            <div className="space-y-2">
              <Label htmlFor={messageId}>Nội dung góp ý</Label>
              <Textarea id={messageId} ref={messageInput} value={message} rows={5} maxLength={MAX_LENGTH}
                required readOnly={send.isPending} aria-invalid={messageInvalid}
                aria-describedby={`${messageId}-hint${messageInvalid ? ` ${messageId}-error` : ''}`}
                onChange={(event) => {
                  setMessage(event.target.value);
                  setMessageInvalid(false);
                  if (send.isError) send.reset();
                }} />
              <div id={`${messageId}-hint`} className="flex flex-wrap justify-between gap-x-2 gap-y-1 text-xs text-muted-foreground">
                <span>Gửi kèm đường dẫn màn hình và ID tài khoản SQB.</span>
                <span>{message.length}/{MAX_LENGTH}</span>
              </div>
              {messageInvalid && <p id={`${messageId}-error`} role="alert" className="text-sm text-destructive">Hãy nhập nội dung góp ý.</p>}
            </div>
            {send.isError && <p role="alert" className="rounded-xl bg-destructive/10 p-3 text-sm text-destructive">{errorMessage}</p>}
            <div className="flex flex-wrap justify-end gap-2">
              <Dialog.Close render={<Button type="button" variant="outline" disabled={send.isPending} />}>
                Đóng
              </Dialog.Close>
              <Button type="submit" disabled={send.isPending}>
                {send.isPending && <Loader2 className="motion-safe:animate-spin" aria-hidden="true" />}
                {send.isPending ? 'Đang gửi…' : 'Gửi góp ý'}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
