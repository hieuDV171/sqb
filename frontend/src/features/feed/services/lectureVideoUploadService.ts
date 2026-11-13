import axios from 'axios';
import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type { MediaItem } from '@/types/post.types';

const LECTURE_UPLOAD_PATH = '/lecturer/medias/lecture-uploads';

type UploadedVideo = Omit<MediaItem, 'objectKey' | 'mediaType'> & { key: string; type: 'VIDEO' };

interface UploadStatus {
  partSize: number;
  uploadedParts: number[];
  media?: UploadedVideo;
}

function toMediaItem({ key, type, ...media }: UploadedVideo): MediaItem {
  return { ...media, objectKey: key, mediaType: type };
}

export async function uploadLectureVideo(
  id: string,
  file: File,
  signal: AbortSignal,
  onProgress: (loaded: number) => void,
  onFinalizing: () => void,
): Promise<MediaItem> {
  const path = `${LECTURE_UPLOAD_PATH}/${id}`;
  const config = { signal, skipToast: true };
  const contentType = /\.mp4$/i.test(file.name) ? 'video/mp4' : /\.webm$/i.test(file.name) ? 'video/webm' : file.type;
  const { data: status } = await axiosClient.put<unknown, GlobalResponse<UploadStatus>>(
    path, { fileName: file.name, fileSize: file.size, contentType }, config,
  );
  if (status.media) return toMediaItem(status.media);

  const uploaded = new Set(status.uploadedParts);
  let loaded = status.uploadedParts.reduce(
    (total, number) => total + Math.min(status.partSize, file.size - (number - 1) * status.partSize),
    0,
  );
  onProgress(loaded);
  for (let offset = 0; offset < file.size; offset += status.partSize) {
    const number = offset / status.partSize + 1;
    if (uploaded.has(number)) continue;

    const part = file.slice(offset, offset + status.partSize);
    const { data } = await axiosClient.post<unknown, GlobalResponse<{ uploadUrl: string }>>(
      `${path}/parts/${number}`, null, config,
    );
    const previouslyLoaded = loaded;
    await axios.put(data.uploadUrl, part, {
      signal,
      headers: { 'Content-Type': 'application/octet-stream' },
      onUploadProgress: (event) => onProgress(previouslyLoaded + Math.min(part.size, event.loaded)),
    });
    loaded += part.size;
    onProgress(loaded);
  }

  onFinalizing();
  const { data } = await axiosClient.post<unknown, GlobalResponse<UploadedVideo>>(`${path}/complete`, null, config);
  return toMediaItem(data);
}

export async function cancelLectureVideoUpload(id: string): Promise<void> {
  await axiosClient.delete(`${LECTURE_UPLOAD_PATH}/${id}`, { skipToast: true });
}
