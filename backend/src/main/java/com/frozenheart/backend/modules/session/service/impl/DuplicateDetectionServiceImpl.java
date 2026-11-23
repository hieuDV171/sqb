package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.DuplicateCheckStatus;
import com.frozenheart.backend.core.entity.session.DuplicateWarning;
import com.frozenheart.backend.core.entity.session.DuplicateWarning.Kind;
import com.frozenheart.backend.core.entity.session.DuplicateWarning.Scope;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.modules.ai.service.AiSidecarClientService;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.session.repository.QuestionMediaRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.service.DuplicateCheckRequested;
import com.frozenheart.backend.modules.session.service.DuplicateDetectionService;
import com.frozenheart.backend.modules.session.service.QuestionImageHash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static com.frozenheart.backend.modules.session.service.impl.QuestionDuplicateComparison.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateDetectionServiceImpl implements DuplicateDetectionService {
    private static final int EMBEDDING_BATCH_SIZE = 32;
    private static final int MAX_MATCHES = 5;
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(6));

    private final QuestionRepository questions;
    private final QuestionMediaRepository media;
    private final SessionRepository sessions;
    private final AiSidecarClientService sidecar;
    private final QuestionImageHash imageHash;
    private final MediaService mediaService;
    private final TransactionTemplate transaction;

    private record Snapshot(List<Candidate> submitted, List<Candidate> bank) {}
    private record Match(DuplicateWarning warning, boolean sameTopic) {}

    @Async
    @TransactionalEventListener
    public void checkAfterCommit(DuplicateCheckRequested event) {
        processSafely(event.sessionId());
    }

    @Async
    @Override
    public void asyncCheckDuplicates(Long sessionId) {
        processSafely(sessionId);
    }

    @Scheduled(fixedDelayString = "${sqb.duplicate-check.poll-ms:60000}")
    public void drainDueChecks() {
        for (Long sessionId : questions.findDuplicateCheckSessions(Instant.now(), PageRequest.of(0, 10))) {
            processSafely(sessionId);
        }
    }

    private void processSafely(Long sessionId) {
        try {
            process(sessionId);
        } catch (RuntimeException error) {
            log.warn("Duplicate check could not persist results for session {}", sessionId, error);
        }
    }

    private void process(Long sessionId) {
        Instant now = Instant.now();
        Snapshot snapshot = transaction.execute(status -> load(sessionId, now));
        if (snapshot == null) return;

        List<Candidate> candidates = Stream.concat(snapshot.submitted().stream(), snapshot.bank().stream()).toList();
        Map<Long, float[]> vectors = new HashMap<>();
        List<Candidate> missing = candidates.stream().filter(candidate -> !validVector(candidate.embedding())).toList();
        for (int start = 0; start < missing.size(); start += EMBEDDING_BATCH_SIZE) {
            List<Candidate> batch = missing.subList(start, Math.min(start + EMBEDDING_BATCH_SIZE, missing.size()));
            List<float[]> generated = sidecar.getBatchTextEmbeddings(batch.stream().map(Candidate::embeddingText).toList());
            if (generated == null || generated.size() != batch.size()) break;
            for (int index = 0; index < generated.size(); index++) {
                if (validVector(generated.get(index))) vectors.put(batch.get(index).id(), generated.get(index));
            }
        }

        var hashes = new HashMap<String, Long>();
        var failedImages = new HashSet<String>();
        for (Candidate candidate : candidates) {
            for (Image image : candidate.images()) {
                if (image.hash() != null && image.key() != null) hashes.put(image.key(), image.hash());
            }
        }
        boolean hasSubmittedImages = snapshot.submitted().stream().anyMatch(candidate -> !candidate.images().isEmpty());
        if (hasSubmittedImages) {
            for (Candidate candidate : candidates) {
                for (Image image : candidate.images()) {
                    if (image.key() == null || hashes.containsKey(image.key()) || failedImages.contains(image.key())) continue;
                    try {
                        hashes.put(image.key(), imageHash.read(image.key()));
                    } catch (Exception error) {
                        failedImages.add(image.key());
                        log.debug("Could not hash question media {}", image.id(), error);
                    }
                }
            }
        }

        boolean textFinished = vectors.size() == missing.size();
        Map<Long, List<DuplicateWarning>> warnings = findMatches(snapshot, vectors, hashes);
        transaction.executeWithoutResult(status -> storeResults(sessionId, snapshot, warnings, vectors, hashes,
                failedImages, textFinished, Instant.now()));
    }

    private Snapshot load(Long sessionId, Instant now) {
        var session = sessions.findByIdFetchCourseClassAndQuestions(sessionId).orElse(null);
        if (session == null || session.getStatus() == SessionStatus.RESOLVED) return null;
        var submitted = session.getQuestions().stream()
                .filter(question -> question.getStatus() != QuestionStatus.REJECTED)
                .sorted(Comparator.comparingInt(Question::getDisplayOrder).thenComparing(Question::getId))
                .map(question -> snapshot(question, isDue(question, now), mediaService::extractObjectKey))
                .toList();
        if (submitted.stream().noneMatch(Candidate::due)) return null;
        var bank = questions.findApprovedBySubjectId(session.getCourseClass().getSubject().getId()).stream()
                .map(question -> snapshot(question, false, mediaService::extractObjectKey)).toList();
        return new Snapshot(submitted, bank);
    }

    private static boolean isDue(Question question, Instant now) {
        var status = question.getDuplicateCheckStatus();
        return question.getStatus() == QuestionStatus.PENDING
                && (status == null || status == DuplicateCheckStatus.PENDING
                || status == DuplicateCheckStatus.RETRY && question.getDuplicateCheckNextAttemptAt() != null
                    && !question.getDuplicateCheckNextAttemptAt().isAfter(now));
    }

    private static Map<Long, List<DuplicateWarning>> findMatches(Snapshot snapshot, Map<Long, float[]> vectors,
            Map<String, Long> hashes) {
        Map<Long, List<DuplicateWarning>> results = new LinkedHashMap<>();
        for (int index = 0; index < snapshot.submitted().size(); index++) {
            Candidate input = snapshot.submitted().get(index);
            if (!input.due()) continue;
            List<Match> matches = new ArrayList<>();
            addMatches(matches, input, snapshot.bank(), Scope.QUESTION_BANK, vectors, hashes);
            addMatches(matches, input, snapshot.submitted().subList(0, index), Scope.SAME_SESSION, vectors, hashes);
            var ranked = matches.stream().sorted(Comparator
                    .comparing((Match match) -> match.warning().kind())
                    .thenComparing(match -> !match.sameTopic())
                    .thenComparing(match -> match.warning().tier())
                    .thenComparing(match -> Objects.requireNonNullElse(match.warning().hammingDistance(), 0))
                    .thenComparing(match -> -Objects.requireNonNullElse(match.warning().similarityScore(), 0.0))
                    .thenComparing(match -> match.warning().similarQuestionId()))
                    .limit(MAX_MATCHES).map(Match::warning).toList();
            results.put(input.id(), ranked);
        }
        return results;
    }

    private static void addMatches(List<Match> matches, Candidate input, List<Candidate> candidates, Scope scope,
            Map<Long, float[]> vectors, Map<String, Long> hashes) {
        for (Candidate candidate : candidates) {
            DuplicateWarning warning = compare(input, candidate, scope, vectors, hashes);
            if (warning != null) {
                matches.add(new Match(warning, input.topicId() != null && input.topicId().equals(candidate.topicId())));
            }
        }
    }

    private void storeResults(Long sessionId, Snapshot snapshot, Map<Long, List<DuplicateWarning>> warnings,
            Map<Long, float[]> vectors, Map<String, Long> hashes, Set<String> failedImages, boolean textFinished,
            Instant now) {
        if (sessions.lockReviewSessions(List.of(sessionId), false).isEmpty()) return;
        List<Question> current = questions.findBySessionId(sessionId);
        Map<Long, Long> expectedVersions = new HashMap<>();
        snapshot.submitted().forEach(candidate -> expectedVersions.put(candidate.id(), candidate.version()));
        var active = current.stream().filter(question -> question.getStatus() != QuestionStatus.REJECTED).toList();
        if (active.size() != expectedVersions.size() || active.stream().anyMatch(question ->
                !expectedVersions.containsKey(question.getId())
                || !Objects.equals(expectedVersions.get(question.getId()), question.getVersion()))) return;

        var bankIds = snapshot.bank().stream().filter(candidate -> vectors.containsKey(candidate.id()))
                .map(Candidate::id).toList();
        if (!bankIds.isEmpty()) {
            Map<Long, Long> bankVersions = new HashMap<>();
            snapshot.bank().forEach(candidate -> bankVersions.put(candidate.id(), candidate.version()));
            for (Question question : questions.findAllById(bankIds)) {
                if (Objects.equals(bankVersions.get(question.getId()), question.getVersion())) {
                    question.setTextEmbedding(vectors.get(question.getId()));
                }
            }
        }

        for (Question question : active) {
            if (vectors.containsKey(question.getId())) question.setTextEmbedding(vectors.get(question.getId()));
            if (!warnings.containsKey(question.getId()) || question.getStatus() != QuestionStatus.PENDING) continue;
            List<DuplicateWarning> matches = warnings.get(question.getId());
            question.setDuplicateWarnings(matches);
            boolean exact = matches.stream().anyMatch(warning -> warning.kind() == Kind.EXACT);
            Candidate input = snapshot.submitted().stream().filter(value -> value.id().equals(question.getId()))
                    .findFirst().orElseThrow();
            boolean imageFinished = input.images().stream().noneMatch(image -> failedImages.contains(image.key()));
            if (exact || textFinished && imageFinished) {
                question.setDuplicateCheckStatus(exact || matches.isEmpty()
                        ? DuplicateCheckStatus.COMPLETE : DuplicateCheckStatus.NEEDS_REVIEW);
                question.setDuplicateCheckAttempts(null);
                question.setDuplicateCheckNextAttemptAt(null);
            } else {
                int attempt = input.attempts() + 1;
                question.setDuplicateCheckAttempts(attempt);
                boolean retry = attempt <= RETRY_DELAYS.size();
                question.setDuplicateCheckStatus(retry ? DuplicateCheckStatus.RETRY : DuplicateCheckStatus.NEEDS_REVIEW);
                question.setDuplicateCheckNextAttemptAt(retry ? now.plus(RETRY_DELAYS.get(attempt - 1)) : null);
            }
        }
        List<Long> mediaIds = Stream.concat(snapshot.submitted().stream(), snapshot.bank().stream())
                .flatMap(candidate -> candidate.images().stream())
                .filter(image -> image.hash() == null && image.key() != null && hashes.containsKey(image.key()))
                .map(Image::id).distinct().toList();
        if (!mediaIds.isEmpty()) {
            for (QuestionMedia attachment : media.findAllById(mediaIds)) {
                if (attachment.getPHash() == null) {
                    String key = attachment.getObjectKey() != null ? attachment.getObjectKey()
                            : mediaService.extractObjectKey(attachment.getUrl());
                    attachment.setObjectKey(key);
                    attachment.setPHash(hashes.get(key));
                }
            }
        }
    }
}
