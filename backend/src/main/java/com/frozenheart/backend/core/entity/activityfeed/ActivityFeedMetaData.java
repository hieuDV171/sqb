package com.frozenheart.backend.core.entity.activityfeed;

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
public class ActivityFeedMetaData {
    private String title;
    private String description;
    private String mediaUrl;
    private String badgeName;
    private String cosmeticName;
    private String subjectCode;
    private String subjectName;
}
