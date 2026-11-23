package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Topic;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.session.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {
    private static final long CODE_SPACE = 3_656_158_440_062_976L; // 36^10

    private final TopicRepository topics;
    private final SubjectRepository subjects;

    public record TopicResponse(Long id, String name, boolean active) {}

    @Transactional(readOnly = true)
    public List<TopicResponse> list(Long subjectId, boolean includeInactive) {
        if (includeInactive) assertCanManage(subjectId);
        return topics.findBySubjectIdOrderByNameAsc(subjectId).stream()
                .filter(topic -> includeInactive || topic.isActive())
                .map(TopicService::response)
                .toList();
    }

    @Transactional
    public TopicResponse save(Long subjectId, Long topicId, String name, Boolean active) {
        assertCanManage(subjectId);
        var subject = subjects.lockTopicCatalog(subjectId)
                .orElseThrow(() -> new AppException(ResponseCode.SUBJECT_NOT_FOUND));
        Topic topic = topicId == null ? Topic.builder().subject(subject)
                .code(Long.toString(ThreadLocalRandom.current().nextLong(CODE_SPACE), 36)).build()
                : selected(subjectId, topicId);

        String cleanName = Normalizer.normalize(name.strip(), Normalizer.Form.NFC).replaceAll("\\s+", " ");
        if (cleanName.isBlank()) throw invalid("Tên chủ đề không được để trống");
        String nameKey = cleanName.toLowerCase(Locale.ROOT);
        boolean duplicate = topics.findBySubjectIdOrderByNameAsc(subjectId).stream()
                .anyMatch(existing -> !Objects.equals(existing.getId(), topicId)
                        && Normalizer.normalize(existing.getName().strip(), Normalizer.Form.NFC)
                                .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT).equals(nameKey));
        if (duplicate) throw invalid("Tên chủ đề đã tồn tại trong môn học");
        topic.setName(cleanName);
        if (active != null) topic.setArchived(!active);
        return response(topics.save(topic));
    }

    public Map<Long, Topic> lookup(Long subjectId, Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<Long, Topic> result = topics.findBySubjectIdAndIdIn(subjectId, ids).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));
        if (result.size() != ids.size()) throw invalid("Chủ đề không thuộc môn học của câu hỏi");
        return result;
    }

    public Topic select(Long subjectId, Long id, Topic current) {
        return id == null ? null : requireActive(selected(subjectId, id), current);
    }

    public static Topic requireActive(Topic topic, Topic current) {
        if (topic != null && !topic.isActive() && (current == null || !topic.getId().equals(current.getId()))) {
            throw invalid("Chủ đề này đã ngừng sử dụng");
        }
        return topic;
    }

    private Topic selected(Long subjectId, Long id) {
        return topics.findByIdAndSubjectId(id, subjectId)
                .orElseThrow(() -> invalid("Chủ đề không thuộc môn học của câu hỏi"));
    }

    private void assertCanManage(Long subjectId) {
        JwtPayload actor = JwtPayload.getCurrentUserPayload();
        if (UserRole.ADMIN.name().equalsIgnoreCase(actor.getRole())) return;
        if (!UserRole.LECTURER.name().equalsIgnoreCase(actor.getRole())
                || topics.countTeachingAssignments(subjectId, actor.getUserId()) == 0) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không quản lý chủ đề của môn học này");
        }
    }

    private static TopicResponse response(Topic topic) {
        return new TopicResponse(topic.getId(), topic.getName(), topic.isActive());
    }

    private static AppException invalid(String message) {
        return new AppException(ResponseCode.INVALID_PARAMETER_VALUE, message);
    }
}
