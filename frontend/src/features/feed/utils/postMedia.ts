import type { MediaItem } from '@/types/post.types';

export type ApiMedia = Omit<MediaItem, 'objectKey' | 'mediaType'> &
  Partial<Pick<MediaItem, 'objectKey' | 'mediaType'>> & { key?: string; type?: string };

export function normalizePostMedia(items?: ApiMedia[] | null): MediaItem[] | undefined {
  return items?.filter((item) => item?.url).map(({ key, type, ...media }) => ({
    ...media,
    objectKey: key || media.objectKey || media.url,
    mediaType: type || media.mediaType || (/\.(mp4|webm|mov|m4v)(?:$|[?#])/i.test(media.url) ? 'VIDEO' : 'IMAGE'),
  }));
}

export function isVideoMedia(media: MediaItem) {
  return /^video(?:\/|$)/i.test(media.mediaType);
}
