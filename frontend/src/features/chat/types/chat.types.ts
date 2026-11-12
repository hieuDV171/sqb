import type { CursorPagination } from '@/types/response.types';

export type ConversationType = 'DIRECT' | 'GROUP';

export type MessageType = 'TEXT' | 'IMAGE' | 'FILE';

export type ConversationRole = 'CHIEF' | 'VILLAGE_ELDER' | 'VILLAGER';

export type MessageDeleteScope = 'ME' | 'EVERYONE';

export type ChatWsEventType = 'NEW_MESSAGE' | 'MESSAGE_REVOKED' | 'MESSAGE_READ' | 'TYPING_STATUS';

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
  revoked?: boolean;
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
  invitedMemberCount: number;
}

export interface GroupCandidateDto {
  userId: number;
  fullName: string;
  avatarUrl: string | null;
  friend: boolean;
  following: boolean;
  follower: boolean;
}

export interface GroupInvitationDto {
  invitationId: number;
  groupName: string;
  groupAvatarUrl: string | null;
  inviterName: string;
  expiresAt: string;
}

export interface ReplyMessagePreviewDto {
  messageId: number;
  senderId?: number | null;
  senderName?: string | null;
  contentPreview: string;
  unavailable?: boolean;
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
  revoked: boolean;
}

export interface MessageListResponseDto {
  groupName?: string | null;
  groupAvatarUrl?: string | null;
  hiddenAt?: string | null;
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
  invitedMemberCount: number;
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
  typing: boolean;
}

export interface WsMessageBroadcastDto {
  eventType: 'NEW_MESSAGE';
  messageId: number;
  conversationId: number;
  conversationType: ConversationType;
  sender: UserSummaryDto;
  content?: string;
  messageType: MessageType;
  mediaUrls?: string[];
  replyToMessageId?: number | null;
  replyToMessage?: ReplyMessagePreviewDto | null;
  createdAt: string;
  isEdited: boolean;
}

export interface WsReadReceiptBroadcastDto {
  eventType: 'MESSAGE_READ';
  conversationId: number;
  messageId: number;
  reader: UserSummaryDto;
  readAt: string;
}

export interface WsMessageRevokedDto {
  eventType: 'MESSAGE_REVOKED';
  conversationId: number;
  messageId: number;
}

export interface WsGroupRolesChangedDto {
  eventType: 'GROUP_ROLES_CHANGED';
  conversationId: number;
}

export interface WsGroupMemberRemovedDto {
  eventType: 'GROUP_MEMBER_REMOVED';
  conversationId: number;
  removedUserId: number;
}

export interface WsGroupMembersAddedDto {
  eventType: 'GROUP_MEMBERS_ADDED';
  conversationId: number;
}

export interface WsTypingBroadcastDto {
  eventType: 'TYPING_STATUS';
  conversationId: number;
  user: UserSummaryDto;
  typing: boolean;
  timestamp: string;
}
