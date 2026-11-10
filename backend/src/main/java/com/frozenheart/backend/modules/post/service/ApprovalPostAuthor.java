package com.frozenheart.backend.modules.post.service;

import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.post.dto.AuthorDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalPostAuthor {
    private final AnonymizerUtil anonymizerUtil;

    public AuthorDto fromStudent(User student) {
        return AuthorDto.builder()
                .fullName(student != null ? anonymizerUtil.encodeUserId(student.getId()) : "Sinh viên ẩn danh")
                .build();
    }
}
