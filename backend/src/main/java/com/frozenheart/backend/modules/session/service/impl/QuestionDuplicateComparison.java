package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.constant.EmbeddingConstants;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.session.DuplicateDetectionTier;
import com.frozenheart.backend.core.entity.session.DuplicateWarning;
import com.frozenheart.backend.core.entity.session.DuplicateWarning.Kind;
import com.frozenheart.backend.core.entity.session.DuplicateWarning.Scope;
import com.frozenheart.backend.core.entity.session.Question;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class QuestionDuplicateComparison {
    record Option(String text, String image, boolean correct) {}
    record Image(Long id, String key, String url, Long hash, MediaTarget target) {}
    record Candidate(Long id, Long version, Long topicId, String text, Set<String> trigrams,
            List<Option> options, List<Image> images, float[] embedding,
            boolean pending, boolean due, int attempts) {
        String embeddingText() {
            return text + "\n" + options.stream().map(Option::text).sorted().reduce("", (a, b) -> a + "\n" + b);
        }
    }

    private QuestionDuplicateComparison() {}

    public record ExactIdentity(String content, List<String> options, List<String> correctOptions, List<String> images) {}

    public static ExactIdentity exactIdentity(Question question) {
        Candidate input = snapshot(question, false, url -> url);
        return new ExactIdentity(input.text(), optionContents(input), correctOptions(input), questionImages(input));
    }

    static Candidate snapshot(Question question, boolean due, Function<String, String> objectKey) {
        List<Option> options = question.getOptions() == null ? List.of() : question.getOptions().stream()
                .filter(Objects::nonNull)
                .map(option -> new Option(normalize(option.getText()), normalize(option.getMediaUrl()),
                        Boolean.TRUE.equals(option.getIsCorrect())))
                .toList();
        Set<String> optionUrls = options.stream().map(Option::image).filter(url -> !url.isBlank())
                .collect(java.util.stream.Collectors.toSet());
        List<Image> images = question.getOwnedMedias() == null ? List.of() : question.getOwnedMedias().stream()
                .filter(media -> media.getDeletedAt() == null && (media.getMediaTarget() == MediaTarget.CONTENT
                        || optionUrls.contains(media.getUrl())))
                .map(media -> new Image(media.getId(), media.getObjectKey() != null ? media.getObjectKey()
                        : objectKey.apply(media.getUrl()), media.getUrl(),
                        media.getPHash(), media.getMediaTarget()))
                .toList();
        String text = normalize(question.getContent());
        return new Candidate(question.getId(), question.getVersion(),
                question.getTopic() == null ? null : question.getTopic().getId(), text, trigrams(text),
                options, images, question.getTextEmbedding(),
                question.getStatus() == com.frozenheart.backend.core.entity.session.QuestionStatus.PENDING,
                due, Objects.requireNonNullElse(question.getDuplicateCheckAttempts(), 0));
    }

    static DuplicateWarning compare(Candidate input, Candidate match, Scope scope, Map<Long, float[]> vectors,
            Map<String, Long> hashes) {
        var warning = DuplicateWarning.builder().questionId(input.id()).similarQuestionId(match.id())
                .scope(scope).matchedSnippet(match.text().substring(0, Math.min(240, match.text().length())));
        boolean sameStem = !input.text().isBlank() && input.text().equals(match.text());
        if (sameStem && optionContents(input).equals(optionContents(match))
                && questionImages(input).equals(questionImages(match))) {
            Kind kind = correctOptions(input).equals(correctOptions(match)) ? Kind.EXACT : Kind.ANSWER_CONFLICT;
            return warning.kind(kind).tier(DuplicateDetectionTier.RULE_BASED).build();
        }
        double trigram = jaccard(input.trigrams(), match.trigrams());
        if (trigram >= 0.85) {
            return warning.kind(Kind.SIMILAR).tier(DuplicateDetectionTier.TRIGRAM).similarityScore(trigram).build();
        }
        int bestDistance = Long.SIZE;
        String bestImage = null;
        for (Image incoming : input.images()) {
            Long left = effectiveHash(incoming, hashes);
            if (left == null) continue;
            for (Image other : match.images()) {
                Long right = effectiveHash(other, hashes);
                if (right == null || incoming.target() != other.target()) continue;
                int distance = Long.bitCount(left ^ right);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestImage = other.url();
                }
            }
        }
        if (bestDistance <= 5) {
            return warning.kind(Kind.SIMILAR).tier(DuplicateDetectionTier.P_HASH)
                    .hammingDistance(bestDistance).mediaUrl(bestImage).build();
        }
        double cosine = cosine(vectors.getOrDefault(input.id(), input.embedding()),
                vectors.getOrDefault(match.id(), match.embedding()));
        return cosine >= 0.90 ? warning.kind(Kind.SIMILAR).tier(DuplicateDetectionTier.TEXT_EMBEDDING)
                .similarityScore(cosine).build() : null;
    }

    private static List<String> optionContents(Candidate candidate) {
        return candidate.options().stream().map(option -> option.text() + "\u0000" + option.image()).sorted().toList();
    }

    private static List<String> correctOptions(Candidate candidate) {
        return candidate.options().stream().filter(Option::correct)
                .map(option -> option.text() + "\u0000" + option.image()).sorted().toList();
    }

    private static List<String> questionImages(Candidate candidate) {
        return candidate.images().stream().filter(image -> image.target() == MediaTarget.CONTENT)
                .map(image -> Objects.toString(image.url(), image.key())).filter(Objects::nonNull).sorted().toList();
    }

    private static Long effectiveHash(Image image, Map<String, Long> hashes) {
        return image.hash() != null ? image.hash() : hashes.get(image.key());
    }

    static boolean validVector(float[] values) {
        if (values == null || values.length != EmbeddingConstants.TEXT_EMBEDDING_DIMS) return false;
        boolean nonzero = false;
        for (float value : values) {
            if (!Float.isFinite(value)) return false;
            nonzero |= value != 0;
        }
        return nonzero;
    }

    private static double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length != right.length) return 0;
        double dot = 0, l2 = 0, r2 = 0;
        for (int i = 0; i < left.length; i++) {
            dot += (double) left[i] * right[i];
            l2 += (double) left[i] * left[i];
            r2 += (double) right[i] * right[i];
        }
        if (l2 == 0 || r2 == 0) return 0;
        return Math.min(1, dot / Math.sqrt(l2 * r2));
    }

    static Set<String> trigrams(String text) {
        Set<String> result = new HashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (token.isEmpty()) continue;
            int[] points = ("  " + token + " ").codePoints().toArray();
            for (int index = 0; index + 2 < points.length; index++) {
                result.add(new String(points, index, 3));
            }
        }
        return result;
    }

    private static double jaccard(Set<String> left, Set<String> right) {
        int intersection = 0;
        for (String token : left) if (right.contains(token)) intersection++;
        int union = left.size() + right.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
    }

    private static String normalize(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFC).strip();
    }
}
