package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.modules.conversation.dto.ChatPresenceDto;
import com.frozenheart.backend.modules.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPresenceService {
    private static final String PREFIX = "sqb:{chat-presence}:";
    private static final String DEADLINES = PREFIX + "deadlines";
    private static final String CHANGES = PREFIX + "changes";
    private static final long ACTIVE_MS = 45_000;
    private static final long DISCONNECT_GRACE_MS = 5_000;
    private static final RedisScript<Long> UPDATE = RedisScript.of(new ClassPathResource("redis/update-chat-presence.lua"), Long.class);
    private static final RedisScript<List> DRAIN = RedisScript.of(new ClassPathResource("redis/drain-chat-presence.lua"), List.class);

    private final StringRedisTemplate redis;
    private final ConversationRepository conversations;
    private final SimpMessagingTemplate messaging;

    public void heartbeat(Long userId, String socketId, boolean active) {
        if (userId == null || socketId == null) return;
        String user = userId.toString();
        try {
            redis.execute(UPDATE, List.of(PREFIX + "sockets:" + user, DEADLINES, CHANGES), user, socketId,
                    Long.toString(System.currentTimeMillis()), Long.toString(active ? ACTIVE_MS : DISCONNECT_GRACE_MS),
                    Boolean.toString(active));
        } catch (DataAccessException unavailable) {
            log.debug("Chat presence update unavailable", unavailable);
        }
    }

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        heartbeat(JwtPayload.getUserId(event.getUser()), event.getSessionId(), false);
    }

    public List<ChatPresenceDto> snapshot(Long viewerId, List<Long> ids) {
        List<Long> conversationIds = ids.stream().distinct().toList();
        if (conversationIds.isEmpty()) return List.of();
        var links = conversations.findPresencePeers(viewerId, conversationIds);
        long now = System.currentTimeMillis();
        Set<Long> active = Set.of();
        try {
            active = activeUsers(links.stream().map(ConversationRepository.PresenceLink::getSubjectId).distinct().toList(), now);
        } catch (DataAccessException unavailable) {
            log.debug("Chat presence snapshot unavailable", unavailable);
        }
        Set<Long> activeChats = new HashSet<>();
        for (var link : links) {
            if (active.contains(link.getSubjectId())) activeChats.add(link.getConversationId());
        }
        return conversationIds.stream().map(id -> new ChatPresenceDto(id, activeChats.contains(id), now)).toList();
    }

    @Scheduled(fixedDelay = 1_000)
    public void publishChanges() {
        List<Long> changed = List.of();
        try {
            List<?> pending = redis.execute(DRAIN, List.of(DEADLINES, CHANGES), Long.toString(System.currentTimeMillis()), "250");
            if (pending == null || pending.isEmpty()) return;
            changed = pending.stream().map(value -> Long.valueOf((String) value)).toList();
            var links = conversations.findPresenceViewers(changed);
            if (links.isEmpty()) return;
            long now = System.currentTimeMillis();
            var active = activeUsers(changed, now);
            Map<Long, List<ChatPresenceDto>> updates = new HashMap<>();
            for (var link : links) {
                updates.computeIfAbsent(link.getViewerId(), ignored -> new ArrayList<>()).add(
                        new ChatPresenceDto(link.getConversationId(), active.contains(link.getSubjectId()), now));
            }
            updates.forEach((viewer, payload) -> messaging.convertAndSendToUser(viewer.toString(), "/queue/chat-presence", payload));
        } catch (DataAccessException unavailable) {
            log.debug("Chat presence publication unavailable", unavailable);
            if (!changed.isEmpty()) {
                try {
                    redis.opsForSet().add(CHANGES, changed.stream().map(String::valueOf).toArray(String[]::new));
                } catch (DataAccessException redisUnavailable) {
                    log.debug("Could not queue retry for chat presence", redisUnavailable);
                }
            }
        }
    }

    private Set<Long> activeUsers(Collection<Long> userIds, long now) {
        if (userIds.isEmpty()) return Set.of();
        List<Double> scores = redis.opsForZSet().score(DEADLINES, userIds.stream().map(String::valueOf).toArray(Object[]::new));
        if (scores == null) return Set.of();
        Set<Long> result = new HashSet<>();
        int index = 0;
        for (Long userId : userIds) {
            Double deadline = scores.get(index++);
            if (deadline != null && deadline > now) result.add(userId);
        }
        return result;
    }
}
