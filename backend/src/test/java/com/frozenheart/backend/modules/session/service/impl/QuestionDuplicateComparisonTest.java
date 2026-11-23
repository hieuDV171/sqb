package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.DuplicateDetectionTier;
import com.frozenheart.backend.core.entity.session.DuplicateWarning.Kind;
import com.frozenheart.backend.core.entity.session.DuplicateWarning.Scope;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.frozenheart.backend.modules.session.service.impl.QuestionDuplicateComparison.*;
import static org.junit.jupiter.api.Assertions.*;

class QuestionDuplicateComparisonTest {
    @Test
    void ignoresOptionOrderButReportsAnswerConflicts() {
        var first = question(1L, "Đâu là đáp án đúng?", option("Một", true), option("Hai", false));
        var reordered = question(2L, "Đâu là đáp án đúng?", option("Hai", false), option("Một", true));
        var conflict = question(3L, "Đâu là đáp án đúng?", option("Hai", true), option("Một", false));
        assertEquals(Kind.EXACT, compare(first, reordered, Scope.QUESTION_BANK, Map.of(), Map.of()).kind());
        assertEquals(Kind.ANSWER_CONFLICT, compare(first, conflict, Scope.SAME_SESSION, Map.of(), Map.of()).kind());
    }

    @Test
    void sharedImageIsOnlyASimilaritySignal() {
        var left = snapshot(Question.builder().id(1L).content("Cây nhị phân có bao nhiêu nút?")
                .ownedMedia(QuestionMedia.builder().id(10L).mediaTarget(MediaTarget.CONTENT).pHash(12L).build())
                .build(), true, url -> url);
        var right = snapshot(Question.builder().id(2L).content("Độ sâu của cây là bao nhiêu?")
                .ownedMedia(QuestionMedia.builder().id(20L).mediaTarget(MediaTarget.CONTENT).pHash(13L).build())
                .build(), false, url -> url);

        var warning = compare(left, right, Scope.QUESTION_BANK, Map.of(), Map.of());
        assertEquals(Kind.SIMILAR, warning.kind());
        assertEquals(DuplicateDetectionTier.P_HASH, warning.tier());
        assertEquals(1, warning.hammingDistance());
    }

    @Test
    void meaningfulCodeWhitespaceRemainsDistinct() {
        var left = question(1L, "Output of print(\"a  b\")?", option("a b", false), option("a  b", true));
        var right = question(2L, "Output of print(\"a b\")?", option("a b", false), option("a  b", true));
        var warning = compare(left, right, Scope.SAME_SESSION, Map.of(), Map.of());
        assertNotNull(warning);
        assertEquals(Kind.SIMILAR, warning.kind());
    }

    @Test
    void processesTwentyQuestionsAgainstFiveThousandCandidates() {
        float[] incomingVector = new float[384];
        incomingVector[0] = 1;
        float[] unrelatedVector = new float[384];
        unrelatedVector[1] = 1;
        Map<Long, float[]> vectors = new java.util.HashMap<>();
        var bank = java.util.stream.LongStream.rangeClosed(1, 5000).mapToObj(id -> {
            vectors.put(id, unrelatedVector);
            return question(id, "Câu lưu trữ về mô hình lomat mã " + id, option("Năm", true));
        }).toList();
        var incoming = java.util.stream.LongStream.rangeClosed(6001, 6020).mapToObj(id -> {
            vectors.put(id, incomingVector);
            return question(id, "Giải hệ phương trình tuyến tính " + id, option("Vô nghiệm", true));
        }).toList();
        vectors.put(5000L, incomingVector);

        long start = System.nanoTime();
        for (Candidate candidate : incoming) {
            long count = bank.stream().filter(other ->
                    compare(candidate, other, Scope.QUESTION_BANK, vectors, Map.of()) != null).count();
            assertEquals(1, count);
        }
        System.out.println("Comparison of 20 x 5000 questions with cached mock vectors: "
                + (System.nanoTime() - start) / 1_000_000 + " ms");
    }

    private static Candidate question(long id, String text, QuestionOption... options) {
        return snapshot(Question.builder().id(id).content(text).status(QuestionStatus.PENDING)
                .options(List.of(options)).build(), true, url -> url);
    }

    private static QuestionOption option(String text, boolean correct) {
        var option = new QuestionOption();
        option.setText(text);
        option.setIsCorrect(correct);
        return option;
    }
}
