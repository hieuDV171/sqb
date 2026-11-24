package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Topic;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.session.repository.TopicRepository;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Chủ đề trong danh mục của một môn học")
    public record TopicResponse(
            @Schema(description = "ID của chủ đề", example = "37") Long id,
            @Schema(description = "Tên hiển thị của chủ đề", example = "Định thời CPU") String name,
            @Schema(description = "Chủ đề đang được sử dụng và có thể gán cho câu hỏi", example = "true") boolean active) {}

    @Schema(description = "Chủ đề kèm phiên bản để quản trị viên và giảng viên quản lý")
    public record ManagedTopicResponse(
            @Schema(description = "ID của chủ đề", example = "37") Long id,
            @Schema(description = "Tên hiển thị của chủ đề", example = "Định thời CPU") String name,
            @Schema(description = "Chủ đề đang được sử dụng", example = "true") boolean active,
            @Schema(description = "Phiên bản hiện tại, cần gửi lại khi cập nhật chủ đề", example = "2") Long version) {}

    @Transactional(readOnly = true)
    public List<TopicResponse> listActive(Long subjectId) {
        return topics.findBySubjectIdOrderByNameAsc(subjectId).stream()
                .filter(Topic::isActive)
                .map(topic -> new TopicResponse(topic.getId(), topic.getName(), topic.isActive()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagedTopicResponse> listManaged(Long subjectId) {
        assertCanManage(subjectId);
        return topics.findBySubjectIdOrderByNameAsc(subjectId).stream()
                .map(TopicService::managedResponse)
                .toList();
    }

    @Transactional
    public ManagedTopicResponse save(Long subjectId, Long topicId, String name, Boolean active, Long expectedVersion) {
        assertCanManage(subjectId);
        var subject = subjects.lockTopicCatalog(subjectId)
                .orElseThrow(() -> new AppException(ResponseCode.SUBJECT_NOT_FOUND));
        Topic topic = topicId == null ? Topic.builder().subject(subject)
                .code(Long.toString(ThreadLocalRandom.current().nextLong(CODE_SPACE), 36)).build()
                : selected(subjectId, topicId);

        if (topicId != null && (expectedVersion == null || !expectedVersion.equals(topic.getVersion()))) {
            throw new AppException(ResponseCode.STALE_DATA_DETECTED, "Chủ đề đã thay đổi. Hãy tải lại danh mục trước khi lưu.");
        }

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
        return managedResponse(topics.saveAndFlush(topic));
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

    private static ManagedTopicResponse managedResponse(Topic topic) {
        return new ManagedTopicResponse(topic.getId(), topic.getName(), topic.isActive(), topic.getVersion());
    }

    private static AppException invalid(String message) {
        return new AppException(ResponseCode.INVALID_PARAMETER_VALUE, message);
    }
}
