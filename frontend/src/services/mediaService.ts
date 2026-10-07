import axios from 'axios';
import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type {
  MediaPurpose,
  MediaUploadResponse,
  PresignMediaRequest,
  PresignMediaResponse,
  MediaVerifyResponse,
} from '@/types/media.types';

/**
 * Tự động nén ảnh sang định dạng WebP bằng HTML5 Canvas để tối ưu 90-95% dung lượng lưu trữ R2:
 * - Avatar: Tối đa 512px, chất lượng 85%
 * - Cover: Tối đa 1280px, chất lượng 82%
 * - Khác (Post, Question, Chat): Tối đa 1920px, chất lượng 82%
 * - Bỏ qua tệp GIF (giữ animation) và SVG (vector)
 */
async function compressImageIfNeeded(file: File, purpose: MediaPurpose = 'QUESTION'): Promise<File> {
  // Chỉ nén các tệp hình ảnh, bỏ qua GIF và SVG
  if (!file.type.startsWith('image/') || file.type === 'image/gif' || file.type === 'image/svg+xml') {
    return file;
  }

  // Cấu hình kích thước trần theo mục đích
  let maxDimension = 1920;
  let quality = 0.82;
  if (purpose === 'AVATAR') {
    maxDimension = 512;
    quality = 0.85;
  } else if (purpose === 'COVER') {
    maxDimension = 1280;
    quality = 0.82;
  }

  return new Promise<File>((resolve) => {
    const img = new Image();
    const objectUrl = URL.createObjectURL(file);

    img.onload = () => {
      URL.revokeObjectURL(objectUrl);
      let { width, height } = img;

      // Tính toán tỉ lệ scale giữ nguyên aspect ratio
      if (width > maxDimension || height > maxDimension) {
        if (width > height) {
          height = Math.round((height * maxDimension) / width);
          width = maxDimension;
        } else {
          width = Math.round((width * maxDimension) / height);
          height = maxDimension;
        }
      }

      const canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;

      const ctx = canvas.getContext('2d');
      if (!ctx) {
        resolve(file); // Fallback nếu không lấy được 2d context
        return;
      }

      ctx.drawImage(img, 0, 0, width, height);

      canvas.toBlob(
        (blob) => {
          if (!blob || blob.size >= file.size) {
            // Nếu nén không làm giảm dung lượng (rất hiếm), giữ nguyên file gốc
            resolve(file);
            return;
          }

          // Thay đuôi mở rộng thành .webp
          const newName = file.name.replace(/\.[^/.]+$/, '') + '.webp';
          const compressedFile = new File([blob], newName, {
            type: 'image/webp',
            lastModified: Date.now(),
          });

          resolve(compressedFile);
        },
        'image/webp',
        quality
      );
    };

    img.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(file); // Fallback an toàn nếu có lỗi giải mã ảnh
    };

    img.src = objectUrl;
  });
}

export const mediaService = {
  /**
   * Upload file media chuẩn 2 bước qua Presigned URL (Tự động nén WebP trước khi gửi):
   * Bước 1: Gọi BE POST /medias/presign lấy uploadUrl (Presigned PUT) và publicUrl
   * Bước 2: Upload trực tiếp payload file từ Browser lên Cloudflare R2 qua HTTP PUT
   */
  uploadViaPresign: async (
    file: File,
    purpose: MediaPurpose = 'QUESTION'
  ): Promise<{ objectKey: string; publicUrl: string; expiresInSeconds: number }> => {
    try {
      // Tự động nén ảnh sang WebP (giảm 90-95% dung lượng) trước khi xin presign và upload
      const processedFile = await compressImageIfNeeded(file, purpose);

      const presignPayload: PresignMediaRequest = {
        files: [
          {
            fileName: processedFile.name,
            fileSize: processedFile.size,
            contentType: processedFile.type || 'application/octet-stream',
            purpose,
          },
        ],
      };

      // Bước 1: Lấy Presigned URL từ Backend Spring Boot
      const presignRes = await axiosClient.post<GlobalResponse<PresignMediaResponse>>(
        '/medias/presign',
        presignPayload
      );

      const presignedItem = presignRes?.data?.data?.presignedUrls?.[0];
      if (!presignedItem || !presignedItem.uploadUrl) {
        throw new Error('Không nhận được Presigned Upload URL từ máy chủ');
      }

      // Bước 2: Dùng axios thông thường (không kèm header Bearer token của App) để PUT trực tiếp lên Cloudflare R2
      await axios.put(presignedItem.uploadUrl, processedFile, {
        headers: {
          'Content-Type': processedFile.type || 'application/octet-stream',
        },
      });

      return {
        objectKey: presignedItem.objectKey,
        publicUrl: presignedItem.publicUrl,
        expiresInSeconds: presignedItem.expiresInSeconds,
      };
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Tải tệp media lên S3/R2 thất bại');
    }
  },

  /**
   * Upload trực tiếp dạng Multipart qua Backend controller (Fallback)
   */
  uploadMedia: async (
    file: File,
    purpose: MediaPurpose = 'QUESTION'
  ): Promise<GlobalResponse<MediaUploadResponse>> => {
    const processedFile = await compressImageIfNeeded(file, purpose);

    const formData = new FormData();
    formData.append('file', processedFile);

    const response = await axiosClient.post<GlobalResponse<MediaUploadResponse>>(
      `/medias/upload?purpose=${purpose}`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );

    if (response?.data?.data) {
      return response.data;
    }
    throw new Error('Không nhận được thông tin tệp tải lên từ máy chủ');
  },

  /**
   * Tối ưu hóa: Kiểm tra Batch toàn bộ danh sách URL ảnh xem có ảnh nào bị thiếu trên R2 không.
   * Gửi đúng 1 request duy nhất POST /medias/verify lên server thay vì tải N ảnh qua mạng từ Client.
   */
  verifyMedias: async (urls: string[]): Promise<string[]> => {
    const validUrls = urls.filter((u) => u && !u.startsWith('blob:') && !u.startsWith('data:'));
    if (validUrls.length === 0) return [];

    try {
      const response = await axiosClient.post<GlobalResponse<MediaVerifyResponse>>('/medias/verify', {
        urls: validUrls,
      });
      return response.data?.data?.missingUrls || [];
    } catch {
      // Giả lập an toàn khi offline / server mock: không chặn oan sinh viên
      return [];
    }
  },
};
