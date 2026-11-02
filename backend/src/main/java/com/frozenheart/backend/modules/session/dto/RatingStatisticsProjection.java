package com.frozenheart.backend.modules.session.dto;

public interface RatingStatisticsProjection {
    Double getAvgRating();

    Long getTotalRatings();

    Long getRating0Count();

    Long getRating1Count();

    Long getRating2Count();

    Long getRating3Count();

    Long getRating4Count();
}
