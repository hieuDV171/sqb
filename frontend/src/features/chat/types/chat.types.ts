import type { CursorPagination } from '@/types/response.types';

export type ConversationType = 'DIRECT' | 'GROUP';

export type MessageType = 'TEXT' | 'IMAGE' | 'FILE';

export type ConversationRole = 'CHIEF' | 'VILLAGE_ELDER' | 'VILLAGER';

export type MessageDeleteScope = 'ME' | 'EVERYONE';

export type ChatWsEventType = 'NEW_MESSAGE' | 'MESSAGE_READ' | 'TYPING_STATUS';

export interface UserSummaryDto {
  userId: number;
  fullName: string;
  avatarUrl?: string | null;
  frameUrl?: string | null;
}

export interface LastMessageDto {
  messageId: number;
  senderId: number;
  senderName: string;
  content: string;
  messageType: MessageType;
  createdAt: string;
}

export interface ConversationDto {
  conversationId: number;
  type: ConversationType;
  displayName: string;
  displayAvatarUrl?: string | null;
  memberCount: number;
  lastMessage?: LastMessageDto | null;
  unreadCount: number;
  isMuted: boolean;
  hiddenAt?: string | null;
  updatedAt: string;
}

export interface ConversationListResponseDto {
  items: ConversationDto[];
  pagination: CursorPagination;
  totalConversations: number;
}

export interface ConversationMemberDto {
  userId: number;
  fullName: string;
  avatarUrl?: string | null;
  frameUrl?: string | null;
  role: ConversationRole;
  createdAt: string;
}

export interface ConversationDetailDto {
  conversationId: number;
  type: ConversationType;
  name?: string;
  avatarUrl?: string | null;
  members: ConversationMemberDto[];
  createdAt: string;
}

export interface ReplyMessagePreviewDto {
  messageId: number;
  senderName: string;
  content: string;
  messageType: MessageType;
}

export interface MessageDto {
  messageId: number;
  senderId: number;
  senderName: string;
  avatarUrl?: string | null;
  frameUrl?: string | null;
  content: string;
  messageType: MessageType;
  mediaUrls?: string[];
  replyToMessage?: ReplyMessagePreviewDto | null;
  createdAt: string;
  isEdited: boolean;
  isRevoked: boolean;
}

export interface MessageListResponseDto {
  conversationId: number;
  conversationType: ConversationType;
  messages: MessageDto[];
  pagination: CursorPagination;
}

// Request DTOs
export interface CreateDirectConversationDto {
  targetUserId: number;
}

export interface CreateGroupConversationDto {
  name: string;
  avatarUrl?: string;
  memberIds: number[];
}

export interface UpdateGroupInfoDto {
  name?: string;
  avatarUrl?: string;
}

export interface AddGroupMembersRequestDto {
  userIds: number[];
}

export interface FailedMemberDto {
  userId: number;
  fullName?: string;
  avatarUrl?: string;
  frameUrl?: string;
  reason: string;
}

export interface AddGroupMembersResponseDto {
  addedMembers: ConversationMemberDto[];
  failedMembers: FailedMemberDto[];
}

export interface UpdateMemberRoleRequestDto {
  role: ConversationRole;
}

export interface LeaveGroupRequestDto {
  newChiefId?: number;
}

export interface LeaveGroupResponseDto {
  conversationId: number;
  leftUserId: number;
  newChiefId?: number | null;
  note?: string;
}

export interface SetHiddenChatPinDto {
  pin: string;
  oldPin?: string;
}

export interface UnlockHiddenChatDto {
  pin: string;
}

export interface DeleteMessageRequestDto {
  scope: MessageDeleteScope;
}

export interface DeleteMessageResponseDto {
  messageId: number;
  scope: MessageDeleteScope;
  note?: string;
}

// WebSocket STOMP DTOs
export interface SendWsMessageRequestDto {
  conversationId: number;
  content?: string;
  messageType?: MessageType;
  mediaUrls?: string[];
  replyToMessageId?: number;
}

export interface ReadWsMessageRequestDto {
  conversationId: number;
  messageId: number;
}

export interface TypingWsRequestDto {
  conversationId: number;
  isTyping: boolean;
}

export interface WsMessageBroadcastDto {
  eventType: ChatWsEventType;
  messageId: number;
  conversationId: number;
  conversationType: ConversationType;
  sender: UserSummaryDto;
  content?: string;
  messageType: MessageType;
  mediaUrls?: string[];
  replyToMessageId?: number | null;
  createdAt: string;
  isEdited: boolean;
}

export interface WsReadReceiptBroadcastDto {
  eventType: ChatWsEventType;
  conversationId: number;
  messageId: number;
  reader: UserSummaryDto;
  readAt: string;
}

export interface WsTypingBroadcastDto {
  eventType: ChatWsEventType;
  conversationId: number;
  user: UserSummaryDto;
  isTyping: boolean;
  timestamp: string;
}
