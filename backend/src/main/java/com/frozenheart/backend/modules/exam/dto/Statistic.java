package com.frozenheart.backend.modules.exam.dto;

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
public class Statistic {
    private Integer easyCount;

    private Integer mediumCount;

    private Integer hardCount;

    private Integer unclassifiedCount;
}