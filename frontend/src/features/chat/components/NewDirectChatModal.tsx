import { useState, useEffect } from 'react';
import { useCreateDirectConversation } from '../hooks/useChat';
import { userService } from '@/services/userService';
import {
  MessageSquarePlus,
  Search,
  X,
  Loader2,
  ArrowRight,
} from 'lucide-react';
import type { FriendDto } from '@/types/user.types';

interface NewDirectChatModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated?: (conversationId: number) => void;
}

export function NewDirectChatModal({
  isOpen,
  onClose,
  onCreated,
}: NewDirectChatModalProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [friends, setFriends] = useState<FriendDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const createDirectMutation = useCreateDirectConversation();

  useEffect(() => {
    if (isOpen) {
      setSearchTerm('');
      setIsLoading(true);
      userService
        .getMyFriends(undefined, 50)
        .then((res) => {
          if (res?.data?.items) {
            setFriends(res.data.items);
          }
        })
        .finally(() => setIsLoading(false));
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleStartChat = (targetUserId: number) => {
    createDirectMutation.mutate(
      { targetUserId },
      {
        onSuccess: (res) => {
          onCreated?.(res.data.conversationId);
          onClose();
        },
      }
    );
  };

  const filteredFriends = friends.filter((f) =>
    f.fullName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in">
      <div
        className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl p-6 relative animate-in zoom-in-95 duration-200 flex flex-col max-h-[85vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
              <MessageSquarePlus className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
                Tin nhắn mới
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Bắt đầu trò chuyện trực tiếp 1-1
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search */}
        <div className="py-4">
          <div className="relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm kiếm bạn bè..."
              className="w-full pl-10 pr-4 py-2 text-sm bg-slate-100 dark:bg-slate-800/80 rounded-xl outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
            />
          </div>
        </div>

        {/* Friend List */}
        <div className="flex-1 overflow-y-auto space-y-1.5 pr-1">
          {isLoading ? (
            <div className="py-12 text-center text-xs text-slate-400 flex flex-col items-center justify-center gap-2">
              <Loader2 className="w-5 h-5 animate-spin text-indigo-500" />
              Đang tải danh sách bạn bè...
            </div>
          ) : filteredFriends.length === 0 ? (
            <div className="py-12 text-center text-xs text-slate-400">
              {searchTerm ? 'Không tìm thấy bạn bè phù hợp' : 'Bạn chưa có người bạn nào'}
            </div>
          ) : (
            filteredFriends.map((friend) => (
              <div
                key={friend.userId}
                onClick={() => handleStartChat(friend.userId)}
                className="flex items-center justify-between p-2.5 rounded-2xl hover:bg-slate-100 dark:hover:bg-slate-800/60 transition-colors cursor-pointer group"
              >
                <div className="flex items-center gap-3 truncate">
                  <div className="w-10 h-10 rounded-full overflow-hidden bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white text-sm font-semibold shrink-0">
                    {friend.avatarUrl ? (
                      <img src={friend.avatarUrl} alt={friend.fullName} className="w-full h-full object-cover" />
                    ) : (
                      friend.fullName.charAt(0).toUpperCase()
                    )}
                  </div>
                  <div className="truncate">
                    <h4 className="text-sm font-medium text-slate-800 dark:text-slate-200 truncate">
                      {friend.fullName}
                    </h4>
                    <span className="text-[11px] text-slate-400">
                      {friend.mutualFriendsCount} bạn chung
                    </span>
                  </div>
                </div>

                <button
                  type="button"
                  disabled={createDirectMutation.isPending}
                  className="p-2 rounded-xl text-slate-400 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 group-hover:bg-indigo-50 dark:group-hover:bg-indigo-950/40 transition-colors cursor-pointer"
                >
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
