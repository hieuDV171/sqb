package com.frozenheart.backend.modules.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorDto {
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private String frameUrl;

    public static AuthorDto getSystemAuthor() {
        return AuthorDto.builder()
                .userId(null)
                .fullName("Hệ thống")
                .avatarUrl("/assets/system-avatar.png")
                .frameUrl(null)
                .build();
    }
}
