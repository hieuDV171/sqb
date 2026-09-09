package com.frozenheart.backend.modules.exam.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamListResponse {

    private List<ExamItem> items;
    private CursorPaginationDto pagination;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamItem {
        private Long examId;

        private String title;

        private String subjectName;

        private Integer questionCount;

        private Instant createdAt;

        private Statistic statistic;

        private String downloadUrl;
    }

}
