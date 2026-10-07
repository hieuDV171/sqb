import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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

  const { activeConversationId, setActiveConversationId } = useChatStore();

  // Modals state
  const [isPinModalOpen, setIsPinModalOpen] = useState(false);
  const [pinModalMode, setPinModalMode] = useState<'UNLOCK' | 'SET_PIN'>('UNLOCK');
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

  // Đồng bộ URL param vào store
  useEffect(() => {
    if (paramConvId) {
      const parsed = parseInt(paramConvId, 10);
      if (!isNaN(parsed) && parsed !== activeConversationId) {
        setActiveConversationId(parsed);
      }
    } else {
      setActiveConversationId(null);
    }
  }, [paramConvId, activeConversationId, setActiveConversationId]);

  const handleSelectConversation = (id: number) => {
    setActiveConversationId(id);
    navigate(`/messages/${id}`);
  };

  const handleBackToList = () => {
    setActiveConversationId(null);
    navigate('/messages');
  };

  const handleOpenPinModal = (mode: 'UNLOCK' | 'SET_PIN') => {
    setPinModalMode(mode);
    setIsPinModalOpen(true);
  };

  return (
    <div className="h-[calc(100vh-5rem)] max-w-7xl mx-auto rounded-3xl border border-slate-200/80 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-xl overflow-hidden flex">
      {/* Cột trái: Danh sách hội thoại (Ẩn trên màn hình nhỏ nếu đang mở chat) */}
      <div
        className={`w-full md:w-80 lg:w-96 shrink-0 h-full flex flex-col ${
          activeConversationId ? 'hidden md:flex' : 'flex'
        }`}
      >
        <ConversationList
          onSelectConversation={handleSelectConversation}
          onOpenNewDirectModal={() => setIsNewDirectModalOpen(true)}
          onOpenCreateGroupModal={() => setIsCreateGroupModalOpen(true)}
          onOpenPinModal={handleOpenPinModal}
        />
      </div>

      {/* Cột phải: Cửa sổ Chat thời gian thực (Ẩn trên màn hình nhỏ nếu chưa chọn chat) */}
      <div
        className={`flex-1 h-full flex flex-col min-w-0 ${
          activeConversationId ? 'flex' : 'hidden md:flex'
        }`}
      >
        {activeConversationId ? (
          <ChatWindow
            conversationId={activeConversationId}
            onBack={handleBackToList}
            onOpenGroupMembers={() => setIsGroupMembersModalOpen(true)}
            onOpenDeleteModal={(msg) => setDeleteModalState({ isOpen: true, message: msg })}
          />
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center p-8 text-center bg-slate-50/50 dark:bg-slate-950/30">
            <div className="w-16 h-16 rounded-3xl bg-linear-to-tr from-indigo-500/10 to-violet-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center mb-4 ring-8 ring-indigo-500/5">
              <MessageSquare className="w-8 h-8" />
            </div>

            <h3 className="text-xl font-bold text-slate-800 dark:text-slate-200">
              Hộp thư & Trao đổi học tập
            </h3>
            <p className="text-sm text-slate-500 dark:text-slate-400 max-w-sm mt-1 mb-6">
              Chọn một cuộc trò chuyện ở danh sách bên trái hoặc tạo cuộc thảo luận mới để trao đổi bài giảng, đề thi.
            </p>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setIsNewDirectModalOpen(true)}
                className="px-4 py-2.5 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs shadow-md shadow-indigo-500/20 transition-all cursor-pointer flex items-center gap-2"
              >
                <MessageSquarePlus className="w-4 h-4" />
                Nhắn tin 1-1
              </button>

              <button
                type="button"
                onClick={() => setIsCreateGroupModalOpen(true)}
                className="px-4 py-2.5 rounded-2xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-semibold text-xs transition-colors cursor-pointer flex items-center gap-2"
              >
                <Users className="w-4 h-4" />
                Tạo nhóm học tập
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      <HiddenChatPinModal
        isOpen={isPinModalOpen}
        initialMode={pinModalMode}
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
