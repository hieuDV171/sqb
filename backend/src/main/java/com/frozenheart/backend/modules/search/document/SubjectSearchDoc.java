package com.frozenheart.backend.modules.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectSearchDoc {

    public static final String INDEX_NAME = "sqb_subjects";

    private Long id;
    private String subjectCode; // Mã học phần (VD: IT3011, MI1010)
    private String subjectName; // Tên môn học (VD: Cấu trúc dữ liệu và giải thuật)

    private Integer totalQuestions; // Tổng số câu hỏi hiện có trong ngân hàng đề
    private Integer totalSessions;
}
