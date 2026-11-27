import { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { cancelLectureVideoUpload, uploadLectureVideo } from '../services/lectureVideoUploadService';
import type { MediaItem } from '@/types/post.types';

type Upload = { id: string; file: File } & (
  | { state: 'uploading'; loaded: number }
  | { state: 'finalizing' }
  | { state: 'failed'; message: string; retryable: boolean }
  | { state: 'ready'; media: MediaItem }
);

export function useLectureVideoUpload() {
  const [upload, setUpload] = useState<Upload | null>(null);
  const [selectionError, setSelectionError] = useState('');
  const active = useRef<AbortController | null>(null);

  useEffect(() => () => active.current?.abort(), []);

  const run = async (id: string, file: File) => {
    if (active.current) return;
    const controller = new AbortController();
    active.current = controller;
    setSelectionError('');
    setUpload({ id, file, state: 'uploading', loaded: 0 });
    try {
      const media = await uploadLectureVideo(id, file, controller.signal,
        (loaded) => {
          if (!controller.signal.aborted) setUpload({ id, file, state: 'uploading', loaded });
        },
        () => {
          if (!controller.signal.aborted) setUpload({ id, file, state: 'finalizing' });
        },
      );
      if (!controller.signal.aborted) setUpload({ id, file, state: 'ready', media });
    } catch (error) {
      if (controller.signal.aborted) return;
      const apiError = !axios.isAxiosError(error) && typeof error === 'object' && error !== null ? error : null;
      const message = apiError && 'message' in apiError && typeof apiError.message === 'string'
        ? apiError.message
        : 'Tải video bị gián đoạn. Bấm Tải tiếp để gửi những phần còn thiếu.';
      const retryable = !(apiError && 'code' in apiError && ['1006', '1009', '1012'].includes(String(apiError.code)));
      setUpload({ id, file, state: 'failed', message, retryable });
    } finally {
      if (active.current === controller) active.current = null;
    }
  };

  const choose = (file: File) => {
    if (!/\.(mp4|webm)$/i.test(file.name)) {
      setSelectionError('Hãy chuyển video sang MP4 hoặc WebM trước khi tải lên.');
      return;
    }
    if (file.size === 0) {
      setSelectionError('Tệp video rỗng. Hãy chọn một tệp khác.');
      return;
    }
    void run(crypto.randomUUID(), file);
  };

  const clear = () => {
    active.current?.abort();
    active.current = null;
    if (upload) void cancelLectureVideoUpload(upload.id).catch(() => undefined);
    setUpload(null);
    setSelectionError('');
  };

  return {
    upload,
    selectionError,
    choose,
    clear,
    retry: () => { if (upload?.state === 'failed' && upload.retryable) void run(upload.id, upload.file); },
    published: () => setUpload(null),
  };
}
