export type MediaPurpose =
  | 'AVATAR'
  | 'COVER'
  | 'QUESTION'
  | 'POST'
  | 'COMMENT'
  | 'CHAT'
  | 'DOCUMENT';

export type MediaType = 'IMAGE' | 'VIDEO' | 'DOCUMENT';

export interface MediaUploadResponse {
  objectKey: string;
  url: string;
  fileSize: number;
  contentType: string;
  mediaType: MediaType;
  width?: number | null;
  height?: number | null;
}

export interface PresignFileItem {
  fileName: string;
  fileSize: number;
  contentType: string;
  purpose: MediaPurpose;
}

export interface PresignMediaRequest {
  files: PresignFileItem[];
}

export interface PresignedUrlItem {
  objectKey: string;
  uploadUrl: string;
  publicUrl: string;
  expiresInSeconds: number;
}

export interface PresignMediaResponse {
  presignedUrls: PresignedUrlItem[];
}

export interface MediaVerifyRequest {
  urls: string[];
}

export interface MediaVerifyResponse {
  missingUrls: string[];
}
