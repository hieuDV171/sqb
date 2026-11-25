package com.frozenheart.backend.modules.exam.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionDifficulty;
import com.frozenheart.backend.core.entity.session.Topic;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.dto.ExamMatrix;
import com.frozenheart.backend.modules.exam.dto.GenerateExamRequest.DifficultyDistribution;
import com.frozenheart.backend.modules.exam.dto.Statistic;

import java.util.*;

import static com.frozenheart.backend.core.entity.session.QuestionDifficulty.*;

final class ExamQuestionSelection {
    private static final List<QuestionDifficulty> DIFFICULTIES = List.of(EASY, MEDIUM, HARD, UNCLASSIFIED);
    private static final Map<QuestionDifficulty, String> LABELS = Map.of(
            EASY, "Dễ", MEDIUM, "Trung bình", HARD, "Khó", UNCLASSIFIED, "Chưa phân loại");

    record Result(List<Question> questions, ExamMatrix.Difficulty difficulty, ExamMatrix.Topics topics) {}

    private record Group(Topic topic, Double weight, Integer quota, Map<QuestionDifficulty, List<Question>> questions) {
        int count() {
            return questions.values().stream().mapToInt(List::size).sum();
        }
    }

    private ExamQuestionSelection() {}

    static Result select(List<Question> bank, int total, DifficultyDistribution difficulty,
                         Map<String, Double> weights, List<Topic> catalog) {
        if (total <= 0) throw invalid("Số câu hỏi phải lớn hơn 0.");
        var difficultyQuotas = difficultyQuotas(difficulty, total);
        Map<Topic, Double> topicWeights = resolveTopics(weights, catalog);
        var topicQuotas = quotas(topicWeights, total, "tỷ lệ chủ đề");
        if (bank.size() < total) throw shortage("ngân hàng có " + bank.size() + "/" + total + " câu đã duyệt.");

        Map<Long, Group> groupsById = new LinkedHashMap<>();
        topicWeights.forEach((topic, weight) -> groupsById.put(topic.getId(),
                new Group(topic, weight, topicQuotas.get(topic), new EnumMap<>(QuestionDifficulty.class))));
        if (groupsById.isEmpty()) groupsById.put(null,
                new Group(null, null, null, new EnumMap<>(QuestionDifficulty.class)));

        for (Question question : bank) {
            Group group = topicWeights.isEmpty() ? groupsById.get(null) : groupsById.get(topicId(question));
            if (group != null) group.questions().computeIfAbsent(difficultyOf(question), ignored -> new ArrayList<>()).add(question);
        }

        var groups = List.copyOf(groupsById.values());
        var deficits = new ArrayList<String>();
        for (Group group : groups) {
            if (group.quota() != null && group.count() < group.quota()) {
                deficits.add(group.topic().getName() + " có " + group.count() + "/" + group.quota() + " câu");
            }
        }
        difficultyQuotas.forEach((level, quota) -> {
            int available = groups.stream().mapToInt(group -> group.questions().getOrDefault(level, List.of()).size()).sum();
            if (available < quota) deficits.add(LABELS.get(level) + " có " + available + "/" + quota + " câu");
        });
        if (!deficits.isEmpty()) throw shortage(String.join(", ", deficits) + ".");

        var selected = selectByCapacity(total, difficultyQuotas, groups);
        Collections.shuffle(selected);
        var actual = new EnumMap<QuestionDifficulty, Integer>(QuestionDifficulty.class);
        Map<Long, Map<QuestionDifficulty, Integer>> topicCounts = new LinkedHashMap<>();
        Map<Long, Topic> topics = new LinkedHashMap<>();
        groupsById.forEach((id, group) -> {
            if (group.topic() != null) topics.put(id, group.topic());
        });
        for (Question question : selected) {
            var level = difficultyOf(question);
            var topicId = topicId(question);
            actual.merge(level, 1, Integer::sum);
            topics.putIfAbsent(topicId, question.getTopic());
            topicCounts.computeIfAbsent(topicId, ignored -> new EnumMap<>(QuestionDifficulty.class))
                    .merge(level, 1, Integer::sum);
        }

        var rows = topics.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.nullsLast(Long::compareTo)))
                .map(entry -> {
                    Topic topic = entry.getValue();
                    var counts = topicCounts.getOrDefault(entry.getKey(), Map.of());
                    return new ExamMatrix.TopicRow(entry.getKey(), topic == null ? "Chưa gán chủ đề" : topic.getName(),
                            topicWeights.get(topic), topicQuotas.get(topic),
                            counts.values().stream().mapToInt(Integer::intValue).sum(), statistic(counts));
                }).toList();
        return new Result(selected, new ExamMatrix.Difficulty(difficulty, statistic(actual)), new ExamMatrix.Topics(rows));
    }

    private static Map<Topic, Double> resolveTopics(Map<String, Double> weights, List<Topic> catalog) {
        if (weights == null || weights.isEmpty()) return Collections.emptyMap();
        Map<String, Topic> byId = new HashMap<>();
        Map<String, Topic> byName = new HashMap<>();
        for (Topic topic : catalog) {
            if (topic.isActive()) {
                byId.put(topic.getId().toString(), topic);
                byName.put(topic.getName(), topic);
            }
        }
        Map<Topic, Double> resolved = new TreeMap<>(Comparator.comparing(Topic::getId));
        weights.forEach((key, weight) -> {
            Topic topic = byId.getOrDefault(key, byName.get(key));
            if (topic == null || resolved.containsKey(topic)) {
                throw invalid("Chủ đề " + key + " không hợp lệ, không còn hoạt động hoặc bị chọn lặp.");
            }
            resolved.put(topic, weight);
        });
        return resolved;
    }

    private static Map<QuestionDifficulty, Integer> difficultyQuotas(DifficultyDistribution distribution, int total) {
        Map<QuestionDifficulty, Double> weights = new LinkedHashMap<>();
        if (distribution != null) {
            weights.put(EASY, Objects.requireNonNullElse(distribution.getEasy(), 0.0));
            weights.put(MEDIUM, Objects.requireNonNullElse(distribution.getMedium(), 0.0));
            weights.put(HARD, Objects.requireNonNullElse(distribution.getHard(), 0.0));
            weights.put(UNCLASSIFIED, Objects.requireNonNullElse(distribution.getUnclassified(), 0.0));
        }
        return quotas(weights, total, "tỷ lệ độ khó");
    }

    // Largest remainder gives whole-question quotas whose sum is exactly the exam size.
    private static <K> Map<K, Integer> quotas(Map<K, Double> weights, int total, String label) {
        Map<K, Integer> result = new LinkedHashMap<>();
        if (weights.isEmpty()) return result;
        if (weights.values().stream().anyMatch(weight -> weight == null || !Double.isFinite(weight) || weight < 0 || weight > 1)) {
            throw invalid("Mỗi " + label + " phải nằm trong khoảng 0% đến 100%.");
        }
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 1.0) > 1e-8) throw invalid("Tổng " + label + " phải bằng 100%.");

        Map<K, Double> remainder = new LinkedHashMap<>();
        weights.forEach((key, weight) -> {
            double exact = total * (weight / sum);
            int floor = (int) Math.floor(exact);
            result.put(key, floor);
            remainder.put(key, exact - floor);
        });
        long left = total - result.values().stream().mapToLong(Integer::longValue).sum();
        remainder.entrySet().stream().sorted(Map.Entry.<K, Double>comparingByValue().reversed()).limit(left)
                .forEach(entry -> result.merge(entry.getKey(), 1, Integer::sum));
        return result;
    }

    private static List<Question> selectByCapacity(int total, Map<QuestionDifficulty, Integer> difficultyQuotas,
                                                    List<Group> groups) {
        // Flow from topic quotas through available topic × difficulty buckets to difficulty quotas.
        List<QuestionDifficulty> levels = difficultyQuotas.isEmpty() ? List.of(UNCLASSIFIED, EASY, MEDIUM, HARD) : DIFFICULTIES;
        int firstDifficulty = 1 + groups.size();
        int sink = firstDifficulty + levels.size();
        var graph = new ArrayList<List<Edge>>();
        for (int node = 0; node <= sink; node++) graph.add(new ArrayList<>());
        var buckets = new LinkedHashMap<Edge, List<Question>>();
        for (int topicIndex = 0; topicIndex < groups.size(); topicIndex++) {
            var group = groups.get(topicIndex);
            int node = 1 + topicIndex;
            connect(graph, 0, node, Objects.requireNonNullElse(group.quota(), total));
            for (int d = 0; d < levels.size(); d++) {
                var questions = group.questions().getOrDefault(levels.get(d), List.of());
                if (!questions.isEmpty()) buckets.put(connect(graph, node, firstDifficulty + d, questions.size()), questions);
            }
        }
        for (int d = 0; d < levels.size(); d++) {
            connect(graph, firstDifficulty + d, sink, difficultyQuotas.getOrDefault(levels.get(d), total));
        }
        int matched = 0;
        while (matched < total) {
            int additional = augment(graph, sink, total - matched);
            if (additional == 0) break;
            matched += additional;
        }
        if (matched < total) throw shortage("chỉ có thể chọn " + matched + "/" + total
                + " câu đáp ứng đồng thời ma trận độ khó và chủ đề. Hãy điều chỉnh tỷ lệ hoặc số câu.");

        var selected = new ArrayList<Question>(total);
        buckets.forEach((edge, questions) -> {
            if (edge.reverse.capacity > 0) {
                Collections.shuffle(questions);
                selected.addAll(questions.subList(0, edge.reverse.capacity));
            }
        });
        return selected;
    }

    private static Edge connect(List<List<Edge>> graph, int from, int to, int capacity) {
        var forward = new Edge(from, to, capacity);
        var reverse = new Edge(to, from, 0);
        forward.reverse = reverse;
        reverse.reverse = forward;
        graph.get(from).add(forward);
        graph.get(to).add(reverse);
        return forward;
    }

    private static int augment(List<List<Edge>> graph, int sink, int limit) {
        var queue = new ArrayDeque<Integer>();
        var previous = new Edge[graph.size()];
        queue.add(0);
        while (!queue.isEmpty() && previous[sink] == null) {
            for (Edge edge : graph.get(queue.remove())) {
                if (edge.capacity > 0 && edge.to != 0 && previous[edge.to] == null) {
                    previous[edge.to] = edge;
                    queue.add(edge.to);
                }
            }
        }
        if (previous[sink] == null) return 0;
        int amount = limit;
        for (int node = sink; node != 0; node = previous[node].from) amount = Math.min(amount, previous[node].capacity);
        for (int node = sink; node != 0; node = previous[node].from) {
            previous[node].capacity -= amount;
            previous[node].reverse.capacity += amount;
        }
        return amount;
    }

    static QuestionDifficulty difficultyOf(Question question) {
        return Objects.requireNonNullElse(question.getDifficulty(), UNCLASSIFIED);
    }

    private static Long topicId(Question question) {
        return question.getTopic() == null ? null : question.getTopic().getId();
    }

    static Statistic statistic(Map<QuestionDifficulty, Integer> counts) {
        return Statistic.builder().easyCount(counts.getOrDefault(EASY, 0)).mediumCount(counts.getOrDefault(MEDIUM, 0))
                .hardCount(counts.getOrDefault(HARD, 0)).unclassifiedCount(counts.getOrDefault(UNCLASSIFIED, 0)).build();
    }

    private static AppException shortage(String detail) {
        return invalid("Không đủ câu hỏi để tạo đề: " + detail);
    }

    private static AppException invalid(String detail) {
        return new AppException(ResponseCode.INVALID_PARAMETER_VALUE, detail);
    }

    private static final class Edge {
        private final int from;
        private final int to;
        private int capacity;
        private Edge reverse;

        private Edge(int from, int to, int capacity) {
            this.from = from;
            this.to = to;
            this.capacity = capacity;
        }
    }
}
