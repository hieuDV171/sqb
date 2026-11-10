import { Button } from '@/components/ui/button';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { useState, useMemo } from 'react';
import {
  useConversations,
  useHiddenConversations,
  useHideConversation,
  useUnhideConversation,
  useLeaveGroup,
} from '../hooks/useChat';
import { useChatStore } from '../stores/useChatStore';
import { chatService } from '../services/chatService';
import { ConversationItem } from './ConversationItem';
import {
  Search,
  Users,
  Lock,
  MessageSquarePlus,
  Mail,
  FolderLock,
  LockOpen,
  Loader2,
} from 'lucide-react';

interface ConversationListProps {
  onSelectConversation: (conversationId: number) => void;
  onOpenNewDirectModal: () => void;
  onOpenCreateGroupModal: () => void;
  onCloseConversation: () => void;
  onOpenPinModal: () => void;
  onOpenGroupInvitations: () => void;
  hasGroupInvitations: boolean;
}

type TabType = 'ALL' | 'GROUPS' | 'HIDDEN';

export function ConversationList({
  onSelectConversation,
  onOpenNewDirectModal,
  onOpenCreateGroupModal,
  onOpenPinModal,
  onCloseConversation,
  onOpenGroupInvitations,
  hasGroupInvitations,
}: ConversationListProps) {
  const { activeConversationId, isPinUnlocked, lockPin } = useChatStore();
  const [activeTab, setActiveTab] = useState<TabType>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const normal = useConversations();
  const hidden = useHiddenConversations(activeTab === 'HIDDEN' && isPinUnlocked);
  const isHidden = activeTab === 'HIDDEN';
  const locked = isHidden && !isPinUnlocked;
  const current = isHidden ? hidden : normal;
  const normalData = normal.data;
  const hiddenData = hidden.data;

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
    const source = activeTab === 'HIDDEN' ? hiddenConversations : allConversations;
    const visible = activeTab === 'GROUPS' ? source.filter((item) => item.type === 'GROUP') : source;

    if (!searchTerm.trim()) return visible;

    const query = searchTerm.trim().toLocaleLowerCase('vi-VN');
    return visible.filter((c) =>
      c.displayName.toLowerCase().includes(query) ||
      (c.lastMessage?.content && c.lastMessage.content.toLowerCase().includes(query))
    );
  }, [activeTab, allConversations, hiddenConversations, searchTerm]);

  const handleTabChange = (tab: TabType) => {
    setActiveTab(tab);
    if (tab === 'HIDDEN' && !isPinUnlocked) onOpenPinModal();
  };

  const handleHide = (id: number) => {
    hideMutation.mutate(id, { onSuccess: () => { if (activeConversationId === id) onCloseConversation(); } });
  };

  const handleUnhide = (id: number) => {
    unhideMutation.mutate(id);
  };

  const handleLeave = (id: number) => {
    if (window.confirm('Bạn có chắc chắn muốn rời khỏi nhóm này không?')) {
      leaveGroupMutation.mutate({ conversationId: id });
    }
  };

  const { sentinel, supportsAutoLoad } = useInfiniteScroll(
    !locked && current.hasNextPage && !current.isFetching && !current.isFetchNextPageError && filteredList.length > 0,
    current.fetchNextPage,
    current.data?.pages.length ?? 0,
  );

  return (
    <section aria-labelledby="inbox-heading" className="flex h-full min-h-0 flex-col bg-card md:border-r md:border-border">
      <header className="shrink-0 space-y-3 border-b border-border p-3 sm:p-4">
        <div className="flex items-center justify-between gap-2">
          <h1 id="inbox-heading" className="text-xl font-bold tracking-tight sm:text-2xl">Tin nhắn</h1>
          <div className="flex gap-0.5">
            {isHidden && isPinUnlocked && (
              <Button variant="ghost" size="icon" className="size-11 text-warning" aria-label="Khóa kho ẩn" title="Khóa kho ẩn"
                onClick={() => {
                  if (hiddenConversations.some((item) => item.conversationId === activeConversationId)) onCloseConversation();
                  lockPin();
                  void chatService.lockHiddenChat().catch(() => {
                    // The API client reports the error. The local view stays locked.
                  });
                  setActiveTab('ALL');
                }}>
                <Lock aria-hidden="true" />
              </Button>
            )}
            <Button variant="ghost" size="icon" className="size-11" aria-label="Nhắn tin trực tiếp" title="Nhắn tin trực tiếp" onClick={onOpenNewDirectModal}>
              <MessageSquarePlus aria-hidden="true" />
            </Button>
            <Button variant="ghost" size="icon" className="size-11" aria-label="Tạo nhóm trò chuyện" title="Tạo nhóm trò chuyện" onClick={onOpenCreateGroupModal}>
              <Users aria-hidden="true" />
            </Button>
          </div>
        </div>
        <label className="relative block">
          <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <span className="sr-only">Tìm hội thoại đã tải</span>
          <input type="search" value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)}
            placeholder="Tìm trong hội thoại đã tải"
            className="min-h-11 w-full rounded-xl border border-input bg-background pr-3 pl-9 text-base outline-none placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring/50 md:text-sm" />
        </label>
        <div role="group" aria-label="Lọc hội thoại" className="flex flex-wrap gap-1 rounded-xl bg-surface-subtle p-1">
          {([['ALL', 'Tất cả'], ['GROUPS', 'Nhóm'], ['HIDDEN', 'Kho ẩn']] as const).map(([key, label]) => (
            <Button key={key} variant="ghost" aria-pressed={activeTab === key} onClick={() => handleTabChange(key)}
              className={cn('min-h-11 flex-1 gap-1.5 px-2', activeTab === key && 'bg-academic-soft text-academic hover:bg-academic-soft hover:text-academic')}>
              {key === 'HIDDEN' && (isPinUnlocked ? <LockOpen aria-hidden="true" /> : <Lock aria-hidden="true" />)}
              {label}
            </Button>
          ))}
        </div>
        <Button variant="outline" className="min-h-11 w-full justify-start" onClick={onOpenGroupInvitations}>
          <Mail aria-hidden="true" /> Lời mời vào nhóm
          {hasGroupInvitations && (
            <>
              <span aria-hidden="true" className="ml-auto size-2 rounded-full bg-academic" />
              <span className="sr-only">Có lời mời đang chờ</span>
            </>
          )}
        </Button>
      </header>

      <div className="min-h-0 flex-1 space-y-1 overflow-y-auto overscroll-contain p-2">
        {locked ? (
          <div className="space-y-3 p-6 text-center">
            <FolderLock aria-hidden="true" className="mx-auto size-7 text-warning" />
            <p className="font-semibold">Kho ẩn đang khóa</p>
            <Button variant="outline" className="min-h-11" onClick={onOpenPinModal}>Mở kho ẩn</Button>
          </div>
        ) : current.isLoading ? (
          <div aria-busy="true" aria-label="Đang tải hội thoại" className="space-y-2">
            {[1, 2, 3, 4].map((row) => <Skeleton key={row} className="h-16 rounded-xl" />)}
          </div>
        ) : current.isLoadingError ? (
          <ErrorState title="Không thể tải hội thoại" onRetry={() => void current.refetch()} />
        ) : filteredList.length ? (
          filteredList.map((conversation) => (
            <ConversationItem key={conversation.conversationId} conversation={conversation}
              isActive={activeConversationId === conversation.conversationId} isHiddenMode={isHidden}
              onClick={() => onSelectConversation(conversation.conversationId)}
              onHide={handleHide} onUnhide={handleUnhide} onLeave={handleLeave} />
          ))
        ) : (
          <div className="space-y-1.5 p-6 text-center">
            <p className="font-semibold">{searchTerm.trim() ? 'Chưa tìm thấy hội thoại' : isHidden ? 'Kho trò chuyện ẩn trống' : 'Chưa có hội thoại'}</p>
            {current.hasNextPage && <p className="text-sm text-muted-foreground">Có thể xem thêm hội thoại cũ hơn.</p>}
          </div>
        )}
        {!locked && current.hasNextPage && (
          <div className="text-center">
            <div ref={sentinel} className="h-px" aria-hidden="true" />
            {current.isFetchingNextPage ? (
              <p role="status" className="inline-flex items-center gap-2 p-2 text-sm text-muted-foreground">
                <Loader2 aria-hidden="true" className="size-4 motion-safe:animate-spin" /> Đang tải hội thoại…
              </p>
            ) : (!supportsAutoLoad || current.isFetchNextPageError || !filteredList.length) && (
              <Button variant="ghost" className="min-h-11" disabled={current.isFetching} onClick={() => void current.fetchNextPage({ cancelRefetch: false })}>
                {current.isFetchNextPageError ? 'Thử tải lại' : 'Tải thêm hội thoại'}
              </Button>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
