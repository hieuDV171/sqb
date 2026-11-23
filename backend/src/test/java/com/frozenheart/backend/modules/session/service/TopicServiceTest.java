package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.session.Topic;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.session.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {
    @Mock TopicRepository topics;
    @Mock SubjectRepository subjects;
    @InjectMocks TopicService service;

    @Test
    void renameAndArchiveKeepsIdAndFiltersStudentDropdown() {
        var subject = Subject.builder().id(7L).build();
        var existing = Topic.builder().id(11L).subject(subject).code("TC01").name("Chương 1").build();
        var lecturer = JwtPayload.builder().role("LECTURER").userId(9L).build();
        try (var jwt = mockStatic(JwtPayload.class)) {
            jwt.when(JwtPayload::getCurrentUserPayload).thenReturn(lecturer);
            when(topics.countTeachingAssignments(7L, 9L)).thenReturn(1L);
            when(subjects.lockTopicCatalog(7L)).thenReturn(Optional.of(subject));
            when(topics.findByIdAndSubjectId(11L, 7L)).thenReturn(Optional.of(existing));
            when(topics.findBySubjectIdOrderByNameAsc(7L)).thenReturn(List.of(existing));
            when(topics.save(existing)).thenReturn(existing);

            var saved = service.save(7L, 11L, "  Lập lịch CPU  ", false);
            assertEquals(11L, saved.id());
            assertEquals("Lập lịch CPU", saved.name());
            assertFalse(saved.active());
            assertTrue(service.list(7L, false).isEmpty());
            assertSame(existing, service.select(7L, 11L, existing));
            assertThrows(AppException.class, () -> service.select(7L, 11L, null));
        }
    }

    @Test
    void deniesManagementOutsideLecturerAssignments() {
        var lecturer = JwtPayload.builder().role("LECTURER").userId(9L).build();
        try (var jwt = mockStatic(JwtPayload.class)) {
            jwt.when(JwtPayload::getCurrentUserPayload).thenReturn(lecturer);
            assertThrows(AppException.class, () -> service.list(7L, true));
            verify(topics, never()).findBySubjectIdOrderByNameAsc(7L);
        }
    }

    @Test
    void rejectsTopicOutsideTheSubject() {
        when(topics.findBySubjectIdAndIdIn(7L, Set.of(11L))).thenReturn(List.of());
        assertThrows(AppException.class, () -> service.lookup(7L, Set.of(11L)));
    }
}
