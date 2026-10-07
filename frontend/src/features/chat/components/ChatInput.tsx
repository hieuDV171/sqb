import { useState, useRef, useEffect } from 'react';
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

interface ChatInputProps {
  conversationId: number;
  replyTo: MessageDto | null;
  onCancelReply: () => void;
}

export function ChatInput({
  conversationId,
  replyTo,
  onCancelReply,
}: ChatInputProps) {
  const { sendMessage, sendTyping } = useChatStore();
  const [content, setContent] = useState('');
  const [selectedImages, setSelectedImages] = useState<{ file: File; previewUrl: string }[]>([]);
  const [isUploading, setIsUploading] = useState(false);

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const typingTimerRef = useRef<any>(null);

  // Focus textarea when conversation or reply changes
  useEffect(() => {
    textareaRef.current?.focus();
  }, [conversationId, replyTo]);

  // Tự động điều chỉnh chiều cao textarea theo nội dung
  useEffect(() => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 120)}px`;
  }, [content]);

  // Xử lý gửi trạng thái Typing
  const handleTypingEvent = () => {
    sendTyping(conversationId, true);
    if (typingTimerRef.current) {
      clearTimeout(typingTimerRef.current);
    }
    typingTimerRef.current = setTimeout(() => {
      sendTyping(conversationId, false);
    }, 2500);
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
    if (!trimmed && selectedImages.length === 0) return;

    try {
      let uploadedUrls: string[] = [];

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

        if (typingTimerRef.current) {
          clearTimeout(typingTimerRef.current);
        }
        sendTyping(conversationId, false);

        if (textareaRef.current) {
          textareaRef.current.style.height = 'auto';
        }
      } else {
        toast.error('Không thể gửi tin nhắn. Kết nối WebSocket bị gián đoạn.');
      }
    } catch (err: any) {
      console.error('[ChatInput] Lỗi khi gửi tin nhắn:', err);
      toast.error('Có lỗi xảy ra khi tải ảnh hoặc gửi tin nhắn');
    } finally {
      setIsUploading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="border-t border-slate-200/80 dark:border-slate-800 bg-white/95 dark:bg-slate-900/95 backdrop-blur-xs p-3 space-y-2">
      {/* Reply Preview Bar */}
      {replyTo && (
        <div className="flex items-center justify-between gap-2 px-3 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-800/80 border-l-4 border-indigo-500 text-xs animate-in fade-in duration-150">
          <div className="flex items-center gap-2 truncate">
            <CornerDownRight className="w-3.5 h-3.5 text-indigo-500 shrink-0" />
            <span className="font-semibold text-slate-700 dark:text-slate-200 truncate">
              Đang trả lời {replyTo.senderName}:
            </span>
            <span className="text-slate-500 dark:text-slate-400 truncate">
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
          title="Đính kèm hình ảnh (Tự động nén WebP)"
          disabled={isUploading}
          className="p-2.5 rounded-xl text-slate-500 hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer disabled:opacity-50"
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
              handleTypingEvent();
            }}
            onKeyDown={handleKeyDown}
            placeholder="Nhập tin nhắn... (Enter để gửi, Shift+Enter xuống dòng)"
            className="w-full py-2.5 px-4 text-sm bg-slate-100/80 dark:bg-slate-800/80 border border-transparent focus:border-indigo-500 focus:bg-white dark:focus:bg-slate-900 rounded-2xl outline-none resize-none transition-all placeholder:text-slate-400 text-slate-900 dark:text-slate-100 max-h-32"
          />
        </div>

        {/* Send Button */}
        <button
          type="button"
          onClick={handleSend}
          disabled={isUploading || (!content.trim() && selectedImages.length === 0)}
          className="p-2.5 rounded-xl bg-linear-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white shadow-xs hover:shadow-indigo-500/25 transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:shadow-none shrink-0"
        >
          {isUploading ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : (
            <Send className="w-5 h-5" />
          )}
        </button>
      </div>
    </div>
  );
}
