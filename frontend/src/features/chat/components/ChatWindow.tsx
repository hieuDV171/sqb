import { UserAvatar } from '@/components/ui/user-avatar';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { useStompStatus } from '@/lib/stompClient';
import { useEffect, useState, useMemo } from 'react';
import { getGroupExitReason, useConversations, useGroupMembers, useHiddenConversations, useHideConversation, useMessages } from '../hooks/useChat';
import { useChatStore } from '../stores/useChatStore';
import { MessageList } from './MessageList';
import { ChatInput } from './ChatInput';
import type { MessageDto } from '../types/chat.types';
import {
  Users,
  ChevronLeft,
  EyeOff,
  Lock,
  UserMinus,
  X,
} from 'lucide-react';

interface ChatWindowProps {
  conversationId: number;
  onBack?: () => void;
  onUnlock?: () => void;
  onOpenGroupMembers?: () => void;
  onOpenDeleteModal?: (message: MessageDto) => void;
  onMembershipLost?: () => void;
}

export function ChatWindow(props: ChatWindowProps) {
  const locked = useChatStore((state) =>
    !state.isPinUnlocked && state.hiddenConversationIds.includes(props.conversationId));

  if (locked) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-3 p-6 text-center">
        <Lock aria-hidden="true" className="size-6 text-warning" />
        <p className="font-semibold">Hội thoại này nằm trong kho ẩn</p>
        {props.onUnlock && <Button onClick={props.onUnlock}>Mở kho ẩn</Button>}
        {props.onBack && <Button variant="ghost" onClick={props.onBack}>Quay lại</Button>}
      </div>
    );
  }
  return <UnlockedChatWindow {...props} />;
}

function UnlockedChatWindow({
  conversationId,
  onBack,
  onOpenGroupMembers,
  onOpenDeleteModal,
  onMembershipLost,
}: ChatWindowProps) {
  const { typingMap, isPinUnlocked } = useChatStore();
  const realtime = useStompStatus();
  const [replyToId, setReplyToId] = useState<number | null>(null);
  const [previewImageUrl, setPreviewImageUrl] = useState<string | null>(null);

  // Lấy thông tin hội thoại từ cache của danh sách
  const { data: conversationsData } = useConversations();
  const { data: hiddenData } = useHiddenConversations(isPinUnlocked);
  const messages = useMessages(conversationId);
  const messageData = messages.data;
  const hideMutation = useHideConversation();
  const replyTo = replyToId === null ? null : messageData?.pages
    .flatMap((page) => page.messages)
    .find((message) => message.messageId === replyToId && !message.revoked) ?? null;
  const imageStillVisible = previewImageUrl && messageData?.pages.some((page) => page.messages.some(
    (message) => !message.revoked && message.mediaUrls?.includes(previewImageUrl),
  ));

  const conversation = useMemo(() => {
    const pages = [...(conversationsData?.pages ?? []), ...(isPinUnlocked ? hiddenData?.pages ?? [] : [])];
    for (const page of pages) {
      const found = page.items.find((c) => c.conversationId === conversationId);
      if (found) return found;
    }
    return null;
  }, [conversationsData, hiddenData, isPinUnlocked, conversationId]);

  const isGroup = (conversation?.type ?? messageData?.pages[0]?.conversationType) === 'GROUP';
  const displayName = conversation?.displayName ?? messageData?.pages[0]?.groupName;
  const displayAvatar = conversation?.displayAvatarUrl ?? messageData?.pages[0]?.groupAvatarUrl;
  const { data: members = [], error: membersError } = useGroupMembers(conversationId, isGroup);
  const groupExitReason = getGroupExitReason(messages.error) ?? getGroupExitReason(membersError);

  useEffect(() => {
    if (groupExitReason) onMembershipLost?.();
  }, [groupExitReason, onMembershipLost]);

  // Danh sách người đang gõ trong cuộc trò chuyện này
  const typingUsers = useMemo(() => {
    const map = typingMap[conversationId];
    if (!map) return [];
    return Object.values(map).map((u) => u.fullName);
  }, [typingMap, conversationId]);

  const handleHide = () => {
    if (window.confirm('Bạn có muốn ẩn cuộc hội thoại này vào kho bí mật?')) {
      hideMutation.mutate(conversationId, { onSuccess: () => onBack?.() });
    }
  };

  if (groupExitReason) {
    return (
      <div className="grid h-full place-items-center overflow-y-auto p-4 bg-surface-subtle" role="status">
        <EmptyState icon={UserMinus}
          title={groupExitReason === 'removed' ? 'Bạn đã được mời ra khỏi nhóm' : 'Bạn không còn là thành viên của nhóm'}
          description="Cuộc trò chuyện chỉ mở cho thành viên đang tham gia."
          actionLabel="Về danh sách tin nhắn" onAction={onBack} />
      </div>
    );
  }

  return (
    <div className="relative flex h-full min-h-0 flex-col overflow-hidden bg-surface-subtle">
      {/* Chat Header */}
      <div className="z-10 flex min-h-16 shrink-0 items-center justify-between gap-2 border-b border-border bg-card px-2 sm:px-4">
        <div className="flex items-center gap-3 min-w-0">
          {/* Mobile Back Button */}
          {onBack && (
            <Button variant="ghost" size="icon"
              onClick={onBack}
              className="size-11 md:hidden"
              aria-label="Quay lại danh sách"
            >
              <ChevronLeft aria-hidden="true" />
            </Button>
          )}

          {isGroup && !displayAvatar ? (
            <span className="grid size-10 shrink-0 place-items-center rounded-full bg-academic-soft text-academic"><Users aria-hidden="true" className="size-5" /></span>
          ) : (
            <UserAvatar name={displayName} avatarUrl={displayAvatar} decorative />
          )}

          {/* Title & Subtitle */}
          <div className="flex flex-col min-w-0">
            <h3 className="text-sm sm:text-base font-bold text-foreground truncate">
              {displayName || (isGroup ? 'Nhóm trò chuyện' : 'Cuộc trò chuyện')}
            </h3>

            <div className="truncate text-xs text-muted-foreground">
              {isGroup ? `${members.length || conversation?.memberCount || 0} thành viên` : 'Trò chuyện trực tiếp'}
            </div>
          </div>
        </div>

        {/* Header Right Actions */}
        <div className="flex items-center gap-1 shrink-0">
          {isGroup && onOpenGroupMembers && (
            <button
              type="button"
              onClick={onOpenGroupMembers}
              title="Xem danh sách thành viên" aria-label="Xem danh sách thành viên"
              className="grid size-11 place-items-center rounded-xl text-muted-foreground hover:bg-muted hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer"
            >
              <Users className="w-5 h-5" />
            </button>
          )}

          {!conversation?.hiddenAt && <button
            type="button"
            onClick={handleHide}
            title="Ẩn hội thoại" aria-label="Ẩn hội thoại" disabled={hideMutation.isPending}
            className="grid size-11 place-items-center rounded-xl text-muted-foreground hover:bg-muted hover:text-amber-600 dark:hover:text-amber-400 transition-colors cursor-pointer"
          >
            <EyeOff aria-hidden="true" className="size-5" />
          </button>}
        </div>
      </div>

      {/* Message List Area */}
      <MessageList
        conversationId={conversationId}
        isGroup={isGroup}
        onReplyMessage={(msg) => setReplyToId(msg.messageId)}
        onDeleteMessage={(msg) => onOpenDeleteModal?.(msg)}
        onImageClick={(url) => setPreviewImageUrl(url)}
      />

      {realtime !== 'CONNECTED' && (
        <p role="status" className="shrink-0 border-t border-border bg-warning-soft px-3 py-2 text-sm text-warning">
          {realtime === 'CONNECTING' ? 'Đang kết nối tin nhắn…' : 'Mất kết nối tin nhắn. Bạn vẫn có thể soạn tin.'}
        </p>
      )}

      {/* Chat Input */}
      <ChatInput
        conversationId={conversationId}
        typingUsers={typingUsers}
        replyTo={replyTo}
        canSend={realtime === 'CONNECTED' && !messages.isPending}
        onCancelReply={() => setReplyToId(null)}
      />

      {/* Image Lightbox Modal */}
      {previewImageUrl && imageStillVisible && (
        <div
          className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in"
          onClick={() => setPreviewImageUrl(null)}
        >
          <button
            type="button"
            onClick={() => setPreviewImageUrl(null)}
            aria-label="Đóng ảnh phóng to"
            className="absolute top-4 right-4 p-2 rounded-full bg-black/50 text-white hover:bg-black/80 transition-colors cursor-pointer"
          >
            <X className="w-6 h-6" />
          </button>
          <img
            src={previewImageUrl}
            alt="Phóng to ảnh"
            className="max-w-full max-h-[90vh] object-contain rounded-xl shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </div>
  );
}
