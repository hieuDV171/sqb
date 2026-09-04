package com.frozenheart.backend.modules.search.document;

import com.frozenheart.backend.core.entity.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDoc {

    public static final String INDEX_NAME = "sqb_users";

    private Long id;
    private String fullName;
    private String studentLecturerCode; // MSSV hoặc Mã GV
    private String email;
    private String bio;

    private String avatarUrl;
    private String avatarFrameUrl;

    private String faculty; // Khoa / Viện (VD: Viện CNTT&TT)
    private String major; // Ngành học (VD: Khoa học máy tính)

    private UserRole role;

    // Chỉ số tương tác / uy tín học thuật
    private Integer totalProposedQuestions;
    private Integer badgesCount;
    private Integer followersCount;

    private LocalDateTime createdAt;
}
