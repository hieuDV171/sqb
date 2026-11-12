import { useRef, useState } from 'react';
import { AlertDialog } from '@base-ui/react/alert-dialog';
import { Dialog } from '@base-ui/react/dialog';
import { Menu } from '@base-ui/react/menu';
import { Check, ChevronDown, Crown, Loader2, Search, Shield, Trash2, User, UserPlus, Users, X } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useAuthStore } from '@/stores/useAuthStore';
import { useAddGroupMembers, useGroupMembers, useRemoveGroupMember, useUpdateMemberRole } from '../hooks/useChat';
import type { ConversationMemberDto, ConversationRole, GroupCandidateDto } from '../types/chat.types';
import { GroupCandidatePicker } from './GroupCandidatePicker';

interface GroupMembersModalProps {
  conversationId: number;
  isOpen: boolean;
  onClose: () => void;
}

type MemberAction =
  | { type: 'remove'; member: ConversationMemberDto }
  | { type: 'changeRole'; member: ConversationMemberDto; role: ConversationRole };

const roleInfo = {
  CHIEF: { label: 'Tù trưởng', icon: Crown, variant: 'gold' },
  VILLAGE_ELDER: { label: 'Già làng', icon: Shield, variant: 'info' },
  VILLAGER: { label: 'Dân làng', icon: User, variant: 'secondary' },
} as const;

const roleOptions = [
  { role: 'VILLAGER', label: 'Dân làng' },
  { role: 'VILLAGE_ELDER', label: 'Già làng' },
  { role: 'CHIEF', label: 'Chuyển quyền Tù trưởng' },
] as const;

function MemberRoleMenu({ member, disabled, onSelect }: {
  member: ConversationMemberDto;
  disabled: boolean;
  onSelect: (role: ConversationRole, trigger: HTMLButtonElement | null) => void;
}) {
  const trigger = useRef<HTMLButtonElement>(null);
  const nextRole = useRef<ConversationRole | null>(null);

  return (
    <Menu.Root modal={false} onOpenChangeComplete={(open) => {
      if (open || nextRole.current === null) return;
      const role = nextRole.current;
      nextRole.current = null;
      onSelect(role, trigger.current);
    }}>
      <Menu.Trigger ref={trigger} disabled={disabled}
        render={<Button type="button" variant="outline" className="min-h-11 justify-between text-base sm:w-40" />}
        aria-label={'Đổi vai trò của ' + member.fullName + ' (' + roleInfo[member.role].label + ')'}>
        {roleInfo[member.role].label}
        <ChevronDown aria-hidden="true" className="text-muted-foreground" />
      </Menu.Trigger>
      <Menu.Portal>
        <Menu.Positioner align="end" sideOffset={6} className="z-[72] max-w-(--available-width)">
          <Menu.Popup className="w-60 max-w-(--available-width) overflow-hidden rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
            {roleOptions.map(({ role, label }) => (
              <Menu.Item key={role} disabled={role === member.role} onClick={() => { nextRole.current = role; }}
                className="flex min-h-11 cursor-pointer items-center justify-between gap-2 rounded-lg px-3 py-2 text-base outline-none data-highlighted:bg-accent data-disabled:cursor-default">
                {label}
                {role === member.role && <Check aria-hidden="true" className="size-4 shrink-0 text-academic" />}
              </Menu.Item>
            ))}
          </Menu.Popup>
        </Menu.Positioner>
      </Menu.Portal>
    </Menu.Root>
  );
}

export function GroupMembersModal({ isOpen, ...props }: GroupMembersModalProps) {
  return isOpen ? <GroupMembersDialog key={props.conversationId} {...props} /> : null;
}

function GroupMembersDialog({ conversationId, onClose }: Omit<GroupMembersModalProps, 'isOpen'>) {
  const currentUserId = useAuthStore((state) => state.user?.id);
  const [searchTerm, setSearchTerm] = useState('');
  const [isAddMode, setIsAddMode] = useState(false);
  const [selectedPeople, setSelectedPeople] = useState<GroupCandidateDto[]>([]);
  const [action, setAction] = useState<MemberAction | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const actionTrigger = useRef<HTMLButtonElement>(null);
  const closeButton = useRef<HTMLButtonElement>(null);

  const { data: members = [], isLoading, isError, refetch } = useGroupMembers(conversationId);
  const addMembers = useAddGroupMembers();
  const removeMember = useRemoveGroupMember();
  const updateRole = useUpdateMemberRole();
  const isSubmitting = removeMember.isPending || updateRole.isPending;

  const myRole = members.find((member) => member.userId === currentUserId)?.role;
  const isChief = myRole === 'CHIEF';
  const canManage = isChief || myRole === 'VILLAGE_ELDER';
  const matchingMembers = members.filter((member) => member.fullName.toLowerCase().includes(searchTerm.trim().toLowerCase()));

  function canRemove(member: ConversationMemberDto) {
    return member.userId !== currentUserId && (
      (isChief && member.role !== 'CHIEF') || (myRole === 'VILLAGE_ELDER' && member.role === 'VILLAGER')
    );
  }

  const target = action && members.find((member) => member.userId === action.member.userId);
  const canConfirm = action && target && target.role === action.member.role && (
    action.type === 'remove' ? canRemove(target) : isChief && target.userId !== currentUserId
  );

  function askForConfirmation(nextAction: MemberAction, trigger: HTMLButtonElement | null) {
    actionTrigger.current = trigger;
    setActionError(null);
    setAction(nextAction);
  }

  async function confirmAction() {
    if (!action || !canConfirm || isSubmitting) return;
    setActionError(null);
    const variables = { conversationId, userId: action.member.userId };
    try {
      if (action.type === 'remove') {
        await removeMember.mutateAsync(variables);
      } else {
        await updateRole.mutateAsync({ ...variables, data: { role: action.role } });
      }
      setAction(null);
    } catch (error) {
      const message = typeof error === 'object' && error !== null && 'message' in error ? error.message : null;
      setActionError(typeof message === 'string' && message ? message : 'Thao tác thất bại. Hãy thử lại.');
    }
  }

  function addSelectedPeople() {
    if (!canManage || selectedPeople.length === 0) return;
    addMembers.mutate({ conversationId, data: { userIds: selectedPeople.map((person) => person.userId) } }, {
      onSuccess: () => {
        setIsAddMode(false);
        setSelectedPeople([]);
      },
    });
  }

  const removing = action?.type === 'remove';
  const transferring = action?.type === 'changeRole' && action.role === 'CHIEF';
  const actionLabel = removing ? 'Xóa thành viên' : transferring ? 'Chuyển quyền Tù trưởng' : 'Đổi vai trò';

  return (
    <Dialog.Root open onOpenChange={(open) => {
      if (!open && !action && !addMembers.isPending) onClose();
    }}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-[70] bg-black/55" />
        <Dialog.Popup className="fixed inset-x-4 top-1/2 z-[71] mx-auto flex max-h-[calc(100dvh-2rem)] max-w-xl -translate-y-1/2 flex-col rounded-2xl border border-border bg-card text-foreground shadow-xl">
          <div className="flex items-start gap-3 border-b border-border p-4 sm:p-5">
            <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-academic-soft text-academic">
              <Users aria-hidden="true" className="size-5" />
            </span>
            <div className="min-w-0 flex-1">
              <Dialog.Title className="text-xl font-bold sm:text-2xl">Thành viên nhóm ({members.length})</Dialog.Title>
              <Dialog.Description className="text-base text-muted-foreground">Tù trưởng, Già làng và Dân làng</Dialog.Description>
            </div>
            <Dialog.Close render={<Button ref={closeButton} type="button" variant="ghost" size="icon" className="size-11" />}
              aria-label="Đóng danh sách thành viên" disabled={addMembers.isPending}>
              <X aria-hidden="true" />
            </Dialog.Close>
          </div>

          {isAddMode && canManage ? (
            <div className="min-h-0 space-y-4 overflow-y-auto p-4 sm:p-5">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h4 className="text-base font-semibold">Thêm hoặc mời vào nhóm</h4>
                <Button type="button" variant="ghost" className="min-h-11 text-base" disabled={addMembers.isPending}
                  onClick={() => setIsAddMode(false)}>Quay lại danh sách</Button>
              </div>
              <GroupCandidatePicker conversationId={conversationId} selected={selectedPeople}
                onSelectionChange={setSelectedPeople} disabled={addMembers.isPending} />
              <Button type="button" className="min-h-11 w-full text-base" onClick={addSelectedPeople}
                disabled={selectedPeople.length === 0 || addMembers.isPending}>
                {addMembers.isPending && <Loader2 aria-hidden="true" className="motion-safe:animate-spin" />}
                Xác nhận ({selectedPeople.length} người)
              </Button>
            </div>
          ) : (
            <div className="flex min-h-0 flex-col gap-3 p-4 sm:p-5">
              <div className="flex flex-wrap gap-2">
                <label className="relative min-w-40 flex-1">
                  <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                  <span className="sr-only">Tìm thành viên trong nhóm</span>
                  <Input type="search" value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)}
                    placeholder="Tìm thành viên trong nhóm…" className="min-h-11 pl-9 md:text-base" />
                </label>
                {canManage && (
                  <Button type="button" variant="outline" className="min-h-11 text-base"
                    onClick={() => { setSelectedPeople([]); setIsAddMode(true); }}>
                    <UserPlus aria-hidden="true" /> Thêm người
                  </Button>
                )}
              </div>

              <div className="min-h-0 overflow-y-auto" aria-busy={isLoading}>
                {isLoading ? (
                  <p role="status" className="flex items-center justify-center gap-2 p-6 text-base text-muted-foreground">
                    <Loader2 aria-hidden="true" className="size-4 motion-safe:animate-spin" /> Đang tải thành viên…
                  </p>
                ) : isError && members.length === 0 ? (
                  <div className="space-y-2 p-6 text-center text-base">
                    <p>Không thể tải danh sách thành viên.</p>
                    <Button type="button" variant="outline" onClick={() => void refetch()}>Thử lại</Button>
                  </div>
                ) : matchingMembers.length === 0 ? (
                  <p className="p-6 text-center text-base text-muted-foreground">Không tìm thấy thành viên phù hợp.</p>
                ) : (
                  <ul className="space-y-1">
                    {matchingMembers.map((member) => {
                      const isMe = member.userId === currentUserId;
                      const { icon: RoleIcon, label, variant } = roleInfo[member.role];
                      return (
                        <li key={member.userId}
                          className="grid gap-2 rounded-xl p-2.5 hover:bg-surface-subtle sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
                          <div className="flex min-w-0 items-center gap-3">
                            <UserAvatar name={member.fullName} avatarUrl={member.avatarUrl} decorative className="size-11 text-base" />
                            <div className="min-w-0">
                              <p className="text-base font-semibold wrap-anywhere">
                                {member.fullName} {isMe && <span className="text-sm font-normal text-muted-foreground">(Bạn)</span>}
                              </p>
                              <Badge variant={variant} className="mt-1 text-sm">
                                <RoleIcon aria-hidden="true" className="size-3.5" /> {label}
                              </Badge>
                            </div>
                          </div>
                          {((isChief && !isMe) || canRemove(member)) && (
                            <div className="flex flex-wrap items-center gap-2">
                              {isChief && !isMe && (
                                <MemberRoleMenu member={member} disabled={isSubmitting}
                                  onSelect={(role, trigger) => askForConfirmation({ type: 'changeRole', member, role }, trigger)} />
                              )}
                              {canRemove(member) && (
                                <Button type="button" variant="ghost" size="icon" className="size-11 text-destructive"
                                  disabled={isSubmitting} aria-label={'Xóa ' + member.fullName + ' khỏi nhóm'}
                                  onClick={(event) => askForConfirmation({ type: 'remove', member }, event.currentTarget)}>
                                  <Trash2 aria-hidden="true" />
                                </Button>
                              )}
                            </div>
                          )}
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>
            </div>
          )}

          <AlertDialog.Root open={action !== null} onOpenChange={(open) => {
            if (!open && !isSubmitting) setAction(null);
          }}>
            {action && (
              <AlertDialog.Portal>
                <AlertDialog.Backdrop className="fixed inset-0 z-[73] bg-black/55" />
                <AlertDialog.Popup className="fixed inset-x-4 top-1/2 z-[74] mx-auto max-h-[calc(100dvh-2rem)] max-w-md -translate-y-1/2 space-y-4 overflow-y-auto rounded-2xl border border-border bg-card p-5 shadow-xl"
                  finalFocus={() => actionTrigger.current?.isConnected ? actionTrigger.current : closeButton.current}>
                  <AlertDialog.Title className="text-xl font-bold">{actionLabel}?</AlertDialog.Title>
                  <AlertDialog.Description className="text-base leading-relaxed wrap-anywhere text-muted-foreground">
                    {removing ? <>Xóa <strong>{action.member.fullName}</strong> khỏi nhóm. Người này sẽ mất quyền truy cập cuộc trò chuyện.</>
                      : transferring ? <>Chuyển quyền Tù trưởng cho <strong>{action.member.fullName}</strong>. Vai trò của bạn sẽ chuyển thành Già làng.</>
                        : action.type === 'changeRole' && <>Đổi vai trò của <strong>{action.member.fullName}</strong> từ <strong>{roleInfo[action.member.role].label}</strong> thành <strong>{roleInfo[action.role].label}</strong>.</>}
                  </AlertDialog.Description>
                  {actionError && <p role="alert" className="text-base text-destructive">{actionError}</p>}
                  {!canConfirm && !isSubmitting && (
                    <p role="status" className="text-sm text-muted-foreground">Vai trò hoặc danh sách thành viên đã thay đổi. Hãy đóng hộp thoại và kiểm tra lại.</p>
                  )}
                  <div className="flex flex-wrap justify-end gap-2">
                    <AlertDialog.Close render={<Button type="button" variant="outline" className="min-h-11 text-base" disabled={isSubmitting} />}>Hủy</AlertDialog.Close>
                    <Button type="button" variant={removing || transferring ? 'destructive' : 'default'} className="min-h-11 text-base"
                      disabled={!canConfirm || isSubmitting} onClick={() => void confirmAction()}>
                      {isSubmitting ? 'Đang xử lý…' : actionLabel}
                    </Button>
                  </div>
                </AlertDialog.Popup>
              </AlertDialog.Portal>
            )}
          </AlertDialog.Root>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
