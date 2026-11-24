package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.service.TopicService;
import com.frozenheart.backend.modules.session.service.TopicService.TopicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "15b. Danh mục Môn học (Subjects Catalog)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/subjects/{subjectId}/topics")
@RequiredArgsConstructor
public class SubjectTopicController {
    private final TopicService topics;

    @Operation(summary = "Liệt kê chủ đề của môn học",
            description = "Trả về chủ đề đang dùng theo tên để người dùng đã đăng nhập chọn khi gán chủ đề cho câu hỏi.")
    @GetMapping
    public GlobalResponse<List<TopicResponse>> list(
            @Parameter(description = "ID môn học cần tra cứu chủ đề", example = "12") @PathVariable Long subjectId) {
        return GlobalResponse.success(topics.listActive(subjectId));
    }
}
