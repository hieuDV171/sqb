package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.constant.EmbeddingConstants;
import com.frozenheart.backend.core.entity.session.DuplicateCheckStatus;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.modules.ai.service.AiSidecarClientService;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.session.repository.QuestionMediaRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.service.DuplicateCheckRequested;
import com.frozenheart.backend.modules.session.service.QuestionImageHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceImplTest {
    @Mock QuestionRepository questions;
    @Mock QuestionMediaRepository media;
    @Mock SessionRepository sessions;
    @Mock AiSidecarClientService sidecar;
    @Mock QuestionImageHash imageHash;
    @Mock MediaService mediaService;
    @Mock TransactionTemplate transaction;
    @InjectMocks DuplicateDetectionServiceImpl worker;

    private Question input;

    @BeforeEach
    void fixtures() {
        input = Question.builder().id(101L).version(7L).content("Một cộng một bằng mấy?")
                .status(QuestionStatus.PENDING).displayOrder(1).build();
        var subject = Subject.builder().id(5L).build();
        var session = Session.builder().id(10L).status(SessionStatus.PENDING).question(input)
                .courseClass(CourseClass.builder().subject(subject).build()).build();
        when(transaction.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> action = invocation.getArgument(0);
            return action.doInTransaction(null);
        });
        when(sessions.findByIdFetchCourseClassAndQuestions(10L)).thenReturn(Optional.of(session));
        when(sessions.lockReviewSessions(List.of(10L), false)).thenReturn(List.of(10L));
        when(questions.findBySessionId(10L)).thenReturn(List.of(input));
    }

    @Test
    void retriesFailedEmbeddingAndCompletesOnlyAfterAValidBatch() {
        float[] vector = new float[EmbeddingConstants.TEXT_EMBEDDING_DIMS];
        vector[0] = 1;
        when(sidecar.getBatchTextEmbeddings(anyList())).thenReturn(List.of()).thenReturn(List.of(vector));

        worker.checkAfterCommit(new DuplicateCheckRequested(10L));
        assertEquals(DuplicateCheckStatus.RETRY, input.getDuplicateCheckStatus());
        assertEquals(1, input.getDuplicateCheckAttempts());
        assertTrue(input.getDuplicateCheckNextAttemptAt().isAfter(Instant.now()));

        input.setDuplicateCheckNextAttemptAt(Instant.EPOCH);
        worker.checkAfterCommit(new DuplicateCheckRequested(10L));
        assertEquals(DuplicateCheckStatus.COMPLETE, input.getDuplicateCheckStatus());
        assertArrayEquals(vector, input.getTextEmbedding());
        assertTrue(input.getDuplicateWarnings().isEmpty());
        assertNull(input.getDuplicateCheckAttempts());
    }

    @Test
    void staleResultCannotOverwriteEditedQuestion() {
        when(sidecar.getBatchTextEmbeddings(anyList())).thenAnswer(invocation -> {
            input.setVersion(8L);
            input.queueDuplicateCheck(true);
            return List.of();
        });

        worker.checkAfterCommit(new DuplicateCheckRequested(10L));
        assertEquals(DuplicateCheckStatus.PENDING, input.getDuplicateCheckStatus());
        assertNull(input.getDuplicateCheckAttempts());
        assertNull(input.getDuplicateWarnings());
    }
}
