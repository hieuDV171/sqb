import { Form, ValidatedField } from '@/components/ui/form';
import { useId, useState, type SubmitEvent } from 'react';
import { Dialog } from '@base-ui/react/dialog';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { getErrorMessage } from '@/lib/errorMessage';
import { managedTopicCatalog, saveTopic, subjectTopicsKey, type ManagedTopic } from '../services/topicService';

interface TopicCatalogDialogProps {
  subjectId: number;
  subjectName?: string;
  onClose: () => void;
}

function isStaleTopicError(error: unknown) {
  return typeof error === 'object' && error !== null && 'code' in error && error.code === '2010';
}

export function TopicCatalogDialog({ subjectId, subjectName, onClose }: TopicCatalogDialogProps) {
  const id = useId();
  const client = useQueryClient();
  const catalog = useQuery(managedTopicCatalog(subjectId));
  const [name, setName] = useState('');
  const [editing, setEditing] = useState<ManagedTopic | null>(null);
  const save = useMutation({
    mutationFn: ({ topic, name, active }: { topic?: ManagedTopic; name: string; active?: boolean }) =>
      saveTopic(subjectId, topic ? { id: topic.id, version: topic.version, name, active } : { name }),
    onSuccess: async () => {
      setName('');
      setEditing(null);
      await client.invalidateQueries({ queryKey: subjectTopicsKey(subjectId) });
    },
    onError: async (error) => {
      if (isStaleTopicError(error)) {
        await client.invalidateQueries({ queryKey: subjectTopicsKey(subjectId) });
      }
    },
  });
  const latestTopic = catalog.data?.find((topic) => topic.id === editing?.id);
  const hasNewerVersion = editing != null && latestTopic != null && latestTopic.version !== editing.version;
  const staleSave = save.isError && isStaleTopicError(save.error);
  const staleEdit = staleSave && editing != null && save.variables?.topic?.id === editing.id
    && save.variables.active === undefined;
  const needsLatestVersion = hasNewerVersion || staleEdit;

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (name.trim() && !save.isPending && !needsLatestVersion) {
      save.mutate({ topic: editing ?? undefined, name: name.trim() });
    }
  }

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open && !save.isPending) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/60" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-50 mx-auto max-h-[calc(100dvh-2rem)] max-w-lg -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-4 shadow-xl">
          <div className="space-y-1">
            <Dialog.Title className="text-lg font-semibold">Chủ đề môn học</Dialog.Title>
            {subjectName && <p className="wrap-anywhere text-sm font-medium">{subjectName}</p>}
          </div>
          <Dialog.Description className="text-sm text-muted-foreground">
            Chủ đề ngừng sử dụng vẫn hiển thị trên các câu hỏi đã gắn.
          </Dialog.Description>
          {catalog.isPending ? <p role="status">Đang tải chủ đề…</p> : catalog.isError && !catalog.data ? (
            <div role="alert" className="flex items-center gap-2 text-sm">
              Không tải được danh mục.
              <Button type="button" variant="outline" onClick={() => void catalog.refetch()}>Thử lại</Button>
            </div>
          ) : (
            <>
              <Form onSubmit={submit} className="flex flex-wrap items-end gap-2">
                <div className="min-w-40 flex-1 space-y-1.5">
                  <Label htmlFor={id}>{editing ? 'Đổi tên chủ đề' : 'Tên chủ đề mới'}</Label>
                  <ValidatedField>
                    <Input id={id} value={name} onChange={(event) => setName(event.target.value)}
                      required maxLength={100} disabled={save.isPending} className="min-h-11" />
                  </ValidatedField>
                </div>
                <Button type="submit" className="min-h-11" disabled={save.isPending || !name.trim() || needsLatestVersion}>
                  {save.isPending ? 'Đang lưu…' : editing ? 'Lưu tên' : 'Thêm chủ đề'}
                </Button>
                {editing && <Button type="button" variant="ghost" disabled={save.isPending}
                  onClick={() => { setEditing(null); setName(''); save.reset(); }}>Hủy sửa</Button>}
              </Form>
              {needsLatestVersion && (
                <div className="space-y-2">
                  <p role="alert" className="wrap-anywhere text-sm">
                    Chủ đề đã thay đổi trong lúc bạn sửa. Tên bạn nhập vẫn được giữ.
                    {hasNewerVersion && latestTopic && (
                      <> Tên hiện tại là <strong>{latestTopic.name}</strong> ({latestTopic.active ? 'đang dùng' : 'đã ngừng dùng'}).</>
                    )}
                  </p>
                  {hasNewerVersion && latestTopic && !catalog.isError ? (
                    <Button type="button" variant="outline" className="min-h-11" disabled={save.isPending || catalog.isFetching}
                      onClick={() => { setEditing(latestTopic); save.reset(); }}>
                      Tiếp tục với phiên bản mới
                    </Button>
                  ) : (
                    <Button type="button" variant="outline" className="min-h-11" disabled={save.isPending || catalog.isFetching}
                      onClick={() => void catalog.refetch()}>
                      Tải lại danh mục
                    </Button>
                  )}
                </div>
              )}
              {staleSave && !needsLatestVersion && (
                <div className="space-y-2">
                  <p role="alert" className="text-sm">
                    {catalog.isError
                      ? 'Chủ đề đã thay đổi. Tải lại danh mục để thử lại.'
                      : 'Chủ đề đã thay đổi. Xem lại trạng thái trong danh sách rồi chọn thao tác.'}
                  </p>
                  {catalog.isError && (
                    <Button type="button" variant="outline" className="min-h-11" disabled={catalog.isFetching}
                      onClick={() => void catalog.refetch()}>Tải lại danh mục</Button>
                  )}
                </div>
              )}
              {save.isError && !staleSave && <p role="alert" className="text-sm text-destructive">{getErrorMessage(save.error) ?? 'Không lưu được chủ đề.'}</p>}
              {!catalog.data?.length ? <p className="text-sm text-muted-foreground">Môn học chưa có chủ đề.</p> : (
                <ul className="divide-y divide-border">
                  {catalog.data.map((topic) => (
                    <li key={topic.id} className="flex flex-wrap items-center gap-2 py-2">
                      <span className="min-w-32 flex-1 wrap-anywhere text-sm">
                        {topic.name}{!topic.active && <span className="block text-muted-foreground">Đã ngừng sử dụng</span>}
                      </span>
                      <Button type="button" variant="ghost" className="min-h-11" disabled={save.isPending}
                        aria-label={`Đổi tên ${topic.name}`}
                        onClick={() => { setEditing(topic); setName(topic.name); save.reset(); }}>Đổi tên</Button>
                      <Button type="button" variant="outline" className="min-h-11" disabled={save.isPending || (staleSave && catalog.isError)}
                        aria-label={`${topic.active ? 'Ngừng sử dụng' : 'Mở lại'} ${topic.name}`}
                        onClick={() => save.mutate({ topic, name: topic.name, active: !topic.active })}>
                        {topic.active ? 'Ngừng dùng' : 'Mở lại'}
                      </Button>
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
          <Dialog.Close render={<Button type="button" variant="outline" className="min-h-11" disabled={save.isPending} />}>
            Đóng
          </Dialog.Close>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
