import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useChatStore } from '../stores/useChatStore';
import { ConversationList } from '../components/ConversationList';
import { ChatWindow } from '../components/ChatWindow';
import { HiddenChatPinModal } from '../components/HiddenChatPinModal';
import { CreateGroupModal } from '../components/CreateGroupModal';
import { NewDirectChatModal } from '../components/NewDirectChatModal';
import { GroupMembersModal } from '../components/GroupMembersModal';
import { DeleteMessageModal } from '../components/DeleteMessageModal';
import type { MessageDto } from '../types/chat.types';
import { MessageSquare, Users, MessageSquarePlus } from 'lucide-react';

export function MessagesPage() {
  const { conversationId: paramConvId } = useParams<{ conversationId?: string }>();
  const navigate = useNavigate();

  const setActiveConversationId = useChatStore((state) => state.setActiveConversationId);
  const parsedId = Number(paramConvId);
  const activeConversationId = Number.isSafeInteger(parsedId) && parsedId > 0 ? parsedId : null;

  // Modals state
  const [isPinModalOpen, setIsPinModalOpen] = useState(false);
  const [isNewDirectModalOpen, setIsNewDirectModalOpen] = useState(false);
  const [isCreateGroupModalOpen, setIsCreateGroupModalOpen] = useState(false);
  const [isGroupMembersModalOpen, setIsGroupMembersModalOpen] = useState(false);
  const [deleteModalState, setDeleteModalState] = useState<{
    isOpen: boolean;
    message: MessageDto | null;
  }>({
    isOpen: false,
    message: null,
  });

  useEffect(() => {
    setActiveConversationId(activeConversationId);
    return () => setActiveConversationId(null);
  }, [activeConversationId, setActiveConversationId]);

  const handleSelectConversation = (id: number) => {
    navigate(`/messages/${id}`);
  };

  const handleBackToList = () => {
    navigate('/messages');
  };

  return (
    <div className="mx-auto flex h-[max(24rem,calc(100dvh-var(--app-header-height)-var(--app-mobile-nav-height)-4rem-env(safe-area-inset-bottom)))] max-w-7xl overflow-hidden rounded-2xl border border-border bg-card md:h-[max(24rem,calc(100dvh-var(--app-header-height)-3rem))] lg:h-[max(24rem,calc(100dvh-var(--app-header-height)-4rem))]">
      {/* Cột trái: Danh sách hội thoại (Ẩn trên màn hình nhỏ nếu đang mở chat) */}
      <div
        className={`h-full min-h-0 w-full shrink-0 flex-col md:w-72 xl:w-96 ${
          activeConversationId ? 'hidden md:flex' : 'flex'
        }`}
      >
        <ConversationList
          onSelectConversation={handleSelectConversation}
          onOpenNewDirectModal={() => setIsNewDirectModalOpen(true)}
          onOpenCreateGroupModal={() => setIsCreateGroupModalOpen(true)}
          onOpenPinModal={() => setIsPinModalOpen(true)}
          onCloseConversation={handleBackToList}
        />
      </div>

      {/* Cột phải: Cửa sổ Chat thời gian thực (Ẩn trên màn hình nhỏ nếu chưa chọn chat) */}
      <div
        className={`h-full min-h-0 min-w-0 flex-1 flex-col ${
          activeConversationId ? 'flex' : 'hidden md:flex'
        }`}
      >
        {activeConversationId ? (
          <ChatWindow
            key={activeConversationId}
            conversationId={activeConversationId}
            onBack={handleBackToList}
            onOpenGroupMembers={() => setIsGroupMembersModalOpen(true)}
            onOpenDeleteModal={(msg) => setDeleteModalState({ isOpen: true, message: msg })}
          />
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center p-5 text-center bg-surface-subtle">
            <div className="mb-4 grid size-16 place-items-center rounded-2xl bg-academic-soft text-academic">
              <MessageSquare className="w-8 h-8" />
            </div>

            <h3 className="text-xl font-bold text-foreground">
              Chọn một cuộc trò chuyện
            </h3>
            <p className="text-sm text-muted-foreground max-w-sm mt-1 mb-6">
              Mở hội thoại trong hộp thư hoặc bắt đầu trao đổi mới.
            </p>

            <div className="flex flex-wrap justify-center gap-2">
              <Button
                type="button"
                onClick={() => setIsNewDirectModalOpen(true)}
                className="min-h-11"
              >
                <MessageSquarePlus className="w-4 h-4" />
                Nhắn tin 1-1
              </Button>

              <Button variant="outline"
                type="button"
                onClick={() => setIsCreateGroupModalOpen(true)}
                className="min-h-11"
              >
                <Users className="w-4 h-4" />
                Tạo nhóm học tập
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      <HiddenChatPinModal
        isOpen={isPinModalOpen}
        onClose={() => setIsPinModalOpen(false)}
      />

      <CreateGroupModal
        isOpen={isCreateGroupModalOpen}
        onClose={() => setIsCreateGroupModalOpen(false)}
        onCreated={(id) => handleSelectConversation(id)}
      />

      <NewDirectChatModal
        isOpen={isNewDirectModalOpen}
        onClose={() => setIsNewDirectModalOpen(false)}
        onCreated={(id) => handleSelectConversation(id)}
      />

      {activeConversationId && (
        <GroupMembersModal
          conversationId={activeConversationId}
          isOpen={isGroupMembersModalOpen}
          onClose={() => setIsGroupMembersModalOpen(false)}
        />
      )}

      {activeConversationId && (
        <DeleteMessageModal
          conversationId={activeConversationId}
          isOpen={deleteModalState.isOpen}
          message={deleteModalState.message}
          onClose={() => setDeleteModalState({ isOpen: false, message: null })}
        />
      )}
    </div>
  );
}
