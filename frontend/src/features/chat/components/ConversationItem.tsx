import { Menu } from '@base-ui/react/menu';
import { Button } from '@/components/ui/button';
import { UserAvatar } from '@/components/ui/user-avatar';
import { cn } from '@/lib/utils';
import type { ConversationDto } from '../types/chat.types';
import { formatChatTime, truncate } from '../utils/chatUtils';
import { useAuthStore } from '@/stores/useAuthStore';
import {
  Users,
  Lock,
  MoreVertical,
  BellOff,
  EyeOff,
  Eye,
  LogOut,
  Image as ImageIcon,
  FileText,
} from 'lucide-react';

interface ConversationItemProps {
  conversation: ConversationDto;
  isActive: boolean;
  isHiddenMode?: boolean;
  onClick: () => void;
  onHide?: (conversationId: number) => void;
  onUnhide?: (conversationId: number) => void;
  onLeave?: (conversationId: number) => void;
}

export function ConversationItem({
  conversation,
  isActive,
  isHiddenMode = false,
  onClick,
  onHide,
  onUnhide,
  onLeave,
}: ConversationItemProps) {
  const currentUserId = useAuthStore((s) => s.user?.id);

  const isGroup = conversation.type === 'GROUP';
  const hasUnread = conversation.unreadCount > 0 && !isActive;

  const renderLastMessage = () => {
    if (!conversation.lastMessage) {
      return (
        <span className="text-muted-foreground italic text-xs">
          Chưa có tin nhắn nào
        </span>
      );
    }

    const { senderId, senderName, content, messageType } = conversation.lastMessage;
    const isMe = senderId === currentUserId;
    const prefix = isMe ? 'Bạn: ' : isGroup ? `${senderName?.split(' ').pop() || 'Thành viên'}: ` : '';

    if (messageType === 'IMAGE') {
      return (
        <span className="inline-flex items-center gap-1 text-muted-foreground text-xs">
          <ImageIcon className="w-3.5 h-3.5 text-academic" />
          {prefix}[Hình ảnh]
        </span>
      );
    }

    if (messageType === 'FILE') {
      return (
        <span className="inline-flex items-center gap-1 text-muted-foreground text-xs">
          <FileText className="w-3.5 h-3.5 text-warning" />
          {prefix}[Tập tin]
        </span>
      );
    }

    return (
      <span
        className={`block truncate text-sm ${
          hasUnread
            ? 'font-semibold text-foreground'
            : 'text-muted-foreground'
        }`}
      >
        {prefix}
        {truncate(content, 32)}
      </span>
    );
  };

  const menuItemClass = 'flex min-h-11 cursor-pointer items-center gap-2 rounded-lg px-3 text-sm outline-none data-highlighted:bg-accent';

  return (
    <div className={cn('flex min-w-0 items-center gap-1 rounded-xl border border-transparent pr-1',
      isActive ? 'border-academic/25 bg-academic-soft' : 'hover:bg-muted')}>
      <button type="button" onClick={onClick} aria-current={isActive ? 'true' : undefined}
        className="flex min-h-16 min-w-0 flex-1 cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5 text-left focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-ring">
        <span className="relative shrink-0">
          {isGroup && !conversation.displayAvatarUrl ? (
            <span className="grid size-11 place-items-center rounded-full bg-academic-soft text-academic"><Users aria-hidden="true" className="size-5" /></span>
          ) : (
            <UserAvatar name={conversation.displayName} avatarUrl={conversation.displayAvatarUrl} className="size-11" decorative />
          )}
          {conversation.hiddenAt && (
            <span className="absolute -right-1 -bottom-1 grid size-5 place-items-center rounded-full bg-warning-soft text-warning ring-2 ring-card">
              <Lock aria-hidden="true" className="size-3" /><span className="sr-only">Hội thoại ẩn</span>
            </span>
          )}
        </span>
        <span className="min-w-0 flex-1 space-y-1">
          <span className="flex min-w-0 items-center gap-1.5">
            <span className={cn('min-w-0 flex-1 truncate text-sm', hasUnread ? 'font-bold' : 'font-semibold')}>{conversation.displayName}</span>
            {conversation.isMuted && <BellOff aria-label="Đã tắt thông báo" className="size-3.5 shrink-0 text-muted-foreground" />}
            <time className="shrink-0 text-xs text-muted-foreground" dateTime={conversation.updatedAt}>{formatChatTime(conversation.updatedAt)}</time>
          </span>
          <span className="flex min-w-0 items-center gap-2">
            <span className="min-w-0 flex-1 truncate">{renderLastMessage()}</span>
            {hasUnread && (
              <span aria-label={conversation.unreadCount + ' tin chưa đọc'} className="grid min-w-5 shrink-0 place-items-center rounded-full bg-primary px-1.5 text-xs font-semibold text-primary-foreground">
                {conversation.unreadCount > 99 ? '99+' : conversation.unreadCount}
              </span>
            )}
          </span>
        </span>
      </button>
      <Menu.Root modal={false}>
        <Menu.Trigger render={<Button variant="ghost" size="icon" className="size-11 text-muted-foreground" />}
          aria-label={'Tùy chọn hội thoại ' + conversation.displayName}>
          <MoreVertical aria-hidden="true" />
        </Menu.Trigger>
        <Menu.Portal>
          <Menu.Positioner align="end" sideOffset={5} className="z-50">
            <Menu.Popup className="min-w-44 rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
              {isHiddenMode ? (
                <Menu.Item className={menuItemClass} onClick={() => onUnhide?.(conversation.conversationId)}>
                  <Eye aria-hidden="true" className="size-4" /> Bỏ ẩn hội thoại
                </Menu.Item>
              ) : (
                <Menu.Item className={menuItemClass} onClick={() => onHide?.(conversation.conversationId)}>
                  <EyeOff aria-hidden="true" className="size-4" /> Ẩn hội thoại
                </Menu.Item>
              )}
              {isGroup && onLeave && (
                <Menu.Item className={cn(menuItemClass, 'text-destructive')} onClick={() => onLeave(conversation.conversationId)}>
                  <LogOut aria-hidden="true" className="size-4" /> Rời nhóm
                </Menu.Item>
              )}
            </Menu.Popup>
          </Menu.Positioner>
        </Menu.Portal>
      </Menu.Root>
    </div>
  );
}
