package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.entity.conversation.Conversation;
import com.frozenheart.backend.core.entity.conversation.ConversationRole;
import com.frozenheart.backend.core.entity.conversation.Message;
import com.frozenheart.backend.core.entity.conversation.MessageType;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.modules.conversation.repository.MessageRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupActivityWriterTest {
    @Test
    void savesOnlyTimelineRowsWithTheActionText() {
        var messages = mock(MessageRepository.class);
        var profiles = mock(UserProfileRepository.class);
        var writer = new GroupActivityWriter(messages, profiles);
        var group = Conversation.builder().id(10L).name("Ôn thi CSDL").build();
        when(profiles.findAllById(any())).thenReturn(List.of(
                UserProfile.builder().userId(1L).fullName("An").build(),
                UserProfile.builder().userId(2L).fullName("Bình").build(),
                UserProfile.builder().userId(3L).fullName("Chi").build(),
                UserProfile.builder().userId(4L).fullName("Dũng").build()));

        writer.membersAdded(group, 1L, List.of(2L, 3L, 4L));
        writer.memberJoined(group, 2L);
        writer.memberRemoved(group, 1L, 4L);
        writer.memberLeft(group, 1L, 3L);
        writer.renamed(group, 1L);
        writer.roleChanged(group, 1L, 3L, ConversationRole.CHIEF);

        var rows = ArgumentCaptor.forClass(Message.class);
        verify(messages, times(6)).save(rows.capture());
        verify(profiles).findAllById(Set.of(1L, 2L, 3L, 4L));
        assertEquals(List.of(
                "An đã thêm Bình, Chi và Dũng vào nhóm",
                "Bình đã tham gia nhóm qua lời mời",
                "An đã mời Dũng ra khỏi nhóm",
                "An đã rời nhóm. Chi trở thành Tù trưởng",
                "An đã đổi tên nhóm thành “Ôn thi CSDL”",
                "An đã chuyển quyền Tù trưởng cho Chi và trở thành Già làng"
        ), rows.getAllValues().stream().map(Message::getContent).toList());
        for (Message row : rows.getAllValues()) {
            assertEquals(MessageType.SYSTEM, row.getMessageType());
            assertSame(group, row.getConversation());
            assertNotNull(row.getCreatedAt());
            assertNull(row.getSender());
        }
        assertNull(group.getLastMessage());
        assertNull(group.getLastMessageAt());
    }
}
