package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.conversation.*;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.conversation.dto.GroupCandidateDto;
import com.frozenheart.backend.modules.conversation.dto.GroupInvitationDto;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import com.frozenheart.backend.modules.conversation.repository.GroupInvitationRepository;
import com.frozenheart.backend.modules.conversation.repository.UserParticipantRepository;
import com.frozenheart.backend.modules.friendship.repository.FriendshipRepository;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.FollowRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupInvitationService {

    public static final int GROUP_CAPACITY = 200;
    public static final Duration INVITATION_LIFETIME = Duration.ofDays(7);

    private final GroupInvitationRepository invitations;
    private final ConversationRepository conversations;
    private final UserParticipantRepository participants;
    private final FriendshipRepository friendships;
    private final FollowRepository follows;
    private final BlockRepository blocks;
    private final UserProfileRepository profiles;
    private final HiddenChatAccess hiddenChatAccess;

    public record Connections(Set<Long> friends, Set<Long> following, Set<Long> followers) {
        public boolean eligible(Long id) {
            return friends.contains(id) || following.contains(id) || followers.contains(id);
        }
    }

    public Connections connections(Long actor, Collection<Long> targets) {
        if (targets.isEmpty()) {
            return new Connections(Set.of(), Set.of(), Set.of());
        }
        return new Connections(friendships.findAcceptedFriendUserIds(actor, targets),
                follows.findFollowedIdsFollowedByUser(actor, targets),
                follows.findFollowerIdsFollowingUser(actor, targets));
    }

    @Transactional(readOnly = true)
    public CursorResponse<GroupCandidateDto> candidates(Long groupId, long after, String search, int limit) {
        Long actor = JwtPayload.getCurrentUserPayload().getUserId();
        if (groupId != null) {
            var group = conversations.findById(groupId)
                    .orElseThrow(() -> new AppException(ResponseCode.CONVERSATION_NOT_FOUND));
            if (group.getType() != ConversationType.GROUP) {
                throw new AppException(ResponseCode.ACTION_NOT_ALLOWED);
            }
            var member = participants.findByUserIdAndConversationId(actor, groupId)
                    .filter(p -> p.getLeftAt() == null)
                    .orElseThrow(() -> new AppException(ResponseCode.ACTION_NOT_ALLOWED));
            hiddenChatAccess.requireIfHidden(member);
        }
        int size = Math.clamp(limit, 1, 50);
        var rows = invitations.findCandidates(actor, groupId, Math.max(after, 0), search.strip(), Instant.now(),
                PageRequest.of(0, size + 1));
        var page = rows.subList(0, Math.min(size, rows.size()));
        var relationship = connections(actor, page.stream().map(UserProfile::getUserId).toList());
        return new CursorResponse<>(page.stream().map(profile -> {
            Long id = profile.getUserId();
            return new GroupCandidateDto(id, profile.getFullName(), profile.getAvatarUrl(),
                    relationship.friends().contains(id), relationship.following().contains(id),
                    relationship.followers().contains(id));
        }).toList(), pagination(rows, size, UserProfile::getUserId));
    }

    @Transactional(readOnly = true)
    public CursorResponse<GroupInvitationDto> incoming(long after, int limit) {
        Long actor = JwtPayload.getCurrentUserPayload().getUserId();
        int size = Math.clamp(limit, 1, 50);
        var rows = invitations.findIncoming(actor, Math.max(after, 0), Instant.now(), PageRequest.of(0, size + 1));
        var page = rows.subList(0, Math.min(size, rows.size()));
        var inviterIds = page.stream().map(i -> i.getInviter().getId()).distinct().toList();
        var names = profiles.findAllById(inviterIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getFullName));
        return new CursorResponse<>(page.stream().map(i -> new GroupInvitationDto(i.getId(),
                i.getConversation().getName(), i.getConversation().getAvatarUrl(),
                names.getOrDefault(i.getInviter().getId(), "Thành viên SQB"), i.getExpiresAt())).toList(),
                pagination(rows, size, GroupInvitation::getId));
    }

    @Transactional
    public Long accept(Long invitationId) {
        Long actor = JwtPayload.getCurrentUserPayload().getUserId();
        Conversation group = lockGroup(invitationId, actor);
        GroupInvitation invitation = invitations.findForInvitee(invitationId, actor)
                .filter(i -> i.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(GroupInvitationService::unavailableInvitation);
        Long inviter = invitation.getInviter().getId();
        if (!invitation.getInviter().isActive()
                || !participants.existsByUserIdAndConversationIdAndLeftAtIsNull(inviter, group.getId())
                || blocks.isBlockedBetween(inviter, actor)) {
            throw unavailableInvitation();
        }
        var membership = participants.findByUserIdAndConversationId(actor, group.getId());
        if (membership.isEmpty() || membership.get().getLeftAt() != null) {
            if (participants.countByConversationIdAndLeftAtIsNull(group.getId()) >= GROUP_CAPACITY) {
                throw new AppException(ResponseCode.CONSTRAINTS_UNSATISFIED, "Nhóm đã đạt số lượng tối đa 200 thành viên");
            }
            UserParticipant member = membership.orElseGet(() -> UserParticipant.builder()
                    .id(new UserParticipantId(actor, group.getId()))
                    .user(invitation.getInvitee()).conversation(group).build());
            member.setRole(ConversationRole.VILLAGER);
            member.setJoinedAt(Instant.now());
            member.setLeftAt(null);
            member.setRemovedFromGroup(null);
            participants.save(member);
        }
        invitations.delete(invitation);
        return group.getId();
    }

    @Transactional
    public void decline(Long invitationId) {
        Long actor = JwtPayload.getCurrentUserPayload().getUserId();
        lockGroup(invitationId, actor);
        if (invitations.deleteForInvitee(invitationId, actor) == 0) {
            throw unavailableInvitation();
        }
    }

    private Conversation lockGroup(Long invitationId, Long actor) {
        Long groupId = invitations.findConversationId(invitationId, actor)
                .orElseThrow(GroupInvitationService::unavailableInvitation);
        return conversations.findForMembershipUpdate(groupId).orElseThrow(GroupInvitationService::unavailableInvitation);
    }

    private static AppException unavailableInvitation() {
        return new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Lời mời không còn hiệu lực");
    }

    private static <T> CursorPaginationDto pagination(List<T> rows, int size, Function<T, Long> id) {
        boolean hasNext = rows.size() > size;
        return CursorPaginationDto.builder().hasNext(hasNext).after(hasNext ? id.apply(rows.get(size - 1)) : null).build();
    }
}
