package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.entity.conversation.Conversation;
import com.frozenheart.backend.core.entity.conversation.ConversationRole;
import com.frozenheart.backend.core.entity.conversation.Message;
import com.frozenheart.backend.core.entity.conversation.MessageType;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class GroupActivityWriter {

    private static final String UNKNOWN_MEMBER = "Một thành viên";

    private final MessageRepository messages;
    private final UserProfileRepository profiles;

    public void membersAdded(Conversation group, long actorId, List<Long> memberIds) {
        var ids = new LinkedHashSet<>(memberIds);
        ids.add(actorId);
        var names = names(ids);
        List<String> members = memberIds.stream().map(id -> name(names, id)).toList();
        String added = members.size() == 1 ? members.getFirst()
                : String.join(", ", members.subList(0, members.size() - 1)) + " và " + members.getLast();
        save(group, "%s đã thêm %s vào nhóm".formatted(name(names, actorId), added));
    }

    public void memberJoined(Conversation group, long memberId) {
        save(group, "%s đã tham gia nhóm qua lời mời".formatted(name(names(List.of(memberId)), memberId)));
    }

    public void memberRemoved(Conversation group, long actorId, long memberId) {
        var names = names(List.of(actorId, memberId));
        save(group, "%s đã mời %s ra khỏi nhóm".formatted(name(names, actorId), name(names, memberId)));
    }

    public void memberLeft(Conversation group, long memberId, Long successorId) {
        var names = names(successorId == null ? List.of(memberId) : List.of(memberId, successorId));
        String content = "%s đã rời nhóm".formatted(name(names, memberId));
        if (successorId != null) {
            content += ". %s trở thành Tù trưởng".formatted(name(names, successorId));
        }
        save(group, content);
    }

    public void renamed(Conversation group, long actorId) {
        save(group, "%s đã đổi tên nhóm thành “%s”".formatted(name(names(List.of(actorId)), actorId), group.getName()));
    }

    public void roleChanged(Conversation group, long actorId, long memberId, ConversationRole newRole) {
        var names = names(List.of(actorId, memberId));
        String actor = name(names, actorId);
        String member = name(names, memberId);
        String content = switch (newRole) {
            case CHIEF -> "%s đã chuyển quyền Tù trưởng cho %s và trở thành Già làng".formatted(actor, member);
            case VILLAGE_ELDER -> "%s đã đổi vai trò của %s thành Già làng".formatted(actor, member);
            case VILLAGER -> "%s đã đổi vai trò của %s thành Dân làng".formatted(actor, member);
        };
        save(group, content);
    }

    private Map<Long, UserProfile> names(Iterable<Long> ids) {
        return profiles.findAllById(ids).stream().collect(Collectors.toMap(UserProfile::getUserId, profile -> profile));
    }

    private static String name(Map<Long, UserProfile> profiles, long userId) {
        UserProfile profile = profiles.get(userId);
        String fullName = profile == null ? null : profile.getFullName();
        return fullName == null || fullName.isBlank() ? UNKNOWN_MEMBER : fullName;
    }

    private void save(Conversation group, String content) {
        Instant now = Instant.now();
        messages.save(Message.builder()
                .conversation(group)
                .messageType(MessageType.SYSTEM)
                .content(content)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
