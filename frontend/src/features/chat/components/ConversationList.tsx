import { useState, useMemo } from 'react';
import {
  useConversations,
  useHiddenConversations,
  useHideConversation,
  useUnhideConversation,
  useLeaveGroup,
} from '../hooks/useChat';
import { useChatStore } from '../stores/useChatStore';
import { ConversationItem } from './ConversationItem';
import {
  Search,
  Users,
  Lock,
  MessageSquarePlus,
  FolderLock,
  LockOpen,
  Inbox,
  Loader2,
} from 'lucide-react';
import type { ConversationDto } from '../types/chat.types';

interface ConversationListProps {
  onSelectConversation: (conversationId: number) => void;
  onOpenNewDirectModal: () => void;
  onOpenCreateGroupModal: () => void;
  onOpenPinModal: (mode: 'UNLOCK' | 'SET_PIN') => void;
}

type TabType = 'ALL' | 'GROUPS' | 'HIDDEN';

export function ConversationList({
  onSelectConversation,
  onOpenNewDirectModal,
  onOpenCreateGroupModal,
  onOpenPinModal,
}: ConversationListProps) {
  const { activeConversationId, isPinUnlocked, lockPin } = useChatStore();
  const [activeTab, setActiveTab] = useState<TabType>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  // 1. Dữ liệu hội thoại thông thường
  const {
    data: normalData,
    isLoading: isNormalLoading,
    hasNextPage: hasNextNormal,
    fetchNextPage: fetchNextNormal,
    isFetchingNextPage: isFetchingNextNormal,
  } = useConversations();

  // 2. Dữ liệu hội thoại ẩn (chỉ fetch khi ở tab HIDDEN và PIN đã mở khóa)
  const {
    data: hiddenData,
    isLoading: isHiddenLoading,
    hasNextPage: hasNextHidden,
    fetchNextPage: fetchNextHidden,
    isFetchingNextPage: isFetchingNextHidden,
  } = useHiddenConversations(activeTab === 'HIDDEN' && isPinUnlocked);

  // Mutations
  const hideMutation = useHideConversation();
  const unhideMutation = useUnhideConversation();
  const leaveGroupMutation = useLeaveGroup();

  // Xử lý danh sách hội thoại phẳng
  const allConversations = useMemo(() => {
    if (!normalData?.pages) return [];
    return normalData.pages.flatMap((page) => page.items);
  }, [normalData]);

  const hiddenConversations = useMemo(() => {
    if (!hiddenData?.pages) return [];
    return hiddenData.pages.flatMap((page) => page.items);
  }, [hiddenData]);

  // Lọc theo Tab và Search
  const filteredList = useMemo(() => {
    let source: ConversationDto[] = [];

    if (activeTab === 'HIDDEN') {
      source = hiddenConversations;
    } else if (activeTab === 'GROUPS') {
      source = allConversations.filter((c) => c.type === 'GROUP');
    } else {
      source = allConversations;
    }

    if (!searchTerm.trim()) return source;

    const query = searchTerm.toLowerCase();
    return source.filter((c) =>
      c.displayName.toLowerCase().includes(query) ||
      (c.lastMessage?.content && c.lastMessage.content.toLowerCase().includes(query))
    );
  }, [activeTab, allConversations, hiddenConversations, searchTerm]);

  const handleTabChange = (tab: TabType) => {
    if (tab === 'HIDDEN' && !isPinUnlocked) {
      onOpenPinModal('UNLOCK');
      return;
    }
    setActiveTab(tab);
  };

  const handleHide = (id: number) => {
    hideMutation.mutate(id);
  };

  const handleUnhide = (id: number) => {
    unhideMutation.mutate(id);
  };

  const handleLeave = (id: number) => {
    if (window.confirm('Bạn có chắc chắn muốn rời khỏi nhóm này không?')) {
      leaveGroupMutation.mutate({ conversationId: id });
    }
  };

  const isLoading = activeTab === 'HIDDEN' ? isHiddenLoading : isNormalLoading;
  const hasNextPage = activeTab === 'HIDDEN' ? hasNextHidden : hasNextNormal;
  const isFetchingNext = activeTab === 'HIDDEN' ? isFetchingNextHidden : isFetchingNextNormal;
  const fetchNext = activeTab === 'HIDDEN' ? fetchNextHidden : fetchNextNormal;

  return (
    <div className="flex flex-col h-full bg-white dark:bg-slate-900 border-r border-slate-200/80 dark:border-slate-800">
      {/* Header: Title + Action Buttons */}
      <div className="p-4 border-b border-slate-100 dark:border-slate-800/80 space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-slate-100 flex items-center gap-2">
            Đoạn chat
            {activeTab === 'HIDDEN' && (
              <span className="text-xs px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-600 dark:text-amber-400 font-semibold border border-amber-500/20">
                Kho bí mật
              </span>
            )}
          </h2>

          <div className="flex items-center gap-1.5">
            {activeTab === 'HIDDEN' && isPinUnlocked ? (
              <button
                type="button"
                onClick={() => {
                  lockPin();
                  setActiveTab('ALL');
                }}
                title="Khóa kho ẩn lại"
                className="p-2 rounded-xl text-amber-600 hover:bg-amber-50 dark:hover:bg-amber-950/40 transition-colors cursor-pointer"
              >
                <Lock className="w-5 h-5" />
              </button>
            ) : null}

            <button
              type="button"
              onClick={onOpenNewDirectModal}
              title="Nhắn tin trực tiếp 1-1"
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer"
            >
              <MessageSquarePlus className="w-5 h-5" />
            </button>

            <button
              type="button"
              onClick={onOpenCreateGroupModal}
              title="Tạo nhóm trò chuyện mới"
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer"
            >
              <Users className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Search Bar */}
        <div className="relative">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm kiếm người hoặc tin nhắn..."
            className="w-full pl-9 pr-4 py-2 text-sm bg-slate-100/80 dark:bg-slate-800/80 border border-transparent focus:border-indigo-500 focus:bg-white dark:focus:bg-slate-900 rounded-xl outline-none transition-all placeholder:text-slate-400 text-slate-800 dark:text-slate-200"
          />
        </div>

        {/* Filter Tabs */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100/70 dark:bg-slate-800/60 rounded-xl text-xs font-semibold">
          <button
            type="button"
            onClick={() => handleTabChange('ALL')}
            className={`flex-1 py-1.5 rounded-lg transition-all cursor-pointer ${
              activeTab === 'ALL'
                ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            }`}
          >
            Tất cả
          </button>

          <button
            type="button"
            onClick={() => handleTabChange('GROUPS')}
            className={`flex-1 py-1.5 rounded-lg transition-all cursor-pointer ${
              activeTab === 'GROUPS'
                ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            }`}
          >
            Nhóm ({allConversations.filter((c) => c.type === 'GROUP').length})
          </button>

          <button
            type="button"
            onClick={() => handleTabChange('HIDDEN')}
            className={`flex-1 py-1.5 rounded-lg flex items-center justify-center gap-1 transition-all cursor-pointer ${
              activeTab === 'HIDDEN'
                ? 'bg-white dark:bg-slate-900 text-amber-600 dark:text-amber-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            }`}
          >
            {isPinUnlocked ? (
              <LockOpen className="w-3.5 h-3.5 text-emerald-500" />
            ) : (
              <Lock className="w-3.5 h-3.5 text-amber-500" />
            )}
            Kho ẩn
          </button>
        </div>
      </div>

      {/* Conversation List Scroll Area */}
      <div className="flex-1 overflow-y-auto px-2 py-2 space-y-1">
        {isLoading ? (
          <div className="p-4 space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex items-center gap-3 animate-pulse">
                <div className="w-12 h-12 rounded-full bg-slate-200 dark:bg-slate-800 shrink-0" />
                <div className="flex-1 space-y-2">
                  <div className="w-2/3 h-4 bg-slate-200 dark:bg-slate-800 rounded" />
                  <div className="w-1/2 h-3 bg-slate-100 dark:bg-slate-800/60 rounded" />
                </div>
              </div>
            ))}
          </div>
        ) : filteredList.length === 0 ? (
          <div className="h-64 flex flex-col items-center justify-center text-center p-6 text-slate-400">
            {activeTab === 'HIDDEN' ? (
              <>
                <div className="w-12 h-12 rounded-2xl bg-amber-500/10 text-amber-500 flex items-center justify-center mb-3">
                  <FolderLock className="w-6 h-6" />
                </div>
                <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                  Kho trò chuyện ẩn trống
                </p>
                <p className="text-xs text-slate-400 mt-1 max-w-xs">
                  Bạn có thể ẩn cuộc trò chuyện nhạy cảm từ menu ba chấm ở danh sách chính.
                </p>
              </>
            ) : (
              <>
                <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 text-indigo-500 flex items-center justify-center mb-3">
                  <Inbox className="w-6 h-6" />
                </div>
                <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                  {searchTerm ? 'Không tìm thấy cuộc trò chuyện' : 'Chưa có cuộc trò chuyện nào'}
                </p>
                <p className="text-xs text-slate-400 mt-1 max-w-xs">
                  Nhấn vào biểu tượng tin nhắn phía trên để bắt đầu trò chuyện ngay.
                </p>
              </>
            )}
          </div>
        ) : (
          <>
            {filteredList.map((conversation) => (
              <ConversationItem
                key={conversation.conversationId}
                conversation={conversation}
                isActive={activeConversationId === conversation.conversationId}
                isHiddenMode={activeTab === 'HIDDEN'}
                onClick={() => onSelectConversation(conversation.conversationId)}
                onHide={handleHide}
                onUnhide={handleUnhide}
                onLeave={handleLeave}
              />
            ))}

            {/* Load More Button */}
            {hasNextPage && (
              <div className="p-2 text-center">
                <button
                  type="button"
                  onClick={() => fetchNext()}
                  disabled={isFetchingNext}
                  className="px-3 py-1.5 text-xs font-medium text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 rounded-lg transition-colors cursor-pointer inline-flex items-center gap-1.5"
                >
                  {isFetchingNext && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  Tải thêm hội thoại cũ
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
