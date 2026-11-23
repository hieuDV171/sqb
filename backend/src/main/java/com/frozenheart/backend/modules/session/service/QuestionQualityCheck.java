package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QuestionQualityCheck {
    private QuestionQualityCheck() {}

    public static String warning(Question question) {
        List<QuestionOption> options = question.getOptions();
        if (!singleAnswer(options) || options.stream().anyMatch(option -> option.getMediaUrl() != null
                || option.getMediaId() != null || option.getText() == null || option.getText().isBlank())) return null;
        int correct = options.stream().filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .mapToInt(QuestionQualityCheck::wordCount).findFirst().orElseThrow();
        int longestDistractor = options.stream().filter(option -> !Boolean.TRUE.equals(option.getIsCorrect()))
                .mapToInt(QuestionQualityCheck::wordCount).max().orElse(0);
        return correct - longestDistractor >= 5 && correct >= 1.5 * longestDistractor
                ? "Đáp án đúng dài nổi bật so với các lựa chọn còn lại." : null;
    }

    public static String sessionWarning(List<Question> questions) {
        Map<Integer, Integer> byPosition = new HashMap<>();
        int total = 0;
        for (Question question : questions) {
            List<QuestionOption> options = question.getOptions();
            if (options == null || options.size() != 4 || !singleAnswer(options)) continue;
            for (int position = 0; position < options.size(); position++) {
                if (Boolean.TRUE.equals(options.get(position).getIsCorrect())) {
                    byPosition.merge(position, 1, Integer::sum);
                    total++;
                }
            }
        }
        if (total < 10) return null;
        for (var entry : byPosition.entrySet()) {
            if (entry.getValue() * 5 >= total * 3) {
                return "Vị trí " + (char) ('A' + entry.getKey()) + " là đáp án đúng ở "
                        + entry.getValue() + "/" + total + " câu có bốn lựa chọn.";
            }
        }
        return null;
    }

    private static boolean singleAnswer(List<QuestionOption> options) {
        return options != null && options.size() >= 2 && options.stream().noneMatch(java.util.Objects::isNull)
                && options.stream().filter(option -> Boolean.TRUE.equals(option.getIsCorrect())).count() == 1;
    }

    private static int wordCount(QuestionOption option) {
        return option.getText().strip().split("\\s+").length;
    }
}
