import { useState, useMemo } from 'react';
import {
  useGroupMembers,
  useAddGroupMembers,
  useRemoveGroupMember,
  useUpdateMemberRole,
} from '../hooks/useChat';
import { userService } from '@/services/userService';
import { useAuthStore } from '@/stores/useAuthStore';
import type { ConversationRole } from '../types/chat.types';
import type { FriendDto } from '@/types/user.types';
import {
  Users,
  Crown,
  Shield,
  User,
  Search,
  UserPlus,
  Trash2,
  X,
  Loader2,
} from 'lucide-react';

interface GroupMembersModalProps {
  conversationId: number;
  isOpen: boolean;
  onClose: () => void;
}

export function GroupMembersModal({
  conversationId,
  isOpen,
  onClose,
}: GroupMembersModalProps) {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const [searchTerm, setSearchTerm] = useState('');
  const [isAddMode, setIsAddMode] = useState(false);
  const [friends, setFriends] = useState<FriendDto[]>([]);
  const [selectedFriendIds, setSelectedFriendIds] = useState<number[]>([]);
  const [isLoadingFriends, setIsLoadingFriends] = useState(false);

  const { data: members = [], isLoading: isLoadingMembers } = useGroupMembers(conversationId, isOpen);
  const addMembersMutation = useAddGroupMembers();
  const removeMemberMutation = useRemoveGroupMember();
  const updateRoleMutation = useUpdateMemberRole();

  // Xác định vai trò của người dùng hiện tại trong nhóm
  const myRole: ConversationRole | null = useMemo(() => {
    const me = members.find((m) => m.userId === currentUserId);
    return me ? me.role : null;
  }, [members, currentUserId]);

  const isChief = myRole === 'CHIEF';
  const isVillageElder = myRole === 'VILLAGE_ELDER';
  const canManage = isChief || isVillageElder;

  if (!isOpen) return null;

  const handleOpenAddMode = () => {
    setIsAddMode(true);
    setIsLoadingFriends(true);
    setSelectedFriendIds([]);
    userService
      .getMyFriends(undefined, 50)
      .then((res) => {
        if (res?.data?.items) {
          // Lọc ra những người bạn chưa có trong nhóm
          const existingIds = new Set(members.map((m) => m.userId));
          setFriends(res.data.items.filter((f) => !existingIds.has(f.userId)));
        }
      })
      .finally(() => setIsLoadingFriends(false));
  };

  const handleAddSelectedFriends = () => {
    if (selectedFriendIds.length === 0) return;
    addMembersMutation.mutate(
      {
        conversationId,
        data: { userIds: selectedFriendIds },
      },
      {
        onSuccess: () => {
          setIsAddMode(false);
          setSelectedFriendIds([]);
        },
      }
    );
  };

  const handleRemoveMember = (userId: number, name: string) => {
    if (window.confirm(`Bạn có chắc chắn muốn xóa thành viên "${name}" khỏi nhóm?`)) {
      removeMemberMutation.mutate({ conversationId, userId });
    }
  };

  const handleRoleChange = (userId: number, newRole: ConversationRole) => {
    updateRoleMutation.mutate({
      conversationId,
      userId,
      data: { role: newRole },
    });
  };

  const filteredMembers = members.filter((m) =>
    m.fullName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const renderRoleBadge = (role: ConversationRole) => {
    switch (role) {
      case 'CHIEF':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
            <Crown className="w-3 h-3 text-amber-500" />
            Tù trưởng
          </span>
        );
      case 'VILLAGE_ELDER':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 border border-indigo-500/20">
            <Shield className="w-3 h-3 text-indigo-500" />
            Già làng
          </span>
        );
      case 'VILLAGER':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400">
            <User className="w-3 h-3" />
            Dân làng
          </span>
        );
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in">
      <div
        className="w-full max-w-lg bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-2xl p-6 relative animate-in zoom-in-95 duration-200 flex flex-col max-h-[85vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
                Thành viên nhóm ({members.length})
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Tù trưởng, Già làng và Dân làng
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

        {/* Content Mode: Add Friends vs Member List */}
        {isAddMode ? (
          <div className="flex-1 flex flex-col overflow-hidden py-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                Chọn bạn bè để thêm vào nhóm
              </span>
              <button
                type="button"
                onClick={() => setIsAddMode(false)}
                className="text-xs text-indigo-600 hover:underline cursor-pointer"
              >
                Quay lại danh sách
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-1 pr-1 border border-slate-100 dark:border-slate-800 rounded-xl p-2">
              {isLoadingFriends ? (
                <div className="py-8 text-center text-xs text-slate-400 flex items-center justify-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Đang tải danh sách bạn bè...
                </div>
              ) : friends.length === 0 ? (
                <div className="py-8 text-center text-xs text-slate-400">
                  Tất cả bạn bè của bạn đều đã có mặt trong nhóm này.
                </div>
              ) : (
                friends.map((f) => {
                  const isChecked = selectedFriendIds.includes(f.userId);
                  return (
                    <div
                      key={f.userId}
                      onClick={() =>
                        setSelectedFriendIds((prev) =>
                          isChecked ? prev.filter((id) => id !== f.userId) : [...prev, f.userId]
                        )
                      }
                      className="flex items-center justify-between p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800/60 cursor-pointer transition-colors"
                    >
                      <div className="flex items-center gap-2.5 truncate">
                        <div className="w-8 h-8 rounded-full overflow-hidden bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-xs font-bold shrink-0">
                          {f.avatarUrl ? (
                            <img src={f.avatarUrl} alt={f.fullName} className="w-full h-full object-cover" />
                          ) : (
                            f.fullName.charAt(0).toUpperCase()
                          )}
                        </div>
                        <span className="text-xs font-medium truncate">{f.fullName}</span>
                      </div>

                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => {}}
                        className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 cursor-pointer"
                      />
                    </div>
                  );
                })
              )}
            </div>

            <div className="pt-2">
              <button
                type="button"
                onClick={handleAddSelectedFriends}
                disabled={selectedFriendIds.length === 0 || addMembersMutation.isPending}
                className="w-full py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {addMembersMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Thêm {selectedFriendIds.length} thành viên vào nhóm
              </button>
            </div>
          </div>
        ) : (
          <div className="flex-1 flex flex-col overflow-hidden py-4 space-y-3">
            {/* Search & Add Button */}
            <div className="flex items-center gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Tìm thành viên trong nhóm..."
                  className="w-full pl-9 pr-3 py-2 text-xs bg-slate-100 dark:bg-slate-800/80 rounded-xl outline-none text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
                />
              </div>

              {canManage && (
                <button
                  type="button"
                  onClick={handleOpenAddMode}
                  className="px-3 py-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-100 dark:hover:bg-indigo-900/50 font-semibold text-xs flex items-center gap-1.5 transition-colors cursor-pointer shrink-0"
                >
                  <UserPlus className="w-4 h-4" />
                  Thêm người
                </button>
              )}
            </div>

            {/* Members List */}
            <div className="flex-1 overflow-y-auto space-y-1.5 pr-1">
              {isLoadingMembers ? (
                <div className="py-8 text-center text-xs text-slate-400 flex items-center justify-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Đang tải danh sách thành viên...
                </div>
              ) : filteredMembers.length === 0 ? (
                <div className="py-8 text-center text-xs text-slate-400">
                  Không tìm thấy thành viên phù hợp
                </div>
              ) : (
                filteredMembers.map((member) => {
                  const isMe = member.userId === currentUserId;
                  const canKick =
                    isChief
                      ? !isMe
                      : isVillageElder
                      ? member.role === 'VILLAGER' && !isMe
                      : false;

                  return (
                    <div
                      key={member.userId}
                      className="flex items-center justify-between p-2.5 rounded-2xl hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors"
                    >
                      <div className="flex items-center gap-3 truncate">
                        <div className="w-10 h-10 rounded-full overflow-hidden bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white text-xs font-semibold shrink-0">
                          {member.avatarUrl ? (
                            <img src={member.avatarUrl} alt={member.fullName} className="w-full h-full object-cover" />
                          ) : (
                            member.fullName.charAt(0).toUpperCase()
                          )}
                        </div>
                        <div className="truncate">
                          <div className="flex items-center gap-1.5">
                            <span className="text-xs sm:text-sm font-semibold text-slate-900 dark:text-slate-100 truncate">
                              {member.fullName}
                            </span>
                            {isMe && (
                              <span className="text-[10px] text-slate-400 font-normal">(Bạn)</span>
                            )}
                          </div>
                          <div className="mt-0.5">{renderRoleBadge(member.role)}</div>
                        </div>
                      </div>

                      {/* Action Menu for Chief / Elder */}
                      <div className="flex items-center gap-1.5 shrink-0">
                        {/* Role Selector (only Chief can change roles) */}
                        {isChief && !isMe && (
                          <select
                            value={member.role}
                            onChange={(e) =>
                              handleRoleChange(member.userId, e.target.value as ConversationRole)
                            }
                            disabled={updateRoleMutation.isPending}
                            className="text-[11px] font-medium py-1 px-2 rounded-lg bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 outline-none cursor-pointer"
                          >
                            <option value="VILLAGER">Dân làng</option>
                            <option value="VILLAGE_ELDER">Già làng</option>
                            <option value="CHIEF">Chuyển quyền Tù trưởng</option>
                          </select>
                        )}

                        {/* Kick Member Button */}
                        {canKick && (
                          <button
                            type="button"
                            onClick={() => handleRemoveMember(member.userId, member.fullName)}
                            title="Xóa thành viên khỏi nhóm"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors cursor-pointer"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
