import { AlertDialog } from '@base-ui/react/alert-dialog';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Loader2, Trash2 } from 'lucide-react';
import { useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { userService } from '@/services/userService';
import { toast } from '@/stores/useToastStore';

const deadlineFormat = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'long', timeStyle: 'short' });

export function AccountDeletionSettings({ userId }: { userId: number }) {
  const queryClient = useQueryClient();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const backButton = useRef<HTMLButtonElement>(null);
  const sectionTitle = useRef<HTMLHeadingElement>(null);
  const queryKey = ['account-deletion', userId];
  const status = useQuery({
    queryKey,
    queryFn: ({ signal }) => userService.getAccountDeletion(signal).then(({ data }) => data),
    refetchOnMount: 'always',
    refetchOnWindowFocus: 'always',
  });
  const deletion = useMutation({
    mutationFn: (action: 'request' | 'cancel') => action === 'request'
      ? userService.requestAccountDeletion()
      : userService.cancelAccountDeletion(),
    onSuccess: async ({ data }, action) => {
      await queryClient.cancelQueries({ queryKey });
      queryClient.setQueryData(queryKey, data);
      setConfirmOpen(false);
      toast.success(action === 'request' ? 'Đã lên lịch xóa tài khoản.' : 'Đã hủy yêu cầu xóa tài khoản.');
    },
    onError: () => queryClient.invalidateQueries({ queryKey }),
  });
  const dueAt = status.data?.scheduledFor;
  const errorMessage = typeof deletion.error === 'object' && deletion.error !== null
    && 'code' in deletion.error && deletion.error.code === '1016'
    ? 'Đã hết thời hạn hủy yêu cầu xóa tài khoản.'
    : 'Không thể lưu thay đổi. Vui lòng thử lại.';

  const closeConfirmation = () => {
    deletion.reset();
    setConfirmOpen(false);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h2 ref={sectionTitle} tabIndex={-1} className="flex items-center gap-2 text-lg">
            <Trash2 className="size-5 text-destructive" aria-hidden="true" /> Xóa tài khoản
          </h2>
        </CardTitle>
        <CardDescription>
          Bạn vẫn sử dụng tài khoản bình thường và có thể hủy yêu cầu tại đây trong 30 ngày chờ xóa.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {status.isPending && <p role="status" className="text-sm text-muted-foreground">Đang tải trạng thái yêu cầu…</p>}
        {status.isLoadingError && (
          <div className="space-y-2">
            <p role="alert" className="text-sm text-destructive">Không thể tải trạng thái yêu cầu.</p>
            <Button variant="outline" disabled={status.isFetching} onClick={() => void status.refetch()}>Thử lại</Button>
          </div>
        )}
        {status.data && (dueAt ? (
          <>
            <p role="status" className="text-sm font-medium">
              {status.data.canCancel ? 'Tài khoản được lên lịch xóa vào ' : 'Đã đến hạn xóa tài khoản vào '}
              <time dateTime={dueAt}>{deadlineFormat.format(new Date(dueAt))}</time>.
            </p>
            {status.data.canCancel && (
              <Button variant="outline" disabled={deletion.isPending} onClick={() => deletion.mutate('cancel')}>
                {deletion.isPending ? 'Đang hủy yêu cầu…' : 'Hủy yêu cầu xóa'}
              </Button>
            )}
          </>
        ) : (
          <Button variant="destructive" disabled={deletion.isPending} onClick={() => { deletion.reset(); setConfirmOpen(true); }}>
            Yêu cầu xóa tài khoản
          </Button>
        ))}
        {deletion.isError && !confirmOpen && <p role="alert" className="text-sm text-destructive">{errorMessage}</p>}
      </CardContent>

      <AlertDialog.Root open={confirmOpen} onOpenChange={(open) => { if (!open && !deletion.isPending) closeConfirmation(); }}>
        <AlertDialog.Portal>
          <AlertDialog.Backdrop className="fixed inset-0 z-50 bg-foreground/45 dark:bg-black/65" />
          <AlertDialog.Popup
            initialFocus={backButton}
            finalFocus={sectionTitle}
            className="fixed left-1/2 top-1/2 z-50 max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 text-card-foreground shadow-xl"
          >
            <AlertDialog.Title className="text-lg font-semibold">Yêu cầu xóa tài khoản?</AlertDialog.Title>
            <AlertDialog.Description className="text-sm leading-relaxed text-muted-foreground">
              Sau 30 ngày, tài khoản sẽ bị vô hiệu hóa và thông tin hồ sơ sẽ được ẩn danh.
              Bạn có thể hủy yêu cầu trong Cài đặt trước khi hết hạn.
            </AlertDialog.Description>
            {deletion.isError && <p role="alert" className="text-sm text-destructive">{errorMessage}</p>}
            <div className="flex flex-wrap justify-end gap-2">
              <Button ref={backButton} variant="outline" disabled={deletion.isPending} onClick={closeConfirmation}>
                Quay lại
              </Button>
              <Button variant="destructive" disabled={deletion.isPending} onClick={() => deletion.mutate('request')}>
                {deletion.isPending && <Loader2 className="motion-safe:animate-spin" aria-hidden="true" />}
                {deletion.isPending ? 'Đang gửi yêu cầu…' : 'Lên lịch xóa'}
              </Button>
            </div>
          </AlertDialog.Popup>
        </AlertDialog.Portal>
      </AlertDialog.Root>
    </Card>
  );
}
