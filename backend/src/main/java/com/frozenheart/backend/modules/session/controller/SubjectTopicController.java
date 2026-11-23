package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.service.TopicService;
import com.frozenheart.backend.modules.session.service.TopicService.TopicResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects/{subjectId}/topics")
@RequiredArgsConstructor
public class SubjectTopicController {
    private final TopicService topics;

    public record TopicRequest(@NotBlank @Size(max = 100) String name, Boolean active) {}

    @GetMapping
    public GlobalResponse<List<TopicResponse>> list(@PathVariable Long subjectId,
            @RequestParam(name = "include_inactive", defaultValue = "false") boolean includeInactive) {
        return GlobalResponse.success(topics.list(subjectId, includeInactive));
    }

    @PostMapping
    public GlobalResponse<TopicResponse> create(@PathVariable Long subjectId, @Valid @RequestBody TopicRequest request) {
        return GlobalResponse.success(topics.save(subjectId, null, request.name(), request.active()));
    }

    @PutMapping("/{topicId}")
    public GlobalResponse<TopicResponse> update(@PathVariable Long subjectId, @PathVariable Long topicId,
            @Valid @RequestBody TopicRequest request) {
        return GlobalResponse.success(topics.save(subjectId, topicId, request.name(), request.active()));
    }
}
