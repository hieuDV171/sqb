import { useState, useRef, useEffect, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { useChatStore } from '../stores/useChatStore';
import { mediaService } from '@/services/mediaService';
import type { MessageDto, MessageType } from '../types/chat.types';
import {
  Send,
  Image as ImageIcon,
  X,
  Loader2,
  CornerDownRight,
} from 'lucide-react';
import { toast } from '@/stores/useToastStore';

const TYPING_IDLE_MS = 2500;

interface ChatInputProps {
  conversationId: number;
  typingUsers: string[];
  replyTo: MessageDto | null;
  canSend: boolean;
  onCancelReply: () => void;
}

export function ChatInput({
  conversationId,
  typingUsers,
  replyTo,
  canSend,
  onCancelReply,
}: ChatInputProps) {
  const { sendMessage, sendTyping } = useChatStore();
  const [content, setContent] = useState('');
  const [selectedImages, setSelectedImages] = useState<{ file: File; previewUrl: string }[]>([]);
  const [isUploading, setIsUploading] = useState(false);

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const typingTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const lastTypingSignal = useRef<number | null>(null);
  const imagePreviewRef = useRef(selectedImages);

  useEffect(() => { imagePreviewRef.current = selectedImages; }, [selectedImages]);
  useEffect(() => () => {
    imagePreviewRef.current.forEach(({ previewUrl }) => URL.revokeObjectURL(previewUrl));
  }, []);

  const stopTyping = useCallback(() => {
    if (typingTimerRef.current !== null) {
      clearTimeout(typingTimerRef.current);
      typingTimerRef.current = null;
    }
    if (lastTypingSignal.current !== null) {
      lastTypingSignal.current = null;
      sendTyping(conversationId, false);
    }
  }, [conversationId, sendTyping]);

  useEffect(() => stopTyping, [stopTyping]);

  // Focus textarea when conversation or reply changes
  useEffect(() => {
    if (replyTo || window.matchMedia('(pointer: fine)').matches) textareaRef.current?.focus();
  }, [conversationId, replyTo]);

  // Tự động điều chỉnh chiều cao textarea theo nội dung
  useEffect(() => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 120)}px`;
  }, [content]);

  // Xử lý gửi trạng thái Typing
  const handleTypingEvent = (text: string) => {
    if (!canSend || !text.trim()) {
      stopTyping();
      return;
    }
    const now = Date.now();
    if (lastTypingSignal.current === null || now - lastTypingSignal.current >= 1200) {
      if (!sendTyping(conversationId, true)) return;
      lastTypingSignal.current = now;
    }
    if (typingTimerRef.current !== null) clearTimeout(typingTimerRef.current);
    typingTimerRef.current = setTimeout(stopTyping, TYPING_IDLE_MS);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    const newItems: { file: File; previewUrl: string }[] = [];
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (!file.type.startsWith('image/')) {
        toast.warning('Chỉ hỗ trợ đính kèm tệp hình ảnh');
        continue;
      }
      newItems.push({
        file,
        previewUrl: URL.createObjectURL(file),
      });
    }

    setSelectedImages((prev) => [...prev, ...newItems]);
    // Reset file input
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const removeImage = (index: number) => {
    setSelectedImages((prev) => {
      const copy = [...prev];
      URL.revokeObjectURL(copy[index].previewUrl);
      copy.splice(index, 1);
      return copy;
    });
  };

  const handleSend = async () => {
    const trimmed = content.trim();
    if (!canSend || isUploading || (!trimmed && selectedImages.length === 0)) return;

    try {
      const uploadedUrls: string[] = [];

      if (selectedImages.length > 0) {
        setIsUploading(true);
        // Upload tuần tự các ảnh (được tự động nén WebP trước khi gửi lên R2)
        for (const item of selectedImages) {
          const res = await mediaService.uploadViaPresign(item.file, 'QUESTION');
          uploadedUrls.push(res.publicUrl);
        }
      }

      let msgType: MessageType = 'TEXT';
      if (uploadedUrls.length > 0) {
        msgType = 'IMAGE';
      }

      const success = sendMessage(
        conversationId,
        trimmed || undefined,
        msgType,
        uploadedUrls.length > 0 ? uploadedUrls : undefined,
        replyTo ? replyTo.messageId : undefined
      );

      if (success) {
        setContent('');
        // Thu hồi object URLs
        selectedImages.forEach((img) => URL.revokeObjectURL(img.previewUrl));
        setSelectedImages([]);
        onCancelReply();

        stopTyping();

        if (textareaRef.current) {
          textareaRef.current.style.height = 'auto';
        }
      } else {
        toast.error('Không thể gửi tin nhắn. Kết nối WebSocket bị gián đoạn.');
      }
    } catch (err) {
      console.error('[ChatInput] Lỗi khi gửi tin nhắn:', err);
      toast.error('Có lỗi xảy ra khi tải ảnh hoặc gửi tin nhắn');
    } finally {
      setIsUploading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="shrink-0 space-y-2 border-t border-border bg-card p-2 sm:p-3">
      {/* Reply Preview Bar */}
      {replyTo && (
        <div className="flex items-center justify-between gap-2 px-3 py-1.5 rounded-xl bg-academic-soft border-l-4 border-academic text-xs animate-in fade-in duration-150">
          <div className="flex items-center gap-2 truncate">
            <CornerDownRight className="w-3.5 h-3.5 text-academic shrink-0" />
            <span className="font-semibold text-foreground truncate">
              Đang trả lời {replyTo.senderName}:
            </span>
            <span className="text-muted-foreground truncate">
              {replyTo.content || '[Hình ảnh]'}
            </span>
          </div>

          <button
            type="button"
            onClick={onCancelReply}
            className="p-1 rounded-md text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Selected Images Strip */}
      {selectedImages.length > 0 && (
        <div className="flex items-center gap-2 overflow-x-auto py-1">
          {selectedImages.map((img, idx) => (
            <div key={idx} className="relative w-16 h-16 shrink-0 rounded-xl overflow-hidden border border-slate-200 dark:border-slate-700 shadow-2xs group">
              <img src={img.previewUrl} alt="Thumbnail" className="w-full h-full object-cover" />
              <button
                type="button"
                onClick={() => removeImage(idx)}
                className="absolute top-1 right-1 p-0.5 rounded-full bg-black/60 text-white hover:bg-black transition-colors cursor-pointer"
              >
                <X className="w-3 h-3" />
              </button>
            </div>
          ))}
        </div>
      )}

      <div role="status" aria-atomic="true" className="flex min-h-4 min-w-0 items-center gap-1.5 px-1 text-xs leading-4 text-academic">
        {typingUsers.length > 0 && (
          <>
            <span aria-hidden="true" className="size-1.5 shrink-0 rounded-full bg-academic" />
            <span className="min-w-0 truncate font-medium">{typingUsers.join(', ')}</span>
            <span className="shrink-0">{' đang soạn tin…'}</span>
          </>
        )}
      </div>

      {/* Input Action Row */}
      <div className="flex items-end gap-2">
        {/* Hidden File Input */}
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          multiple
          className="hidden"
          onChange={handleFileChange}
        />

        {/* Attach Image Button */}
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          title="Đính kèm hình ảnh"
          aria-label="Đính kèm hình ảnh"
          disabled={isUploading}
          className="grid size-11 shrink-0 place-items-center rounded-xl text-muted-foreground hover:bg-muted hover:text-foreground disabled:opacity-50"
        >
          <ImageIcon className="w-5 h-5" />
        </button>

        {/* Textarea Input */}
        <div className="flex-1 relative flex items-center">
          <textarea
            ref={textareaRef}
            rows={1}
            value={content}
            onChange={(e) => {
              setContent(e.target.value);
              handleTypingEvent(e.target.value);
            }}
            onBlur={stopTyping}
            onKeyDown={handleKeyDown}
            aria-label="Nội dung tin nhắn"
            placeholder="Nhập tin nhắn…"
            className="min-h-11 w-full min-w-0 resize-none rounded-xl border border-input bg-background px-3 py-2.5 text-base text-foreground outline-none placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring/50 md:text-sm"
          />
        </div>

        {/* Send Button */}
        <Button size="icon"
          type="button"
          aria-label="Gửi tin nhắn"
          onClick={() => void handleSend()}
          disabled={!canSend || isUploading || (!content.trim() && selectedImages.length === 0)}
          className="size-11"
        >
          {isUploading ? (
            <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />
          ) : (
            <Send aria-hidden="true" />
          )}
        </Button>
      </div>
    </div>
  );
}
